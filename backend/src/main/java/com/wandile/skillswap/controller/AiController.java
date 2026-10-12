package com.wandile.skillswap.controller;

import com.wandile.skillswap.dto.ConversationResponse;
import com.wandile.skillswap.model.User;
import com.wandile.skillswap.service.AiStudyPartnerService;
import com.wandile.skillswap.service.ConversationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiStudyPartnerService aiService;
    private final ConversationService conversationService;

    public AiController(AiStudyPartnerService aiService, ConversationService conversationService) {
        this.aiService = aiService;
        this.conversationService = conversationService;
    }

    @PostMapping("/start")
    public ResponseEntity<ConversationResponse> start(@RequestParam Long userId) {
        User bot = aiService.getBotUser();
        ConversationResponse conversation = conversationService.startDirectConversation(userId, bot.getId());
        aiService.postGreetingIfNew(conversation.getId());
        return ResponseEntity.ok(conversation);
    }
}