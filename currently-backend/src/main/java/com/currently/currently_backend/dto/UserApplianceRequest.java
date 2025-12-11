/*
 * File: UserApplianceRequest.java
 * Description: DTO representing incoming JSON when the user creates or updates
 *              a UserAppliance entity.
 * Author: Liam Connell
 * Date: 2025-12-01
 */

package com.currently.currently_backend.dto;

/**
 * Class: UserApplianceRequest
 * Purpose: Represent incoming JSON when the user creates or updates a UserAppliance.
 */
public class UserApplianceRequest {

    // Name of the base appliance from the catalogue (e.g. "Fridge")
    private String applianceName;

    // Optional user-friendly label (e.g. "Kitchen fridge")
    private String customName;

    // "continuous" or "perUse"
    private String usageType;

    // For continuous devices
    private Double hoursPerDay;

    // For per-use devices
    private Double usesPerDay;

    //for using custom rooms
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
