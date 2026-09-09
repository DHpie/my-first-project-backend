package com.example.myfirst.mapper;

import com.example.myfirst.dto.request.UserCreateRequest;
import com.example.myfirst.dto.request.UserUpdateRequest;
import com.example.myfirst.dto.response.UserResponse;
import com.example.myfirst.entity.User;

public class UserMapper {

    private UserMapper() {
    }

    public static User toEntity(UserCreateRequest request) {
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        return user;
    }

    public static UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    public static void updateEntity(User user, UserUpdateRequest request) {
        if (request.getUsername() != null) {
            user.setUsername(request.getUsername());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
    }
}
