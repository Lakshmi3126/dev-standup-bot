package com.example.standupbot.scheduler;

import com.example.standupbot.entity.Member;
import com.example.standupbot.entity.Team;
import com.example.standupbot.repository.TeamRepository;
import com.example.standupbot.service.StandUpAutomationService;
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

    private final TeamRepository teamRepository;
    private final StandUpAutomationService standUpAutomationService;

    /*
     * Keeps track of reminders that have already been processed.
     *
     * Key example:
     * 1-2026-09-10
     *
     * This prevents the same team's reminder from being
     * sent again on every 1-minute tick.
     */
    private final Set<String> processedReminders =
            ConcurrentHashMap.newKeySet();

    /*
     * Keeps track of deadlines that have already been processed.
     *
     * Key example:
     * 1-2026-09-10
     */
    private final Set<String> processedDeadlines =
            ConcurrentHashMap.newKeySet();

    public StandUpScheduler(
            TeamRepository teamRepository,
            StandUpAutomationService standUpAutomationService) {

        this.teamRepository = teamRepository;
        this.standUpAutomationService = standUpAutomationService;
    }

    /**
     * Single fixed poller.
     *
     * Runs once every minute.
     */
    @Scheduled(fixedRate = 60_000)
    public void pollTeams() {

        List<Team> teams = teamRepository.findAll();

        for (Team team : teams) {

            /*
             * One team's failure should not stop
             * processing of other teams.
             */
            try {
                processTeam(team);
            } catch (Exception e) {

                System.err.println(
                        "Error processing team "
                                + team.getId()
                                + ": "
                                + e.getMessage()
                );
            }
        }
    }

    /**
     * Processes one team independently.
     */
    private void processTeam(Team team) {

        ZoneId zoneId = ZoneId.of(team.getTimezone());

        /*
         * IMPORTANT:
         *
         * We calculate the current time using
         * THIS TEAM'S timezone.
         */
        ZonedDateTime now =
                ZonedDateTime.now(zoneId);

        LocalDate today =
                now.toLocalDate();

        LocalTime currentTime =
                now.toLocalTime();

        /*
         * Only process weekdays.
         */
        DayOfWeek day =
                today.getDayOfWeek();

        if (day == DayOfWeek.SATURDAY
                || day == DayOfWeek.SUNDAY) {

            return;
        }

        LocalTime deadline =
                team.getDeadline();

        /*
         * -----------------------------------------
         * 10-MINUTE REMINDER
         * -----------------------------------------
         *
         * Example:
         *
         * Deadline = 10:00
         *
         * Reminder window = 09:50 - 10:00
         */
        LocalTime reminderStart =
                deadline.minusMinutes(10);

        boolean insideReminderWindow =
                !currentTime.isBefore(reminderStart)
                        && currentTime.isBefore(deadline);

        if (insideReminderWindow) {

            processReminder(team, today);
        }

        /*
         * -----------------------------------------
         * DEADLINE
         * -----------------------------------------
         *
         * We allow a one-minute window because
         * the scheduler runs once every minute.
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

    /**
     * Finds members who have not submitted today's
     * standup.
     *
     * P4 will use the missing members to send
     * personal Slack reminders.
     */
    private void processReminder(
            Team team,
            LocalDate today) {

        String reminderKey =
                team.getId() + "-" + today;

        /*
         * Don't process the same reminder repeatedly
         * during the 10-minute window.
         */
        if (!processedReminders.add(reminderKey)) {
            return;
        }

        List<Member> missingMembers =
                standUpAutomationService.findMissingMembers(
                        team,
                        today
                );

        /*
         * Only members with a valid Slack user ID
         * are eligible for a personal Slack reminder.
         */
        List<Member> membersWithSlackId =
                missingMembers.stream()
                        .filter(member ->
                                member.getSlackUserId() != null
                                        && !member.getSlackUserId().isBlank())
                        .toList();

        System.out.println(
                "Team " + team.getName()
                        + " has "
                        + membersWithSlackId.size()
                        + " missing members for reminder."
        );

        /*
         * P4 owns NotificationService and Slack sending.
         *
         * We intentionally do NOT create another Slack client here.
         *
         * P4 should use:
         *
         * notificationService.sendPersonalMessage(
         *      team.getSlackBotToken(),
         *      member.getSlackUserId(),
         *      Message
         * );
         *
         * using ReminderContent + SlackMessageFormatter.
         */
    }

    /**
     * Processes the team's deadline.
     */
    private void processDeadline(
            Team team,
            LocalDate today) {

        String deadlineKey =
                team.getId() + "-" + today;

        /*
         * Prevent duplicate deadline processing
         * during the one-minute window.
         */
        if (!processedDeadlines.add(deadlineKey)) {
            return;
        }

        /*
         * StandUpAutomationService handles:
         *
         * - finding today's standups
         * - determining missing members
         * - building the digest
         * - creating the PENDING DigestLog
         */
        standUpAutomationService.processDeadline(
                team,
                today
        );
    }
}