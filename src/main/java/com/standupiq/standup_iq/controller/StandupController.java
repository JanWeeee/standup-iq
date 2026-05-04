package com.standupiq.standup_iq.controller;

import com.standupiq.standup_iq.dto.GitHubActivityResponse;
import com.standupiq.standup_iq.dto.StandupHistoryResponse;
import com.standupiq.standup_iq.dto.StandupResponse;
import com.standupiq.standup_iq.service.GitHubService;
import com.standupiq.standup_iq.service.SlackService;
import com.standupiq.standup_iq.service.StandupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "Standups", description = "Generate, store, and retrieve AI standups")
@RestController
@RequestMapping("/api/standup")
public class StandupController {

    private final GitHubService gitHubService;
    private final StandupService standupService;
    private final SlackService slackService;

    public StandupController(GitHubService gitHubService, StandupService standupService, SlackService slackService) {
        this.gitHubService = gitHubService;
        this.standupService = standupService;
        this.slackService = slackService;
    }

    @Operation(
            summary = "Generate a standup",
            description = "Fetches GitHub activity, generates a Gemini standup, saves it, and optionally sends it to Slack."
    )
    @GetMapping("/generate/{username}")
    public ResponseEntity<StandupResponse> generateStandup(
            @PathVariable String username,
            @RequestParam(defaultValue = "1") int days,
            @RequestParam(required = false) String owner,
            @RequestParam(required = false) String repo,
            @RequestParam(required = false) String branch,
            @RequestParam(defaultValue = "false") boolean sendToSlack
    ) {
        GitHubActivityResponse activity = gitHubService.getActivity(username, owner, repo, branch, days);
        StandupResponse response = standupService.generateAndSaveStandup(username, days, activity);
        if (sendToSlack) {
            response = response.withSlackDelivered(slackService.sendStandup(response));
        }
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get standup history", description = "Returns saved standups for a GitHub username.")
    @GetMapping("/history/{username}")
    public ResponseEntity<List<StandupHistoryResponse>> getHistory(@PathVariable String username) {
        return ResponseEntity.ok(standupService.getHistory(username));
    }
}
