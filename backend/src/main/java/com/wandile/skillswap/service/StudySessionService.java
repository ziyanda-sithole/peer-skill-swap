package com.wandile.skillswap.service;

import com.wandile.skillswap.dto.CreateSessionRequest;
import com.wandile.skillswap.dto.InviteRequest;
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
    private final NotificationService notificationService;

    public StudySessionService(StudySessionRepository sessionRepository,
                               SessionMembershipRepository membershipRepository,
                               UserRepository userRepository,
                               NotificationService notificationService) {
        this.sessionRepository = sessionRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
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
        requireRoomForOneOnOne(session);

        SessionMembership membership = membershipRepository.save(
                new SessionMembership(session, user, MembershipRole.MEMBER, MembershipStatus.PENDING));

        for (SessionMembership m : membershipRepository.findBySession_IdAndStatus(sessionId, MembershipStatus.APPROVED)) {
            if (m.getRole() == MembershipRole.HOST || m.getRole() == MembershipRole.ADMIN) {
                notificationService.notifyJoinRequest(m.getUser().getId(), sessionId, session.getTitle(), user.getName());
            }
        }

        return toMembershipResponse(membership);
    }

    public MembershipResponse decideRequest(Long sessionId, Long targetUserId, Long actingUserId, boolean approve) {
        requireHostOrAdmin(sessionId, actingUserId);

        SessionMembership membership = membershipRepository.findBySession_IdAndUser_Id(sessionId, targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("Membership not found"));

        membership.setStatus(approve ? MembershipStatus.APPROVED : MembershipStatus.DECLINED);
        SessionMembership saved = membershipRepository.save(membership);
        notificationService.notifyMembershipDecision(targetUserId, sessionId, saved.getSession().getTitle(), approve);
        return toMembershipResponse(saved);
    }

    public void leaveSession(Long sessionId, Long userId) {
        SessionMembership membership = membershipRepository.findBySession_IdAndUser_Id(sessionId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Membership not found"));
        if (membership.getRole() == MembershipRole.HOST) {
            throw new IllegalArgumentException("The host can't leave their own session");
        }
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
        StudySession session = membership.getSession();
        return new MembershipResponse(membership.getId(), session.getId(), session.getTitle(),
                session.getHost().getName(), membership.getUser().getId(), membership.getUser().getName(),
                membership.getRole(), membership.getStatus(), membership.getCreatedAt());
    }

    public List<MembershipResponse> listMyMemberships(Long userId) {
        return membershipRepository.findByUser_Id(userId)
                .stream().map(this::toMembershipResponse).toList();
    }

    public List<MembershipResponse> listMembers(Long sessionId, Long requestingUserId) {
        SessionMembership requester = membershipRepository.findBySession_IdAndUser_Id(sessionId, requestingUserId)
                .orElseThrow(() -> new IllegalArgumentException("You are not a member of this session"));
        if (requester.getStatus() != MembershipStatus.APPROVED) {
            throw new IllegalArgumentException("Your membership is not approved yet");
        }
        return membershipRepository.findBySession_IdAndStatus(sessionId, MembershipStatus.APPROVED)
                .stream().map(this::toMembershipResponse).toList();
    }

    public MembershipResponse promoteToAdmin(Long sessionId, Long targetUserId, Long actingUserId) {
        SessionMembership acting = membershipRepository.findBySession_IdAndUser_Id(sessionId, actingUserId)
                .orElseThrow(() -> new IllegalArgumentException("You are not a member of this session"));
        if (acting.getRole() != MembershipRole.HOST) {
            throw new IllegalArgumentException("Only the host can promote members to admin");
        }
        SessionMembership target = membershipRepository.findBySession_IdAndUser_Id(sessionId, targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("Membership not found"));
        if (target.getStatus() != MembershipStatus.APPROVED) {
            throw new IllegalArgumentException("Only approved members can be promoted");
        }
        if (target.getRole() == MembershipRole.HOST) {
            throw new IllegalArgumentException("The host is already the top role");
        }
        target.setRole(MembershipRole.ADMIN);
        return toMembershipResponse(membershipRepository.save(target));
    }

    private void requireRoomForOneOnOne(StudySession session) {
        if (session.getType() != SessionType.ONE_ON_ONE) return;
        boolean hasOtherParticipant = membershipRepository.findBySession_Id(session.getId()).stream()
                .anyMatch(m -> m.getRole() != MembershipRole.HOST && m.getStatus() != MembershipStatus.DECLINED);
        if (hasOtherParticipant) {
            throw new IllegalArgumentException("This is a one-on-one session and already has a participant");
        }
    }

    public MembershipResponse inviteMember(Long sessionId, Long actingUserId, InviteRequest request) {
        requireHostOrAdmin(sessionId, actingUserId);
        StudySession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));
        User invitee = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("No user found with that email"));

        membershipRepository.findBySession_IdAndUser_Id(sessionId, invitee.getId()).ifPresent(m -> {
            throw new IllegalArgumentException("That user already has a membership for this session");
        });
        requireRoomForOneOnOne(session);

        SessionMembership membership = membershipRepository.save(
                new SessionMembership(session, invitee, MembershipRole.MEMBER, MembershipStatus.INVITED));
        notificationService.notifyInvite(invitee.getId(), sessionId, session.getTitle());
        return toMembershipResponse(membership);
    }

    public MembershipResponse respondToInvite(Long sessionId, Long userId, boolean accept) {
        SessionMembership membership = membershipRepository.findBySession_IdAndUser_Id(sessionId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Invite not found"));
        if (membership.getStatus() != MembershipStatus.INVITED) {
            throw new IllegalArgumentException("There is no pending invite to respond to");
        }
        membership.setStatus(accept ? MembershipStatus.APPROVED : MembershipStatus.DECLINED);
        SessionMembership saved = membershipRepository.save(membership);
        notificationService.notifyInviteResponse(saved.getSession().getHost().getId(), sessionId,
                saved.getSession().getTitle(), saved.getUser().getName(), accept);
        return toMembershipResponse(saved);
    }

    public List<MembershipResponse> listMyInvites(Long userId) {
        return membershipRepository.findByUser_IdAndStatus(userId, MembershipStatus.INVITED)
                .stream().map(this::toMembershipResponse).toList();
    }
}