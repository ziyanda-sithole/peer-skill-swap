package com.wandile.skillswap.controller;

import com.wandile.skillswap.dto.ParticipantSummary;
import com.wandile.skillswap.model.User;
import com.wandile.skillswap.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserLookupController {

    private final UserRepository userRepository;

    public UserLookupController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/lookup")
    public ResponseEntity<ParticipantSummary> lookup(@RequestParam String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("No user found with that email"));
        return ResponseEntity.ok(new ParticipantSummary(user.getId(), user.getName()));
    }
}