package com.currently.currently_backend.service;

import com.currently.currently_backend.dto.InsightDTO;
import com.currently.currently_backend.dto.InsightGenerateRequest;
import com.currently.currently_backend.dto.InsightGenerateResponse;
import com.currently.currently_backend.model.Appliance;
import com.currently.currently_backend.model.User;
import com.currently.currently_backend.model.UserAppliance;
import com.currently.currently_backend.repository.UserApplianceRepository;
import com.currently.currently_backend.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class InsightService {

    private static final double DEFAULT_PRICE_PER_KWH = 0.30;
    private static final int BATCH_SIZE = 4;
    private static final int MAX_PER_APPLIANCE = 2;
    private static final int MAX_PER_CATEGORY = 3;
    private static final double MATERIAL_MIN_MONTHLY_IMPACT = 2.50;
    private static final double MATERIAL_MIN_SCORE = 0.30;
    private static final double FALLBACK_MIN_MONTHLY_IMPACT = 1.00;
    private static final double FALLBACK_MIN_SCORE = 0.20;
    private static final Duration RUN_TTL = Duration.ofHours(2);
    private static final String NO_MORE_REASON = "No more high-impact practical recommendations remain.";

    private final UserRepository userRepository;
    private final UserApplianceRepository userApplianceRepository;
    private final ApplianceService applianceService;
    private final UserLookupHashService userLookupHashService;

    // this in-memory run store keeps generate/more simple now, then we can swap this for postgres later
    private final Map<String, InsightRunState> runStore = new ConcurrentHashMap<>();

    public InsightService(
            UserRepository userRepository,
            UserApplianceRepository userApplianceRepository,
            ApplianceService applianceService,
            UserLookupHashService userLookupHashService
    ) {
        this.userRepository = userRepository;
        this.userApplianceRepository = userApplianceRepository;
        this.applianceService = applianceService;
        this.userLookupHashService = userLookupHashService;
    }

    @Transactional(readOnly = true)
    public InsightGenerateResponse generateInsights(InsightGenerateRequest request) {
        cleanupExpiredRuns();

        User user = getCurrentUser();
        double pricePerKwh = resolvePricePerKwh(user, request != null ? request.getPricePerKwh() : null);

        List<UserAppliance> entities = userApplianceRepository.findByUserOrderByCreatedAtAsc(user);
        if (entities.isEmpty()) {
            return new InsightGenerateResponse(List.of(), null, false, "No appliance data available yet.");
        }

        List<ApplianceSnapshot> snapshots = entities.stream()
                .map(entity -> toSnapshot(entity, pricePerKwh))
                .sorted(Comparator.comparingDouble(ApplianceSnapshot::dailyCost).reversed())
                .collect(Collectors.toList());

        List<RawCandidate> rawCandidates = new ArrayList<>();
        addTopCostDriverCandidates(rawCandidates, snapshots);
        addBehaviourCandidates(rawCandidates, snapshots);
        addAlwaysOnCandidates(rawCandidates, snapshots);
        addRoomCandidates(rawCandidates, snapshots);
        addTariffCandidates(rawCandidates, snapshots, pricePerKwh);

        List<RankedInsight> ranked = rankAndFilter(rawCandidates);
        if (ranked.isEmpty()) {
            return new InsightGenerateResponse(List.of(), null, false, NO_MORE_REASON);
        }

        String runId = UUID.randomUUID().toString();
        InsightRunState state = new InsightRunState(user.getId(), ranked, LocalDateTime.now());
        runStore.put(runId, state);

        return nextBatch(runId, state);
    }

    public InsightGenerateResponse generateMore(String runId) {
        cleanupExpiredRuns();

        User user = getCurrentUser();
        InsightRunState state = runStore.get(runId);
        if (state == null) {
            return new InsightGenerateResponse(List.of(), runId, false, "This insights run expired. Generate again.");
        }

        if (!state.userId.equals(user.getId())) {
            throw new IllegalStateException("You are not allowed to access this insights run.");
        }

        return nextBatch(runId, state);
    }

    private InsightGenerateResponse nextBatch(String runId, InsightRunState state) {
        if (state.cursor >= state.rankedInsights.size()) {
            return new InsightGenerateResponse(List.of(), runId, false, NO_MORE_REASON);
        }

        int end = Math.min(state.cursor + BATCH_SIZE, state.rankedInsights.size());
        List<InsightDTO> batch = state.rankedInsights.subList(state.cursor, end).stream()
                .map(RankedInsight::insight)
                .collect(Collectors.toList());

        state.cursor = end;
        boolean hasMore = state.cursor < state.rankedInsights.size();
        String stopReason = hasMore ? null : NO_MORE_REASON;
        return new InsightGenerateResponse(batch, runId, hasMore, stopReason);
    }

    private List<RankedInsight> rankAndFilter(List<RawCandidate> rawCandidates) {
        if (rawCandidates.isEmpty()) {
            return List.of();
        }

        // dedupe by key first so we do not rank near-identical ideas twice
        Map<String, RawCandidate> bestByKey = new HashMap<>();
        for (RawCandidate raw : rawCandidates) {
            RawCandidate existing = bestByKey.get(raw.key());
            if (existing == null || raw.impactMonthly() > existing.impactMonthly()) {
                bestByKey.put(raw.key(), raw);
            }
        }

        List<RawCandidate> deduped = new ArrayList<>(bestByKey.values());
        double maxImpact = deduped.stream().mapToDouble(RawCandidate::impactMonthly).max().orElse(1.0);

        List<RankedInsight> ranked = deduped.stream()
                .map(raw -> toRanked(raw, maxImpact))
                .sorted(Comparator
                        .comparingDouble(RankedInsight::score).reversed()
                        .thenComparing(RankedInsight::impactMonthly, Comparator.reverseOrder()))
                .collect(Collectors.toList());

        List<RankedInsight> material = ranked.stream()
                .filter(r -> r.impactMonthly() >= MATERIAL_MIN_MONTHLY_IMPACT)
                .filter(r -> r.score() >= MATERIAL_MIN_SCORE)
                .filter(r -> r.feasibilityScore() >= 0.45)
                .collect(Collectors.toList());

        if (material.size() < 3) {
            material = ranked.stream()
                    .filter(r -> r.impactMonthly() >= FALLBACK_MIN_MONTHLY_IMPACT)
                    .filter(r -> r.score() >= FALLBACK_MIN_SCORE)
                    .filter(r -> r.feasibilityScore() >= 0.35)
                    .collect(Collectors.toList());
        }

        return applyVarietyCaps(material);
    }

    private List<RankedInsight> applyVarietyCaps(List<RankedInsight> ranked) {
        Map<String, Integer> applianceCounts = new HashMap<>();
        Map<String, Integer> categoryCounts = new HashMap<>();
        List<RankedInsight> out = new ArrayList<>();

        for (RankedInsight r : ranked) {
            int forAppliance = applianceCounts.getOrDefault(r.applianceKey(), 0);
            int forCategory = categoryCounts.getOrDefault(r.category(), 0);
            if (forAppliance >= MAX_PER_APPLIANCE || forCategory >= MAX_PER_CATEGORY) {
                continue;
            }

            out.add(r);
            applianceCounts.put(r.applianceKey(), forAppliance + 1);
            categoryCounts.put(r.category(), forCategory + 1);
        }

        return out;
    }

    private RankedInsight toRanked(RawCandidate raw, double maxImpact) {
        double impactNormalized = clamp(raw.impactMonthly() / Math.max(maxImpact, 0.01), 0.0, 1.0);
        double score = (0.60 * impactNormalized) + (0.25 * raw.feasibilityScore()) + (0.15 * raw.confidenceScore());

        InsightDTO dto = new InsightDTO();
        dto.setTitle(raw.title());
        dto.setReasoning(raw.reasoning());
        dto.setAction(raw.action());
        dto.setImpactWeekly(round2(raw.impactWeekly()));
        dto.setImpactMonthly(round2(raw.impactMonthly()));
        dto.setConfidence(confidenceLabel(raw.confidenceScore()));
        dto.setCategory(raw.category());
        dto.setReferences(raw.references());

        return new RankedInsight(raw.key(), raw.applianceKey(), raw.category(), dto, score, raw.impactMonthly(), raw.feasibilityScore());
    }

    private void addTopCostDriverCandidates(List<RawCandidate> out, List<ApplianceSnapshot> snapshots) {
        int topCount = Math.min(5, snapshots.size());
        for (int i = 0; i < topCount; i++) {
            ApplianceSnapshot s = snapshots.get(i);

            double reductionPct = s.alwaysOn() ? 0.08 : 0.13;
            String action = s.alwaysOn()
                    ? String.format(Locale.US, "Tune %s settings and maintenance instead of switching it off.", s.displayName())
                    : String.format(Locale.US, "Cut %s usage by around %.0f%% this month.", s.displayName(), reductionPct * 100.0);

            double impactDaily = s.dailyCost() * reductionPct;
            out.add(new RawCandidate(
                    "top-driver:" + normalizeKey(s.displayName()),
                    normalizeKey(s.displayName()),
                    "appliance",
                    String.format("Lower %s running cost", s.displayName()),
                    String.format(Locale.US,
                            "%s uses %.2f kWh/day (EUR %.2f/day), which is one of your highest cost drivers.",
                            s.displayName(), s.dailyKwh(), s.dailyCost()),
                    action,
                    impactDaily * 7.0,
                    impactDaily * 30.0,
                    s.alwaysOn() ? 0.62 : 0.82,
                    s.alwaysOn() ? 0.72 : 0.88,
                    List.of(
                            String.format(Locale.US, "dailyKWh=%.2f", s.dailyKwh()),
                            String.format(Locale.US, "dailyCost=EUR %.2f", s.dailyCost())
                    )
            ));
        }
    }

    private void addBehaviourCandidates(List<RawCandidate> out, List<ApplianceSnapshot> snapshots) {
        List<ApplianceSnapshot> behaviourDevices = snapshots.stream()
                .filter(s -> isBehaviourAppliance(s.applianceName()))
                .sorted(Comparator.comparingDouble(ApplianceSnapshot::dailyCost).reversed())
                .limit(2)
                .collect(Collectors.toList());

        for (ApplianceSnapshot s : behaviourDevices) {
            double impactDaily = s.dailyCost() * 0.14;
            out.add(new RawCandidate(
                    "behaviour:" + normalizeKey(s.displayName()),
                    normalizeKey(s.displayName()),
                    "behaviour",
                    "Shift heavy cycles to efficient settings",
                    String.format(Locale.US,
                            "%s is costing about EUR %.2f/day from %.2f kWh/day, so cycle behaviour matters.",
                            s.displayName(), s.dailyCost(), s.dailyKwh()),
                    "Use eco mode, run full loads, and avoid repeated hot cycles in the same day.",
                    impactDaily * 7.0,
                    impactDaily * 30.0,
                    0.84,
                    0.86,
                    List.of(
                            String.format(Locale.US, "appliance=%s", s.displayName()),
                            String.format(Locale.US, "dailyCost=EUR %.2f", s.dailyCost())
                    )
            ));
        }
    }

    private void addAlwaysOnCandidates(List<RawCandidate> out, List<ApplianceSnapshot> snapshots) {
        Optional<ApplianceSnapshot> alwaysOn = snapshots.stream()
                .filter(ApplianceSnapshot::alwaysOn)
                .max(Comparator.comparingDouble(ApplianceSnapshot::dailyCost));

        if (alwaysOn.isEmpty()) {
            return;
        }

        ApplianceSnapshot s = alwaysOn.get();
        double impactDaily = s.dailyCost() * 0.06;

        out.add(new RawCandidate(
                "always-on:" + normalizeKey(s.displayName()),
                normalizeKey(s.displayName()),
                "appliance",
                "Optimize always-on usage",
                String.format(Locale.US,
                        "%s runs for %.1f h/day and costs around EUR %.2f/day.",
                        s.displayName(), s.hoursPerDay(), s.dailyCost()),
                "Lower one setting slightly or plan a more efficient replacement when practical.",
                impactDaily * 7.0,
                impactDaily * 30.0,
                0.50,
                0.58,
                List.of(
                        String.format(Locale.US, "hoursPerDay=%.1f", s.hoursPerDay()),
                        String.format(Locale.US, "usageType=%s", s.usageType())
                )
        ));
    }

    private void addRoomCandidates(List<RawCandidate> out, List<ApplianceSnapshot> snapshots) {
        double totalKwh = snapshots.stream().mapToDouble(ApplianceSnapshot::dailyKwh).sum();
        if (totalKwh <= 0) {
            return;
        }

        Map<String, Double> kwhByRoom = new HashMap<>();
        Map<String, Double> costByRoom = new HashMap<>();

        for (ApplianceSnapshot s : snapshots) {
            String room = s.roomName() != null ? s.roomName() : "Unassigned";
            kwhByRoom.put(room, kwhByRoom.getOrDefault(room, 0.0) + s.dailyKwh());
            costByRoom.put(room, costByRoom.getOrDefault(room, 0.0) + s.dailyCost());
        }

        Map.Entry<String, Double> topRoom = kwhByRoom.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElse(null);

        if (topRoom == null) {
            return;
        }

        double share = topRoom.getValue() / totalKwh;
        if (share < 0.35) {
            return;
        }

        double roomDailyCost = costByRoom.getOrDefault(topRoom.getKey(), 0.0);
        double impactDaily = roomDailyCost * 0.10;

        out.add(new RawCandidate(
                "room-focus:" + normalizeKey(topRoom.getKey()),
                normalizeKey(topRoom.getKey()),
                "room",
                String.format("Focus on %s first", topRoom.getKey()),
                String.format(Locale.US,
                        "%s drives %.0f%% of your usage (%.2f kWh/day).",
                        topRoom.getKey(), share * 100.0, topRoom.getValue()),
                "Prioritise one high-cost device in this room and trim its usage this week.",
                impactDaily * 7.0,
                impactDaily * 30.0,
                0.68,
                0.70,
                List.of(
                        String.format(Locale.US, "room=%s", topRoom.getKey()),
                        String.format(Locale.US, "roomDailyKWh=%.2f", topRoom.getValue())
                )
        ));
    }

    private void addTariffCandidates(List<RawCandidate> out, List<ApplianceSnapshot> snapshots, double pricePerKwh) {
        double totalDailyCost = snapshots.stream().mapToDouble(ApplianceSnapshot::dailyCost).sum();
        if (totalDailyCost <= 0) {
            return;
        }

        double impactDaily = totalDailyCost * 0.04;
        out.add(new RawCandidate(
                "tariff-check",
                "home",
                "tariff",
                "Check tariff and timing strategy",
                String.format(Locale.US,
                        "At EUR %.2f/kWh, your current appliance mix costs about EUR %.2f/day.",
                        pricePerKwh, totalDailyCost),
                "Review tariff options and shift flexible usage to cheaper periods if available.",
                impactDaily * 7.0,
                impactDaily * 30.0,
                0.46,
                0.60,
                List.of(
                        String.format(Locale.US, "pricePerKwh=EUR %.2f", pricePerKwh),
                        String.format(Locale.US, "totalDailyCost=EUR %.2f", totalDailyCost)
                )
        ));
    }

    private ApplianceSnapshot toSnapshot(UserAppliance entity, double pricePerKwh) {
        Appliance base = findBaseApplianceOrThrow(entity.getApplianceName());
        double dailyKwh = calculateDailyKwh(entity, base);
        double dailyCost = dailyKwh * pricePerKwh;

        String displayName = entity.getCustomName() != null && !entity.getCustomName().isBlank()
                ? entity.getCustomName()
                : entity.getApplianceName();

        return new ApplianceSnapshot(
                entity.getApplianceName(),
                displayName,
                entity.getUsageType(),
                entity.getHoursPerDay() != null ? entity.getHoursPerDay() : 0.0,
                dailyKwh,
                dailyCost,
                entity.getRoom() != null ? entity.getRoom().getName() : null,
                isAlwaysOn(entity)
        );
    }

    private Appliance findBaseApplianceOrThrow(String applianceName) {
        return applianceService.getAllAppliances().stream()
                .filter(a -> a.getName().equalsIgnoreCase(applianceName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Appliance not found in catalogue: " + applianceName));
    }

    private double calculateDailyKwh(UserAppliance entity, Appliance baseAppliance) {
        if ("continuous".equalsIgnoreCase(entity.getUsageType())) {
            double watts = baseAppliance.getAverageWatts();
            double hours = entity.getHoursPerDay() != null ? entity.getHoursPerDay() : 0.0;
            return (watts * hours) / 1000.0;
        }

        if ("perUse".equalsIgnoreCase(entity.getUsageType())) {
            double wattsPerUse = baseAppliance.getAverageWattsPerUse();
            double uses = entity.getUsesPerDay() != null ? entity.getUsesPerDay() : 0.0;
            return (wattsPerUse * uses) / 1000.0;
        }

        return 0.0;
    }

    private boolean isBehaviourAppliance(String applianceName) {
        String n = applianceName.toLowerCase(Locale.ROOT);
        return n.contains("dryer")
                || n.contains("washing")
                || n.contains("dishwasher")
                || n.contains("kettle")
                || n.contains("oven");
    }

    private boolean isAlwaysOn(UserAppliance entity) {
        String usageType = entity.getUsageType() != null ? entity.getUsageType().toLowerCase(Locale.ROOT) : "";
        String name = entity.getApplianceName() != null ? entity.getApplianceName().toLowerCase(Locale.ROOT) : "";
        double hours = entity.getHoursPerDay() != null ? entity.getHoursPerDay() : 0.0;

        if ("continuous".equals(usageType) && hours >= 20.0) {
            return true;
        }

        return name.contains("fridge")
                || name.contains("freezer")
                || name.contains("router")
                || name.contains("aquarium")
                || name.contains("server");
    }

    private void cleanupExpiredRuns() {
        LocalDateTime now = LocalDateTime.now();
        runStore.entrySet().removeIf(entry -> Duration.between(entry.getValue().createdAt, now).compareTo(RUN_TTL) > 0);
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String emailOrUsername = auth.getName();
        return userRepository.findByEmailHash(userLookupHashService.emailHash(emailOrUsername))
                
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
    }

    private double resolvePricePerKwh(User user, Double requestPricePerKwh) {
        if (requestPricePerKwh != null && requestPricePerKwh > 0) {
            return requestPricePerKwh;
        }

        Double userPrice = user.getPricePerKwh();
        if (userPrice != null && userPrice > 0) {
            return userPrice;
        }

        return DEFAULT_PRICE_PER_KWH;
    }

    private String confidenceLabel(double confidenceScore) {
        if (confidenceScore >= 0.80) return "high";
        if (confidenceScore >= 0.60) return "medium";
        return "low";
    }

    private String normalizeKey(String value) {
        return value == null
                ? "unknown"
                : value.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static class InsightRunState {
        private final Long userId;
        private final List<RankedInsight> rankedInsights;
        private final LocalDateTime createdAt;
        private int cursor;

        private InsightRunState(Long userId, List<RankedInsight> rankedInsights, LocalDateTime createdAt) {
            this.userId = userId;
            this.rankedInsights = rankedInsights;
            this.createdAt = createdAt;
            this.cursor = 0;
        }
    }

    private record ApplianceSnapshot(
            String applianceName,
            String displayName,
            String usageType,
            double hoursPerDay,
            double dailyKwh,
            double dailyCost,
            String roomName,
            boolean alwaysOn
    ) {}

    private record RawCandidate(
            String key,
            String applianceKey,
            String category,
            String title,
            String reasoning,
            String action,
            double impactWeekly,
            double impactMonthly,
            double confidenceScore,
            double feasibilityScore,
            List<String> references
    ) {}

    private record RankedInsight(
            String key,
            String applianceKey,
            String category,
            InsightDTO insight,
            double score,
            double impactMonthly,
            double feasibilityScore
    ) {}
}

