package com.standupiq.standup_iq.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record StandupResponse(
        Long id,
        String username,
        int days,
        LocalDateTime generatedAt,
        GitHubActivityResponse activity,
        String standupText,
        Boolean slackDelivered
) {
    public StandupResponse(
            Long id,
            String username,
            int days,
            LocalDateTime generatedAt,
            GitHubActivityResponse activity,
            String standupText
    ) {
        this(id, username, days, generatedAt, activity, standupText, null);
    }

    public StandupResponse withSlackDelivered(Boolean delivered) {
        return new StandupResponse(id, username, days, generatedAt, activity, standupText, delivered);
    }
}
