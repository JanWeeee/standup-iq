package com.standupiq.standup_iq.dto;

import java.time.LocalDateTime;

public record StandupHistoryResponse(
        Long id,
        String username,
        String standupText,
        LocalDateTime generatedAt,
        int commitCount,
        int prCount
) {
}
