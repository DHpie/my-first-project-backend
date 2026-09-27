package com.example.myfirst.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 通知类型枚举
 */
@Getter
@AllArgsConstructor
public enum NotificationType {

    LIKE("liked"),
    COMMENT("commented on"),
    REPLY("replied to");

    /**
     动作描述文本，用于拼接通知消息
     */
    private final String actionText;
}
