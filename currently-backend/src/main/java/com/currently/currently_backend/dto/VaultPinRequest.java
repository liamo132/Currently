package com.currently.currently_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class VaultPinRequest {
    @NotBlank(message = "pin is required.")
    @Pattern(regexp = "^\\d{4}$", message = "pin must be exactly 4 digits.")
    private String pin;

    public VaultPinRequest() {}
    public VaultPinRequest(String pin) { this.pin = pin; }

    public String getPin() { return pin; }
    public void setPin(String pin) { this.pin = pin; }
}
