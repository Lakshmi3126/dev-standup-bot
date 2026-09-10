package com.example.standupbot.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "blocker")
public class Blocker {

    public enum Status {
        ACTIVE,
        RESOLVED,
        UNRESOLVED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "standup_id", nullable = false)
    private Long standupId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "first_reported_at", nullable = false)
    private Instant firstReportedAt;

    @Column(name = "last_reported_at", nullable = false)
    private Instant lastReportedAt;

    @Column(name = "consecutive_days", nullable = false)
    private int consecutiveDays;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }

    public Long getStandupId() {
        return standupId;
    }

    public void setStandupId(Long standupId) {
        this.standupId = standupId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getFirstReportedAt() {
        return firstReportedAt;
    }

    public void setFirstReportedAt(Instant firstReportedAt) {
        this.firstReportedAt = firstReportedAt;
    }

    public Instant getLastReportedAt() {
        return lastReportedAt;
    }

    public void setLastReportedAt(Instant lastReportedAt) {
        this.lastReportedAt = lastReportedAt;
    }

    public int getConsecutiveDays() {
        return consecutiveDays;
    }

    public void setConsecutiveDays(int consecutiveDays) {
        this.consecutiveDays = consecutiveDays;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}