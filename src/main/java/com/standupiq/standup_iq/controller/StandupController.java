package com.standupiq.standup_iq.controller;

import com.standupiq.standup_iq.dto.GitHubActivityResponse;
import com.standupiq.standup_iq.dto.StandupHistoryResponse;
import com.standupiq.standup_iq.dto.StandupResponse;
import com.standupiq.standup_iq.service.GitHubService;
import com.standupiq.standup_iq.service.StandupService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/standup")
public class StandupController {

    private final GitHubService gitHubService;
    private final StandupService standupService;

    public StandupController(GitHubService gitHubService, StandupService standupService) {
        this.gitHubService = gitHubService;
        this.standupService = standupService;
    }

    @GetMapping("/generate/{username}")
    public ResponseEntity<StandupResponse> generateStandup(
            @PathVariable String username,
            @RequestParam(defaultValue = "1") int days,
            @RequestParam(required = false) String owner,
            @RequestParam(required = false) String repo,
            @RequestParam(required = false) String branch
    ) {
        GitHubActivityResponse activity = gitHubService.getActivity(username, owner, repo, branch, days);
        return ResponseEntity.ok(standupService.generateAndSaveStandup(username, days, activity));
    }

    @GetMapping("/history/{username}")
    public ResponseEntity<List<StandupHistoryResponse>> getHistory(@PathVariable String username) {
        return ResponseEntity.ok(standupService.getHistory(username));
    }
}
