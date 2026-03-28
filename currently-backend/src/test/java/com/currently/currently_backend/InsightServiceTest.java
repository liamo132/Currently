package com.currently.currently_backend;

import com.currently.currently_backend.dto.InsightGenerateRequest;
import com.currently.currently_backend.dto.InsightGenerateResponse;
import com.currently.currently_backend.model.Appliance;
import com.currently.currently_backend.model.Room;
import com.currently.currently_backend.model.User;
import com.currently.currently_backend.model.UserAppliance;
import com.currently.currently_backend.repository.UserApplianceRepository;
import com.currently.currently_backend.repository.UserRepository;
import com.currently.currently_backend.service.ApplianceService;
import com.currently.currently_backend.service.InsightService;
import com.currently.currently_backend.service.UserLookupHashService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.within;

@ExtendWith(MockitoExtension.class)
class InsightServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserApplianceRepository userApplianceRepository;

    @Mock
    private ApplianceService applianceService;

    @Mock
    private UserLookupHashService userLookupHashService;

    private InsightService insightService;
    private User currentUser;

    @BeforeEach
    void setUp() {
        insightService = new InsightService(
                userRepository,
                userApplianceRepository,
                applianceService,
                userLookupHashService
        );

        currentUser = new User();
        currentUser.setId(101L);
        currentUser.setEmail("insight.owner@example.com");
        currentUser.setPricePerKwh(0.40);
        authenticateAs("insight.owner@example.com");

        when(userLookupHashService.emailHash(anyString())).thenReturn("insight-hash");
        when(userRepository.findByEmailHash("insight-hash")).thenReturn(Optional.of(currentUser));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // Verifies a simple appliance dataset produces a forecast with the expected monthly impact.
    @Test
    void generateInsightsReturnsForecastWithCalculatedImpactForSingleAppliance() {
        when(applianceService.getAllAppliances()).thenReturn(sampleCatalogue());
        UserAppliance entity = createUserAppliance("Fridge", "continuous", 10.0, null, createRoom("Kitchen"));
        when(userApplianceRepository.findByUserOrderByCreatedAtAsc(currentUser)).thenReturn(List.of(entity));

        InsightGenerateResponse response = insightService.generateInsights(new InsightGenerateRequest());

        assertAll(
                () -> assertThat(response.isHasMore()).isFalse(),
                () -> assertThat(response.getRunId()).isNotBlank(),
                () -> assertThat(response.getInsights()).isNotEmpty(),
                () -> assertThat(response.getInsights()).hasSize(3),
                () -> assertThat(response.getInsights().get(0).getImpactMonthly()).isEqualTo(1.8, within(0.01)),
                () -> assertThat(response.getInsights().get(0).getCategory()).isEqualTo("room"),
                () -> assertThat(response.getStopReason()).isEqualTo("No more high-impact practical recommendations remain."),
                () -> assertThat(response.getInsights().get(0).getConfidence()).isIn("high", "medium", "low")
        );
    }

    // Verifies forecast math uses request-level tariff overrides against the sample dataset.
    @Test
    void forecastingAccuracyMatchesSampleDatasetAndRequestPriceOverride() {
        when(applianceService.getAllAppliances()).thenReturn(sampleCatalogue());
        currentUser.setPricePerKwh(null);
        UserAppliance entity = createUserAppliance("Fridge", "continuous", 10.0, null, createRoom("Kitchen"));
        when(userApplianceRepository.findByUserOrderByCreatedAtAsc(currentUser)).thenReturn(List.of(entity));

        InsightGenerateRequest request = new InsightGenerateRequest();
        request.setPricePerKwh(0.50);

        InsightGenerateResponse response = insightService.generateInsights(request);

        assertThat(response.isHasMore()).isFalse();
        assertThat(response.getRunId()).isNotBlank();
        assertThat(response.getStopReason()).isEqualTo("No more high-impact practical recommendations remain.");
        assertThat(response.getInsights()).hasSize(3);
        assertThat(response.getInsights().get(0).getImpactMonthly()).isEqualTo(2.25, within(0.01));
    }

    // Verifies large result sets can be paged across generate and generateMore calls.
    @Test
    void generateInsightsCanPaginateWhenEnoughCandidatesAreGenerated() {
        when(applianceService.getAllAppliances()).thenReturn(sampleCatalogue());
        UserAppliance a1 = createUserAppliance("Fridge", "continuous", 24.0, null, createRoom("Kitchen"));
        UserAppliance a2 = createUserAppliance("Washing Machine", "perUse", null, 1.0, createRoom("Utility"));
        UserAppliance a3 = createUserAppliance("Electric Kettle", "perUse", null, 3.0, createRoom("Kitchen"));
        UserAppliance a4 = createUserAppliance("Television", "continuous", 8.0, null, createRoom("Living Room"));
        UserAppliance a5 = createUserAppliance("Desktop Computer", "continuous", 6.0, null, createRoom("Office"));
        when(userApplianceRepository.findByUserOrderByCreatedAtAsc(currentUser))
                .thenReturn(List.of(a1, a2, a3, a4, a5));

        InsightGenerateResponse firstPage = insightService.generateInsights(new InsightGenerateRequest());

        assertThat(firstPage.isHasMore()).isTrue();
        assertThat(firstPage.getInsights()).hasSize(4);

        InsightGenerateResponse secondPage = insightService.generateMore(firstPage.getRunId());
        assertThat(secondPage.getInsights()).isNotEmpty();
        assertThat(secondPage.isHasMore()).isFalse();
    }

    // Verifies follow-up pages are blocked when the run belongs to a different user.
    @Test
    void generateMoreRejectsRunOwnedByDifferentUser() {
        when(applianceService.getAllAppliances()).thenReturn(sampleCatalogue());
        UserAppliance entity = createUserAppliance("Fridge", "continuous", 10.0, null, null);
        when(userApplianceRepository.findByUserOrderByCreatedAtAsc(currentUser)).thenReturn(List.of(entity));

        InsightGenerateResponse response = insightService.generateInsights(new InsightGenerateRequest());

        authenticateAs("other@example.com");
        User otherUser = new User();
        otherUser.setId(999L);
        when(userRepository.findByEmailHash(anyString())).thenReturn(Optional.of(otherUser));

        assertThatThrownBy(() -> insightService.generateMore(response.getRunId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("You are not allowed to access this insights run.");
    }

    // Verifies generation returns a safe empty response when no appliance data exists yet.
    @Test
    void generateInsightsFallsBackWhenNoAppliancesFound() {
        when(userApplianceRepository.findByUserOrderByCreatedAtAsc(currentUser)).thenReturn(List.of());

        InsightGenerateResponse response = insightService.generateInsights(null);

        assertAll(
                () -> assertThat(response.getInsights()).isEmpty(),
                () -> assertThat(response.getRunId()).isNull(),
                () -> assertThat(response.isHasMore()).isFalse(),
                () -> assertThat(response.getStopReason()).isEqualTo("No appliance data available yet.")
        );
    }

    private List<Appliance> sampleCatalogue() {
        Appliance fridge = new Appliance();
        fridge.setName("Fridge");
        fridge.setUsageType("continuous");
        fridge.setAverageWatts(150);

        Appliance kettle = new Appliance();
        kettle.setName("Electric Kettle");
        kettle.setUsageType("perUse");
        kettle.setAverageWattsPerUse(2000);

        Appliance washer = new Appliance();
        washer.setName("Washing Machine");
        washer.setUsageType("perUse");
        washer.setAverageWattsPerUse(1000);

        Appliance tv = new Appliance();
        tv.setName("Television");
        tv.setUsageType("continuous");
        tv.setAverageWatts(100);

        Appliance desktop = new Appliance();
        desktop.setName("Desktop Computer");
        desktop.setUsageType("continuous");
        desktop.setAverageWatts(200);

        return List.of(fridge, kettle, washer, tv, desktop);
    }

    private UserAppliance createUserAppliance(String applianceName, String usageType, Double hoursPerDay, Double usesPerDay, Room room) {
        UserAppliance appliance = new UserAppliance();
        appliance.setUser(currentUser);
        appliance.setApplianceName(applianceName);
        appliance.setUsageType(usageType);
        appliance.setHoursPerDay(hoursPerDay);
        appliance.setUsesPerDay(usesPerDay);
        appliance.setRoom(room);
        return appliance;
    }

    private Room createRoom(String name) {
        Room room = new Room(currentUser, name, "Ground", null);
        ReflectionTestUtils.setField(room, "id", 1L);
        return room;
    }

    private void authenticateAs(String principal) {
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, java.util.List.of());
        SecurityContext context = new SecurityContextImpl();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
    }
}
