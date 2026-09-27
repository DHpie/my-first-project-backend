package com.example.myfirst.service;

import com.example.myfirst.dto.request.UserCreateRequest;
import com.example.myfirst.dto.request.UserUpdateRequest;
import com.example.myfirst.dto.response.UserResponse;
import com.example.myfirst.dto.response.UserSearchResultResponse;

import java.util.List;
import java.util.UUID;

public interface UserService {

    UserResponse createUser(UserCreateRequest request);

    UserResponse getUserByUuid(UUID uuid);

    List<UserResponse> getAllUsers();

    UserResponse updateUser(UUID uuid, UserUpdateRequest request);

    void deleteUser(UUID uuid);

    List<UserSearchResultResponse> searchUsersByNickname(String nickname, Long excludeUserId);
}
