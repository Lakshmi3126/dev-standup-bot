package com.example.standupbot.scheduler;

import com.example.standupbot.entity.Member;
import com.example.standupbot.entity.Team;
import com.example.standupbot.notification.NotificationService;
import com.example.standupbot.notification.ReminderContent;
import com.example.standupbot.notification.SlackMessageFormatter;
import com.example.standupbot.repository.TeamRepository;
import com.example.standupbot.service.StandUpAutomationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class StandUpScheduler {

    private static final Logger log =
            LoggerFactory.getLogger(StandUpScheduler.class);

    private final TeamRepository teamRepository;
    private final StandUpAutomationService standUpAutomationService;
    private final NotificationService notificationService;
    private final SlackMessageFormatter slackMessageFormatter;

    private final Set<String> processedReminders =
            ConcurrentHashMap.newKeySet();

    private final Set<String> processedDeadlines =
            ConcurrentHashMap.newKeySet();

    public StandUpScheduler(
            TeamRepository teamRepository,
            StandUpAutomationService standUpAutomationService,
            NotificationService notificationService,
            SlackMessageFormatter slackMessageFormatter) {

        this.teamRepository = teamRepository;
        this.standUpAutomationService = standUpAutomationService;
        this.notificationService = notificationService;
        this.slackMessageFormatter = slackMessageFormatter;
    }

    @Scheduled(fixedRate = 60_000)
    public void pollTeams() {

        List<Team> teams =
                teamRepository.findByActiveTrue();

        for (Team team : teams) {
            try {
                processTeam(team);
            } catch (Exception e) {
                log.error(
                        "Error processing team {}",
                        team.getId(),
                        e
                );
            }
        }
    }

    private void processTeam(Team team) {

        ZoneId zoneId =
                ZoneId.of(team.getTimezone());

        ZonedDateTime now =
                ZonedDateTime.now(zoneId);

        LocalDate today =
                now.toLocalDate();

        LocalTime currentTime =
                now.toLocalTime();

        DayOfWeek day =
                today.getDayOfWeek();

        if (day == DayOfWeek.SATURDAY
                || day == DayOfWeek.SUNDAY) {
            return;
        }

        LocalTime deadline =
                team.getDeadline();

        /*
         * Reminder should run only during the
         * single 1-minute scheduler tick that
         * starts exactly 10 minutes before deadline.
         */
        LocalTime reminderStart =
                deadline.minusMinutes(10);

        LocalTime reminderEnd =
                reminderStart.plusMinutes(1);

        boolean insideReminderWindow =
                !currentTime.isBefore(reminderStart)
                        && currentTime.isBefore(reminderEnd);

        if (insideReminderWindow) {
            processReminder(team, today);
        }

        /*
         * Deadline processing should run only
         * during the single 1-minute tick starting
         * exactly at the deadline.
         */
        LocalTime deadlineEnd =
                deadline.plusMinutes(1);

        boolean insideDeadlineWindow =
                !currentTime.isBefore(deadline)
                        && currentTime.isBefore(deadlineEnd);

        if (insideDeadlineWindow) {
            processDeadline(team, today);
        }
    }

    private void processReminder(
            Team team,
            LocalDate today) {

        String reminderKey =
                team.getId() + "-" + today;

        /*
         * Prevent duplicate reminders for the
         * same team on the same day.
         */
        if (!processedReminders.add(reminderKey)) {
            return;
        }

        List<Member> missingMembers =
                standUpAutomationService.findMissingMembers(
                        team,
                        today
                );

        for (Member member : missingMembers) {

            if (member.getSlackUserId() == null
                    || member.getSlackUserId().isBlank()) {
                continue;
            }

            ReminderContent reminderContent =
                    new ReminderContent(
                            member.getName(),
                            team.getDeadline().toString()
                    );

            String message =
                    slackMessageFormatter.formatPersonalReminder(
                            reminderContent
                    );

            notificationService.sendPersonalMessage(
                    team.getSlackBotToken(),
                    member.getSlackUserId(),
                    message
            );
        }
    }

    private void processDeadline(
            Team team,
            LocalDate today) {

        String deadlineKey =
                team.getId() + "-" + today;

        /*
         * Prevent duplicate deadline processing
         * for the same team on the same day.
         */
        if (!processedDeadlines.add(deadlineKey)) {
            return;
        }

        standUpAutomationService.processDeadline(
                team,
                today
        );
    }

    /*
     * TeamService still calls this method.
     *
     * Actual deadline scheduling is handled by
     * the fixed 1-minute poller above.
     */
    public void scheduleTeamDeadline(Team team) {
        // No individual task is required.
    }

    /*
     * TeamService still calls this method.
     *
     * Inactive teams are automatically excluded
     * by findByActiveTrue().
     */
    public void cancelTeamDeadline(Long teamId) {
        // No individual task is required.
    }
}