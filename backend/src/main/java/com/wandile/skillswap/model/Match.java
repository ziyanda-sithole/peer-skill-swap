package com.wandile.skillswap.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "matches")
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "post_id")
    private Post post;

    @ManyToOne(optional = false)
    @JoinColumn(name = "responder_id")
    private User responder;

    @Column(length = 500)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchStatus status = MatchStatus.PENDING;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Match() {}

    public Match(Post post, User responder, String message) {
        this.post = post;
        this.responder = responder;
        this.message = message;
    }

    public Long getId() { return id; }
    public Post getPost() { return post; }
    public User getResponder() { return responder; }
    public String getMessage() { return message; }
    public MatchStatus getStatus() { return status; }
    public void setStatus(MatchStatus status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
}