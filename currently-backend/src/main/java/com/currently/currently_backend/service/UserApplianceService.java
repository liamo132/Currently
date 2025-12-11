/*
 * File: UserApplianceService.java
 * Description: Business logic for managing user-selected appliances and computing
 *              estimated energy usage based on the base appliance catalogue.
 * Author: Liam Connell
 * Date: 2025-12-01
 */

package com.currently.currently_backend.service;
import com.currently.currently_backend.model.Room;
import com.currently.currently_backend.repository.RoomRepository;


import com.currently.currently_backend.dto.UserApplianceRequest;
import com.currently.currently_backend.dto.UserApplianceResponse;
import com.currently.currently_backend.model.Appliance;
import com.currently.currently_backend.model.User;
import com.currently.currently_backend.model.UserAppliance;
import com.currently.currently_backend.repository.UserApplianceRepository;
import com.currently.currently_backend.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;


import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Class: UserApplianceService
 * Purpose: Provide higher-level operations for the "My Appliances" feature,
 *          including creating, updating, deleting, and listing user appliances
 *          and calculating estimated energy usage.
 */
@Service
public class UserApplianceService {


    // Example tariff: €0.30 per kWh (you can move this to config later)
    private static final double PRICE_PER_KWH = 0.30;

    private final UserRepository userRepository;
    private final UserApplianceRepository userApplianceRepository;
    private final ApplianceService applianceService;
    private final RoomRepository roomRepository;


    public UserApplianceService(
            UserRepository userRepository,
            UserApplianceRepository userApplianceRepository,
            ApplianceService applianceService,
            RoomRepository roomRepository
    ) {
        this.userRepository = userRepository;
        this.userApplianceRepository = userApplianceRepository;
        this.applianceService = applianceService;
        this.roomRepository = roomRepository;
    }


    // Function: getCurrentUser
    // Purpose: Retrieve the currently authenticated user using Spring Security's context.
    // Inputs: none
    // Outputs: User entity from the database
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // In your setup, auth.getName() should correspond to the user's email or username.
        String emailOrUsername = auth.getName();

