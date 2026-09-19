package com.example.standupbot.scheduler;

import com.example.standupbot.entity.Member;
import com.example.standupbot.entity.Team;
import com.example.standupbot.notification.NotificationService;
import com.example.standupbot.notification.ReminderContent;
import com.example.standupbot.notification.SlackMessageFormatter;
import com.example.standupbot.repository.TeamRepository;
import com.example.standupbot.service.StandUpAutomationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StandUpSchedulerTest {

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private StandUpAutomationService standUpAutomationService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private SlackMessageFormatter slackMessageFormatter;

    private StandUpScheduler scheduler;

    private Team team;

    @BeforeEach
    void setUp() {

        scheduler = new StandUpScheduler(
                teamRepository,
                standUpAutomationService,
                notificationService,
                slackMessageFormatter
        );

        team = new Team();

        team.setId(1L);
        team.setName("Engineering");
        team.setTimezone("Asia/Kolkata");
        team.setDeadline(LocalTime.of(10, 0));
        team.setSlackBotToken("bot-token");

        when(teamRepository.findByActiveTrue())
                .thenReturn(List.of(team));
    }

    @Test
    void shouldProcessReminderAtExactlyTenMinutesBeforeDeadline() {

        LocalDate date =
                LocalDate.of(2026, 9, 10);

        ZonedDateTime now =
                ZonedDateTime.of(
                        2026,
                        9,
                        10,
                        9,
                        50,
                        0,
                        0,
                        ZoneId.of("Asia/Kolkata")
                );

        Member member = new Member();

        member.setId(1L);
        member.setName("Alice");
        member.setSlackUserId("U123");

        when(standUpAutomationService.findMissingMembers(
                team,
                date
        )).thenReturn(List.of(member));

        when(slackMessageFormatter.formatPersonalReminder(
                any(ReminderContent.class)
        )).thenReturn("Standup reminder");

        try (MockedStatic<ZonedDateTime> mocked =
                     mockStatic(ZonedDateTime.class)) {

            mocked.when(() ->
                    ZonedDateTime.now(
                            ZoneId.of("Asia/Kolkata")
                    )
            ).thenReturn(now);

            scheduler.pollTeams();
        }

        verify(standUpAutomationService)
                .findMissingMembers(team, date);

        verify(slackMessageFormatter)
                .formatPersonalReminder(
                        any(ReminderContent.class)
                );

        verify(notificationService)
                .sendPersonalMessage(
                        "bot-token",
                        "U123",
                        "Standup reminder"
                );
    }

    @Test
    void shouldNotProcessReminderOutsideReminderWindow() {

        ZonedDateTime now =
                ZonedDateTime.of(
                        2026,
                        9,
                        10,
                        9,
                        40,
                        0,
                        0,
                        ZoneId.of("Asia/Kolkata")
                );

        try (MockedStatic<ZonedDateTime> mocked =
                     mockStatic(ZonedDateTime.class)) {

            mocked.when(() ->
                    ZonedDateTime.now(
                            ZoneId.of("Asia/Kolkata")
                    )
            ).thenReturn(now);

            scheduler.pollTeams();
        }

        verify(
                standUpAutomationService,
                never()
        ).findMissingMembers(any(), any());

        verify(
                notificationService,
                never()
        ).sendPersonalMessage(any(), any(), any());
    }

    @Test
    void shouldProcessDeadlineAtDeadlineTime() {

        LocalDate date =
                LocalDate.of(2026, 9, 10);

        ZonedDateTime now =
                ZonedDateTime.of(
                        2026,
                        9,
                        10,
                        10,
                        0,
                        0,
                        0,
                        ZoneId.of("Asia/Kolkata")
                );

        when(standUpAutomationService.processDeadline(
                team,
                date
        )).thenReturn(null);

        try (MockedStatic<ZonedDateTime> mocked =
                     mockStatic(ZonedDateTime.class)) {

            mocked.when(() ->
                    ZonedDateTime.now(
                            ZoneId.of("Asia/Kolkata")
                    )
            ).thenReturn(now);

            scheduler.pollTeams();
        }

        verify(standUpAutomationService)
                .processDeadline(team, date);
    }

    @Test
    void shouldNotProcessDeadlineBeforeDeadline() {

        ZonedDateTime now =
                ZonedDateTime.of(
                        2026,
                        9,
                        10,
                        9,
                        59,
                        0,
                        0,
                        ZoneId.of("Asia/Kolkata")
                );

        try (MockedStatic<ZonedDateTime> mocked =
                     mockStatic(ZonedDateTime.class)) {

            mocked.when(() ->
                    ZonedDateTime.now(
                            ZoneId.of("Asia/Kolkata")
                    )
            ).thenReturn(now);

            scheduler.pollTeams();
        }

        verify(
                standUpAutomationService,
                never()
        ).processDeadline(any(), any());
    }

    @Test
    void shouldSkipAutomationOnSaturday() {

        LocalDate saturday =
                LocalDate.of(2026, 9, 12);

        assertEquals(
                DayOfWeek.SATURDAY,
                saturday.getDayOfWeek()
        );

        ZonedDateTime now =
                ZonedDateTime.of(
                        2026,
                        9,
                        12,
                        10,
                        0,
                        0,
                        0,
                        ZoneId.of("Asia/Kolkata")
                );

        try (MockedStatic<ZonedDateTime> mocked =
                     mockStatic(ZonedDateTime.class)) {

            mocked.when(() ->
                    ZonedDateTime.now(
                            ZoneId.of("Asia/Kolkata")
                    )
            ).thenReturn(now);

            scheduler.pollTeams();
        }

        verify(
                standUpAutomationService,
                never()
        ).findMissingMembers(any(), any());

        verify(
                standUpAutomationService,
                never()
        ).processDeadline(any(), any());

        verify(
                notificationService,
                never()
        ).sendPersonalMessage(any(), any(), any());
    }

    @Test
    void shouldSkipAutomationOnSunday() {

        LocalDate sunday =
                LocalDate.of(2026, 9, 13);

        assertEquals(
                DayOfWeek.SUNDAY,
                sunday.getDayOfWeek()
        );

        ZonedDateTime now =
                ZonedDateTime.of(
                        2026,
                        9,
                        13,
                        10,
                        0,
                        0,
                        0,
                        ZoneId.of("Asia/Kolkata")
                );

        try (MockedStatic<ZonedDateTime> mocked =
                     mockStatic(ZonedDateTime.class)) {

            mocked.when(() ->
                    ZonedDateTime.now(
                            ZoneId.of("Asia/Kolkata")
                    )
            ).thenReturn(now);

            scheduler.pollTeams();
        }

        verify(
                standUpAutomationService,
                never()
        ).findMissingMembers(any(), any());

        verify(
                standUpAutomationService,
                never()
        ).processDeadline(any(), any());

        verify(
                notificationService,
                never()
        ).sendPersonalMessage(any(), any(), any());
    }

    @Test
    void shouldUseOnlyActiveTeams() {

        when(teamRepository.findByActiveTrue())
                .thenReturn(List.of(team));

        ZonedDateTime now =
                ZonedDateTime.of(
                        2026,
                        9,
                        10,
                        8,
                        0,
                        0,
                        0,
                        ZoneId.of("Asia/Kolkata")
                );

        try (MockedStatic<ZonedDateTime> mocked =
                     mockStatic(ZonedDateTime.class)) {

            mocked.when(() ->
                    ZonedDateTime.now(
                            ZoneId.of("Asia/Kolkata")
                    )
            ).thenReturn(now);

            scheduler.pollTeams();
        }

        verify(teamRepository)
                .findByActiveTrue();

        verify(teamRepository, never())
                .findAll();
    }

    @Test
    void shouldUseTeamTimezone() {

        LocalDate date =
                LocalDate.of(2026, 9, 10);

        team.setTimezone("America/New_York");
        team.setDeadline(LocalTime.of(10, 0));

        ZonedDateTime now =
                ZonedDateTime.of(
                        2026,
                        9,
                        10,
                        9,
                        50,
                        0,
                        0,
                        ZoneId.of("America/New_York")
                );

        when(standUpAutomationService.findMissingMembers(
                team,
                date
        )).thenReturn(List.of());

        try (MockedStatic<ZonedDateTime> mocked =
                     mockStatic(ZonedDateTime.class)) {

            mocked.when(() ->
                    ZonedDateTime.now(
                            ZoneId.of("America/New_York")
                    )
            ).thenReturn(now);

            scheduler.pollTeams();
        }

        verify(standUpAutomationService)
                .findMissingMembers(team, date);
    }
}