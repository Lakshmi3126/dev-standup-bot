package com.example.standupbot.repository;

import com.example.standupbot.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

    // Get all members belonging to a particular team.
    List<Member> findByTeamId(Long teamId);

    // Find a member only if they belong to the given team.
    Optional<Member> findByIdAndTeamId(Long memberId, Long teamId);

    // Check whether a team has at least one member.
    boolean existsByTeamId(Long teamId);
}