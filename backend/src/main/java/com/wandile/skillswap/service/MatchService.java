package com.wandile.skillswap.service;

import com.wandile.skillswap.dto.MatchResponse;
import com.wandile.skillswap.dto.RespondRequest;
import com.wandile.skillswap.model.*;
import com.wandile.skillswap.repository.MatchRepository;
import com.wandile.skillswap.repository.PostRepository;
import com.wandile.skillswap.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Stream;

@Service
public class MatchService {

    private final MatchRepository matchRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    public MatchService(MatchRepository matchRepository, PostRepository postRepository,
                        UserRepository userRepository) {
        this.matchRepository = matchRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
    }

    public MatchResponse respond(Long postId, Long responderId, RespondRequest request) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));
        User responder = userRepository.findById(responderId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (post.getUser().getId().equals(responderId)) {
            throw new IllegalArgumentException("You can't respond to your own post");
        }

        Match saved = matchRepository.save(new Match(post, responder, request.getMessage()));
        return toResponse(saved);
    }

    public MatchResponse accept(Long matchId, Long userId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Match not found"));

        if (!match.getPost().getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Only the post owner can accept this match");
        }

        match.setStatus(MatchStatus.ACCEPTED);
        return toResponse(matchRepository.save(match));
    }

    public List<MatchResponse> listForUser(Long userId) {
        return Stream.concat(
                        matchRepository.findByPost_User_Id(userId).stream(),
                        matchRepository.findByResponder_Id(userId).stream())
                .map(this::toResponse)
                .toList();
    }

    private MatchResponse toResponse(Match match) {
        Post post = match.getPost();
        return new MatchResponse(match.getId(), post.getId(), post.getSkillTag(), post.getType(),
                post.getUser().getName(), match.getResponder().getName(), match.getMessage(),
                match.getStatus(), match.getCreatedAt());
    }
}