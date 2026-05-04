package com.standupiq.standup_iq.dto;

public record HealthResponse(
        String status,
        String app,
        String message
) {
}
