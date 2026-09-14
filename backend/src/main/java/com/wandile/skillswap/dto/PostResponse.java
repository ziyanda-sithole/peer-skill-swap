package com.wandile.skillswap.dto;

import com.wandile.skillswap.model.PostType;
import java.time.Instant;

public class PostResponse {
    private Long id;
    private String authorName;
    private PostType type;
    private String skillTag;
    private String description;
    private Instant createdAt;

    public PostResponse(Long id, String authorName, PostType type, String skillTag,
                        String description, Instant createdAt) {
        this.id = id;
        this.authorName = authorName;
        this.type = type;
        this.skillTag = skillTag;
        this.description = description;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public String getAuthorName() { return authorName; }
    public PostType getType() { return type; }
    public String getSkillTag() { return skillTag; }
    public String getDescription() { return description; }
    public Instant getCreatedAt() { return createdAt; }
}