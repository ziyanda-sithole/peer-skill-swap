package com.wandile.skillswap.controller;

import com.wandile.skillswap.dto.*;
import com.wandile.skillswap.service.ConversationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @PostMapping("/direct")
    public ResponseEntity<ConversationResponse> startDirect(@RequestParam Long userId, @RequestParam Long otherUserId) {
        return ResponseEntity.ok(conversationService.startDirectConversation(userId, otherUserId));
    }

    @PostMapping("/group")
    public ResponseEntity<ConversationResponse> startGroup(@RequestParam Long creatorId,
                                                           @Valid @RequestBody StartConversationRequest request) {
        return ResponseEntity.ok(conversationService.startGroupConversation(creatorId, request));
    }

    @GetMapping("/mine")
    public ResponseEntity<List<ConversationResponse>> mine(@RequestParam Long userId) {
        return ResponseEntity.ok(conversationService.listMyConversations(userId));
    }

    @PostMapping("/{conversationId}/participants")
    public ResponseEntity<ConversationResponse> addParticipant(@PathVariable Long conversationId,
                                                               @RequestParam Long actingUserId,
                                                               @Valid @RequestBody InviteRequest request) {
        return ResponseEntity.ok(conversationService.addParticipant(conversationId, actingUserId, request));
    }

    @DeleteMapping("/{conversationId}/leave")
    public ResponseEntity<Void> leave(@PathVariable Long conversationId, @RequestParam Long userId) {
        conversationService.leaveConversation(conversationId, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{conversationId}/messages")
    public ResponseEntity<DirectMessageResponse> sendMessage(@PathVariable Long conversationId,
                                                             @RequestParam Long senderId,
                                                             @Valid @RequestBody SendDirectMessageRequest request) {
        return ResponseEntity.ok(conversationService.sendMessage(conversationId, senderId, request));
    }

    @GetMapping("/{conversationId}/messages")
    public ResponseEntity<List<DirectMessageResponse>> listMessages(@PathVariable Long conversationId,
                                                                    @RequestParam Long userId) {
        return ResponseEntity.ok(conversationService.listMessages(conversationId, userId));
    }
}