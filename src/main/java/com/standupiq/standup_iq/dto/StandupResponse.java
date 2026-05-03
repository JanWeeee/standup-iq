package com.standupiq.standup_iq.dto;

import java.time.LocalDateTime;

public record StandupResponse(
        Long id,
        String username,
        int days,
        LocalDateTime generatedAt,
        GitHubActivityResponse activity,
        String standupText
) {
}
