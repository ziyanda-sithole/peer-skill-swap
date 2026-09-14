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

    private void send(Long userId, NotificationPayload payload) {
        messagingTemplate.convertAndSend("/topic/notifications/" + userId, payload);
    }
}