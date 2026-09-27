package com.example.myfirst.service;

import com.example.myfirst.dto.response.UserSearchResultResponse;

import java.util.List;

public interface UserBlockService {

    void blockUser(Long blockerId, Long blockedId);

    void unblockUser(Long blockerId, Long blockedId);

    List<UserSearchResultResponse> getBlockedUsers(Long blockerId);

    boolean isBlocked(Long blockerId, Long blockedId);

    /**
     * 检查 blockedId 是否屏蔽了 blockerId（即 blockerId 被对方屏蔽）
     */
    boolean isBlockedBy(Long blockerId, Long blockedId);
}
