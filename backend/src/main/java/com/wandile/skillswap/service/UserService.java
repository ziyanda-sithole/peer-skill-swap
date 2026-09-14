package com.wandile.skillswap.service;

import com.wandile.skillswap.dto.AuthResponse;
import com.wandile.skillswap.dto.LoginRequest;
import com.wandile.skillswap.dto.RegisterRequest;
import com.wandile.skillswap.model.User;
import com.wandile.skillswap.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already registered");
        }
        String hashed = passwordEncoder.encode(request.getPassword());
        User saved = userRepository.save(new User(request.getName(), request.getEmail(), hashed));
        return new AuthResponse(saved.getId(), saved.getName(), saved.getEmail());
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }
        return new AuthResponse(user.getId(), user.getName(), user.getEmail());
    }
}