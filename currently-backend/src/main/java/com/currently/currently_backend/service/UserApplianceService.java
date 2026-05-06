/*
 * File: UserApplianceService.java
 * Description: Business logic for managing user-selected appliances and calculating appliance-level energy usage.
 * Project: Currently
 * Author: Liam Connell
 *
 */

package com.currently.currently_backend.service;

import com.currently.currently_backend.dto.UserApplianceRequest;
import com.currently.currently_backend.dto.UserApplianceResponse;
import com.currently.currently_backend.model.Appliance;
import com.currently.currently_backend.model.Room;
import com.currently.currently_backend.model.User;
import com.currently.currently_backend.model.UserAppliance;
import com.currently.currently_backend.repository.RoomRepository;
import com.currently.currently_backend.repository.UserApplianceRepository;
import com.currently.currently_backend.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class UserApplianceService {

    // Cost default: fallback electricity tariff used when the user has not saved a price-per-kWh setting.
    private static final double DEFAULT_PRICE_PER_KWH = 0.30;

    private final UserRepository userRepository;
    private final UserApplianceRepository userApplianceRepository;
    private final ApplianceService applianceService;
    private final RoomRepository roomRepository;
    private final UserLookupHashService userLookupHashService;

    public UserApplianceService(
            UserRepository userRepository,
            UserApplianceRepository userApplianceRepository,
            ApplianceService applianceService,
            RoomRepository roomRepository,
            UserLookupHashService userLookupHashService
    ) {
        this.userRepository = userRepository;
        this.userApplianceRepository = userApplianceRepository;
        this.applianceService = applianceService;
        this.roomRepository = roomRepository;
        this.userLookupHashService = userLookupHashService;
    }

    /*
     * Service helper: Current User
     * Purpose: Resolves the authenticated user from Spring Security's JWT Authentication context.
     * Output: User entity used to scope Appliance, Room, Usage, and Cost database operations.
     */
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String emailOrUsername = auth.getName();
        return userRepository.findByEmailHash(userLookupHashService.emailHash(emailOrUsername))
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
    }

    /*
     * Service: List user Appliances
     * Purpose: Loads all Appliances saved by the authenticated user, joins Room data, and calculates derived
     * daily kWh plus estimated daily Cost before returning DTOs to React.
     */
    @Transactional(readOnly = true)
    public List<UserApplianceResponse> getUserAppliances() {
        User user = getCurrentUser();
        double pricePerKwh = resolvePricePerKwh(user);
        List<UserAppliance> entities = userApplianceRepository.findByUserOrderByCreatedAtAsc(user);
        Map<String, Appliance> catalogueByName = catalogueByName();

        return entities.stream()
                .map(entity -> mapToResponseWithDerivedValues(entity, pricePerKwh, catalogueByName))
                .collect(Collectors.toList());
    }

    /*
     * Service: Create user Appliance
     * Purpose: Validates a new Appliance request against the catalogue, optional Room ownership, and usage rules,
     * saves it to the Database, then returns calculated Usage and Cost values.
     */
    @Transactional
    public UserApplianceResponse createUserAppliance(UserApplianceRequest request) {
        User user = getCurrentUser();
        if (request == null) {
            throw new IllegalArgumentException("User appliance request is required.");
        }
        if (request.getApplianceName() == null || request.getApplianceName().trim().isEmpty()) {
            throw new IllegalArgumentException("applianceName is required.");
        }
        if (request.getUsageType() == null || request.getUsageType().trim().isEmpty()) {
            throw new IllegalArgumentException("usageType is required.");
        }

        String applianceName = request.getApplianceName().trim();
        String usageType = request.getUsageType().trim();
        Appliance baseAppliance = findBaseApplianceOrThrow(applianceName);

        if (!baseAppliance.getUsageType().equalsIgnoreCase(usageType)) {
            throw new IllegalArgumentException("Usage type does not match base appliance configuration.");
        }

        validateUsageFields(request);

        UserAppliance entity = new UserAppliance();
        entity.setUser(user);
        entity.setApplianceName(applianceName);
        entity.setCustomName(normalizeNullableText(request.getCustomName()));
        entity.setUsageType(usageType);
        entity.setHoursPerDay(request.getHoursPerDay());
        entity.setUsesPerDay(request.getUsesPerDay());

        // Room assignment: if roomId is supplied, verify the Room exists and belongs to the same user.
        Room room = null;
        if (request.getRoomId() != null) {
            room = roomRepository.findById(request.getRoomId())
                    .orElseThrow(() -> new IllegalArgumentException("Room not found"));

            if (!room.getUser().getId().equals(user.getId())) {
                throw new IllegalStateException("Cannot assign appliance to another user's room.");
            }
        }
        entity.setRoom(room);

        entity.setCreatedAt(LocalDateTime.now());

        UserAppliance saved = userApplianceRepository.save(entity);
        double pricePerKwh = resolvePricePerKwh(user);
        return mapToResponseWithDerivedValues(saved, pricePerKwh, catalogueByName());
    }

    /*
     * Service: Update user Appliance
     * Purpose: Loads an Appliance by id, confirms ownership, applies custom name, Usage, and Room changes,
     * validates the final entity, and returns recalculated Cost values.
     */
    @Transactional
    public UserApplianceResponse updateUserAppliance(Long id, UserApplianceRequest request) {
        User user = getCurrentUser();
        if (request == null) {
            throw new IllegalArgumentException("User appliance request is required.");
        }

        UserAppliance entity = userApplianceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User appliance not found."));

        // Security: ensure the Appliance belongs to the current authenticated user.
        if (!entity.getUser().getId().equals(user.getId())) {
            throw new IllegalStateException("You are not allowed to modify this appliance.");
        }

        // Important logic: applianceName and usageType stay immutable to keep catalogue calculations consistent.
        if (request.getCustomName() != null) {
            entity.setCustomName(normalizeNullableText(request.getCustomName()));
        }

        if (request.getHoursPerDay() != null) {
            entity.setHoursPerDay(request.getHoursPerDay());
        }

        if (request.getUsesPerDay() != null) {
            entity.setUsesPerDay(request.getUsesPerDay());
        }

        // Room assignment: request roomId null means unassign the Appliance from a Room.
        if (request.getRoomId() != null) {
            Room room = roomRepository.findById(request.getRoomId())
                    .orElseThrow(() -> new IllegalArgumentException("Room not found"));

            if (!room.getUser().getId().equals(user.getId())) {
                throw new IllegalStateException("Cannot assign appliance to another user's room.");
            }

            entity.setRoom(room);
        } else {
            entity.setRoom(null);
        }

        validateUsageFieldsForEntity(entity);

        entity.setUpdatedAt(LocalDateTime.now());

        UserAppliance updated = userApplianceRepository.save(entity);
        double pricePerKwh = resolvePricePerKwh(user);
        return mapToResponseWithDerivedValues(updated, pricePerKwh, catalogueByName());
    }

    // Service: deletes one Appliance only after confirming it belongs to the authenticated user.
    @Transactional
    public void deleteUserAppliance(Long id) {
        User user = getCurrentUser();

        UserAppliance entity = userApplianceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User appliance not found."));

        if (!entity.getUser().getId().equals(user.getId())) {
            throw new IllegalStateException("You are not allowed to delete this appliance.");
        }

        userApplianceRepository.delete(entity);
    }

    // Catalogue helper: finds the base Appliance metadata by name so usage formulas have wattage values.
    private Appliance findBaseApplianceOrThrow(String applianceName) {
        List<Appliance> catalogue = applianceService.getAllAppliances();

        Optional<Appliance> match = catalogue.stream()
                .filter(a -> a.getName().equalsIgnoreCase(applianceName))
                .findFirst();

        return match.orElseThrow(() ->
                new IllegalArgumentException("Appliance not found in catalogue: " + applianceName));
    }

    // Validation helper: enforces the correct Usage input for continuous versus per-use Appliances.
    private void validateUsageFields(UserApplianceRequest request) {
        if ("continuous".equalsIgnoreCase(request.getUsageType())) {
            if (request.getHoursPerDay() == null || request.getHoursPerDay() <= 0) {
                throw new IllegalArgumentException("hoursPerDay must be provided and > 0 for continuous appliances.");
            }
        } else if ("perUse".equalsIgnoreCase(request.getUsageType())) {
            if (request.getUsesPerDay() == null || request.getUsesPerDay() <= 0) {
                throw new IllegalArgumentException("usesPerDay must be provided and > 0 for per-use appliances.");
            }
        }
    }

    // Validation helper: checks the final saved Appliance entity still has valid Usage values after an update.
    private void validateUsageFieldsForEntity(UserAppliance entity) {
        if ("continuous".equalsIgnoreCase(entity.getUsageType())) {
            if (entity.getHoursPerDay() == null || entity.getHoursPerDay() <= 0) {
                throw new IllegalArgumentException("hoursPerDay must be provided and > 0 for continuous appliances.");
            }
        } else if ("perUse".equalsIgnoreCase(entity.getUsageType())) {
            if (entity.getUsesPerDay() == null || entity.getUsesPerDay() <= 0) {
                throw new IllegalArgumentException("usesPerDay must be provided and > 0 for per-use appliances.");
            }
        }
    }

    /*
     * DTO mapper: Appliance response
     * Purpose: Converts a UserAppliance entity into frontend JSON and adds derived Cost metrics.
     * Important calculation: dailyKWh is multiplied by pricePerKwh to produce estimatedDailyCost.
     */
    private UserApplianceResponse mapToResponseWithDerivedValues(
            UserAppliance entity,
            double pricePerKwh,
            Map<String, Appliance> catalogueByName
    ) {
        UserApplianceResponse response = new UserApplianceResponse();
        response.setId(entity.getId());
        response.setApplianceName(entity.getApplianceName());
        response.setCustomName(entity.getCustomName());
        response.setUsageType(entity.getUsageType());
        response.setHoursPerDay(entity.getHoursPerDay());
        response.setUsesPerDay(entity.getUsesPerDay());

        Appliance baseAppliance = findBaseApplianceOrThrow(entity.getApplianceName(), catalogueByName);

        double dailyKWh = calculateDailyKWh(entity, baseAppliance);
        response.setDailyKWh(dailyKWh);
        response.setEstimatedDailyCost(dailyKWh * pricePerKwh);

        Room room = entity.getRoom();
        if (room != null) {
            response.setRoomId(room.getId());
            response.setRoomName(room.getName());
        }

        return response;
    }

    // Catalogue helper: builds a normalized name lookup once per request so response mapping stays efficient.
    private Map<String, Appliance> catalogueByName() {
        return applianceService.getAllAppliances().stream()
                .collect(Collectors.toMap(
                        appliance -> normalizeCatalogueKey(appliance.getName()),
                        Function.identity(),
                        (first, ignoredDuplicate) -> first
                ));
    }

    // Catalogue helper: finds an Appliance in the prebuilt lookup map using a normalized name key.
    private Appliance findBaseApplianceOrThrow(String applianceName, Map<String, Appliance> catalogueByName) {
        Appliance match = catalogueByName.get(normalizeCatalogueKey(applianceName));
        if (match == null) {
            throw new IllegalArgumentException("Appliance not found in catalogue: " + applianceName);
        }
        return match;
    }

    // Validation helper: trims optional text and stores blank strings as null.
    private String normalizeNullableText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    // Cost helper: resolves the user's saved electricity price or falls back to the default tariff.
    private double resolvePricePerKwh(User user) {
        Double userPrice = user.getPricePerKwh();
        if (userPrice != null && userPrice > 0) {
            return userPrice;
        }
        return DEFAULT_PRICE_PER_KWH;
    }

    // Catalogue helper: normalizes Appliance names before lookup comparisons.
    private String normalizeCatalogueKey(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    /*
     * Cost Calculation: daily kWh
     * Purpose: Converts appliance wattage and usage into daily energy consumption.
     * Formula: continuous = watts * hoursPerDay / 1000; perUse = wattHoursPerUse * usesPerDay / 1000.
     */
    private double calculateDailyKWh(UserAppliance entity, Appliance baseAppliance) {
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
}
