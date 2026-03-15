package com.currently.currently_backend.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

public class InsightGenerateRequest {
    @DecimalMin(value = "0.01", message = "pricePerKwh must be greater than 0.")
    @DecimalMax(value = "5.0", message = "pricePerKwh is outside expected range.")
    private Double pricePerKwh;

    public Double getPricePerKwh() {
        return pricePerKwh;
    }

    public void setPricePerKwh(Double pricePerKwh) {
        this.pricePerKwh = pricePerKwh;
    }
}
