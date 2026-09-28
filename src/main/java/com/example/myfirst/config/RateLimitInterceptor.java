package com.example.myfirst.config;

import com.example.myfirst.common.CurrentUserUtil;
import com.example.myfirst.common.RateLimitException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 消息发送频率限制拦截器。
 * 每用户每分钟最多 20 条消息，基于内存滑动窗口计数器。
 */
@Slf4j
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final int MAX_MESSAGES_PER_MINUTE = 20;

    /** userId -> 窗口开始时间（毫秒） */
    private final ConcurrentHashMap<Long, Long> windowStarts = new ConcurrentHashMap<>();

    /** userId -> 窗口内消息计数 */
    private final ConcurrentHashMap<Long, AtomicInteger> counters = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 仅限制 POST 请求（发送消息），GET 请求（获取消息历史）不限流
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        Long userId = CurrentUserUtil.getUserId(request);
        if (userId == null) {
            return true; // 未认证请求由其他处理器处理
        }

        long now = System.currentTimeMillis();
        long windowStart = windowStarts.compute(userId, (key, existing) -> {
            if (existing == null || now - existing > 60_000) {
                return now; // 新窗口
            }
            return existing;
        });

        AtomicInteger counter = counters.computeIfAbsent(userId, k -> new AtomicInteger(0));

        // 如果窗口已过期，重置计数
        if (now - windowStart > 60_000) {
            counter.set(0);
            windowStarts.put(userId, now);
        }

        int count = counter.incrementAndGet();
        if (count > MAX_MESSAGES_PER_MINUTE) {
            log.warn("Rate limit exceeded for userId={}: {} messages in current window", userId, count);
            throw new RateLimitException("Too many messages, please slow down");
        }

        return true;
    }
}
