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

    // Endpoint: GET /api/users/me/appliances
    // Purpose: Return all appliances selected by the current user.
    @GetMapping
    public ResponseEntity<List<UserApplianceResponse>> getMyAppliances() {
        List<UserApplianceResponse> appliances = userApplianceService.getUserAppliances();
        return ResponseEntity.ok(appliances);
    }

    // Endpoint: POST /api/users/me/appliances
    // Purpose: Create a new user appliance entry.
    @PostMapping
    public ResponseEntity<UserApplianceResponse> createMyAppliance(
            @RequestBody UserApplianceRequest request
    ) {
        UserApplianceResponse created = userApplianceService.createUserAppliance(request);
        return ResponseEntity.ok(created);
    }

    // Endpoint: PUT /api/users/me/appliances/{id}
    // Purpose: Update custom name or usage values for an existing user appliance.
    @PutMapping("/{id}")
    public ResponseEntity<UserApplianceResponse> updateMyAppliance(
            @PathVariable Long id,
            @RequestBody UserApplianceRequest request
    ) {
        UserApplianceResponse updated = userApplianceService.updateUserAppliance(id, request);
        return ResponseEntity.ok(updated);
    }

    // Endpoint: DELETE /api/users/me/appliances/{id}
    // Purpose: Delete a user appliance entry.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMyAppliance(@PathVariable Long id) {
        userApplianceService.deleteUserAppliance(id);
        return ResponseEntity.noContent().build();
    }
}
