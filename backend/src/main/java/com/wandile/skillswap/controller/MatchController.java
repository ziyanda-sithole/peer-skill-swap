package com.wandile.skillswap.controller;

import com.wandile.skillswap.dto.MatchResponse;
import com.wandile.skillswap.dto.RespondRequest;
import com.wandile.skillswap.service.MatchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class MatchController {

    private final MatchService matchService;

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    @PostMapping("/api/posts/{postId}/respond")
    public ResponseEntity<MatchResponse> respond(@PathVariable Long postId,
                                                 @RequestParam Long responderId,
                                                 @RequestBody(required = false) RespondRequest request) {
        return ResponseEntity.ok(matchService.respond(postId, responderId,
                request != null ? request : new RespondRequest()));
    }

    @PatchMapping("/api/matches/{matchId}/accept")
    public ResponseEntity<MatchResponse> accept(@PathVariable Long matchId,
                                                @RequestParam Long userId) {
        return ResponseEntity.ok(matchService.accept(matchId, userId));
    }

    @GetMapping("/api/matches")
    public ResponseEntity<List<MatchResponse>> listMatches(@RequestParam Long userId) {
        return ResponseEntity.ok(matchService.listForUser(userId));
    }
}