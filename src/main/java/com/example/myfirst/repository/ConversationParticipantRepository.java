package com.example.myfirst.repository;

import com.example.myfirst.entity.ConversationParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant, ConversationParticipant.ConversationParticipantId> {

    Optional<ConversationParticipant> findByIdConversationIdAndIdUserId(Long conversationId, Long userId);

    @Query("SELECT cp FROM ConversationParticipant cp WHERE cp.id.conversationId = :conversationId")
    List<ConversationParticipant> findByConversationId(@Param("conversationId") Long conversationId);

    @Query("SELECT COUNT(cp) FROM ConversationParticipant cp WHERE cp.id.userId = :userId AND cp.unreadCount > 0")
    long countUnreadConversationsByUserId(@Param("userId") Long userId);
}
