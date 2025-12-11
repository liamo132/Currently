package com.currently.currently_backend.dto;

public class RoomResponse {

    private Long id;
    private String name;
    private String floorLabel;
    private String type;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
