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
import java.time.Clock;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class StandUpScheduler {

        private final Clock clock;

    private static final Logger log =
            LoggerFactory.getLogger(StandUpScheduler.class);

    private final TeamRepository teamRepository;
    private final StandUpAutomationService standUpAutomationService;
    private final NotificationService notificationService;
    private final SlackMessageFormatter slackMessageFormatter;

    // Track successful (or intentionally skipped) recipients individually.
    // A failed recipient remains eligible for the next scheduler poll.
    private final Set<String> processedReminderRecipients =
            ConcurrentHashMap.newKeySet();

    public StandUpScheduler(
            TeamRepository teamRepository,
            StandUpAutomationService standUpAutomationService,
            NotificationService notificationService,
            SlackMessageFormatter slackMessageFormatter,
            Clock clock) {

        this.teamRepository = teamRepository;
        this.standUpAutomationService = standUpAutomationService;
        this.notificationService = notificationService;
        this.slackMessageFormatter = slackMessageFormatter;
        this.clock = clock;
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
                ZonedDateTime.now(clock.withZone(zoneId));

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
         * Start reminders 10 minutes before the deadline.
         * Failed recipients remain eligible for retry until the deadline.
         */
        LocalTime reminderStart =
                deadline.minusMinutes(10);

        // Keep checking until the deadline so a failed DM can be retried
        // by the next poll without resending to members already notified.
        boolean insideReminderWindow =
                !currentTime.isBefore(reminderStart)
                        && currentTime.isBefore(deadline);

        if (insideReminderWindow) {
            processReminder(team, today);
        }

        // Keep checking after the deadline. The service skips SENT and fresh
        // PENDING digests, but can retry FAILED or abandoned PENDING digests.
        boolean deadlineReached = !currentTime.isBefore(deadline);

        if (deadlineReached) {
            processDeadline(team, today);
        }
    }

    private void processReminder(
            Team team,
            LocalDate today) {

        List<Member> missingMembers =
                standUpAutomationService.findMissingMembers(
                        team,
                        today
                );

        for (Member member : missingMembers) {
            String recipientKey =
                    team.getId() + "-" + today + "-" + member.getId();

            if (processedReminderRecipients.contains(recipientKey)) {
                continue;
            }

            if (member.getSlackUserId() == null
                    || member.getSlackUserId().isBlank()) {
                log.warn(
                        "Skipping reminder for member {} in team {}: Slack user ID is missing",
                        member.getId(),
                        team.getId()
                );
                // Missing configuration is not transient; do not retry every minute.
                processedReminderRecipients.add(recipientKey);
                continue;
            }

            try {
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

                processedReminderRecipients.add(recipientKey);
            } catch (Exception exception) {
                // One member's Slack failure must not block the rest of the batch.
                // The recipient is deliberately not marked processed, so the next
                // scheduler poll can retry while the reminder window is open.
                log.error(
                        "Failed to send stand-up reminder for team {} and member {}",
                        team.getId(),
                        member.getId(),
                        exception
                );
            }
        }
    }

    private void processDeadline(
            Team team,
            LocalDate today) {

        try {
            // The service is idempotent for SENT digests and protects fresh PENDING
            // digests. Polling after the deadline also recovers FAILED or stale
            // PENDING digests after transient failures or an application restart.
            standUpAutomationService.processDeadline(
                    team,
                    today
            );
        } catch (Exception exception) {
            log.error(
                    "Error processing deadline for team {} on {}",
                    team.getId(),
                    today,
                    exception
            );
        }
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