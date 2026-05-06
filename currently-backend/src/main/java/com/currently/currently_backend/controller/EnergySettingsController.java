package com.currently.currently_backend.controller;

import com.currently.currently_backend.dto.EnergySettingsRequest;
import com.currently.currently_backend.dto.EnergySettingsResponse;
import com.currently.currently_backend.service.UserEnergySettingsService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/me/energy-settings")
public class EnergySettingsController {

    private final UserEnergySettingsService service;

    public EnergySettingsController(UserEnergySettingsService service) {
        this.service = service;
    }

    // Controller API: loads the current user's electricity tariff settings for Dashboard and Forecast calculations.
    @GetMapping
    public ResponseEntity<EnergySettingsResponse> getSettings() {
        EnergySettingsResponse res = service.getSettings();
        return ResponseEntity.ok(res);
    }

    // Controller API: saves price-per-kWh and provider name used in Cost, Forecast, and Savings features.
    @PutMapping
    public ResponseEntity<EnergySettingsResponse> saveSettings(@Valid @RequestBody EnergySettingsRequest request) {
        EnergySettingsResponse res = service.saveSettings(request);
        return ResponseEntity.ok(res);
    }
}
