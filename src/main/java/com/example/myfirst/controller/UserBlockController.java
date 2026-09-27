package com.example.myfirst.controller;

import com.example.myfirst.common.CurrentUserUtil;
import com.example.myfirst.common.Result;
import com.example.myfirst.dto.response.UserSearchResultResponse;
import com.example.myfirst.service.UserBlockService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserBlockController {

    private final UserBlockService userBlockService;

    @PostMapping("/{id}/block")
    public Result<Void> blockUser(
            @PathVariable Long id,
            HttpServletRequest request) {
        Long userId = CurrentUserUtil.requireUserId(request);
        userBlockService.blockUser(userId, id);
        return Result.success();
    }

    @DeleteMapping("/{id}/block")
    public Result<Void> unblockUser(
            @PathVariable Long id,
            HttpServletRequest request) {
        Long userId = CurrentUserUtil.requireUserId(request);
        userBlockService.unblockUser(userId, id);
        return Result.success();
    }

    @GetMapping("/blocked")
    public Result<List<UserSearchResultResponse>> getBlockedUsers(HttpServletRequest request) {
        Long userId = CurrentUserUtil.requireUserId(request);
        List<UserSearchResultResponse> responses = userBlockService.getBlockedUsers(userId);
        return Result.success(responses);
    }

    /**
     * 检查当前用户与目标用户之间的屏蔽关系。
     * 返回 status: "none" | "blocked-by-me" | "blocked-by-other"
     */
    @GetMapping("/{id}/block-status")
    public Result<Map<String, String>> getBlockStatus(
            @PathVariable Long id,
            HttpServletRequest request) {
        Long userId = CurrentUserUtil.requireUserId(request);
        String status = "none";
        if (userBlockService.isBlocked(userId, id)) {
            status = "blocked-by-me";
        } else if (userBlockService.isBlockedBy(userId, id)) {
            status = "blocked-by-other";
        }
        Map<String, String> result = new HashMap<>();
        result.put("status", status);
        return Result.success(result);
    }
}
