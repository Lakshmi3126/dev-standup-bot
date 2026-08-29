package com.example.standupbot.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "team")
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String timezone;

    @Column(nullable = false)
    private LocalTime deadline;

    @Column(name = "webhook_url")
    private String webhookUrl;

    @Column(name = "slack_bot_token")
    private String slackBotToken;

    @Column(name = "slack_workspace_id")
    private String slackWorkspaceId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public String getSlackWorkspaceId() {
        return slackWorkspaceId;
    }

    public void setSlackWorkspaceId(String slackWorkspaceId) {
        this.slackWorkspaceId = slackWorkspaceId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}