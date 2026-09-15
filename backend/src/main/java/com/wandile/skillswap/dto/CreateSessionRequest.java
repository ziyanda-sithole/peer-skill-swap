package com.wandile.skillswap.dto;

import com.wandile.skillswap.model.SessionType;
import com.wandile.skillswap.model.SessionVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateSessionRequest {
    @NotBlank
    private String title;
    @NotBlank
    private String topic;
    @NotBlank
    private String description;
    @NotNull
    private SessionVisibility visibility;
    @NotNull
    private SessionType type;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public SessionVisibility getVisibility() { return visibility; }
    public void setVisibility(SessionVisibility visibility) { this.visibility = visibility; }
    public SessionType getType() { return type; }
    public void setType(SessionType type) { this.type = type; }
}