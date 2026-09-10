package com.example.standupbot.dto;

import java.time.LocalDate;
import java.util.List;

public class DailyDigestData {

    private Long teamId;

    private String teamName;

    private LocalDate date;

    private List<MemberUpdate> onTimeSubmissions;

    private List<MemberUpdate> lateSubmissions;

    private List<String> missingMembers;

    public DailyDigestData() {
    }

    public DailyDigestData(
            Long teamId,
            String teamName,
            LocalDate date,
            List<MemberUpdate> onTimeSubmissions,
            List<MemberUpdate> lateSubmissions,
            List<String> missingMembers) {

        this.teamId = teamId;
        this.teamName = teamName;
        this.date = date;
        this.onTimeSubmissions = onTimeSubmissions;
        this.lateSubmissions = lateSubmissions;
        this.missingMembers = missingMembers;
    }

    public Long getTeamId() {
        return teamId;
    }

    public String getTeamName() {
        return teamName;
    }

    public LocalDate getDate() {
        return date;
    }

    public List<MemberUpdate> getOnTimeSubmissions() {
        return onTimeSubmissions;
    }

    public List<MemberUpdate> getLateSubmissions() {
        return lateSubmissions;
    }

    public List<String> getMissingMembers() {
        return missingMembers;
    }

    public static class MemberUpdate {

        private Long memberId;

        private String memberName;

        private String yesterday;

        private String today;

        private String blockers;

        public MemberUpdate() {
        }

        public MemberUpdate(
                Long memberId,
                String memberName,
                String yesterday,
                String today,
                String blockers) {

            this.memberId = memberId;
            this.memberName = memberName;
            this.yesterday = yesterday;
            this.today = today;
            this.blockers = blockers;
        }

        public Long getMemberId() {
            return memberId;
        }

        public String getMemberName() {
            return memberName;
        }

        public String getYesterday() {
            return yesterday;
        }

        public String getToday() {
            return today;
        }

        public String getBlockers() {
            return blockers;
        }
    }
}