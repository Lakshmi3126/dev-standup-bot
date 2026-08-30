package com.example.standupbot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public class CreateTeamRequest {

    @NotBlank(message = "Team name is required")
    private String name;

    @NotBlank(message = "Timezone is required")
    private String timezone;

    @NotNull(message = "Deadline is required")
    private LocalTime deadline;

    @NotBlank(message = "Webhook URL is required")
    private String webhookUrl;

    @NotBlank(message = "Slack bot token is required")
    private String slackBotToken;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public LocalTime getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalTime deadline) {
        this.deadline = deadline;
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public String getSlackBotToken() {
        return slackBotToken;
    }

    public void setSlackBotToken(String slackBotToken) {
        this.slackBotToken = slackBotToken;
    }
}