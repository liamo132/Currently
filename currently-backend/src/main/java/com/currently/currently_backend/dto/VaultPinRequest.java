package com.currently.currently_backend.dto;

public class VaultPinRequest {
    private String pin;

    public VaultPinRequest() {}
    public VaultPinRequest(String pin) { this.pin = pin; }

    public String getPin() { return pin; }
    public void setPin(String pin) { this.pin = pin; }
}
