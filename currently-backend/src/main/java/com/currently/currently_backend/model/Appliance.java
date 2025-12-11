/*
 * File: Appliance.java
 * Description: Represents a household appliance with metadata used for energy estimation.
 * Author: Liam Connell
 * Date: 2025-11-12
 *
 * Notes:
 * - Supports both continuous-use and per-use appliances via the "usageType" discriminator.
 * - Mirrors the structure stored in appliances.json.
 * - No database persistence; this model is purely loaded from static JSON.
 */

package com.currently.currently_backend.model;

public class Appliance {

    private String name;
    private String category;
    private String usageType; // "continuous" or "perUse"

    // Continuous-use fields
    private Integer averageWatts;
    private Integer defaultHoursPerDay;

    // Per-use fields
    private Integer averageWattsPerUse;
    private Integer defaultUsesPerDay;

    public Appliance() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getUsageType() { return usageType; }
    public void setUsageType(String usageType) { this.usageType = usageType; }

    public Integer getAverageWatts() { return averageWatts; }
    public void setAverageWatts(Integer averageWatts) { this.averageWatts = averageWatts; }

    public Integer getDefaultHoursPerDay() { return defaultHoursPerDay; }
    public void setDefaultHoursPerDay(Integer defaultHoursPerDay) { this.defaultHoursPerDay = defaultHoursPerDay; }

    public Integer getAverageWattsPerUse() { return averageWattsPerUse; }
    public void setAverageWattsPerUse(Integer averageWattsPerUse) { this.averageWattsPerUse = averageWattsPerUse; }

    public Integer getDefaultUsesPerDay() { return defaultUsesPerDay; }
    public void setDefaultUsesPerDay(Integer defaultUsesPerDay) { this.defaultUsesPerDay = defaultUsesPerDay; }
}
