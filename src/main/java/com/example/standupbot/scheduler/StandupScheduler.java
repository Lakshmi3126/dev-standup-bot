package com.example.standupbot.scheduler;

import com.example.standupbot.entity.Member;
import com.example.standupbot.entity.Standup;
import com.example.standupbot.entity.Team;
import com.example.standupbot.repository.MemberRepository;
import com.example.standupbot.repository.StandupRepository;
import com.example.standupbot.repository.TeamRepository;
import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;
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
        this.standupRepository = standupRepository;
        this.memberRepository = memberRepository;
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

        // Reminder should happen 10 minutes before the deadline.
        ZonedDateTime reminderTime =
                nextDeadline.minusMinutes(10);

        ScheduledFuture<?> existingTask =
                scheduledTasks.get(team.getId());

        if (existingTask != null) {
            existingTask.cancel(false);
        }

        ScheduledFuture<?> future =
                taskScheduler.schedule(
                        () -> processReminder(team.getId()),
                        reminderTime.toInstant());

        scheduledTasks.put(team.getId(), future);
    }

    public void processReminder(Long teamId) {

        Team team = teamRepository.findById(teamId)
                .orElse(null);

        if (team == null) {
            scheduledTasks.remove(teamId);
            return;
        }

        ZoneId zoneId = ZoneId.of(team.getTimezone());

        ZonedDateTime now =
                ZonedDateTime.now(zoneId);

        LocalDate today = now.toLocalDate();

        List<Member> members =
                memberRepository.findByTeamId(teamId);

        List<Standup> standups =
                standupRepository
                        .findByTeamIdAndStandupDateOrderByStandupDateAsc(
                                teamId,
                                today);

        Set<Long> submittedMemberIds =
                standups.stream()
                        .filter(standup ->
                                standup.getSubmittedAt() != null)
                        .map(Standup::getMemberId)
                        .collect(Collectors.toSet());

        List<Member> pendingMembers =
                members.stream()
                        .filter(member ->
                                !submittedMemberIds.contains(member.getId()))
                        .toList();


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