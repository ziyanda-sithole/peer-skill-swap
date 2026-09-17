package com.wandile.skillswap.controller;

import com.wandile.skillswap.dto.ChatMessageResponse;
import com.wandile.skillswap.dto.SendMessageRequest;
import com.wandile.skillswap.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sessions/{sessionId}/messages")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ResponseEntity<ChatMessageResponse> send(@PathVariable Long sessionId,
                                                    @RequestParam Long senderId,
                                                    @Valid @RequestBody SendMessageRequest request) {
        return ResponseEntity.ok(chatService.sendMessage(sessionId, senderId, request));
    }

    @GetMapping
    public ResponseEntity<List<ChatMessageResponse>> list(@PathVariable Long sessionId,
                                                          @RequestParam Long userId) {
        return ResponseEntity.ok(chatService.listMessages(sessionId, userId));
    }
}