
package com.example.standupbot.service;

import com.example.standupbot.entity.Leave;
import com.example.standupbot.entity.Member;
import com.example.standupbot.entity.Team;
import com.example.standupbot.repository.MemberRepository;
import com.example.standupbot.repository.StandupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DailyDigestServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private StandupRepository standupRepository;

    @Mock
    private LeaveService leaveService;

    private DailyDigestService service;
    private Team team;

    @BeforeEach
    void setUp() {
        service = new DailyDigestService(
                memberRepository,
                standupRepository,
                leaveService
        );

        team = new Team();
        team.setId(1L);
        team.setName("Engineering");
    }

    @Test
    void buildDailyDigest_shouldShowMembersOnLeaveAndExcludeThemFromMissing() {

        Member alice = new Member();
        alice.setId(1L);
        alice.setName("Alice");

        Member bob = new Member();
        bob.setId(2L);
        bob.setName("Bob");

        Leave aliceLeave = new Leave();
        aliceLeave.setMember(alice);

        LocalDate date = LocalDate.of(2026, 9, 10);

        when(memberRepository.findByTeamId(1L))
                .thenReturn(List.of(alice, bob));

        when(standupRepository
                .findByTeamIdAndStandupDateOrderByStandupDateAsc(
                        1L, date))
                .thenReturn(List.of());

        when(leaveService.getTeamLeaves(1L, date))
                .thenReturn(List.of(aliceLeave));

        var result = service.buildDailyDigest(team, date);

        assertEquals(
                List.of("Alice"),
                result.getOnLeaveMembers()
        );

        assertEquals(
                List.of("Bob"),
                result.getMissingMembers()
        );
    }
}
