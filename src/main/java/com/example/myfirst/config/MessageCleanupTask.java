package com.example.myfirst.config;

import com.example.myfirst.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 消息自动清理定时任务。
 * 每日执行一次，删除 createdAt 早于 30 天前的消息记录。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageCleanupTask {

    private static final int RETENTION_DAYS = 30;

    private final MessageRepository messageRepository;

    @Scheduled(cron = "0 0 3 * * *") // 每日凌晨 3 点执行
    @Transactional
    public void cleanupOldMessages() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(RETENTION_DAYS);
        log.info("Starting message cleanup for messages older than {}", cutoff);
        int deleted = messageRepository.deleteOlderThan(cutoff);
        log.info("Message cleanup completed: {} messages deleted", deleted);
    }
}
