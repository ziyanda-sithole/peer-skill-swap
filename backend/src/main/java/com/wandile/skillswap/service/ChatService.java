package com.wandile.skillswap.service;

import com.wandile.skillswap.dto.ChatMessageResponse;
import com.wandile.skillswap.dto.SendMessageRequest;
import com.wandile.skillswap.model.*;
import com.wandile.skillswap.repository.ChatMessageRepository;
import com.wandile.skillswap.repository.SessionMembershipRepository;
import com.wandile.skillswap.repository.StudySessionRepository;
import com.wandile.skillswap.repository.UserRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChatService {

    private final ChatMessageRepository messageRepository;
    private final StudySessionRepository sessionRepository;
    private final SessionMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationService notificationService;

    public ChatService(ChatMessageRepository messageRepository, StudySessionRepository sessionRepository,
                       SessionMembershipRepository membershipRepository, UserRepository userRepository,
                       SimpMessagingTemplate messagingTemplate, NotificationService notificationService) {
        this.messageRepository = messageRepository;
        this.sessionRepository = sessionRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
        this.notificationService = notificationService;
    }

    public ChatMessageResponse sendMessage(Long sessionId, Long senderId, SendMessageRequest request) {
        StudySession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));
        requireApprovedMember(sessionId, senderId);
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        ChatMessage saved = messageRepository.save(new ChatMessage(session, sender, request.getContent()));
        ChatMessageResponse response = toResponse(saved);

        messagingTemplate.convertAndSend("/topic/sessions/" + sessionId + "/chat", response);

        List<SessionMembership> members = membershipRepository.findBySession_IdAndStatus(sessionId, MembershipStatus.APPROVED);
        for (SessionMembership member : members) {
            if (!member.getUser().getId().equals(senderId)) {
                String preview = request.getContent().length() > 60
                        ? request.getContent().substring(0, 60) + "..."
                        : request.getContent();
                notificationService.notifyNewChatMessage(member.getUser().getId(), sessionId, session.getTitle(), sender.getName(), preview);
            }
        }

        return response;
    }

    public List<ChatMessageResponse> listMessages(Long sessionId, Long requestingUserId) {
        requireApprovedMember(sessionId, requestingUserId);
        return messageRepository.findBySession_IdOrderByCreatedAtAsc(sessionId)
                .stream().map(this::toResponse).toList();
    }

    private void requireApprovedMember(Long sessionId, Long userId) {
        SessionMembership membership = membershipRepository.findBySession_IdAndUser_Id(sessionId, userId)
                .orElseThrow(() -> new IllegalArgumentException("You are not a member of this session"));
        if (membership.getStatus() != MembershipStatus.APPROVED) {
            throw new IllegalArgumentException("Your membership is not approved yet");
        }
    }

    private ChatMessageResponse toResponse(ChatMessage message) {
        return new ChatMessageResponse(message.getId(), message.getSession().getId(),
                message.getSender().getId(), message.getSender().getName(),
                message.getContent(), message.getCreatedAt());
    }
}