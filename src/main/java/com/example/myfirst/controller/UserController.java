package com.example.myfirst.controller;

import com.example.myfirst.common.Result;
import com.example.myfirst.dto.request.UserCreateRequest;
import com.example.myfirst.dto.request.UserUpdateRequest;
import com.example.myfirst.dto.response.UserResponse;
import com.example.myfirst.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public Result<UserResponse> createUser(@Valid @RequestBody UserCreateRequest request) {
        UserResponse response = userService.createUser(request);
        return Result.success(response);
    }

    @GetMapping("/{uuid}")
    public Result<UserResponse> getUserByUuid(@PathVariable UUID uuid) {
        UserResponse response = userService.getUserByUuid(uuid);
        return Result.success(response);
    }

    @GetMapping
    public Result<List<UserResponse>> getAllUsers() {
        List<UserResponse> responses = userService.getAllUsers();
        return Result.success(responses);
    }

    @PutMapping("/{uuid}")
    public Result<UserResponse> updateUser(@PathVariable UUID uuid,
                                           @Valid @RequestBody UserUpdateRequest request) {
        UserResponse response = userService.updateUser(uuid, request);
        return Result.success(response);
    }

    @DeleteMapping("/{uuid}")
    public Result<Void> deleteUser(@PathVariable UUID uuid) {
        userService.deleteUser(uuid);
        return Result.success();
    }
}