        // Adjust this if your login uses username instead of email.
        return userRepository.findByEmail(emailOrUsername)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
    }

    // Function: getUserAppliances
    // Purpose: Return all UserAppliance entries for the current user with derived metrics.
    // Inputs: none
    // Outputs: List of UserApplianceResponse DTOs
    public List<UserApplianceResponse> getUserAppliances() {
        User user = getCurrentUser();
        List<UserAppliance> entities = userApplianceRepository.findByUserOrderByCreatedAtAsc(user);

        return entities.stream()
                .map(this::mapToResponseWithDerivedValues)
                .collect(Collectors.toList());
    }

    // Function: createUserAppliance
    // Purpose: Create a new UserAppliance for the current user after validating input.
    // Inputs: UserApplianceRequest DTO
    // Outputs: UserApplianceResponse DTO with derived metrics
    public UserApplianceResponse createUserAppliance(UserApplianceRequest request) {
        User user = getCurrentUser();

        Appliance baseAppliance = findBaseApplianceOrThrow(request.getApplianceName());

        if (!baseAppliance.getUsageType().equalsIgnoreCase(request.getUsageType())) {
            throw new IllegalArgumentException("Usage type does not match base appliance configuration.");
        }

        validateUsageFields(request);

        UserAppliance entity = new UserAppliance();
        entity.setUser(user);
        entity.setApplianceName(request.getApplianceName());
        entity.setCustomName(request.getCustomName());
        entity.setUsageType(request.getUsageType());
        entity.setHoursPerDay(request.getHoursPerDay());
        entity.setUsesPerDay(request.getUsesPerDay());

// ROOM ASSIGNMENT (POST)
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
        return mapToResponseWithDerivedValues(saved);

    }


    // Function: updateUserAppliance
    // Purpose: Update an existing UserAppliance's usage values and custom name.
    // Inputs: id (Long), request DTO
    // Outputs: Updated UserApplianceResponse
    public UserApplianceResponse updateUserAppliance(Long id, UserApplianceRequest request) {
        User user = getCurrentUser();

        UserAppliance entity = userApplianceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User appliance not found."));

        // Ensure the appliance belongs to the current user
        if (!entity.getUser().getId().equals(user.getId())) {
            throw new IllegalStateException("You are not allowed to modify this appliance.");
        }

        // Usage type and appliance name are immutable here to keep things simple.
        // You can relax this later if needed.
        if (request.getCustomName() != null) {
            entity.setCustomName(request.getCustomName());
        }

        if (request.getHoursPerDay() != null) {
            entity.setHoursPerDay(request.getHoursPerDay());
        }

        if (request.getUsesPerDay() != null) {
            entity.setUsesPerDay(request.getUsesPerDay());
        }

        // ROOM ASSIGNMENT (PUT)
        if (request.getRoomId() != null) {
            Room room = roomRepository.findById(request.getRoomId())
                    .orElseThrow(() -> new IllegalArgumentException("Room not found"));

            if (!room.getUser().getId().equals(user.getId())) {
                throw new IllegalStateException("Cannot assign appliance to another user's room.");
            }

            entity.setRoom(room);
        } else {
            // Allow unassigning room if request explicitly sets roomId: null
            entity.setRoom(null);
        }


        validateUsageFieldsForEntity(entity);

        entity.setUpdatedAt(LocalDateTime.now());

        UserAppliance updated = userApplianceRepository.save(entity);
        return mapToResponseWithDerivedValues(updated);

    }

    // Function: deleteUserAppliance
    // Purpose: Remove a UserAppliance belonging to the current user.
    // Inputs: id (Long)
    // Outputs: void (throws if not found or not owned)
    public void deleteUserAppliance(Long id) {
        User user = getCurrentUser();

        UserAppliance entity = userApplianceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User appliance not found."));

        if (!entity.getUser().getId().equals(user.getId())) {
            throw new IllegalStateException("You are not allowed to delete this appliance.");
        }

        userApplianceRepository.delete(entity);
    }

    // Helper: find base appliance from catalogue
    private Appliance findBaseApplianceOrThrow(String applianceName) {
        // IMPORTANT:
        // This assumes ApplianceService exposes a method like:
        //   List<Appliance> getAllAppliances()
        // If your method name is different, either:
        //   - rename it, or
        //   - add a wrapper method in ApplianceService with this signature.
        List<Appliance> catalogue = applianceService.getAllAppliances();

        Optional<Appliance> match = catalogue.stream()
                .filter(a -> a.getName().equalsIgnoreCase(applianceName))
                .findFirst();

        return match.orElseThrow(() ->
                new IllegalArgumentException("Appliance not found in catalogue: " + applianceName));
    }

    // Helper: validate usage fields for a request
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

    // Helper: validate usage fields for an existing entity
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

    private UserApplianceResponse mapToResponseWithDerivedValues(UserAppliance entity) {
        UserApplianceResponse response = new UserApplianceResponse();
        response.setId(entity.getId());
        response.setApplianceName(entity.getApplianceName());
        response.setCustomName(entity.getCustomName());
        response.setUsageType(entity.getUsageType());
        response.setHoursPerDay(entity.getHoursPerDay());
        response.setUsesPerDay(entity.getUsesPerDay());

        Appliance baseAppliance = findBaseApplianceOrThrow(entity.getApplianceName());

        double dailyKWh = calculateDailyKWh(entity, baseAppliance);
        response.setDailyKWh(dailyKWh);
        response.setEstimatedDailyCost(dailyKWh * PRICE_PER_KWH);

        // ROOM MAPPING
        Room room = entity.getRoom();
        if (room != null) {
            response.setRoomId(room.getId());
            response.setRoomName(room.getName());
        }

        return response;
    }



    // Helper: core energy calculation logic
    private double calculateDailyKWh(UserAppliance entity, Appliance baseAppliance) {
        // For continuous devices: (averageWatts * hoursPerDay) / 1000
        if ("continuous".equalsIgnoreCase(entity.getUsageType())) {
            double watts = baseAppliance.getAverageWatts();
            double hours = entity.getHoursPerDay() != null ? entity.getHoursPerDay() : 0.0;
            return (watts * hours) / 1000.0;
        }

        // For per-use devices: (averageWattsPerUse * usesPerDay) / 1000
        if ("perUse".equalsIgnoreCase(entity.getUsageType())) {
            double wattsPerUse = baseAppliance.getAverageWattsPerUse();
            double uses = entity.getUsesPerDay() != null ? entity.getUsesPerDay() : 0.0;
            return (wattsPerUse * uses) / 1000.0;
        }

        return 0.0;
    }
}
