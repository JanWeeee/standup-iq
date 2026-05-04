package com.standupiq.standup_iq.service;

import com.standupiq.standup_iq.exception.ExternalServiceException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GitHubServiceTest {

    @Test
    void getActivityThrowsCleanErrorWhenTokenIsMissing() {
        GitHubService gitHubService = new GitHubService("https://api.github.com");
        ReflectionTestUtils.setField(gitHubService, "githubToken", "");

        assertThatThrownBy(() -> gitHubService.getActivity("JanWeeee", 1))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining("GitHub token is missing");
    }

    @Test
    void getActivityRejectsInvalidDayRangeBeforeCallingGitHub() {
        GitHubService gitHubService = new GitHubService("https://api.github.com");
        ReflectionTestUtils.setField(gitHubService, "githubToken", "test-token");

        assertThatThrownBy(() -> gitHubService.getActivity("JanWeeee", 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("days must be between 1 and 30");
    }
}
