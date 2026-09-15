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

    private final NotificationService notificationService;

    public MatchService(MatchRepository matchRepository, PostRepository postRepository,
                        UserRepository userRepository, NotificationService notificationService) {
        this.matchRepository = matchRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
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
        MatchResponse response = toResponse(saved);
        notificationService.notifyNewMatch(post.getUser().getId(), response);
        return response;
    }

    public MatchResponse accept(Long matchId, Long userId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Match not found"));

        if (!match.getPost().getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Only the post owner can accept this match");
        }

        match.setStatus(MatchStatus.ACCEPTED);
        Match saved = matchRepository.save(match);
        MatchResponse response = toResponse(saved);
        notificationService.notifyMatchAccepted(match.getResponder().getId(), response);
        return response;
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
                post.getUser().getId(), post.getUser().getName(),
                match.getResponder().getId(), match.getResponder().getName(),
                match.getMessage(), match.getStatus(), match.getCreatedAt());
    }
}