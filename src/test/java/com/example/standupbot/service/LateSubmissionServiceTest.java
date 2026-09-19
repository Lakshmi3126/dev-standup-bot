package com.example.standupbot.service;

import com.example.standupbot.dto.DailyDigestData;
import com.example.standupbot.entity.DigestLog;
import com.example.standupbot.entity.DigestLogStatus;
import com.example.standupbot.entity.Team;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LateSubmissionServiceTest {

    @Mock
    private DigestLogService digestLogService;

    @Mock
    private DailyDigestService dailyDigestService;

    @Mock
    private DigestDebounceService digestDebounceService;

    private LateSubmissionService service;

    private Team team;

    private final LocalDate date =
            LocalDate.of(2026, 9, 10);

    @BeforeEach
    void setUp() {

        service = new LateSubmissionService(
                digestLogService,
                dailyDigestService,
                digestDebounceService
        );

        team = new Team();
        team.setId(1L);
        team.setName("Engineering");
    }

    @Test
    void shouldDoNothingWhenDigestDoesNotExist() {

        when(digestLogService.findToday(1L, date))
                .thenReturn(Optional.empty());

        service.handleLateSubmission(team, date);

        verifyNoInteractions(
                dailyDigestService,
                digestDebounceService
        );
    }

    @Test
    void shouldDoNothingWhenDigestIsPending() {

        DigestLog log = mock(DigestLog.class);

        when(log.getStatus())
                .thenReturn(DigestLogStatus.PENDING);

        when(digestLogService.findToday(1L, date))
                .thenReturn(Optional.of(log));

        service.handleLateSubmission(team, date);

        verifyNoInteractions(
                dailyDigestService,
                digestDebounceService
        );
    }

    @Test
    void shouldRetryWhenDigestFailed() {

        DigestLog log = mock(DigestLog.class);

        when(log.getStatus())
                .thenReturn(DigestLogStatus.FAILED);

        DailyDigestData digest =
                new DailyDigestData(
                        1L,
                        "Engineering",
                        date,
                        List.of(),
                        List.of(),
                        List.of()
                );

        when(digestLogService.findToday(1L, date))
                .thenReturn(Optional.of(log));

        when(dailyDigestService.buildDailyDigest(team, date))
                .thenReturn(digest);

        service.handleLateSubmission(team, date);

        verify(dailyDigestService)
                .buildDailyDigest(team, date);

        verify(digestDebounceService)
                .sendFailedDigest(
                        team,
                        date,
                        digest,
                        log
                );

        verify(digestDebounceService, never())
                .scheduleUpdatedDigest(any(), any());
    }

    @Test
    void shouldScheduleUpdatedDigestWhenDigestWasSent() {

        DigestLog log = mock(DigestLog.class);

        when(log.getStatus())
                .thenReturn(DigestLogStatus.SENT);

        when(digestLogService.findToday(1L, date))
                .thenReturn(Optional.of(log));

        service.handleLateSubmission(team, date);

        verify(digestDebounceService)
                .scheduleUpdatedDigest(team, date);

        verify(digestDebounceService, never())
                .sendFailedDigest(
                        any(),
                        any(),
                        any(),
                        any()
                );

        verifyNoInteractions(dailyDigestService);
    }
}