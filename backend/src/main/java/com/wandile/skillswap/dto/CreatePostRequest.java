package com.wandile.skillswap.dto;

import com.wandile.skillswap.model.PostType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreatePostRequest {
    @NotNull
    private PostType type;

    @NotBlank
    private String skillTag;

    @NotBlank
    private String description;

    public PostType getType() { return type; }
    public void setType(PostType type) { this.type = type; }
    public String getSkillTag() { return skillTag; }
    public void setSkillTag(String skillTag) { this.skillTag = skillTag; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}