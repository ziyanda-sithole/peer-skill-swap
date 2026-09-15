package com.wandile.skillswap.dto;

import com.wandile.skillswap.model.MembershipRole;
import com.wandile.skillswap.model.MembershipStatus;
import java.time.Instant;

public class MembershipResponse {
    private Long id;
    private Long sessionId;
    private Long userId;
    private String userName;
    private MembershipRole role;
    private MembershipStatus status;
    private Instant createdAt;

    public MembershipResponse(Long id, Long sessionId, Long userId, String userName,
                              MembershipRole role, MembershipStatus status, Instant createdAt) {
        this.id = id;
        this.sessionId = sessionId;
        this.userId = userId;
        this.userName = userName;
        this.role = role;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public Long getSessionId() { return sessionId; }
    public Long getUserId() { return userId; }
    public String getUserName() { return userName; }
    public MembershipRole getRole() { return role; }
    public MembershipStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}