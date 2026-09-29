package com.example.myfirst.repository;

import com.example.myfirst.entity.AiConversation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class AiConversationRepositoryTest {

    @Autowired
    private AiConversationRepository repository;

    @Test
    @DisplayName("save and find active by userId")
    void shouldSaveAndFindActiveByUserId() {
        AiConversation conv = new AiConversation();
        conv.setUserId(1L);
        conv.setTitle("AI Travel Assistant");
        repository.save(conv);

        Optional<AiConversation> found = repository.findActiveByUserId(1L);
        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("AI Travel Assistant");
        assertThat(found.get().getArchivedAt()).isNull();
    }

    @Test
    @DisplayName("archived conversation not returned by findActiveByUserId")
    void shouldNotReturnArchivedConversation() {
        AiConversation conv = new AiConversation();
        conv.setUserId(2L);
        conv.setTitle("Old Chat");
        conv.setArchivedAt(java.time.LocalDateTime.now());
        repository.save(conv);

        Optional<AiConversation> found = repository.findActiveByUserId(2L);
        assertThat(found).isEmpty();
    }
}
