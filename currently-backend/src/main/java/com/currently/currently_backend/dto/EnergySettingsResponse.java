package com.currently.currently_backend.dto;

public class EnergySettingsResponse {
    private Double pricePerKwh;
    private String providerName;

    public EnergySettingsResponse() {}

    public EnergySettingsResponse(Double pricePerKwh, String providerName) {
        this.pricePerKwh = pricePerKwh;
        this.providerName = providerName;
    }

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
