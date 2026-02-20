package com.currently.currently_backend.controller;

import com.currently.currently_backend.dto.EnergySettingsRequest;
import com.currently.currently_backend.dto.EnergySettingsResponse;
import com.currently.currently_backend.service.UserEnergySettingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/me/energy-settings")
public class EnergySettingsController {

    private final UserEnergySettingsService service;

    public EnergySettingsController(UserEnergySettingsService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<EnergySettingsResponse> getSettings() {
        EnergySettingsResponse res = service.getSettings();
        return ResponseEntity.ok(res);
    }

    @PutMapping
    public ResponseEntity<EnergySettingsResponse> saveSettings(@RequestBody EnergySettingsRequest request) {
        EnergySettingsResponse res = service.saveSettings(request);
        return ResponseEntity.ok(res);
    }
}
