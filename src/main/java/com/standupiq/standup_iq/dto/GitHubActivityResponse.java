package com.standupiq.standup_iq.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GitHubActivityResponse(
        String username,
        String owner,
        String repo,
        String branch,
        int days,
        int totalCommits,
        int totalPRs,
        List<CommitSummary> commits,
        List<PullRequestSummary> pullRequests,
        String message
) {
}
