package com.wandile.skillswap.repository;

import com.wandile.skillswap.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
}