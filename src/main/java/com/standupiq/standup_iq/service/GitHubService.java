package com.standupiq.standup_iq.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Service
public class GitHubService {

    private final WebClient webClient;

    @Value("${github.token}")
    private String githubToken;

    public GitHubService(@Value("${github.api.base-url}") String baseUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Accept", "application/vnd.github+json")
                .build();
    }

    public List<Map> getUserCommits(String username) {
        try {
            String since = sinceHoursAgo(72);
            String query = "author:" + username + " committer-date:>" + since;

            Map response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search/commits")
                            .queryParam("q", query)
                            .queryParam("sort", "committer-date")
                            .queryParam("order", "desc")
                            .build())
                    .header("Authorization", "Bearer " + githubToken)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.get("items") != null) {
                return (List<Map>) response.get("items");
            }
            return List.of();
        } catch (Exception e) {
            return List.of();
        }
    }

    public List<Map> getUserCommits(String owner, String repo, String branch, String username) {
        try {
            String since = sinceHoursAgo(72);

            List response = webClient.get()
                    .uri(uriBuilder -> {
                        var builder = uriBuilder
                                .path("/repos/{owner}/{repo}/commits")
                                .queryParam("author", username)
                                .queryParam("since", since)
                                .queryParam("per_page", 100);

                        if (branch != null && !branch.isBlank()) {
                            builder.queryParam("sha", branch);
                        }

                        return builder.build(owner, repo);
                    })
                    .header("Authorization", "Bearer " + githubToken)
                    .retrieve()
                    .bodyToMono(List.class)
                    .block();

            if (response != null) {
                return response;
            }
            return List.of();
        } catch (Exception e) {
            return List.of();
        }
    }

    public List<Map> getUserPullRequests(String username) {
        try {
            String since = sinceHoursAgo(24);
            String query = "author:" + username + " type:pr updated:>" + since;

            Map response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search/issues")
                            .queryParam("q", query)
                            .queryParam("sort", "updated")
                            .queryParam("order", "desc")
                            .build())
                    .header("Authorization", "Bearer " + githubToken)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.get("items") != null) {
                return (List<Map>) response.get("items");
            }
            return List.of();
        } catch (Exception e) {
            return List.of();
        }
    }

    public List<Map> getUserPullRequests(String owner, String repo, String username) {
        try {
            String since = sinceHoursAgo(24);
            String query = "repo:" + owner + "/" + repo + " author:" + username + " type:pr updated:>" + since;

            Map response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search/issues")
                            .queryParam("q", query)
                            .queryParam("sort", "updated")
                            .queryParam("order", "desc")
                            .build())
                    .header("Authorization", "Bearer " + githubToken)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.get("items") != null) {
                return (List<Map>) response.get("items");
            }
            return List.of();
        } catch (Exception e) {
            return List.of();
        }
    }

    private String sinceHoursAgo(long hours) {
        return Instant.now().minus(hours, ChronoUnit.HOURS).toString();
    }
}
