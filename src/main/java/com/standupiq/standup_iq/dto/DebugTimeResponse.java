package com.standupiq.standup_iq.dto;

public record DebugTimeResponse(
        String currentTimeUtc,
        int days,
        String sinceDate,
        String username
) {
}
