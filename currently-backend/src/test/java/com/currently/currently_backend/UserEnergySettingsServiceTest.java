package com.currently.currently_backend;

import com.currently.currently_backend.dto.EnergySettingsRequest;
import com.currently.currently_backend.dto.EnergySettingsResponse;
import com.currently.currently_backend.model.User;
import com.currently.currently_backend.repository.UserRepository;
import com.currently.currently_backend.service.UserEnergySettingsService;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserEnergySettingsServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserLookupHashService userLookupHashService;

    private UserEnergySettingsService service;
    private User currentUser;

    @BeforeEach
    void setUp() {
        service = new UserEnergySettingsService(userRepository, userLookupHashService);

        currentUser = new User();
        currentUser.setId(55L);
        currentUser.setEmail("settings.user@example.com");
        currentUser.setUsername("settings-user");
        currentUser.setPricePerKwh(0.40);
        currentUser.setProviderName("GridCo");

        authenticateAs("settings.user@example.com");
        when(userLookupHashService.emailHash(anyString())).thenReturn("hashed-settings");
        when(userRepository.findByEmailHash("hashed-settings")).thenReturn(Optional.of(currentUser));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // Verifies settings retrieval returns the values already stored for the current user.
    @Test
    void getSettingsReturnsStoredValues() {
        EnergySettingsResponse response = service.getSettings();

        assertAll(
                () -> assertThat(response.getPricePerKwh()).isEqualTo(0.40),
                () -> assertThat(response.getProviderName()).isEqualTo("GridCo")
        );
    }

    // Verifies save updates both tariff and provider and trims surrounding whitespace.
    @Test
    void saveSettingsUpdatesPriceAndProvider() {
        EnergySettingsRequest request = new EnergySettingsRequest();
        request.setPricePerKwh(0.50);
        request.setProviderName("  New Provider  ");

        when(userRepository.save(currentUser)).thenReturn(currentUser);

        EnergySettingsResponse response = service.saveSettings(request);

        assertAll(
                () -> assertThat(response.getPricePerKwh()).isEqualTo(0.50),
                () -> assertThat(response.getProviderName()).isEqualTo("New Provider")
        );
        assertThat(currentUser.getPricePerKwh()).isEqualTo(0.50);
        assertThat(currentUser.getProviderName()).isEqualTo("New Provider");
    }

    // Verifies save applies the default tariff when neither request nor user has one set.
    @Test
    void saveSettingsWithoutPriceAppliesDefaultForMissingUserValue() {
        currentUser.setPricePerKwh(null);
        currentUser.setProviderName(null);
        EnergySettingsRequest request = new EnergySettingsRequest();
        request.setPricePerKwh(null);
        request.setProviderName("");

        when(userRepository.save(currentUser)).thenReturn(currentUser);

        EnergySettingsResponse response = service.saveSettings(request);

        assertThat(response.getPricePerKwh()).isEqualTo(0.30);
        assertThat(response.getProviderName()).isNull();
    }

    // Verifies save rejects a missing request object instead of silently proceeding.
    @Test
    void saveSettingsRejectsNullRequest() {
        assertThatThrownBy(() -> service.saveSettings(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Energy settings request is required.");
    }

    private void authenticateAs(String principal) {
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, java.util.List.of());
        SecurityContext context = new SecurityContextImpl();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
    }
}
