package com.wandile.skillswap.service;

import com.wandile.skillswap.dto.*;
import com.wandile.skillswap.model.*;
import com.wandile.skillswap.repository.ConversationParticipantRepository;
import com.wandile.skillswap.repository.ConversationRepository;
import com.wandile.skillswap.repository.DirectMessageRepository;
import com.wandile.skillswap.repository.UserRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final DirectMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationService notificationService;

    public ConversationService(ConversationRepository conversationRepository,
                               ConversationParticipantRepository participantRepository,
                               DirectMessageRepository messageRepository,
                               UserRepository userRepository,
                               SimpMessagingTemplate messagingTemplate,
                               NotificationService notificationService) {
        this.conversationRepository = conversationRepository;
        this.participantRepository = participantRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
        this.notificationService = notificationService;
    }

    public ConversationResponse startDirectConversation(Long userId, Long otherUserId) {
        if (userId.equals(otherUserId)) {
            throw new IllegalArgumentException("You can't start a conversation with yourself");
        }
        User me = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
        User other = userRepository.findById(otherUserId).orElseThrow(() -> new IllegalArgumentException("User not found"));

        Optional<Conversation> existing = findExistingOneOnOne(userId, otherUserId);
        if (existing.isPresent()) {
            reactivateIfLeft(existing.get().getId(), userId);
            reactivateIfLeft(existing.get().getId(), otherUserId);
            return toResponse(existing.get());
        }

        Conversation conversation = conversationRepository.save(new Conversation(ConversationType.ONE_ON_ONE));
        participantRepository.save(new ConversationParticipant(conversation, me));
        participantRepository.save(new ConversationParticipant(conversation, other));
        return toResponse(conversation);
    }

    public ConversationResponse startGroupConversation(Long creatorId, StartConversationRequest request) {
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Conversation conversation = conversationRepository.save(new Conversation(ConversationType.GROUP));
        participantRepository.save(new ConversationParticipant(conversation, creator));
        for (String email : request.getParticipantEmails()) {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("No user found with email: " + email));
            if (!user.getId().equals(creatorId)) {
                participantRepository.save(new ConversationParticipant(conversation, user));
            }
        }
        return toResponse(conversation);
    }

    public ConversationResponse addParticipant(Long conversationId, Long actingUserId, InviteRequest request) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));
        requireActiveParticipant(conversationId, actingUserId);
        User actingUser = userRepository.findById(actingUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        User newUser = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("No user found with that email"));

        Optional<ConversationParticipant> existing =
                participantRepository.findByConversation_IdAndUser_Id(conversationId, newUser.getId());
        if (existing.isPresent() && existing.get().getLeftAt() == null) {
            throw new IllegalArgumentException("That user is already in this conversation");
        }
        if (existing.isPresent()) {
            existing.get().setLeftAt(null);
            participantRepository.save(existing.get());
        } else {
            participantRepository.save(new ConversationParticipant(conversation, newUser));
        }

        long activeCount = participantRepository.findByConversation_Id(conversationId).stream()
                .filter(p -> p.getLeftAt() == null).count();
        if (conversation.getType() == ConversationType.ONE_ON_ONE && activeCount > 2) {
            conversation.setType(ConversationType.GROUP);
            conversationRepository.save(conversation);
        }

        notificationService.notifyAddedToConversation(newUser.getId(), conversationId, actingUser.getName());
        return toResponse(conversation);
    }

    public void leaveConversation(Long conversationId, Long userId) {
        ConversationParticipant participant = participantRepository.findByConversation_IdAndUser_Id(conversationId, userId)
                .orElseThrow(() -> new IllegalArgumentException("You are not part of this conversation"));
        participant.setLeftAt(Instant.now());
        participantRepository.save(participant);
    }

    public List<ConversationResponse> listMyConversations(Long userId) {
        return participantRepository.findByUser_IdAndLeftAtIsNull(userId).stream()
                .map(p -> toResponse(p.getConversation()))
                .toList();
    }

    public DirectMessageResponse sendMessage(Long conversationId, Long senderId, SendDirectMessageRequest request) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));
        requireActiveParticipant(conversationId, senderId);
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        DirectMessage saved = messageRepository.save(new DirectMessage(conversation, sender, request.getContent()));
        DirectMessageResponse response = toMessageResponse(saved);

        messagingTemplate.convertAndSend("/topic/conversations/" + conversationId + "/messages", response);

        for (ConversationParticipant p : participantRepository.findByConversation_Id(conversationId)) {
            if (p.getLeftAt() == null && !p.getUser().getId().equals(senderId)) {
                String preview = request.getContent().length() > 60
                        ? request.getContent().substring(0, 60) + "..."
                        : request.getContent();
                notificationService.notifyNewDirectMessage(p.getUser().getId(), conversationId, sender.getName(), preview);
            }
        }
        return response;
    }

    public List<DirectMessageResponse> listMessages(Long conversationId, Long requestingUserId) {
        requireActiveParticipant(conversationId, requestingUserId);
        return messageRepository.findByConversation_IdOrderByCreatedAtAsc(conversationId)
                .stream().map(this::toMessageResponse).toList();
    }

    private Optional<Conversation> findExistingOneOnOne(Long userId, Long otherUserId) {
        for (ConversationParticipant p : participantRepository.findByUser_Id(userId)) {
            Conversation conversation = p.getConversation();
            if (conversation.getType() != ConversationType.ONE_ON_ONE) continue;
            List<ConversationParticipant> all = participantRepository.findByConversation_Id(conversation.getId());
            boolean hasOther = all.stream().anyMatch(cp -> cp.getUser().getId().equals(otherUserId));
            if (hasOther && all.size() == 2) {
                return Optional.of(conversation);
            }
        }
        return Optional.empty();
    }

    private void reactivateIfLeft(Long conversationId, Long userId) {
        participantRepository.findByConversation_IdAndUser_Id(conversationId, userId).ifPresent(p -> {
            if (p.getLeftAt() != null) {
                p.setLeftAt(null);
                participantRepository.save(p);
            }
        });
    }

    private void requireActiveParticipant(Long conversationId, Long userId) {
        ConversationParticipant participant = participantRepository.findByConversation_IdAndUser_Id(conversationId, userId)
                .orElseThrow(() -> new IllegalArgumentException("You are not part of this conversation"));
        if (participant.getLeftAt() != null) {
            throw new IllegalArgumentException("You have left this conversation");
        }
    }

    private ConversationResponse toResponse(Conversation conversation) {
        List<ParticipantSummary> participants = participantRepository.findByConversation_Id(conversation.getId()).stream()
                .filter(p -> p.getLeftAt() == null)
                .map(p -> new ParticipantSummary(p.getUser().getId(), p.getUser().getName()))
                .toList();
        return new ConversationResponse(conversation.getId(), conversation.getType(), participants, conversation.getCreatedAt());
    }

    private DirectMessageResponse toMessageResponse(DirectMessage message) {
        return new DirectMessageResponse(message.getId(), message.getConversation().getId(),
                message.getSender().getId(), message.getSender().getName(),
                message.getContent(), message.getCreatedAt());
    }
}