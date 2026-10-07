package com.wandile.skillswap.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "conversations")
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConversationType type;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Conversation() {}

    public Conversation(ConversationType type) {
        this.type = type;
    }

    public Long getId() { return id; }
    public ConversationType getType() { return type; }
    public void setType(ConversationType type) { this.type = type; }
    public Instant getCreatedAt() { return createdAt; }
}