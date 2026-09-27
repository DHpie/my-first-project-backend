package com.example.myfirst.repository;

import com.example.myfirst.entity.Conversation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Optional<Conversation> findByUser1IdAndUser2Id(Long user1Id, Long user2Id);

    @Query("SELECT c FROM Conversation c JOIN ConversationParticipant cp ON cp.id.conversationId = c.id " +
            "WHERE cp.id.userId = :userId ORDER BY c.lastMessageAt DESC")
    List<Conversation> findConversationsByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT c FROM Conversation c JOIN ConversationParticipant cp ON cp.id.conversationId = c.id " +
            "WHERE cp.id.userId = :userId AND c.id = :conversationId")
    Optional<Conversation> findConversationByIdAndUserId(@Param("conversationId") Long conversationId, @Param("userId") Long userId);
}
