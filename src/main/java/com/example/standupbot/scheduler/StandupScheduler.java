package com.example.standupbot.scheduler;

import com.example.standupbot.entity.Team;
import com.example.standupbot.repository.MemberRepository;
import com.example.standupbot.repository.StandupRepository;
import com.example.standupbot.repository.TeamRepository;
import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

@Component
public class StandupScheduler {

    private final TeamRepository teamRepository;
    private final TaskScheduler taskScheduler;
    private final StandupRepository standupRepository;
    private final MemberRepository memberRepository;

    private final Map<Long, ScheduledFuture<?>> scheduledTasks =
            new ConcurrentHashMap<>();

    public StandupScheduler(
            TeamRepository teamRepository,
            TaskScheduler taskScheduler,
            StandupRepository standupRepository,
            MemberRepository memberRepository) {
        this.teamRepository = teamRepository;
        this.taskScheduler = taskScheduler;
        this.standupRepository=standupRepository;
        this.memberRepository=memberRepository;
    }

    @PostConstruct
    public void scheduleTeamDeadlines() {

        List<Team> teams = teamRepository.findAll();

        for (Team team : teams) {
            scheduleTeamDeadline(team);
        }
    }

    public void scheduleTeamDeadline(Team team) {

        ZoneId zoneId = ZoneId.of(team.getTimezone());

        ZonedDateTime now = ZonedDateTime.now(zoneId);

        LocalDate today = now.toLocalDate();
        LocalTime deadline = team.getDeadline();

        ZonedDateTime nextDeadline =
                ZonedDateTime.of(today, deadline, zoneId);

        if (!nextDeadline.isAfter(now)) {
            nextDeadline = nextDeadline.plusDays(1);
        }

        ScheduledFuture<?> existingTask =
                scheduledTasks.get(team.getId());

        if (existingTask != null) {
            existingTask.cancel(false);
        }

        ScheduledFuture<?> future =
                taskScheduler.schedule(
                        () -> processDeadline(team.getId()),
                        nextDeadline.toInstant());

        scheduledTasks.put(team.getId(), future);
    }

    public void processDeadline(Long teamId) {

        Team team = teamRepository.findById(teamId)
                .orElse(null);

        if (team == null) {
            scheduledTasks.remove(teamId);
            return;
        }

        ZoneId zoneId = ZoneId.of(team.getTimezone());

        ZonedDateTime deadline =
                ZonedDateTime.now(zoneId)
                        .with(team.getDeadline());

        LocalDate today = deadline.toLocalDate();

        List<com.example.standupbot.entity.Member> members =
                memberRepository.findByTeamId(teamId);

        List<com.example.standupbot.entity.Standup> standups =
                standupRepository
                        .findByTeamIdAndStandupDateOrderByStandupDateAsc(
                                teamId,
                                today);

        java.util.Set<Long> submittedMemberIds =
                standups.stream()
                        .filter(standup ->
                                standup.getSubmittedAt() != null
                                        && !standup.getSubmittedAt()
                                        .isAfter(deadline.toInstant()))
                        .map(com.example.standupbot.entity.Standup::getMemberId)
                        .collect(java.util.stream.Collectors.toSet());

        List<com.example.standupbot.entity.Member> missedMembers =
                members.stream()
                        .filter(member ->
                                !submittedMemberIds.contains(member.getId()))
                        .toList();

        // missedMembers contains members who did not submit
        // their standup by the team's deadline.

        scheduleTeamDeadline(team);
    }
    public void cancelTeamDeadline(Long teamId) {

        ScheduledFuture<?> existingTask =
                scheduledTasks.remove(teamId);

        if (existingTask != null) {
            existingTask.cancel(false);
        }
    }
}