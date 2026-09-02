package com.example.standupbot.notification;

public interface NotificationService {

    void sendPersonalReminder(ReminderContent reminder);

    void sendBlockerAlert(BlockerAlertContent alert);

    void sendUnresolvedBlockerAlert(BlockerAlertContent alert);
}