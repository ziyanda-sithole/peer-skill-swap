package com.wandile.skillswap.repository;

import com.wandile.skillswap.model.DirectMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DirectMessageRepository extends JpaRepository<DirectMessage, Long> {
    List<DirectMessage> findByConversation_IdOrderByCreatedAtAsc(Long conversationId);
}