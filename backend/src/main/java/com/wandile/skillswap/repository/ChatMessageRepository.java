package com.wandile.skillswap.repository;

import com.wandile.skillswap.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findBySession_IdOrderByCreatedAtAsc(Long sessionId);
    void deleteBySession_Id(Long sessionId);
}