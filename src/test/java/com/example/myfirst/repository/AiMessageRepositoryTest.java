package com.example.myfirst.repository;

import com.example.myfirst.entity.AiConversation;
import com.example.myfirst.entity.AiMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class AiMessageRepositoryTest {

    @Autowired
    private AiMessageRepository messageRepository;

    @Autowired
    private AiConversationRepository conversationRepository;

    private Long conversationId;

    @BeforeEach
    void setUp() {
        AiConversation conv = new AiConversation();
        conv.setUserId(1L);
        conv.setTitle("Test");
        conversationId = conversationRepository.save(conv).getId();
    }

    @Test
    @DisplayName("find messages ordered by createdAt ascending")
    void shouldFindMessagesOrderedByCreatedAtAsc() {
        AiMessage msg1 = new AiMessage();
        msg1.setConversationId(conversationId);
        msg1.setRole("user");
        msg1.setContent("Hello");
        messageRepository.save(msg1);

        AiMessage msg2 = new AiMessage();
        msg2.setConversationId(conversationId);
        msg2.setRole("assistant");
        msg2.setContent("Hi there!");
        messageRepository.save(msg2);

        List<AiMessage> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        assertThat(messages).hasSize(2);
        assertThat(messages.get(0).getRole()).isEqualTo("user");
        assertThat(messages.get(1).getRole()).isEqualTo("assistant");
    }

    @Test
    @DisplayName("find top 20 messages ordered by createdAt descending")
    void shouldFindTop20ByCreatedAtDesc() {
        for (int i = 0; i < 25; i++) {
            AiMessage msg = new AiMessage();
            msg.setConversationId(conversationId);
            msg.setRole("user");
            msg.setContent("Message " + i);
            messageRepository.save(msg);
        }

        List<AiMessage> top20 = messageRepository.findTop20ByConversationIdOrderByCreatedAtDesc(conversationId);
        assertThat(top20).hasSize(20);
        assertThat(top20.get(0).getContent()).isEqualTo("Message 24");
    }

    @Test
    @DisplayName("count messages by conversationId")
    void shouldCountMessagesByConversationId() {
        for (int i = 0; i < 5; i++) {
            AiMessage msg = new AiMessage();
            msg.setConversationId(conversationId);
            msg.setRole("user");
            msg.setContent("Msg " + i);
            messageRepository.save(msg);
        }

        long count = messageRepository.countByConversationId(conversationId);
        assertThat(count).isEqualTo(5L);
    }
}
