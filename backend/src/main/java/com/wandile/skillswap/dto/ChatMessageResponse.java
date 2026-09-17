package com.wandile.skillswap.dto;

import java.time.Instant;

public class ChatMessageResponse {
    private Long id;
    private Long sessionId;
    private Long senderId;
    private String senderName;
    private String content;
    private Instant createdAt;

    public ChatMessageResponse(Long id, Long sessionId, Long senderId, String senderName,
                               String content, Instant createdAt) {
        this.id = id;
        this.sessionId = sessionId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.content = content;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public Long getSessionId() { return sessionId; }
    public Long getSenderId() { return senderId; }
    public String getSenderName() { return senderName; }
    public String getContent() { return content; }
    public Instant getCreatedAt() { return createdAt; }
}