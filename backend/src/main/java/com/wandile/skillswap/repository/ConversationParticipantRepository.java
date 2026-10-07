package com.wandile.skillswap.repository;

import com.wandile.skillswap.model.ConversationParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant, Long> {
    List<ConversationParticipant> findByConversation_Id(Long conversationId);
    Optional<ConversationParticipant> findByConversation_IdAndUser_Id(Long conversationId, Long userId);
    List<ConversationParticipant> findByUser_Id(Long userId);
    List<ConversationParticipant> findByUser_IdAndLeftAtIsNull(Long userId);
}