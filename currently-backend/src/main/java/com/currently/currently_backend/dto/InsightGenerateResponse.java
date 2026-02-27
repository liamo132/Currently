package com.currently.currently_backend.dto;

import java.util.List;

public class InsightGenerateResponse {
    private List<InsightDTO> insights;
    private String runId;
    private boolean hasMore;
    private String stopReason;

    public InsightGenerateResponse() {
    }

    public InsightGenerateResponse(List<InsightDTO> insights, String runId, boolean hasMore, String stopReason) {
        this.insights = insights;
        this.runId = runId;
        this.hasMore = hasMore;
        this.stopReason = stopReason;
    }

    public List<InsightDTO> getInsights() {
        return insights;
    }

    public void setInsights(List<InsightDTO> insights) {
        this.insights = insights;
    }

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public boolean isHasMore() {
        return hasMore;
    }

    public void setHasMore(boolean hasMore) {
        this.hasMore = hasMore;
    }

    public String getStopReason() {
        return stopReason;
    }

    public void setStopReason(String stopReason) {
        this.stopReason = stopReason;
    }
}
