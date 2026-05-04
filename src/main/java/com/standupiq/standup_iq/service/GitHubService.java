package com.standupiq.standup_iq.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.standupiq.standup_iq.dto.CommitSummary;
import com.standupiq.standup_iq.dto.GitHubActivityResponse;
import com.standupiq.standup_iq.dto.PullRequestSummary;
import com.standupiq.standup_iq.exception.ExternalServiceException;
import com.standupiq.standup_iq.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
public class GitHubService {

    private static final int MAX_REPO_PAGES = 5;
    private static final int PER_PAGE = 100;

    private final WebClient webClient;

    @Value("${github.token:}")
    private String githubToken;

    public GitHubService(@Value("${github.api.base-url}") String baseUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Accept", "application/vnd.github+json")
                .build();
    }

    public GitHubActivityResponse getActivity(String username, int days) {
        return getActivity(username, null, null, null, days);
    }

    public GitHubActivityResponse getActivity(String username, String owner, String repo, String branch, int days) {
        validateToken();
        validateDays(days);

        Instant since = Instant.now().minus(days, ChronoUnit.DAYS);
        boolean hasRepoScope = hasText(owner) && hasText(repo);
        List<GitHubRepositoryResponse> repositories = hasRepoScope
                ? List.of(new GitHubRepositoryResponse(repo, owner + "/" + repo, new GitHubUser(owner), false))
                : getRepositoriesForUser(username);

        Map<String, CommitSummary> commitsBySha = new LinkedHashMap<>();
        fetchCommitSearchResults(username, since).forEach(commit -> commitsBySha.putIfAbsent(commit.sha(), commit));

        for (GitHubRepositoryResponse repository : repositories) {
            fetchRepositoryCommitsSafely(repository, username, hasRepoScope ? branch : null, since)
                    .forEach(commit -> commitsBySha.putIfAbsent(commit.sha(), commit));
        }

        Map<String, PullRequestSummary> prsByKey = new LinkedHashMap<>();
        fetchPullRequestSearchResults(username, hasRepoScope ? owner + "/" + repo : null, since)
                .forEach(pr -> prsByKey.putIfAbsent(pullRequestKey(pr), pr));

        for (GitHubRepositoryResponse repository : repositories) {
            fetchRepositoryPullRequestsSafely(repository, username, since)
                    .forEach(pr -> prsByKey.putIfAbsent(pullRequestKey(pr), pr));
        }

        List<CommitSummary> commits = commitsBySha.values().stream()
                .filter(commit -> commit.sha() != null)
                .sorted(Comparator.comparing(CommitSummary::date, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        List<PullRequestSummary> pullRequests = prsByKey.values().stream()
                .sorted(Comparator.comparing(PullRequestSummary::updatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        return new GitHubActivityResponse(
                username,
                hasRepoScope ? owner : null,
                hasRepoScope ? repo : null,
                hasRepoScope ? (hasText(branch) ? branch : "default") : null,
                days,
                commits.size(),
                pullRequests.size(),
                commits,
                pullRequests,
                "GitHub activity fetched successfully"
        );
    }

    private List<GitHubRepositoryResponse> getRepositoriesForUser(String username) {
        Map<String, GitHubRepositoryResponse> repositoriesByName = new LinkedHashMap<>();

        fetchPublicRepositories(username).forEach(repository -> repositoriesByName.putIfAbsent(repository.fullName(), repository));
        fetchAuthenticatedRepositories(username).forEach(repository -> repositoriesByName.putIfAbsent(repository.fullName(), repository));

        if (repositoriesByName.isEmpty()) {
            throw new ResourceNotFoundException("No GitHub repositories found for user: " + username);
        }

        return new ArrayList<>(repositoriesByName.values());
    }

    private List<GitHubRepositoryResponse> fetchPublicRepositories(String username) {
        List<GitHubRepositoryResponse> repositories = new ArrayList<>();
        for (int page = 1; page <= MAX_REPO_PAGES; page++) {
            int currentPage = page;
            List<GitHubRepositoryResponse> pageResults = getList(uriBuilder -> uriBuilder
                    .path("/users/{username}/repos")
                    .queryParam("type", "owner")
                    .queryParam("per_page", PER_PAGE)
                    .queryParam("page", currentPage)
                    .build(username), repositoryListType(), "fetch public repositories");

            if (pageResults.isEmpty()) {
                break;
            }
            repositories.addAll(pageResults);
        }
        return repositories;
    }

    private List<GitHubRepositoryResponse> fetchAuthenticatedRepositories(String username) {
        List<GitHubRepositoryResponse> repositories = new ArrayList<>();
        for (int page = 1; page <= MAX_REPO_PAGES; page++) {
            int currentPage = page;
            List<GitHubRepositoryResponse> pageResults = getList(uriBuilder -> uriBuilder
                    .path("/user/repos")
                    .queryParam("visibility", "all")
                    .queryParam("affiliation", "owner,collaborator,organization_member")
                    .queryParam("per_page", PER_PAGE)
                    .queryParam("page", currentPage)
                    .build(), repositoryListType(), "fetch authenticated repositories");

            if (pageResults.isEmpty()) {
                break;
            }
            pageResults.stream()
                    .filter(repository -> repository.owner() != null)
                    .filter(repository -> username.equalsIgnoreCase(repository.owner().login()))
                    .forEach(repositories::add);
        }
        return repositories;
    }

    private List<CommitSummary> fetchCommitSearchResults(String username, Instant since) {
        String query = "author:" + username + " committer-date:>" + since;
        SearchResponse<GitHubCommitResponse> response = getObject(uriBuilder -> uriBuilder
                .path("/search/commits")
                .queryParam("q", query)
                .queryParam("sort", "committer-date")
                .queryParam("order", "desc")
                .queryParam("per_page", PER_PAGE)
                .build(), commitSearchType(), "search commits");

        return response.items().stream()
                .map(commit -> toCommitSummary(commit, repositoryName(commit.repository())))
                .filter(Objects::nonNull)
                .toList();
    }

    private List<CommitSummary> fetchRepositoryCommits(
            GitHubRepositoryResponse repository,
            String username,
            String branch,
            Instant since
    ) {
        List<GitHubCommitResponse> response = getList(uriBuilder -> {
            var builder = uriBuilder
                    .path("/repos/{owner}/{repo}/commits")
                    .queryParam("author", username)
                    .queryParam("since", since)
                    .queryParam("per_page", PER_PAGE);

            if (hasText(branch)) {
                builder.queryParam("sha", branch);
            }

            return builder.build(repository.owner().login(), repository.name());
        }, commitListType(), "fetch commits for " + repository.fullName());

        return response.stream()
                .map(commit -> toCommitSummary(commit, repository.fullName()))
                .filter(Objects::nonNull)
                .toList();
    }

    private List<CommitSummary> fetchRepositoryCommitsSafely(
            GitHubRepositoryResponse repository,
            String username,
            String branch,
            Instant since
    ) {
        try {
            return fetchRepositoryCommits(repository, username, branch, since);
        } catch (ExternalServiceException e) {
            log.warn("Skipping commit lookup for {} because GitHub returned an error: {}",
                    repository.fullName(), e.getMessage());
            return List.of();
        }
    }

    private List<PullRequestSummary> fetchPullRequestSearchResults(String username, String repository, Instant since) {
        String searchQuery = "author:" + username + " type:pr updated:>" + since;
        if (hasText(repository)) {
            searchQuery = "repo:" + repository + " " + searchQuery;
        }
        String query = searchQuery;

        SearchResponse<GitHubPullRequestResponse> response = getObject(uriBuilder -> uriBuilder
                .path("/search/issues")
                .queryParam("q", query)
                .queryParam("sort", "updated")
                .queryParam("order", "desc")
                .queryParam("per_page", PER_PAGE)
                .build(), pullRequestSearchType(), "search pull requests");

        return response.items().stream()
                .map(this::toPullRequestSummary)
                .filter(Objects::nonNull)
                .toList();
    }

    private List<PullRequestSummary> fetchRepositoryPullRequests(
            GitHubRepositoryResponse repository,
            String username,
            Instant since
    ) {
        List<GitHubPullRequestResponse> response = getList(uriBuilder -> uriBuilder
                .path("/repos/{owner}/{repo}/pulls")
                .queryParam("state", "all")
                .queryParam("sort", "updated")
                .queryParam("direction", "desc")
                .queryParam("per_page", PER_PAGE)
                .build(repository.owner().login(), repository.name()), pullRequestListType(),
                "fetch pull requests for " + repository.fullName());

        return response.stream()
                .filter(pr -> pr.user() != null && username.equalsIgnoreCase(pr.user().login()))
                .filter(pr -> pr.updatedAt() != null && !pr.updatedAt().isBefore(since))
                .map(pr -> toPullRequestSummary(pr, repository.fullName()))
                .filter(Objects::nonNull)
                .toList();
    }

    private List<PullRequestSummary> fetchRepositoryPullRequestsSafely(
            GitHubRepositoryResponse repository,
            String username,
            Instant since
    ) {
        try {
            return fetchRepositoryPullRequests(repository, username, since);
        } catch (ExternalServiceException e) {
            log.warn("Skipping pull request lookup for {} because GitHub returned an error: {}",
                    repository.fullName(), e.getMessage());
            return List.of();
        }
    }

    private CommitSummary toCommitSummary(GitHubCommitResponse commit, String repository) {
        if (commit == null || commit.commit() == null) {
            return null;
        }

        GitCommitDetails details = commit.commit();
        Instant date = details.author() != null ? details.author().date() : null;
        if (date == null && details.committer() != null) {
            date = details.committer().date();
        }

        return new CommitSummary(
                commit.sha(),
                details.message(),
                commit.htmlUrl(),
                date,
                repository
        );
    }

    private PullRequestSummary toPullRequestSummary(GitHubPullRequestResponse pullRequest) {
        if (pullRequest == null) {
            return null;
        }

        String repository = null;
        if (hasText(pullRequest.repositoryUrl())) {
            repository = pullRequest.repositoryUrl().replace("https://api.github.com/repos/", "");
        }
        return toPullRequestSummary(pullRequest, repository);
    }

    private PullRequestSummary toPullRequestSummary(GitHubPullRequestResponse pullRequest, String repository) {
        if (pullRequest == null) {
            return null;
        }

        return new PullRequestSummary(
                pullRequest.number(),
                pullRequest.title(),
                pullRequest.htmlUrl(),
                pullRequest.state(),
                pullRequest.updatedAt(),
                repository
        );
    }

    private <T> T getObject(
            java.util.function.Function<org.springframework.web.util.UriBuilder, java.net.URI> uriFunction,
            ParameterizedTypeReference<T> responseType,
            String operation
    ) {
        try {
            return webClient.get()
                    .uri(uriFunction)
                    .headers(headers -> headers.setBearerAuth(githubToken))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, response -> response.bodyToMono(String.class)
                            .defaultIfEmpty("GitHub returned an error")
                            .flatMap(body -> Mono.error(new ExternalServiceException("GitHub API failed during " + operation + ": " + body))))
                    .bodyToMono(responseType)
                    .block();
        } catch (ExternalServiceException e) {
            throw e;
        } catch (Exception e) {
            log.warn("GitHub API call failed during {}", operation, e);
            throw new ExternalServiceException("GitHub API failed during " + operation);
        }
    }

    private <T> List<T> getList(
            java.util.function.Function<org.springframework.web.util.UriBuilder, java.net.URI> uriFunction,
            ParameterizedTypeReference<List<T>> responseType,
            String operation
    ) {
        List<T> response = getObject(uriFunction, responseType, operation);
        return response != null ? response : List.of();
    }

    private String repositoryName(GitHubRepositoryResponse repository) {
        return repository != null ? repository.fullName() : null;
    }

    private String pullRequestKey(PullRequestSummary pullRequest) {
        return pullRequest.repository() + "#" + pullRequest.number();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private void validateToken() {
        if (!hasText(githubToken)) {
            throw new ExternalServiceException("GitHub token is missing. Set GITHUB_TOKEN in .env or your environment.");
        }
    }

    private void validateDays(int days) {
        if (days < 1 || days > 30) {
            throw new IllegalArgumentException("days must be between 1 and 30");
        }
    }

    private ParameterizedTypeReference<List<GitHubRepositoryResponse>> repositoryListType() {
        return new ParameterizedTypeReference<>() {
        };
    }

    private ParameterizedTypeReference<List<GitHubCommitResponse>> commitListType() {
        return new ParameterizedTypeReference<>() {
        };
    }

    private ParameterizedTypeReference<SearchResponse<GitHubCommitResponse>> commitSearchType() {
        return new ParameterizedTypeReference<>() {
        };
    }

    private ParameterizedTypeReference<List<GitHubPullRequestResponse>> pullRequestListType() {
        return new ParameterizedTypeReference<>() {
        };
    }

    private ParameterizedTypeReference<SearchResponse<GitHubPullRequestResponse>> pullRequestSearchType() {
        return new ParameterizedTypeReference<>() {
        };
    }

    private record SearchResponse<T>(List<T> items) {
        private SearchResponse {
            items = items != null ? items : List.of();
        }
    }

    private record GitHubRepositoryResponse(
            String name,
            @JsonProperty("full_name") String fullName,
            GitHubUser owner,
            @JsonProperty("private") Boolean privateRepository
    ) {
    }

    private record GitHubCommitResponse(
            String sha,
            GitCommitDetails commit,
            @JsonProperty("html_url") String htmlUrl,
            GitHubRepositoryResponse repository
    ) {
    }

    private record GitCommitDetails(
            GitActor author,
            GitActor committer,
            String message
    ) {
    }

    private record GitActor(
            String name,
            String email,
            Instant date
    ) {
    }

    private record GitHubPullRequestResponse(
            Integer number,
            String title,
            @JsonProperty("html_url") String htmlUrl,
            String state,
            @JsonProperty("updated_at") Instant updatedAt,
            @JsonProperty("repository_url") String repositoryUrl,
            GitHubUser user
    ) {
    }

    private record GitHubUser(String login) {
    }
}
