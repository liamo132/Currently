package com.currently.currently_backend;

import com.currently.currently_backend.dto.UserApplianceRequest;
import com.currently.currently_backend.dto.UserApplianceResponse;
import com.currently.currently_backend.model.Appliance;
import com.currently.currently_backend.model.Room;
import com.currently.currently_backend.model.User;
import com.currently.currently_backend.model.UserAppliance;
import com.currently.currently_backend.repository.RoomRepository;
import com.currently.currently_backend.repository.UserApplianceRepository;
import com.currently.currently_backend.repository.UserRepository;
import com.currently.currently_backend.service.ApplianceService;
import com.currently.currently_backend.service.UserApplianceService;
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

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.within;

@ExtendWith(MockitoExtension.class)
class UserApplianceServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserApplianceRepository userApplianceRepository;

    @Mock
    private ApplianceService applianceService;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private UserLookupHashService userLookupHashService;

    private UserApplianceService userApplianceService;
    private User currentUser;

    @BeforeEach
    void setUp() {
        userApplianceService = new UserApplianceService(
                userRepository,
                userApplianceRepository,
                applianceService,
                roomRepository,
                userLookupHashService
        );

        currentUser = new User();
        currentUser.setId(11L);
        currentUser.setEmail("owner@example.com");
        currentUser.setUsername("owner-user");
        currentUser.setPricePerKwh(0.40);

        authenticateAs("owner@example.com");
        when(userLookupHashService.emailHash(anyString())).thenReturn("email-hash");
        when(userRepository.findByEmailHash("email-hash")).thenReturn(Optional.of(currentUser));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // Verifies read responses derive kWh and daily cost from catalog watts and user tariff.
    @Test
    void getUserAppliancesUsesUserPricingAndCatalogTotals() {
        when(applianceService.getAllAppliances()).thenReturn(sampleCatalogue());
        UserAppliance saved = new UserAppliance();
        setEntityId(saved, 1L);
        saved.setUser(currentUser);
        saved.setApplianceName("Fridge");
        saved.setUsageType("continuous");
        saved.setHoursPerDay(10.0);

        when(userApplianceRepository.findByUserOrderByCreatedAtAsc(currentUser)).thenReturn(List.of(saved));

        List<UserApplianceResponse> result = userApplianceService.getUserAppliances();

        assertThat(result).hasSize(1);
        UserApplianceResponse response = result.get(0);

        assertAll(
                () -> assertThat(response.getDailyKWh()).isEqualTo(1.5),
                () -> assertThat(response.getEstimatedDailyCost()).isEqualTo(0.6, within(0.0001))
        );
    }

    // Verifies continuous-usage appliances compute estimated cost from hours-per-day input.
    @Test
    void createUserApplianceForContinuousDeviceCalculatesDerivedValues() {
        when(applianceService.getAllAppliances()).thenReturn(sampleCatalogue());
        UserApplianceRequest request = new UserApplianceRequest();
        request.setApplianceName("Fridge");
        request.setUsageType("continuous");
        request.setHoursPerDay(8.0);

        when(userApplianceRepository.save(any(UserAppliance.class))).thenAnswer(i -> {
            UserAppliance entity = i.getArgument(0);
            setEntityId(entity, 77L);
            return entity;
        });

        UserApplianceResponse response = userApplianceService.createUserAppliance(request);

        assertThat(response.getId()).isEqualTo(77L);
        assertThat(response.getEstimatedDailyCost()).isEqualTo(0.48);
    }

    // Verifies per-use appliances compute both kWh and cost from uses-per-day input.
    @Test
    void createUserApplianceForPerUseDeviceCalculatesCostAndKwh() {
        when(applianceService.getAllAppliances()).thenReturn(sampleCatalogue());
        UserApplianceRequest request = new UserApplianceRequest();
        request.setApplianceName("Washing Machine");
        request.setUsageType("perUse");
        request.setUsesPerDay(3.0);

        when(userApplianceRepository.save(any(UserAppliance.class))).thenAnswer(i -> {
            UserAppliance entity = i.getArgument(0);
            setEntityId(entity, 88L);
            return entity;
        });

        UserApplianceResponse response = userApplianceService.createUserAppliance(request);

        assertThat(response.getId()).isEqualTo(88L);
        assertThat(response.getDailyKWh()).isEqualTo(3.0);
        assertThat(response.getEstimatedDailyCost()).isEqualTo(1.2, within(0.0001));
    }

    // Verifies validation rejects missing usage fields required by the selected usage type.
    @Test
    void createUserApplianceRejectsInvalidUsageFieldsForUsageType() {
        when(applianceService.getAllAppliances()).thenReturn(sampleCatalogue());
        UserApplianceRequest request = new UserApplianceRequest();
        request.setApplianceName("Fridge");
        request.setUsageType("continuous");

        assertThatThrownBy(() -> userApplianceService.createUserAppliance(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("hoursPerDay");
    }

    // Verifies appliances cannot be attached to rooms owned by another user.
    @Test
    void createUserApplianceRejectsRoomBelongingToOtherUser() {
        when(applianceService.getAllAppliances()).thenReturn(sampleCatalogue());
        User otherUser = new User();
        otherUser.setId(999L);
        otherUser.setEmail("other@example.com");

        Room room = new Room(otherUser, "Other Room", "Ground", "Guest");
        setEntityId(room, 11L);

        when(roomRepository.findById(11L)).thenReturn(Optional.of(room));

        UserApplianceRequest request = new UserApplianceRequest();
        request.setApplianceName("Fridge");
        request.setUsageType("continuous");
        request.setHoursPerDay(5.0);
        request.setRoomId(11L);

        assertThatThrownBy(() -> userApplianceService.createUserAppliance(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot assign appliance to another user's room.");
    }

    // Verifies a user cannot update an appliance they do not own.
    @Test
    void updateApplianceRejectsWrongOwner() {
        User attacker = new User();
        attacker.setId(222L);

        UserAppliance entity = new UserAppliance();
        setEntityId(entity, 99L);
        entity.setUser(attacker);
        entity.setApplianceName("Fridge");
        entity.setUsageType("continuous");
        entity.setHoursPerDay(1.0);

        when(userApplianceRepository.findById(99L)).thenReturn(Optional.of(entity));

        UserApplianceRequest request = new UserApplianceRequest();
        request.setCustomName("Try to edit");

        assertThatThrownBy(() -> userApplianceService.updateUserAppliance(99L, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("You are not allowed to modify this appliance.");
    }

    // Verifies a user cannot delete an appliance they do not own.
    @Test
    void deleteApplianceRejectsWrongOwner() {
        User attacker = new User();
        attacker.setId(333L);

        UserAppliance entity = new UserAppliance();
        setEntityId(entity, 100L);
        entity.setUser(attacker);

        when(userApplianceRepository.findById(100L)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> userApplianceService.deleteUserAppliance(100L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("You are not allowed to delete this appliance.");
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

        return asList(fridge, kettle, washer);
    }

    private void authenticateAs(String principal) {
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, List.of());
        SecurityContext context = new SecurityContextImpl();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
    }

    private void setEntityId(Object entity, Long id) {
        ReflectionTestUtils.setField(entity, "id", id);
    }
}
