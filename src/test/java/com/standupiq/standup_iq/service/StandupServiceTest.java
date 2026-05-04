package com.standupiq.standup_iq.service;

import com.standupiq.standup_iq.dto.CommitSummary;
import com.standupiq.standup_iq.dto.GitHubActivityResponse;
import com.standupiq.standup_iq.dto.StandupHistoryResponse;
import com.standupiq.standup_iq.dto.StandupResponse;
import com.standupiq.standup_iq.entity.Standup;
import com.standupiq.standup_iq.exception.ResourceNotFoundException;
import com.standupiq.standup_iq.repository.StandupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StandupServiceTest {

    @Mock
    private StandupRepository standupRepository;

    private StandupService standupService;

    @BeforeEach
    void setUp() {
        standupService = new StandupService(
                standupRepository,
                "https://generativelanguage.googleapis.com",
                "gemini-2.5-flash"
        );
        ReflectionTestUtils.setField(standupService, "geminiApiKey", "");
    }

    @Test
    void generateAndSaveStandupUsesFallbackAndPersistsSummary() {
        GitHubActivityResponse activity = sampleActivity();
        LocalDateTime generatedAt = LocalDateTime.of(2026, 5, 3, 9, 0);
        ArgumentCaptor<Standup> standupCaptor = ArgumentCaptor.forClass(Standup.class);

        when(standupRepository.save(any(Standup.class))).thenAnswer(invocation -> {
            Standup standup = invocation.getArgument(0);
            standup.setId(10L);
            standup.setGeneratedAt(generatedAt);
            return standup;
        });

        StandupResponse response = standupService.generateAndSaveStandup("JanWeeee", 1, activity);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.username()).isEqualTo("JanWeeee");
        assertThat(response.standupText()).contains("Yesterday:", "Today:", "Blockers:");
        org.mockito.Mockito.verify(standupRepository).save(standupCaptor.capture());
        assertThat(standupCaptor.getValue().getCommitCount()).isEqualTo(1);
        assertThat(standupCaptor.getValue().getPrCount()).isZero();
    }

    @Test
    void getHistoryReturnsSavedStandupsNewestFirst() {
        Standup saved = new Standup();
        saved.setId(7L);
        saved.setUsername("JanWeeee");
        saved.setStandupText("Yesterday:\nWorked on StandupIQ.");
        saved.setGeneratedAt(LocalDateTime.of(2026, 5, 3, 8, 45));
        saved.setCommitCount(2);
        saved.setPrCount(1);
        when(standupRepository.findByUsernameOrderByGeneratedAtDesc("JanWeeee")).thenReturn(List.of(saved));

        List<StandupHistoryResponse> history = standupService.getHistory("JanWeeee");

        assertThat(history).hasSize(1);
        assertThat(history.getFirst().id()).isEqualTo(7L);
        assertThat(history.getFirst().commitCount()).isEqualTo(2);
        assertThat(history.getFirst().prCount()).isEqualTo(1);
    }

    @Test
    void getHistoryThrowsWhenNoStandupsExist() {
        when(standupRepository.findByUsernameOrderByGeneratedAtDesc("unknown")).thenReturn(List.of());

        assertThatThrownBy(() -> standupService.getHistory("unknown"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No standup history found");
    }

    private GitHubActivityResponse sampleActivity() {
        return new GitHubActivityResponse(
                "JanWeeee",
                "JanWeeee",
                "standup-iq",
                "feature/phase-1-2-3-foundation",
                1,
                1,
                0,
                List.of(new CommitSummary(
                        "abc123",
                        "Add scheduler and Slack delivery",
                        "https://github.com/JanWeeee/standup-iq/commit/abc123",
                        Instant.parse("2026-05-03T03:30:00Z"),
                        "JanWeeee/standup-iq"
                )),
                List.of(),
                "GitHub activity fetched successfully"
        );
    }
}
