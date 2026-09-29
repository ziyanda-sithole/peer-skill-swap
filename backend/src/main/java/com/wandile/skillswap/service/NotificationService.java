package com.wandile.skillswap.service;

import com.wandile.skillswap.dto.MatchResponse;
import com.wandile.skillswap.dto.NotificationPayload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public NotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void notifyNewMatch(Long postOwnerId, MatchResponse match) {
        send(postOwnerId, new NotificationPayload("NEW_MATCH",
                match.getResponderName() + " responded to your \"" + match.getSkillTag() + "\" post",
                match));
    }

    public void notifyMatchAccepted(Long responderId, MatchResponse match) {
        send(responderId, new NotificationPayload("MATCH_ACCEPTED",
                match.getPosterName() + " accepted your response on \"" + match.getSkillTag() + "\"",
                match));
    }

    public void notifyNewChatMessage(Long userId, Long sessionId, String sessionTitle, String senderName, String preview) {
        send(userId, new NotificationPayload("NEW_CHAT_MESSAGE",
                senderName + " in \"" + sessionTitle + "\": " + preview,
                java.util.Map.of("sessionId", sessionId)));
    }

    private void send(Long userId, NotificationPayload payload) {
        messagingTemplate.convertAndSend("/topic/notifications/" + userId, payload);
    }

    public void notifyJoinRequest(Long userId, Long sessionId, String sessionTitle, String requesterName) {
        send(userId, new NotificationPayload("JOIN_REQUEST",
                requesterName + " asked to join \"" + sessionTitle + "\"",
                java.util.Map.of("sessionId", sessionId)));
    }

    public void notifyMembershipDecision(Long userId, Long sessionId, String sessionTitle, boolean approved) {
        String message = approved
                ? "You're in! Your request to join \"" + sessionTitle + "\" was approved"
                : "Your request to join \"" + sessionTitle + "\" was declined";
        send(userId, new NotificationPayload(approved ? "REQUEST_APPROVED" : "REQUEST_DECLINED",
                message, java.util.Map.of("sessionId", sessionId)));
    }

    public void notifyInvite(Long userId, Long sessionId, String sessionTitle) {
        send(userId, new NotificationPayload("SESSION_INVITE",
                "You've been invited to join \"" + sessionTitle + "\"",
                java.util.Map.of("sessionId", sessionId)));
    }

    public void notifyInviteResponse(Long hostId, Long sessionId, String sessionTitle, String userName, boolean accepted) {
        String message = accepted
                ? userName + " accepted your invite to \"" + sessionTitle + "\""
                : userName + " declined your invite to \"" + sessionTitle + "\"";
        send(hostId, new NotificationPayload(accepted ? "INVITE_ACCEPTED" : "INVITE_DECLINED",
                message, java.util.Map.of("sessionId", sessionId)));
    }
}