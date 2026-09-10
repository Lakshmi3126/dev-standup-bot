package com.example.standupbot.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(
        name = "digest_log",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_digest_team_date",
                        columnNames = {"team_id", "digest_date"}
                )
        }
)
public class DigestLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Column(name = "digest_date", nullable = false)
    private LocalDate digestDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DigestLogStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "last_updated_at")
    private Instant lastUpdatedAt;

    @Column(name = "slack_message_ts")
    private String slackMessageTs;

    @Column(name = "update_count", nullable = false)
    private Integer updateCount = 0;

    public Long getId() {
        return id;
    }

    public Long getTeamId() {
        return teamId;
    }

    public LocalDate getDigestDate() {
        return digestDate;
    }

    public DigestLogStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public Instant getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public String getSlackMessageTs() {
        return slackMessageTs;
    }

    public Integer getUpdateCount() {
        return updateCount;
    }

    public void setStatus(DigestLogStatus status) {
        this.status = status;
    }

    public void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
    }

    public void setLastUpdatedAt(Instant lastUpdatedAt) {
        this.lastUpdatedAt = lastUpdatedAt;
    }

    public void setSlackMessageTs(String slackMessageTs) {
        this.slackMessageTs = slackMessageTs;
    }

    public void setUpdateCount(Integer updateCount) {
        this.updateCount = updateCount;
    }

    public static DigestLog pending(
            Long teamId,
            LocalDate date) {

        DigestLog log = new DigestLog();

        log.teamId = teamId;
        log.digestDate = date;
        log.status = DigestLogStatus.PENDING;
        log.createdAt = Instant.now();
        log.updateCount = 0;

        return log;
    }
}