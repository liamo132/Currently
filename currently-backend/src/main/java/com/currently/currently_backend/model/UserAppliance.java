/*
 * File: UserAppliance.java
 * Description: JPA entity representing an appliance selected by a specific user,
 *              including customized usage patterns such as hours per day or uses per day.
 * Project: Currently
 * Author: Liam Connell
 *
 */

package com.currently.currently_backend.model;

import com.currently.currently_backend.persistence.EncryptedStringConverter;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_appliances",
        indexes = {
                @Index(name = "idx_user_appliances_user_created", columnList = "user_id, created_at"),
                @Index(name = "idx_user_appliances_room", columnList = "room_id")
        }
)
public class UserAppliance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Database relation: many Appliance selections belong to one authenticated User.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Room relation: optional link to a Map My House Room for room-level summaries.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room;

    // Appliance catalogue key: references the base appliance name in appliances.json.
    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "appliance_name", nullable = false)
    private String applianceName;

    // Frontend label: optional user-friendly name such as "Kitchen fridge".
    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "custom_name")
    private String customName;

    // Usage model: either "continuous" or "perUse"; it must match appliances.json usageType.
    @Column(name = "usage_type", nullable = false)
    private String usageType;

    // Usage input: hours per day for continuous appliances such as fridges or heaters.
    @Column(name = "hours_per_day")
    private Double hoursPerDay;

    // Usage input: uses per day for per-use appliances such as kettles or washing machines.
    @Column(name = "uses_per_day")
    private Double usesPerDay;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

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
    }

    // Database lifecycle: sets created/updated timestamps before first insert.
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
    }

    // Database lifecycle: refreshes updatedAt whenever the Appliance entity changes.
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

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

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }
}
