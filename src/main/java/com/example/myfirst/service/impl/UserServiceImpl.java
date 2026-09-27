package com.example.myfirst.service.impl;

import com.example.myfirst.dto.request.UserCreateRequest;
import com.example.myfirst.dto.request.UserUpdateRequest;
import com.example.myfirst.dto.response.UserResponse;
import com.example.myfirst.dto.response.UserSearchResultResponse;
import com.example.myfirst.entity.User;
import com.example.myfirst.mapper.UserMapper;
import com.example.myfirst.repository.UserRepository;
import com.example.myfirst.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        User user = UserMapper.toEntity(request);
        User saved = userRepository.save(user);
        return UserMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserByUuid(UUID uuid) {
        User user = userRepository.findByUuid(uuid)
                .orElseThrow(() -> new EntityNotFoundException("User not found with uuid: " + uuid));
        return UserMapper.toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UserResponse updateUser(UUID uuid, UserUpdateRequest request) {
        User user = userRepository.findByUuid(uuid)
                .orElseThrow(() -> new EntityNotFoundException("User not found with uuid: " + uuid));
        UserMapper.updateEntity(user, request);
        User updated = userRepository.save(user);
        return UserMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void deleteUser(UUID uuid) {
        if (!userRepository.existsByUuid(uuid)) {
            throw new EntityNotFoundException("User not found with uuid: " + uuid);
        }
        User user = userRepository.findByUuid(uuid)
                .orElseThrow(() -> new EntityNotFoundException("User not found with uuid: " + uuid));
        userRepository.delete(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSearchResultResponse> searchUsersByNickname(String nickname, Long excludeUserId) {
        if (nickname == null || nickname.length() < 2) {
            throw new IllegalArgumentException("Search query must be at least 2 characters");
        }
        List<User> users = userRepository.findByNicknameContainingAndIdNot(
                nickname, excludeUserId, PageRequest.of(0, 10));
        return users.stream()
                .map(user -> new UserSearchResultResponse(
                        user.getId(),
                        user.getNickname() != null ? user.getNickname() : user.getUsername(),
                        user.getAvatarUrl()))
                .collect(Collectors.toList());
    }
}
