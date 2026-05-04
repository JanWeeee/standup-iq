package com.standupiq.standup_iq.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PullRequestSummary(
        Integer number,
        String title,
        String url,
        String state,
        Instant updatedAt,
        String repository
) {
}
