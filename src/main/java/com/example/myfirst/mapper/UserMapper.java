package com.example.myfirst.mapper;

import com.example.myfirst.dto.request.UserCreateRequest;
import com.example.myfirst.dto.request.UserUpdateRequest;
import com.example.myfirst.dto.response.UserResponse;
import com.example.myfirst.entity.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;

@Slf4j
public class UserMapper {

    private static final ObjectMapper objectMapper = new ObjectMapper();

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
                user.getUuid().toString(),
                user.getUsername(),
                user.getEmail(),
                user.getNickname(),
                user.getBio(),
                user.getAvatarUrl(),
                deserializeTags(user.getInterestTags()),
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

    public static String serializeTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(tags);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize interest tags", e);
            return null;
        }
    }

    public static List<String> deserializeTags(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize interest tags: {}", json, e);
            return Collections.emptyList();
        }
    }
}
