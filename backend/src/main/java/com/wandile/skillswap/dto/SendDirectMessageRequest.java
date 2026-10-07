package com.wandile.skillswap.dto;

import jakarta.validation.constraints.NotBlank;

public class SendDirectMessageRequest {
    @NotBlank
    private String content;

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}