package com.example.myfirst.service.impl;

import com.example.myfirst.dto.response.UserSearchResultResponse;
import com.example.myfirst.entity.User;
import com.example.myfirst.entity.UserBlock;
import com.example.myfirst.repository.UserBlockRepository;
import com.example.myfirst.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserBlockServiceImplTest {

    @Mock private UserBlockRepository userBlockRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private UserBlockServiceImpl userBlockService;

    @Nested
    @DisplayName("blockUser")
    class BlockUser {

        @Test
        @DisplayName("屏蔽自己 → IllegalArgumentException")
        void shouldThrowWhenBlockingSelf() {
            assertThatThrownBy(() -> userBlockService.blockUser(1L, 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Cannot block yourself");
        }

        @Test
        @DisplayName("被屏蔽用户不存在 → EntityNotFoundException")
        void shouldThrowWhenBlockedUserNotFound() {
            when(userRepository.existsById(999L)).thenReturn(false);

            assertThatThrownBy(() -> userBlockService.blockUser(1L, 999L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("User not found with id: 999");
        }

        @Test
        @DisplayName("正常屏蔽 → 创建屏蔽记录")
        void shouldCreateBlockRecord() {
            when(userRepository.existsById(2L)).thenReturn(true);
            when(userBlockRepository.existsByBlockerIdAndBlockedId(1L, 2L)).thenReturn(false);

            userBlockService.blockUser(1L, 2L);

            verify(userBlockRepository).save(any(UserBlock.class));
        }

        @Test
        @DisplayName("重复屏蔽 → 幂等（不重复创建）")
        void shouldIdempotentWhenAlreadyBlocked() {
            when(userRepository.existsById(2L)).thenReturn(true);
            when(userBlockRepository.existsByBlockerIdAndBlockedId(1L, 2L)).thenReturn(true);

            userBlockService.blockUser(1L, 2L);

            verify(userBlockRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("unblockUser")
    class UnblockUser {

        @Test
        @DisplayName("屏蔽记录不存在 → EntityNotFoundException")
        void shouldThrowWhenBlockRecordNotFound() {
            when(userBlockRepository.findByBlockerIdAndBlockedId(1L, 2L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> userBlockService.unblockUser(1L, 2L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Block record not found");
        }

        @Test
        @DisplayName("正常取消屏蔽 → 删除屏蔽记录")
        void shouldDeleteBlockRecord() {
            UserBlock block = new UserBlock();
            block.setBlockerId(1L);
            block.setBlockedId(2L);
            when(userBlockRepository.findByBlockerIdAndBlockedId(1L, 2L))
                    .thenReturn(Optional.of(block));

            userBlockService.unblockUser(1L, 2L);

            verify(userBlockRepository).delete(block);
        }
    }

    @Nested
    @DisplayName("屏蔽状态检查")
    class BlockCheck {

        @Test
        @DisplayName("isBlocked → 检查 A 是否屏蔽了 B")
        void shouldCheckIfBlocked() {
            when(userBlockRepository.existsByBlockerIdAndBlockedId(1L, 2L)).thenReturn(true);

            assertThat(userBlockService.isBlocked(1L, 2L)).isTrue();
        }

        @Test
        @DisplayName("isBlockedBy → 检查 A 是否被 B 屏蔽")
        void shouldCheckIfBlockedBy() {
            // A(1L) 是否被 B(2L) 屏蔽 → 检查 B 是否屏蔽了 A
            when(userBlockRepository.existsByBlockerIdAndBlockedId(2L, 1L)).thenReturn(true);

            assertThat(userBlockService.isBlockedBy(1L, 2L)).isTrue();
        }
    }
}
