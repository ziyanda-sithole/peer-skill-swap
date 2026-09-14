package com.wandile.skillswap.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "posts")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PostType type;

    @Column(nullable = false)
    private String skillTag;

    @Column(nullable = false, length = 1000)
    private String description;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Post() {}

    public Post(User user, PostType type, String skillTag, String description) {
        this.user = user;
        this.type = type;
        this.skillTag = skillTag;
        this.description = description;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public PostType getType() { return type; }
    public String getSkillTag() { return skillTag; }
    public String getDescription() { return description; }
    public Instant getCreatedAt() { return createdAt; }
}