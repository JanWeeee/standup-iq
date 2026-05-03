package com.standupiq.standup_iq.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.standupiq.standup_iq.dto.CommitSummary;
import com.standupiq.standup_iq.dto.GitHubActivityResponse;
import com.standupiq.standup_iq.dto.PullRequestSummary;
import com.standupiq.standup_iq.dto.StandupHistoryResponse;
import com.standupiq.standup_iq.dto.StandupResponse;
import com.standupiq.standup_iq.entity.Standup;
import com.standupiq.standup_iq.exception.ResourceNotFoundException;
import com.standupiq.standup_iq.repository.StandupRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Slf4j
@Service
public class StandupService {

    private final StandupRepository standupRepository;
    private final WebClient geminiClient;
    private final String model;

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    public StandupService(
            StandupRepository standupRepository,
            @Value("${gemini.api.base-url}") String geminiApiBaseUrl,
            @Value("${gemini.model}") String model
    ) {
        this.standupRepository = standupRepository;
        this.geminiClient = WebClient.builder().baseUrl(geminiApiBaseUrl).build();
        this.model = model;
    }

    public StandupResponse generateAndSaveStandup(String username, int days, GitHubActivityResponse activity) {
        String standupText = generateStandupText(username, days, activity);

        Standup standup = new Standup();
        standup.setUsername(username);
        standup.setStandupText(standupText);
        standup.setCommitCount(activity.totalCommits());
        standup.setPrCount(activity.totalPRs());
        Standup savedStandup = standupRepository.save(standup);

        return new StandupResponse(
                savedStandup.getId(),
                username,
                days,
                savedStandup.getGeneratedAt(),
                activity,
                standupText
        );
    }

    public List<StandupHistoryResponse> getHistory(String username) {
        List<StandupHistoryResponse> history = standupRepository.findByUsernameOrderByGeneratedAtDesc(username).stream()
                .map(this::toHistoryResponse)
                .toList();

        if (history.isEmpty()) {
            throw new ResourceNotFoundException("No standup history found for user: " + username);
        }

        return history;
    }

    private String generateStandupText(String username, int days, GitHubActivityResponse activity) {
        if (!hasText(geminiApiKey)) {
            log.warn("Gemini API key is missing. Returning fallback standup for {}", username);
            return fallbackStandup(activity);
        }

        try {
            GeminiRequest request = new GeminiRequest(
                    new GeminiSystemInstruction(List.of(new GeminiPart(
                            "You generate concise, professional daily standups for software developers. "
                                    + "Use exactly these section labels: Yesterday, Today, Blockers. "
                                    + "Write clean natural language, not bullet points."
                    ))),
                    List.of(new GeminiContent(
                            "user",
                            List.of(new GeminiPart(buildPrompt(username, days, activity)))
                    )),
                    new GeminiGenerationConfig(800, 0.35)
            );

            GeminiResponse response = geminiClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1beta/models/{model}:generateContent")
                            .build(model))
                    .header("x-goog-api-key", geminiApiKey)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(GeminiResponse.class)
                    .block();

            String text = extractText(response);
            if (hasText(text)) {
                return text.trim();
            }
        } catch (Exception e) {
            log.warn("Gemini standup generation failed for {}", username, e);
        }

        return fallbackStandup(activity);
    }

    private String buildPrompt(String username, int days, GitHubActivityResponse activity) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Generate a standup for developer ").append(username)
                .append(" using the last ").append(days).append(days == 1 ? " day" : " days")
                .append(" of GitHub activity.\n\n");

        prompt.append("Commits:\n");
        if (activity.commits().isEmpty()) {
            prompt.append("No commits found.\n");
        } else {
            for (CommitSummary commit : activity.commits()) {
                prompt.append("- Repo: ").append(nullToUnknown(commit.repository()))
                        .append(" | Message: ").append(nullToUnknown(commit.message()))
                        .append(" | Date: ").append(commit.date())
                        .append("\n");
            }
        }

        prompt.append("\nPull requests:\n");
        if (activity.pullRequests().isEmpty()) {
            prompt.append("No pull requests found.\n");
        } else {
            for (PullRequestSummary pr : activity.pullRequests()) {
                prompt.append("- Repo: ").append(nullToUnknown(pr.repository()))
                        .append(" | Title: ").append(nullToUnknown(pr.title()))
                        .append(" | State: ").append(nullToUnknown(pr.state()))
                        .append(" | Updated: ").append(pr.updatedAt())
                        .append("\n");
            }
        }

        prompt.append("\nReturn exactly this format:\n")
                .append("Yesterday:\n")
                .append("<natural language summary of completed work>\n\n")
                .append("Today:\n")
                .append("<natural language summary of what the developer will likely continue>\n\n")
                .append("Blockers:\n")
                .append("<None if no blockers are visible from the activity>");

        return prompt.toString();
    }

    private String fallbackStandup(GitHubActivityResponse activity) {
        String yesterday = activity.totalCommits() == 0 && activity.totalPRs() == 0
                ? "No GitHub activity was found for this period."
                : "Worked on " + activity.totalCommits() + " commit"
                + plural(activity.totalCommits()) + " and " + activity.totalPRs() + " pull request"
                + plural(activity.totalPRs()) + " across GitHub activity.";

        return "Yesterday:\n"
                + yesterday
                + "\n\nToday:\n"
                + "Continue the in-progress development work and follow up on any related repository changes.\n\n"
                + "Blockers:\n"
                + "None.";
    }

    private StandupHistoryResponse toHistoryResponse(Standup standup) {
        return new StandupHistoryResponse(
                standup.getId(),
                standup.getUsername(),
                standup.getStandupText(),
                standup.getGeneratedAt(),
                standup.getCommitCount(),
                standup.getPrCount()
        );
    }

    private String extractText(GeminiResponse response) {
        if (response == null || response.candidates() == null) {
            return null;
        }

        return response.candidates().stream()
                .filter(candidate -> candidate.content() != null && candidate.content().parts() != null)
                .flatMap(candidate -> candidate.content().parts().stream())
                .map(GeminiPart::text)
                .filter(this::hasText)
                .findFirst()
                .orElse(null);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String nullToUnknown(String value) {
        return hasText(value) ? value : "Unknown";
    }

    private String plural(int count) {
        return count == 1 ? "" : "s";
    }

    private record GeminiRequest(
            @JsonProperty("system_instruction") GeminiSystemInstruction systemInstruction,
            List<GeminiContent> contents,
            @JsonProperty("generationConfig") GeminiGenerationConfig generationConfig
    ) {
    }

    private record GeminiSystemInstruction(
            List<GeminiPart> parts
    ) {
    }

    private record GeminiContent(
            String role,
            List<GeminiPart> parts
    ) {
    }

    private record GeminiPart(
            String text
    ) {
    }

    private record GeminiGenerationConfig(
            @JsonProperty("maxOutputTokens") int maxOutputTokens,
            double temperature
    ) {
    }

    private record GeminiResponse(
            List<GeminiCandidate> candidates
    ) {
    }

    private record GeminiCandidate(
            GeminiContent content
    ) {
    }
}
