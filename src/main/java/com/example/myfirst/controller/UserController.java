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

    @GetMapping("/{id}")
    public Result<UserResponse> getUserById(@PathVariable Long id) {
        UserResponse response = userService.getUserById(id);
        return Result.success(response);
    }

    @GetMapping
    public Result<List<UserResponse>> getAllUsers() {
        List<UserResponse> responses = userService.getAllUsers();
        return Result.success(responses);
    }

    @PutMapping("/{id}")
    public Result<UserResponse> updateUser(@PathVariable Long id,
                                           @Valid @RequestBody UserUpdateRequest request) {
        UserResponse response = userService.updateUser(id, request);
        return Result.success(response);
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.success();
    }
}
