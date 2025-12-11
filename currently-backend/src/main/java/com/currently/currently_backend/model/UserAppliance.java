/*
 * File: UserAppliance.java
 * Description: JPA entity representing an appliance selected by a specific user,
 *              including their customised usage patterns (hours per day or uses per day).
 * Author: Liam Connell
 * Date: 2025-12-01
 */

package com.currently.currently_backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity: UserAppliance
 * Purpose: Link a User to a selected appliance from the base catalogue and
 *          store user-specific usage data to support energy estimation.
 */
@Entity
@Table(name = "user_appliances")
public class UserAppliance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Many user-appliances belong to one user
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // We reference the base appliance by name as a key into appliances.json
    @Column(name = "appliance_name", nullable = false)
    private String applianceName;

    // Optional user-friendly name e.g. "Kitchen Fridge", "Liam's Xbox"
    @Column(name = "custom_name")
    private String customName;

    // Either "continuous" or "perUse" – must match your appliances.json usageType values
    @Column(name = "usage_type", nullable = false)
    private String usageType;

    // For continuous appliances (e.g. fridge, heater)
    @Column(name = "hours_per_day")
    private Double hoursPerDay;

    // For per-use appliances (e.g. kettle, washing machine)
    @Column(name = "uses_per_day")
    private Double usesPerDay;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Constructors
    public UserAppliance() {
    }

    public UserAppliance(User user,
                         String applianceName,
                         String customName,
                         String usageType,
                         Double hoursPerDay,
                         Double usesPerDay) {
        this.user = user;
        this.applianceName = applianceName;
        this.customName = customName;
        this.usageType = usageType;
        this.hoursPerDay = hoursPerDay;
        this.usesPerDay = usesPerDay;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Convenience method to update timestamps on change
    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
