package com.wandile.skillswap.repository;

import com.wandile.skillswap.model.MembershipStatus;
import com.wandile.skillswap.model.SessionMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SessionMembershipRepository extends JpaRepository<SessionMembership, Long> {
    Optional<SessionMembership> findBySession_IdAndUser_Id(Long sessionId, Long userId);
    List<SessionMembership> findBySession_IdAndStatus(Long sessionId, MembershipStatus status);
    List<SessionMembership> findByUser_IdAndStatus(Long userId, MembershipStatus status);
}