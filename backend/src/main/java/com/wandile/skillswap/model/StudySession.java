package com.wandile.skillswap.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "study_sessions")
public class StudySession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "host_id")
    private User host;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String topic;

    @Column(nullable = false, length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionVisibility visibility;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionType type;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected StudySession() {}

    public StudySession(User host, String title, String topic, String description,
                        SessionVisibility visibility, SessionType type) {
        this.host = host;
        this.title = title;
        this.topic = topic;
        this.description = description;
        this.visibility = visibility;
        this.type = type;
    }

    public Long getId() { return id; }
    public User getHost() { return host; }
    public String getTitle() { return title; }
    public String getTopic() { return topic; }
    public String getDescription() { return description; }
    public SessionVisibility getVisibility() { return visibility; }
    public SessionType getType() { return type; }
    public Instant getCreatedAt() { return createdAt; }
}