package com.currently.currently_backend.dto;

import jakarta.validation.constraints.Size;

public class RoomRequest {

    @Size(max = 80, message = "Room name must be 80 characters or fewer.")
    private String name;
    @Size(max = 80, message = "Floor label must be 80 characters or fewer.")
    private String floorLabel;
    @Size(max = 60, message = "Room type must be 60 characters or fewer.")
    private String type;

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
