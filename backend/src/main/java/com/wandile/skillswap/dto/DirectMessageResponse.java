package com.wandile.skillswap.dto;

import java.time.Instant;

public class DirectMessageResponse {
    private Long id;
    private Long conversationId;
    private Long senderId;
    private String senderName;
    private String content;
    private Instant createdAt;

    public DirectMessageResponse(Long id, Long conversationId, Long senderId, String senderName,
                                 String content, Instant createdAt) {
        this.id = id;
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.content = content;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public Long getConversationId() { return conversationId; }
    public Long getSenderId() { return senderId; }
    public String getSenderName() { return senderName; }
    public String getContent() { return content; }
    public Instant getCreatedAt() { return createdAt; }
}