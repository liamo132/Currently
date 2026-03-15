package com.currently.currently_backend.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

public class EnergySettingsRequest {
    @DecimalMin(value = "0.01", message = "pricePerKwh must be greater than 0.")
    @DecimalMax(value = "5.0", message = "pricePerKwh is outside expected range.")
    private Double pricePerKwh;
    @Size(max = 100, message = "providerName must be 100 characters or fewer.")
    private String providerName;

    public Double getPricePerKwh() {
        return pricePerKwh;
    }

    public void setPricePerKwh(Double pricePerKwh) {
        this.pricePerKwh = pricePerKwh;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }
}
