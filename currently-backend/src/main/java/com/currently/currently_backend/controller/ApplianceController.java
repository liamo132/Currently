/*
 * File: ApplianceController.java
 * Description: REST endpoints for retrieving appliance metadata.
 * Project: Currently
 * Author: Liam Connell
 * Date: 2025-11-12
 *
 * Notes:
 * - Public endpoint (no authentication) as per current Sprint 3 requirements.
 * - Backend reads from static JSON; no DB storage at this stage.
 */

package com.currently.currently_backend.controller;

import com.currently.currently_backend.model.Appliance;
import com.currently.currently_backend.service.ApplianceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/appliances")
public class ApplianceController {

    private final ApplianceService applianceService;

    @Autowired
    public ApplianceController(ApplianceService applianceService) {
        this.applianceService = applianceService;
    }

    // Controller API: returns the public Appliance catalogue used by My Appliances and Watch Your Watts.
    @GetMapping
    public List<Appliance> getAllAppliances() {
        return applianceService.getAllAppliances();
    }
}
