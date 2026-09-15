package com.wandile.skillswap.dto;

import com.wandile.skillswap.model.SessionType;
import com.wandile.skillswap.model.SessionVisibility;
import java.time.Instant;

public class SessionResponse {
    private Long id;
    private Long hostId;
    private String hostName;
    private String title;
    private String topic;
    private String description;
    private SessionVisibility visibility;
    private SessionType type;
    private Instant createdAt;

    public SessionResponse(Long id, Long hostId, String hostName, String title, String topic,
                           String description, SessionVisibility visibility, SessionType type,
                           Instant createdAt) {
        this.id = id;
        this.hostId = hostId;
        this.hostName = hostName;
        this.title = title;
        this.topic = topic;
        this.description = description;
        this.visibility = visibility;
        this.type = type;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public Long getHostId() { return hostId; }
    public String getHostName() { return hostName; }
    public String getTitle() { return title; }
    public String getTopic() { return topic; }
    public String getDescription() { return description; }
    public SessionVisibility getVisibility() { return visibility; }
    public SessionType getType() { return type; }
    public Instant getCreatedAt() { return createdAt; }
}