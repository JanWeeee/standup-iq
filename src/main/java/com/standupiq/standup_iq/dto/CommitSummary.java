package com.standupiq.standup_iq.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CommitSummary(
        String sha,
        String message,
        String url,
        Instant date,
        String repository
) {
}
