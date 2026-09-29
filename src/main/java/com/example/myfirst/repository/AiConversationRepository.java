package com.example.myfirst.repository;

import com.example.myfirst.entity.AiConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AiConversationRepository extends JpaRepository<AiConversation, Long> {

    Optional<AiConversation> findByUserIdAndArchivedAtIsNull(Long userId);

    default Optional<AiConversation> findActiveByUserId(Long userId) {
        return findByUserIdAndArchivedAtIsNull(userId);
    }
}
