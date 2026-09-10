package com.example.standupbot.notification;

import com.example.standupbot.dto.DailyDigestData;
import org.springframework.stereotype.Component;

/**
 * Converts notification inputs into human-readable Slack text.
 * Does not call Slack.
 */
@Component
public class SlackMessageFormatter {

    private static final String NONE = "None";

    public String formatPersonalReminder(ReminderContent reminder) {
        String name = display(reminder.memberName());
        String deadline = display(reminder.deadline());

        return "Hi " + name
                + " — your standup has not been submitted yet. The deadline is "
                + deadline + ".";
    }

    public String formatBlockerAlert(BlockerAlertContent alert) {
        return "Blocker alert — "
                + display(alert.teamName())
                + "\n"
                + display(alert.memberName())
                + " reported: "
                + display(alert.description())
                + "\n";
    }

    public String formatUnresolvedBlockerAlert(
            BlockerAlertContent alert) {

        int days = alert.consecutiveDays();

        return "Unresolved blocker — "
                + display(alert.teamName())
                + "\n"
                + display(alert.memberName())
                + " has reported the same blocker for "
                + days
                + " consecutive standup"
                + (days == 1 ? "" : "s")
                + ":\n"
                + display(alert.description())
                + "\n";
    }

    public String formatDailyDigest(
            DailyDigestData digest) {

        StringBuilder message =
                new StringBuilder();

        message.append("Daily Standup Digest — ")
                .append(display(digest.getTeamName()))
                .append(" (")
                .append(digest.getDate())
                .append(")\n\n");

        message.append("On-time submissions:\n");

        if (digest.getOnTimeSubmissions() == null
                || digest.getOnTimeSubmissions().isEmpty()) {

            message.append(NONE).append("\n");

        } else {

            for (DailyDigestData.MemberUpdate update
                    : digest.getOnTimeSubmissions()) {

                appendMemberUpdate(
                        message,
                        update
                );
            }
        }

        message.append("\nLate submissions:\n");

        if (digest.getLateSubmissions() == null
                || digest.getLateSubmissions().isEmpty()) {

            message.append(NONE).append("\n");

        } else {

            for (DailyDigestData.MemberUpdate update
                    : digest.getLateSubmissions()) {

                appendMemberUpdate(
                        message,
                        update
                );
            }
        }

        message.append("\nMissing members:\n");

        if (digest.getMissingMembers() == null
                || digest.getMissingMembers().isEmpty()) {

            message.append(NONE).append("\n");

        } else {

            for (String memberName
                    : digest.getMissingMembers()) {

                message.append("- ")
                        .append(display(memberName))
                        .append("\n");
            }
        }

        return message.toString().trim();
    }

    private static void appendMemberUpdate(
            StringBuilder message,
            DailyDigestData.MemberUpdate update) {

        message.append("- ")
                .append(display(update.getMemberName()))
                .append("\n");

        message.append("  Yesterday: ")
                .append(display(update.getYesterday()))
                .append("\n");

        message.append("  Today: ")
                .append(display(update.getToday()))
                .append("\n");

        message.append("  Blockers: ")
                .append(display(update.getBlockers()))
                .append("\n");
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String display(String value) {
        return hasText(value)
                ? value.trim()
                : NONE;
    }
}