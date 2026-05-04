package com.standupiq.standup_iq.service;

import com.standupiq.standup_iq.dto.GitHubActivityResponse;
import com.standupiq.standup_iq.dto.StandupResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ScheduledStandupJob {

    private final GitHubService gitHubService;
    private final StandupService standupService;
    private final SlackService slackService;

    @Value("${standup.scheduler.enabled:false}")
    private boolean schedulerEnabled;

    @Value("${standup.scheduler.username:}")
    private String username;

    @Value("${standup.scheduler.owner:}")
    private String owner;

    @Value("${standup.scheduler.repo:}")
    private String repo;

    @Value("${standup.scheduler.branch:}")
    private String branch;

    @Value("${standup.scheduler.days:1}")
    private int days;

    @Value("${standup.scheduler.send-to-slack:false}")
    private boolean sendToSlack;

    public ScheduledStandupJob(GitHubService gitHubService, StandupService standupService, SlackService slackService) {
        this.gitHubService = gitHubService;
        this.standupService = standupService;
        this.slackService = slackService;
    }

    @Scheduled(cron = "${standup.scheduler.cron}", zone = "${standup.scheduler.zone}")
    public void generateScheduledStandup() {
        if (!schedulerEnabled) {
            return;
        }

        if (!hasText(username)) {
            log.warn("Scheduled standup skipped because STANDUP_SCHEDULER_USERNAME is missing");
            return;
        }

        try {
            GitHubActivityResponse activity = gitHubService.getActivity(
                    username,
                    blankToNull(owner),
                    blankToNull(repo),
                    blankToNull(branch),
                    days
            );
            StandupResponse response = standupService.generateAndSaveStandup(username, days, activity);

            if (sendToSlack) {
                boolean delivered = slackService.sendStandup(response);
                log.info("Scheduled Slack delivery for {} completed with delivered={}", username, delivered);
            }

            log.info("Scheduled standup generated for {} with {} commits and {} PRs",
                    username, activity.totalCommits(), activity.totalPRs());
        } catch (Exception e) {
            log.warn("Scheduled standup generation failed for {}", username, e);
        }
    }

    private String blankToNull(String value) {
        return hasText(value) ? value : null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
