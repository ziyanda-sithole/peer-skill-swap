package com.wandile.skillswap.controller;

import com.wandile.skillswap.dto.CreateSessionRequest;
import com.wandile.skillswap.dto.MembershipResponse;
import com.wandile.skillswap.dto.SessionResponse;
import com.wandile.skillswap.service.StudySessionService;
import com.wandile.skillswap.dto.InviteRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sessions")
public class StudySessionController {

    private final StudySessionService sessionService;

    public StudySessionController(StudySessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping
    public ResponseEntity<SessionResponse> create(@RequestParam Long hostId,
                                                  @Valid @RequestBody CreateSessionRequest request) {
        return ResponseEntity.ok(sessionService.createSession(hostId, request));
    }

    @GetMapping
    public ResponseEntity<List<SessionResponse>> listPublic() {
        return ResponseEntity.ok(sessionService.listPublicSessions());
    }

    @GetMapping("/mine")
    public ResponseEntity<List<SessionResponse>> listMine(@RequestParam Long userId) {
        return ResponseEntity.ok(sessionService.listMySessions(userId));
    }

    @PostMapping("/{sessionId}/join")
    public ResponseEntity<MembershipResponse> requestToJoin(@PathVariable Long sessionId,
                                                            @RequestParam Long userId) {
        return ResponseEntity.ok(sessionService.requestToJoin(sessionId, userId));
    }

    @PostMapping("/{sessionId}/members/{userId}/approve")
    public ResponseEntity<MembershipResponse> approve(@PathVariable Long sessionId, @PathVariable Long userId,
                                                      @RequestParam Long actingUserId) {
        return ResponseEntity.ok(sessionService.decideRequest(sessionId, userId, actingUserId, true));
    }

    @PostMapping("/{sessionId}/members/{userId}/decline")
    public ResponseEntity<MembershipResponse> decline(@PathVariable Long sessionId, @PathVariable Long userId,
                                                      @RequestParam Long actingUserId) {
        return ResponseEntity.ok(sessionService.decideRequest(sessionId, userId, actingUserId, false));
    }

    @DeleteMapping("/{sessionId}/leave")
    public ResponseEntity<Void> leave(@PathVariable Long sessionId, @RequestParam Long userId) {
        sessionService.leaveSession(sessionId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{sessionId}/requests")
    public ResponseEntity<List<MembershipResponse>> pendingRequests(@PathVariable Long sessionId,
                                                                    @RequestParam Long actingUserId) {
        return ResponseEntity.ok(sessionService.listPendingRequests(sessionId, actingUserId));
    }

    @GetMapping("/memberships")
    public ResponseEntity<List<MembershipResponse>> myMemberships(@RequestParam Long userId) {
        return ResponseEntity.ok(sessionService.listMyMemberships(userId));
    }

    @GetMapping("/{sessionId}/members")
    public ResponseEntity<List<MembershipResponse>> members(@PathVariable Long sessionId,
                                                            @RequestParam Long userId) {
        return ResponseEntity.ok(sessionService.listMembers(sessionId, userId));
    }

    @PostMapping("/{sessionId}/members/{userId}/promote")
    public ResponseEntity<MembershipResponse> promote(@PathVariable Long sessionId, @PathVariable Long userId,
                                                      @RequestParam Long actingUserId) {
        return ResponseEntity.ok(sessionService.promoteToAdmin(sessionId, userId, actingUserId));
    }

    @PostMapping("/{sessionId}/invite")
    public ResponseEntity<MembershipResponse> invite(@PathVariable Long sessionId,
                                                     @RequestParam Long actingUserId,
                                                     @Valid @RequestBody InviteRequest request) {
        return ResponseEntity.ok(sessionService.inviteMember(sessionId, actingUserId, request));
    }

    @PostMapping("/{sessionId}/invite/accept")
    public ResponseEntity<MembershipResponse> acceptInvite(@PathVariable Long sessionId, @RequestParam Long userId) {
        return ResponseEntity.ok(sessionService.respondToInvite(sessionId, userId, true));
    }

    @PostMapping("/{sessionId}/invite/decline")
    public ResponseEntity<MembershipResponse> declineInvite(@PathVariable Long sessionId, @RequestParam Long userId) {
        return ResponseEntity.ok(sessionService.respondToInvite(sessionId, userId, false));
    }

    @GetMapping("/invites")
    public ResponseEntity<List<MembershipResponse>> myInvites(@RequestParam Long userId) {
        return ResponseEntity.ok(sessionService.listMyInvites(userId));
    }

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Void> delete(@PathVariable Long sessionId, @RequestParam Long actingUserId) {
        sessionService.deleteSession(sessionId, actingUserId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{sessionId}/members/{userId}")
    public ResponseEntity<Void> removeMember(@PathVariable Long sessionId, @PathVariable Long userId,
                                             @RequestParam Long actingUserId) {
        sessionService.removeMember(sessionId, userId, actingUserId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{sessionId}/members")
    public ResponseEntity<MembershipResponse> addMember(@PathVariable Long sessionId,
                                                        @RequestParam Long actingUserId,
                                                        @Valid @RequestBody InviteRequest request) {
        return ResponseEntity.ok(sessionService.addMemberDirectly(sessionId, actingUserId, request));
    }
}