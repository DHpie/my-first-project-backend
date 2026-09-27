package com.example.myfirst.controller;

import com.example.myfirst.common.CurrentUserUtil;
import com.example.myfirst.common.Result;
import com.example.myfirst.dto.response.UserSearchResultResponse;
import com.example.myfirst.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserSearchController {

    private final UserService userService;

    @GetMapping("/search")
    public Result<List<UserSearchResultResponse>> searchUsers(
            @RequestParam String nickname,
            HttpServletRequest request) {
        Long userId = CurrentUserUtil.requireUserId(request);
        List<UserSearchResultResponse> responses = userService.searchUsersByNickname(nickname, userId);
        return Result.success(responses);
    }
}
