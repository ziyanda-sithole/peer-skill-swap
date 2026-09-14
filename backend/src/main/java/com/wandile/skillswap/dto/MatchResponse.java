package com.wandile.skillswap.dto;

import com.wandile.skillswap.model.MatchStatus;
import com.wandile.skillswap.model.PostType;
import java.time.Instant;

public class MatchResponse {
    private Long id;
    private Long postId;
    private String skillTag;
    private PostType postType;
    private String posterName;
    private String responderName;
    private String message;
    private MatchStatus status;
    private Instant createdAt;

    public MatchResponse(Long id, Long postId, String skillTag, PostType postType,
                         String posterName, String responderName, String message,
                         MatchStatus status, Instant createdAt) {
        this.id = id;
        this.postId = postId;
        this.skillTag = skillTag;
        this.postType = postType;
        this.posterName = posterName;
        this.responderName = responderName;
        this.message = message;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public Long getPostId() { return postId; }
    public String getSkillTag() { return skillTag; }
    public PostType getPostType() { return postType; }
    public String getPosterName() { return posterName; }
    public String getResponderName() { return responderName; }
    public String getMessage() { return message; }
    public MatchStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}