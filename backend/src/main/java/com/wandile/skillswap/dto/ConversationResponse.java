package com.wandile.skillswap.dto;

import com.wandile.skillswap.model.ConversationType;
import java.time.Instant;
import java.util.List;

public class ConversationResponse {
    private Long id;
    private ConversationType type;
    private List<ParticipantSummary> participants;
    private Instant createdAt;

    public ConversationResponse(Long id, ConversationType type, List<ParticipantSummary> participants, Instant createdAt) {
        this.id = id;
        this.type = type;
        this.participants = participants;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public ConversationType getType() { return type; }
    public List<ParticipantSummary> getParticipants() { return participants; }
    public Instant getCreatedAt() { return createdAt; }
}