package com.example.myfirst.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * AI 聊天相关 Bean 配置
 */
@Configuration
public class AiChatConfig {

    /**
     * AI 聊天流式处理专用线程池（固定大小，避免无界线程导致资源耗尽）
     */
    @Bean(destroyMethod = "shutdown")
    public ExecutorService aiChatExecutor() {
        return Executors.newFixedThreadPool(10);
    }
}
