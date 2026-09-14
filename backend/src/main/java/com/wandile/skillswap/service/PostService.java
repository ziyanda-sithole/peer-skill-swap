package com.wandile.skillswap.service;

import com.wandile.skillswap.dto.CreatePostRequest;
import com.wandile.skillswap.dto.PostResponse;
import com.wandile.skillswap.model.Post;
import com.wandile.skillswap.model.PostType;
import com.wandile.skillswap.model.User;
import com.wandile.skillswap.repository.PostRepository;
import com.wandile.skillswap.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;

    public PostService(PostRepository postRepository, UserRepository userRepository) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
    }

    public PostResponse createPost(Long userId, CreatePostRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Post saved = postRepository.save(
                new Post(user, request.getType(), request.getSkillTag(), request.getDescription()));
        return toResponse(saved);
    }

    public List<PostResponse> listPosts(PostType type) {
        List<Post> posts = (type != null)
                ? postRepository.findByType(type)
                : postRepository.findAllByOrderByCreatedAtDesc();

        return posts.stream().map(this::toResponse).toList();
    }

    private PostResponse toResponse(Post post) {
        return new PostResponse(post.getId(), post.getUser().getName(), post.getType(),
                post.getSkillTag(), post.getDescription(), post.getCreatedAt());
    }
}