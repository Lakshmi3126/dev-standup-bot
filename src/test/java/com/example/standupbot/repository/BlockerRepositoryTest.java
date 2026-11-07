package com.example.standupbot.repository;

import com.example.standupbot.entity.Blocker;
import com.example.standupbot.entity.Standup;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
class BlockerRepositoryTest {

    @Autowired
    private BlockerRepository blockerRepository;

    @Autowired
    private StandupRepository standupRepository;

    @Test
    void shouldReturnOnlyBlockersForRequestedTeam() {
        Standup teamAStandup = createStandup(1L, 10L);
        Standup teamBStandup = createStandup(2L, 20L);

        teamAStandup = standupRepository.save(teamAStandup);
        teamBStandup = standupRepository.save(teamBStandup);

        blockerRepository.save(createBlocker(
                10L, teamAStandup.getId(), "Team A blocker"));

        blockerRepository.save(createBlocker(
                20L, teamBStandup.getId(), "Team B blocker"));

        List<Blocker> teamABlockers =
                blockerRepository.findByTeamIdOrderByLastReportedAtDesc(1L);

        assertEquals(1, teamABlockers.size());
        assertEquals("Team A blocker",
                teamABlockers.get(0).getDescription());

        List<Blocker> teamBBlockers =
                blockerRepository.findByTeamIdOrderByLastReportedAtDesc(2L);

        assertEquals(1, teamBBlockers.size());
        assertEquals("Team B blocker",
                teamBBlockers.get(0).getDescription());
    }

    @Test
    void shouldFilterBlockersByMemberAndTeam() {
        Standup teamAStandup = standupRepository.save(
                createStandup(1L, 10L));

        Standup teamBStandup = standupRepository.save(
                createStandup(2L, 10L));

        Standup anotherMemberStandup = standupRepository.save(
                createStandup(1L, 11L));
        
        blockerRepository.save(createBlocker(
                10L, teamAStandup.getId(), "Team A blocker"));

        blockerRepository.save(createBlocker(
                10L, teamBStandup.getId(), "Team B blocker"));

        blockerRepository.save(createBlocker(
                11L, anotherMemberStandup.getId(), "Another member blocker"));

        List<Blocker> result =
                blockerRepository
                        .findByMemberIdAndTeamIdOrderByLastReportedAtDesc(
                                10L, 1L);

        assertEquals(1, result.size());
        assertEquals("Team A blocker", result.get(0).getDescription());
    }

    private Standup createStandup(Long teamId, Long memberId) {
        Standup standup = new Standup();
        standup.setTeamId(teamId);
        standup.setMemberId(memberId);
        standup.setStandupDate(LocalDate.now());
        standup.setYesterday("Completed yesterday's work");
        standup.setToday("Working on today's tasks");
        standup.setBlockers("Test blocker");
        standup.setSubmittedAt(Instant.now());
        standup.setStatus(Standup.Status.ON_TIME);
        standup.setCreatedAt(Instant.now());
        return standup;
    }

    private Blocker createBlocker(
            Long memberId,
            Long standupId,
            String description) {

        Blocker blocker = new Blocker();
        blocker.setMemberId(memberId);
        blocker.setStandupId(standupId);
        blocker.setDescription(description);
        blocker.setFirstReportedAt(Instant.now());
        blocker.setLastReportedAt(Instant.now());
        blocker.setConsecutiveDays(1);
        blocker.setStatus(Blocker.Status.ACTIVE);
        return blocker;
    }
}