package com.wandile.skillswap.controller;

import com.wandile.skillswap.dto.CreatePostRequest;
import com.wandile.skillswap.dto.PostResponse;
import com.wandile.skillswap.model.PostType;
import com.wandile.skillswap.service.PostService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping
    public ResponseEntity<PostResponse> createPost(@RequestParam Long userId,
                                                   @Valid @RequestBody CreatePostRequest request) {
        return ResponseEntity.ok(postService.createPost(userId, request));
    }

    @GetMapping
    public ResponseEntity<List<PostResponse>> listPosts(@RequestParam(required = false) PostType type) {
        return ResponseEntity.ok(postService.listPosts(type));
    }
}