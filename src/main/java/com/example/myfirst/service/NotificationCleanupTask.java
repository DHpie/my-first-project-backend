package com.example.myfirst.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 通知自动清理定时任务 —— 每日删除超过 14 天的通知记录
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationCleanupTask {

    private final NotificationService notificationService;

    /**
     每日凌晨 2:00 执行通知清理
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupOldNotifications() {
        log.info("Starting notification cleanup task...");
        int deleted = notificationService.cleanupOldNotifications();
        log.info("Notification cleanup task completed, deleted {} records", deleted);
    }
}
