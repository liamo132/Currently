package com.currently.currently_backend.service;

import com.currently.currently_backend.dto.EnergySettingsRequest;
import com.currently.currently_backend.dto.EnergySettingsResponse;
import com.currently.currently_backend.model.User;
import com.currently.currently_backend.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class UserEnergySettingsService {

    private static final double DEFAULT_PRICE_PER_KWH = 0.30;

    private final UserRepository userRepository;
    private final UserLookupHashService userLookupHashService;

    public UserEnergySettingsService(UserRepository userRepository, UserLookupHashService userLookupHashService) {
        this.userRepository = userRepository;
        this.userLookupHashService = userLookupHashService;
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String emailOrUsername = auth.getName();
        return userRepository.findByEmailHash(userLookupHashService.emailHash(emailOrUsername))
                
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
    }

    public EnergySettingsResponse getSettings() {
        User user = getCurrentUser();
        Double price = user.getPricePerKwh();
        String provider = user.getProviderName();
        return new EnergySettingsResponse(price, provider);
    }

    public EnergySettingsResponse saveSettings(EnergySettingsRequest request) {
        User user = getCurrentUser();

        if (request.getPricePerKwh() != null) {
            user.setPricePerKwh(request.getPricePerKwh());
        } else if (user.getPricePerKwh() == null) {
            user.setPricePerKwh(DEFAULT_PRICE_PER_KWH);
        }

        if (request.getProviderName() != null) {
            user.setProviderName(request.getProviderName());
        }

        User saved = userRepository.save(user);
        return new EnergySettingsResponse(saved.getPricePerKwh(), saved.getProviderName());
    }
}

