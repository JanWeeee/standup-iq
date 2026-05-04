package com.standupiq.standup_iq.service;

import com.standupiq.standup_iq.dto.GitHubActivityResponse;
import com.standupiq.standup_iq.dto.StandupResponse;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SlackServiceTest {

    @Test
    void sendStandupReturnsFalseWhenSlackIsDisabled() {
        SlackService slackService = new SlackService();
        ReflectionTestUtils.setField(slackService, "slackEnabled", false);
        ReflectionTestUtils.setField(slackService, "webhookUrl", "https://hooks.slack.com/services/test");

        assertThat(slackService.sendStandup(sampleResponse())).isFalse();
    }

    @Test
    void sendStandupReturnsFalseWhenWebhookIsMissing() {
        SlackService slackService = new SlackService();
        ReflectionTestUtils.setField(slackService, "slackEnabled", true);
        ReflectionTestUtils.setField(slackService, "webhookUrl", "");

        assertThat(slackService.sendStandup(sampleResponse())).isFalse();
    }

    private StandupResponse sampleResponse() {
        GitHubActivityResponse activity = new GitHubActivityResponse(
                "JanWeeee",
                "JanWeeee",
                "standup-iq",
                "main",
                1,
                1,
                0,
                List.of(),
                List.of(),
                "GitHub activity fetched successfully"
        );

        return new StandupResponse(
                1L,
                "JanWeeee",
                1,
                LocalDateTime.of(2026, 5, 3, 8, 45),
                activity,
                "Yesterday:\nWorked on StandupIQ.\n\nToday:\nContinue hardening the project.\n\nBlockers:\nNone."
        );
    }
}
