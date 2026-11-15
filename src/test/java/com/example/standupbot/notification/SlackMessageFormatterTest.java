package com.example.standupbot.notification;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.standupbot.dto.DailyDigestData;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SlackMessageFormatterTest {

    @Test
void dailyDigestShouldIncludeOnLeaveMembers() {

    DailyDigestData digest = new DailyDigestData(
            1L,
            "Engineering",
            LocalDate.of(2026, 9, 10),
            List.of(),
            List.of(),
            List.of("Bob"),
            List.of("Alice")
    );

    String message = formatter.formatDailyDigest(digest);

    assertTrue(message.contains("On leave:"));
    assertTrue(message.contains("Alice"));
    assertTrue(message.contains("Bob"));
}

    private SlackMessageFormatter formatter;

    @BeforeEach
    void setUp() {
        formatter = new SlackMessageFormatter();
    }

    @Test
    void personalReminderIncludesMemberNameAndDeadline() {
        String message = formatter.formatPersonalReminder(new ReminderContent("Lavanya", "10:00 AM"));

        assertTrue(message.contains("Lavanya"));
        assertTrue(message.contains("10:00 AM"));
        assertTrue(message.contains("has not been submitted yet"));
        assertFalse(message.contains("{"));
    }

    @Test
    void blockerAlertHighlightsMemberAndDescription() {
        String message = formatter.formatBlockerAlert(new BlockerAlertContent(
                "Backend Team", "Lavanya", "Waiting for database credentials", 1));

        assertTrue(message.contains("Blocker alert — Backend Team"));
        assertTrue(message.contains("Lavanya reported: Waiting for database credentials"));
        assertFalse(message.contains("Unresolved blocker"));
        assertFalse(message.contains("{"));
    }

    @Test
    void unresolvedBlockerAlertStatesConsecutiveStreak() {
        String message = formatter.formatUnresolvedBlockerAlert(new BlockerAlertContent(
                "Backend Team", "Lavanya", "Waiting for database credentials", 3));

        assertTrue(message.contains("Unresolved blocker — Backend Team"));
        assertTrue(message.contains("3 consecutive standups"));
        assertTrue(message.contains("Waiting for database credentials"));
        assertFalse(message.contains("Blocker alert —"));
    }
}
