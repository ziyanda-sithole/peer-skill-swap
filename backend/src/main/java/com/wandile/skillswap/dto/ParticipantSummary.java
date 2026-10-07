package com.wandile.skillswap.dto;

public class ParticipantSummary {
    private Long userId;
    private String userName;

    public ParticipantSummary(Long userId, String userName) {
        this.userId = userId;
        this.userName = userName;
    }

    public Long getUserId() { return userId; }
    public String getUserName() { return userName; }
}