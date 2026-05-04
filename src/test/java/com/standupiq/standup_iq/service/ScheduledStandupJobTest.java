package com.standupiq.standup_iq.service;

import com.standupiq.standup_iq.dto.GitHubActivityResponse;
import com.standupiq.standup_iq.dto.StandupResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduledStandupJobTest {

    @Mock
    private GitHubService gitHubService;

    @Mock
    private StandupService standupService;

    @Mock
    private SlackService slackService;

    private ScheduledStandupJob scheduledStandupJob;

    @BeforeEach
    void setUp() {
        scheduledStandupJob = new ScheduledStandupJob(gitHubService, standupService, slackService);
    }

    @Test
    void generateScheduledStandupDoesNothingWhenDisabled() {
        ReflectionTestUtils.setField(scheduledStandupJob, "schedulerEnabled", false);

        scheduledStandupJob.generateScheduledStandup();

        verifyNoInteractions(gitHubService, standupService, slackService);
    }

    @Test
    void generateScheduledStandupDoesNothingWhenUsernameIsMissing() {
        ReflectionTestUtils.setField(scheduledStandupJob, "schedulerEnabled", true);
        ReflectionTestUtils.setField(scheduledStandupJob, "username", "");

        scheduledStandupJob.generateScheduledStandup();

        verifyNoInteractions(gitHubService, standupService, slackService);
    }

    @Test
    void generateScheduledStandupFetchesActivitySavesStandupAndSendsSlack() {
        GitHubActivityResponse activity = sampleActivity();
        StandupResponse response = new StandupResponse(
                3L,
                "JanWeeee",
                1,
                LocalDateTime.of(2026, 5, 3, 8, 45),
                activity,
                "Yesterday:\nWorked on StandupIQ.\n\nToday:\nContinue deployment work.\n\nBlockers:\nNone."
        );

        ReflectionTestUtils.setField(scheduledStandupJob, "schedulerEnabled", true);
        ReflectionTestUtils.setField(scheduledStandupJob, "username", "JanWeeee");
        ReflectionTestUtils.setField(scheduledStandupJob, "owner", "JanWeeee");
        ReflectionTestUtils.setField(scheduledStandupJob, "repo", "standup-iq");
        ReflectionTestUtils.setField(scheduledStandupJob, "branch", "main");
        ReflectionTestUtils.setField(scheduledStandupJob, "days", 1);
        ReflectionTestUtils.setField(scheduledStandupJob, "sendToSlack", true);

        when(gitHubService.getActivity("JanWeeee", "JanWeeee", "standup-iq", "main", 1)).thenReturn(activity);
        when(standupService.generateAndSaveStandup("JanWeeee", 1, activity)).thenReturn(response);
        when(slackService.sendStandup(response)).thenReturn(true);

        scheduledStandupJob.generateScheduledStandup();

        verify(gitHubService).getActivity("JanWeeee", "JanWeeee", "standup-iq", "main", 1);
        verify(standupService).generateAndSaveStandup("JanWeeee", 1, activity);
        verify(slackService).sendStandup(response);
    }

    @Test
    void generateScheduledStandupSkipsSlackWhenDeliveryIsDisabled() {
        GitHubActivityResponse activity = sampleActivity();
        StandupResponse response = new StandupResponse(
                4L,
                "JanWeeee",
                1,
                LocalDateTime.of(2026, 5, 3, 8, 45),
                activity,
                "Yesterday:\nWorked on StandupIQ."
        );

        ReflectionTestUtils.setField(scheduledStandupJob, "schedulerEnabled", true);
        ReflectionTestUtils.setField(scheduledStandupJob, "username", "JanWeeee");
        ReflectionTestUtils.setField(scheduledStandupJob, "days", 1);
        ReflectionTestUtils.setField(scheduledStandupJob, "sendToSlack", false);

        when(gitHubService.getActivity("JanWeeee", null, null, null, 1)).thenReturn(activity);
        when(standupService.generateAndSaveStandup("JanWeeee", 1, activity)).thenReturn(response);

        scheduledStandupJob.generateScheduledStandup();

        verify(slackService, never()).sendStandup(response);
    }

    private GitHubActivityResponse sampleActivity() {
        return new GitHubActivityResponse(
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
    }
}
