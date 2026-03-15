/*
 * File: UserApplianceRequest.java
 * Description: DTO representing incoming JSON when the user creates or updates
 *              a UserAppliance entity.
 * Author: Liam Connell
 * Date: 2025-12-01
 */

package com.currently.currently_backend.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Class: UserApplianceRequest
 * Purpose: Represent incoming JSON when the user creates or updates a UserAppliance.
 */
public class UserApplianceRequest {

    // Name of the base appliance from the catalogue (e.g. "Fridge")
    @Size(max = 100, message = "Appliance name must be 100 characters or fewer.")
    private String applianceName;

    // Optional user-friendly label (e.g. "Kitchen fridge")
    @Size(max = 100, message = "Custom name must be 100 characters or fewer.")
    private String customName;

    // "continuous" or "perUse"
    @Pattern(regexp = "^(continuous|perUse)$", message = "usageType must be continuous or perUse.")
    private String usageType;

    // For continuous devices
    @DecimalMin(value = "0.01", message = "hoursPerDay must be greater than 0.")
    @DecimalMax(value = "24.0", message = "hoursPerDay cannot be greater than 24.")
    private Double hoursPerDay;

    // For per-use devices
    @DecimalMin(value = "0.01", message = "usesPerDay must be greater than 0.")
    @DecimalMax(value = "100.0", message = "usesPerDay cannot be greater than 100.")
    private Double usesPerDay;

    //for using custom rooms
    @Positive(message = "roomId must be a positive number.")
    private Long roomId;


    public String getApplianceName() {
        return applianceName;
    }

    public void setApplianceName(String applianceName) {
        this.applianceName = applianceName;
    }

    public String getCustomName() {
        return customName;
    }

    public void setCustomName(String customName) {
        this.customName = customName;
    }

    public String getUsageType() {
        return usageType;
    }

    public void setUsageType(String usageType) {
        this.usageType = usageType;
    }

    public Double getHoursPerDay() {
        return hoursPerDay;
    }

    public void setHoursPerDay(Double hoursPerDay) {
        this.hoursPerDay = hoursPerDay;
    }

    public Double getUsesPerDay() {
        return usesPerDay;
    }

    public void setUsesPerDay(Double usesPerDay) {
        this.usesPerDay = usesPerDay;
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

}
