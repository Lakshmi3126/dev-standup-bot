package com.example.standupbot.notification;

/**
 * Converts notification inputs into human-readable Slack text. Does not call Slack.
 * Daily/updated digest formatting is deferred until Person 3 publishes the digest payload contract.
 */
public class SlackMessageFormatter {

    private static final String NONE = "None";

    public String formatPersonalReminder(ReminderContent reminder) {
        String name = display(reminder.memberName());
        String deadline = display(reminder.deadline());
        return "Hi " + name + " — your standup has not been submitted yet. The deadline is "
                + deadline + ".";
    }

    public String formatBlockerAlert(BlockerAlertContent alert) {
        return "Blocker alert — " + display(alert.teamName()) + "\n"
                + display(alert.memberName()) + " reported: " + display(alert.description()) + "\n";
    }

    public String formatUnresolvedBlockerAlert(BlockerAlertContent alert) {
        int days = alert.consecutiveDays();
        return "Unresolved blocker — " + display(alert.teamName()) + "\n"
                + display(alert.memberName())
                + " has reported the same blocker for "
                + days + " consecutive standup"
                + (days == 1 ? "" : "s") + ":\n"
                + display(alert.description()) + "\n";
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String display(String value) {
        return hasText(value) ? value.trim() : NONE;
    }
}
