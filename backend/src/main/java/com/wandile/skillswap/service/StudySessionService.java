package com.wandile.skillswap.service;

import com.wandile.skillswap.dto.CreateSessionRequest;
import com.wandile.skillswap.dto.MembershipResponse;
import com.wandile.skillswap.dto.SessionResponse;
import com.wandile.skillswap.model.*;
import com.wandile.skillswap.repository.SessionMembershipRepository;
import com.wandile.skillswap.repository.StudySessionRepository;
import com.wandile.skillswap.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StudySessionService {

    private final StudySessionRepository sessionRepository;
    private final SessionMembershipRepository membershipRepository;
    private final UserRepository userRepository;

    public StudySessionService(StudySessionRepository sessionRepository,
                               SessionMembershipRepository membershipRepository,
                               UserRepository userRepository) {
        this.sessionRepository = sessionRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
    }

    public SessionResponse createSession(Long hostId, CreateSessionRequest request) {
        User host = userRepository.findById(hostId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        StudySession session = sessionRepository.save(new StudySession(
                host, request.getTitle(), request.getTopic(), request.getDescription(),
                request.getVisibility(), request.getType()));

        membershipRepository.save(new SessionMembership(session, host, MembershipRole.HOST, MembershipStatus.APPROVED));

        return toSessionResponse(session);
    }

    public List<SessionResponse> listPublicSessions() {
        return sessionRepository.findByVisibilityOrderByCreatedAtDesc(SessionVisibility.PUBLIC)
                .stream().map(this::toSessionResponse).toList();
    }

    public List<SessionResponse> listMySessions(Long userId) {
        return membershipRepository.findByUser_IdAndStatus(userId, MembershipStatus.APPROVED)
                .stream().map(m -> toSessionResponse(m.getSession())).toList();
    }

    public MembershipResponse requestToJoin(Long sessionId, Long userId) {
        StudySession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        membershipRepository.findBySession_IdAndUser_Id(sessionId, userId).ifPresent(m -> {
            throw new IllegalArgumentException("You already have a membership for this session");
        });

        SessionMembership membership = membershipRepository.save(
                new SessionMembership(session, user, MembershipRole.MEMBER, MembershipStatus.PENDING));
        return toMembershipResponse(membership);
    }

    public MembershipResponse decideRequest(Long sessionId, Long targetUserId, Long actingUserId, boolean approve) {
        requireHostOrAdmin(sessionId, actingUserId);

        SessionMembership membership = membershipRepository.findBySession_IdAndUser_Id(sessionId, targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("Membership not found"));

        membership.setStatus(approve ? MembershipStatus.APPROVED : MembershipStatus.DECLINED);
        return toMembershipResponse(membershipRepository.save(membership));
    }

    public void leaveSession(Long sessionId, Long userId) {
        SessionMembership membership = membershipRepository.findBySession_IdAndUser_Id(sessionId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Membership not found"));
        membershipRepository.delete(membership);
    }

    public List<MembershipResponse> listPendingRequests(Long sessionId, Long actingUserId) {
        requireHostOrAdmin(sessionId, actingUserId);
        return membershipRepository.findBySession_IdAndStatus(sessionId, MembershipStatus.PENDING)
                .stream().map(this::toMembershipResponse).toList();
    }

    private void requireHostOrAdmin(Long sessionId, Long actingUserId) {
        SessionMembership actingMembership = membershipRepository.findBySession_IdAndUser_Id(sessionId, actingUserId)
                .orElseThrow(() -> new IllegalArgumentException("You are not a member of this session"));
        if (actingMembership.getRole() != MembershipRole.HOST && actingMembership.getRole() != MembershipRole.ADMIN) {
            throw new IllegalArgumentException("Only the host or an admin can do this");
        }
    }

    private SessionResponse toSessionResponse(StudySession session) {
        return new SessionResponse(session.getId(), session.getHost().getId(), session.getHost().getName(),
                session.getTitle(), session.getTopic(), session.getDescription(),
                session.getVisibility(), session.getType(), session.getCreatedAt());
    }

    private MembershipResponse toMembershipResponse(SessionMembership membership) {
        return new MembershipResponse(membership.getId(), membership.getSession().getId(),
                membership.getUser().getId(), membership.getUser().getName(),
                membership.getRole(), membership.getStatus(), membership.getCreatedAt());
    }
}