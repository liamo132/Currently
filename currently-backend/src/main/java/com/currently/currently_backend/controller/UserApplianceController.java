/*
 * File: UserApplianceController.java
 * Description: REST controller exposing endpoints for the "My Appliances" feature,
 *              allowing authenticated users to manage their selected appliances.
 * Author: Liam Connell
 * Date: 2025-12-01
 */

package com.currently.currently_backend.controller;

import com.currently.currently_backend.dto.UserApplianceRequest;
import com.currently.currently_backend.dto.UserApplianceResponse;
import com.currently.currently_backend.service.UserApplianceService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Class: UserApplianceController
 * Purpose: Define HTTP endpoints under /api/users/me/appliances for CRUD operations
 *          on user-specific appliances.
 */
@RestController
@RequestMapping("/api/users/me/appliances")
public class UserApplianceController {

    private final UserApplianceService userApplianceService;

    public UserApplianceController(UserApplianceService userApplianceService) {
        this.userApplianceService = userApplianceService;
    }

    // Controller API: returns all Appliances selected by the current user with calculated Usage and Cost fields.
    @GetMapping
    public ResponseEntity<List<UserApplianceResponse>> getMyAppliances() {
        List<UserApplianceResponse> appliances = userApplianceService.getUserAppliances();
        return ResponseEntity.ok(appliances);
    }

    // Controller API: creates a user Appliance entry and validates it against the catalogue usage model.
    @PostMapping
    public ResponseEntity<UserApplianceResponse> createMyAppliance(
            @Valid @RequestBody UserApplianceRequest request
    ) {
        UserApplianceResponse created = userApplianceService.createUserAppliance(request);
        return ResponseEntity.ok(created);
    }

    // Controller API: updates Appliance name, Room assignment, or Usage fields after ownership checks in the Service.
    @PutMapping("/{id}")
    public ResponseEntity<UserApplianceResponse> updateMyAppliance(
            @PathVariable Long id,
            @Valid @RequestBody UserApplianceRequest request
    ) {
        UserApplianceResponse updated = userApplianceService.updateUserAppliance(id, request);
        return ResponseEntity.ok(updated);
    }

    // Controller API: deletes one user Appliance after the Service confirms it belongs to the authenticated user.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMyAppliance(@PathVariable Long id) {
        userApplianceService.deleteUserAppliance(id);
        return ResponseEntity.noContent().build();
    }
}
