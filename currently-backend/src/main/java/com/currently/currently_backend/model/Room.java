/*
 * File: Room.java
 * Description: JPA entity representing a room in the user's house (Map My House).
 * Author: Liam Connell
 */

package com.currently.currently_backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "rooms")
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Each room belongs to exactly one user
    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    // Display name (e.g. "Liam's Room", "Kitchen")
    @Column(nullable = false)
    private String name;

    // Floor label (e.g. "Ground Floor", "First Floor")
    @Column(nullable = false)
    private String floorLabel;

    // Optional type (e.g. "Bedroom", "Living Room", "Bathroom")
    private String type;

    public Room() {
    }

    public Room(User user, String name, String floorLabel, String type) {
        this.user = user;
        this.name = name;
        this.floorLabel = floorLabel;
        this.type = type;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFloorLabel() {
        return floorLabel;
    }

    public void setFloorLabel(String floorLabel) {
        this.floorLabel = floorLabel;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
