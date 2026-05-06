package com.currently.currently_backend.dto;

import java.util.List;

/*
 * DTO: InsightDTO
 * Purpose: Represents one Smart Insights recommendation with reasoning, action, confidence, and Savings values.
 */
public class InsightDTO {
    private String title;
    private String reasoning;
    private String action;
    private Double impactWeekly;
    private Double impactMonthly;
    private String confidence;
    private String category;
    private List<String> references;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getReasoning() {
        return reasoning;
    }

    public void setReasoning(String reasoning) {
        this.reasoning = reasoning;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Double getImpactWeekly() {
        return impactWeekly;
    }

    public void setImpactWeekly(Double impactWeekly) {
        this.impactWeekly = impactWeekly;
    }

    public Double getImpactMonthly() {
        return impactMonthly;
    }

    public void setImpactMonthly(Double impactMonthly) {
        this.impactMonthly = impactMonthly;
    }

    public String getConfidence() {
        return confidence;
    }

    public void setConfidence(String confidence) {
        this.confidence = confidence;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public List<String> getReferences() {
        return references;
    }

    public void setReferences(List<String> references) {
        this.references = references;
    }
}
