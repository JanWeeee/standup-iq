package com.standupiq.standup_iq.service;

import com.standupiq.standup_iq.dto.StandupResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
public class SlackService {

    private final WebClient webClient;

    @Value("${slack.enabled:false}")
    private boolean slackEnabled;

    @Value("${slack.webhook.url:}")
    private String webhookUrl;

    public SlackService() {
        this.webClient = WebClient.builder().build();
    }

    public boolean sendStandup(StandupResponse response) {
        if (!slackEnabled) {
            log.info("Slack delivery skipped because slack.enabled=false");
            return false;
        }

        if (!hasText(webhookUrl)) {
            log.warn("Slack delivery skipped because SLACK_WEBHOOK_URL is missing");
            return false;
        }

        try {
            webClient.post()
                    .uri(webhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(new SlackMessage(formatMessage(response)))
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            log.info("Standup sent to Slack for {}", response.username());
            return true;
        } catch (Exception e) {
            log.warn("Slack delivery failed for {}", response.username(), e);
            return false;
        }
    }

    private String formatMessage(StandupResponse response) {
        int commitCount = response.activity() != null ? response.activity().totalCommits() : 0;
        int prCount = response.activity() != null ? response.activity().totalPRs() : 0;
        return "*StandupIQ for " + response.username() + "*\n"
                + "Range: last " + response.days() + " " + (response.days() == 1 ? "day" : "days") + "\n"
                + "Activity: " + commitCount + " commits, " + prCount + " PRs\n\n"
                + response.standupText();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record SlackMessage(String text) {
    }
}
