/*
 * File: UserApplianceResponse.java
 * Description: DTO representing outgoing JSON for a UserAppliance, including
 *              derived values such as estimated daily kWh and cost.
 * Author: Liam Connell
 * Date: 2025-12-01
 */

package com.currently.currently_backend.dto;

/**
 * Class: UserApplianceResponse
 * Purpose: Represent outgoing JSON for UserAppliance, including derived values
 *          such as estimated energy usage and cost.
 */
public class UserApplianceResponse {

    private Long id;
    private String applianceName;
    private String customName;
    private String usageType;
    private Double hoursPerDay;
    private Double usesPerDay;
    private Long roomId;
    private String roomName;




    // Derived metrics (calculated in service)
    private Double dailyKWh;
    private Double estimatedDailyCost;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public Double getDailyKWh() {
        return dailyKWh;
    }

    public void setDailyKWh(Double dailyKWh) {
        this.dailyKWh = dailyKWh;
    }

    public Double getEstimatedDailyCost() {
        return estimatedDailyCost;
    }

    public void setEstimatedDailyCost(Double estimatedDailyCost) {
        this.estimatedDailyCost = estimatedDailyCost;
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

}
