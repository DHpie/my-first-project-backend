package com.example.myfirst.service.impl;

import com.example.myfirst.dto.response.UserSearchResultResponse;
import com.example.myfirst.entity.User;
import com.example.myfirst.entity.UserBlock;
import com.example.myfirst.repository.UserBlockRepository;
import com.example.myfirst.repository.UserRepository;
import com.example.myfirst.service.UserBlockService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserBlockServiceImpl implements UserBlockService {

    private final UserBlockRepository userBlockRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void blockUser(Long blockerId, Long blockedId) {
        if (blockerId.equals(blockedId)) {
            throw new IllegalArgumentException("Cannot block yourself");
        }
        // 校验被屏蔽用户存在
        if (!userRepository.existsById(blockedId)) {
            throw new EntityNotFoundException("User not found with id: " + blockedId);
        }
        // 检查是否已屏蔽
        if (userBlockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId)) {
            return; // 幂等
        }
        UserBlock block = new UserBlock();
        block.setBlockerId(blockerId);
        block.setBlockedId(blockedId);
        userBlockRepository.save(block);
    }

    @Override
    @Transactional
    public void unblockUser(Long blockerId, Long blockedId) {
        UserBlock block = userBlockRepository.findByBlockerIdAndBlockedId(blockerId, blockedId)
                .orElseThrow(() -> new EntityNotFoundException("Block record not found"));
        userBlockRepository.delete(block);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSearchResultResponse> getBlockedUsers(Long blockerId) {
        List<UserBlock> blocks = userBlockRepository.findByBlockerId(blockerId);
        List<Long> blockedIds = blocks.stream()
                .map(UserBlock::getBlockedId)
                .collect(Collectors.toList());
        List<User> blockedUsers = userRepository.findAllById(blockedIds);
        return blockedUsers.stream()
                .map(user -> new UserSearchResultResponse(user.getId(), user.getNickname(), user.getAvatarUrl()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isBlocked(Long blockerId, Long blockedId) {
        return userBlockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isBlockedBy(Long blockerId, Long blockedId) {
        // blockerId 是否被 blockedId 屏蔽
        return userBlockRepository.existsByBlockerIdAndBlockedId(blockedId, blockerId);
    }
}
