package com.example.myfirst.service.impl;

import com.example.myfirst.common.FileUploadException;
import com.example.myfirst.dto.request.ProfileUpdateRequest;
import com.example.myfirst.dto.response.AvatarUploadResponse;
import com.example.myfirst.dto.response.ProfileResponse;
import com.example.myfirst.entity.User;
import com.example.myfirst.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProfileServiceImpl 单元测试")
class ProfileServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProfileServiceImpl profileService;

    private UUID testUuid;
    private User testUser;

    @BeforeEach
    void setUp() {
        // 注入 uploadDir 配置值
        ReflectionTestUtils.setField(profileService, "uploadDir", "target/test-uploads/avatars");

        testUuid = UUID.randomUUID();
        testUser = new User();
        testUser.setUuid(testUuid);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setNickname("TestNick");
        testUser.setBio("A test bio");
        testUser.setAvatarUrl(null);
        testUser.setInterestTags("[\"Food\",\"History\"]");
    }

    // ========== getProfile ==========

    @Nested
    @DisplayName("getProfile")
    class GetProfile {

        @Test
        @DisplayName("正常流：用户存在 → 返回完整资料")
        void shouldReturnProfileWhenUserExists() {
            when(userRepository.findByUuid(testUuid)).thenReturn(Optional.of(testUser));

            ProfileResponse response = profileService.getProfile(testUuid);

            assertThat(response.getId()).isEqualTo(testUuid.toString());
            assertThat(response.getUsername()).isEqualTo("testuser");
            assertThat(response.getEmail()).isEqualTo("test@example.com");
            assertThat(response.getNickname()).isEqualTo("TestNick");
            assertThat(response.getBio()).isEqualTo("A test bio");
            assertThat(response.getInterestTags()).containsExactly("Food", "History");
        }

        @Test
        @DisplayName("异常流：用户不存在 → 抛出 EntityNotFoundException")
        void shouldThrowWhenUserNotFound() {
            when(userRepository.findByUuid(testUuid)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> profileService.getProfile(testUuid))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("User not found with uuid");
        }

        @Test
        @DisplayName("边界值：用户无昵称/简介/标签 → 返回 null/空列表")
        void shouldReturnNullsAndEmptyWhenFieldsAreNull() {
            testUser.setNickname(null);
            testUser.setBio(null);
            testUser.setInterestTags(null);
            when(userRepository.findByUuid(testUuid)).thenReturn(Optional.of(testUser));

            ProfileResponse response = profileService.getProfile(testUuid);

            assertThat(response.getNickname()).isNull();
            assertThat(response.getBio()).isNull();
            assertThat(response.getInterestTags()).isEmpty();
        }
    }

    // ========== updateProfile ==========

    @Nested
    @DisplayName("updateProfile")
    class UpdateProfile {

        @Test
        @DisplayName("正常流：更新昵称、简介、标签 → 返回更新后资料")
        void shouldUpdateFieldsSuccessfully() {
            ProfileUpdateRequest request = new ProfileUpdateRequest();
            request.setNickname("NewNick");
            request.setBio("New bio");
            request.setInterestTags(List.of("Nature", "Music"));

            when(userRepository.findByUuid(testUuid)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            ProfileResponse response = profileService.updateProfile(testUuid, request);

            assertThat(response.getNickname()).isEqualTo("NewNick");
            assertThat(response.getBio()).isEqualTo("New bio");
            assertThat(response.getInterestTags()).containsExactly("Nature", "Music");
            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("校验：昵称为空白 → 抛出 IllegalArgumentException 'Nickname is required'")
        void shouldRejectBlankNickname() {
            ProfileUpdateRequest request = new ProfileUpdateRequest();
            request.setNickname("   ");

            when(userRepository.findByUuid(testUuid)).thenReturn(Optional.of(testUser));

            assertThatThrownBy(() -> profileService.updateProfile(testUuid, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Nickname is required");
        }

        @Test
        @DisplayName("校验：昵称包含 HTML 标签 → 抛出 'Nickname cannot contain HTML tags'")
        void shouldRejectHtmlInNickname() {
            ProfileUpdateRequest request = new ProfileUpdateRequest();
            request.setNickname("<script>alert('x')</script>");

            when(userRepository.findByUuid(testUuid)).thenReturn(Optional.of(testUser));

            assertThatThrownBy(() -> profileService.updateProfile(testUuid, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Nickname cannot contain HTML tags");
        }

        @Test
        @DisplayName("校验：兴趣标签超过 5 项 → 抛出 'Maximum 5 tags allowed'")
        void shouldRejectMoreThan5Tags() {
            ProfileUpdateRequest request = new ProfileUpdateRequest();
            request.setNickname("Nick");
            request.setInterestTags(List.of("Food", "History", "Nature", "Music", "Culture", "Shopping"));

            when(userRepository.findByUuid(testUuid)).thenReturn(Optional.of(testUser));

            assertThatThrownBy(() -> profileService.updateProfile(testUuid, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Maximum 5 tags allowed");
        }

        @Test
        @DisplayName("校验：兴趣标签不在预定义列表 → 抛出 'Invalid interest tag'")
        void shouldRejectInvalidTag() {
            ProfileUpdateRequest request = new ProfileUpdateRequest();
            request.setNickname("Nick");
            request.setInterestTags(List.of("Gaming"));

            when(userRepository.findByUuid(testUuid)).thenReturn(Optional.of(testUser));

            assertThatThrownBy(() -> profileService.updateProfile(testUuid, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid interest tag");
        }

        @Test
        @DisplayName("边界值：恰好 5 个有效标签 → 成功")
        void shouldAcceptExactly5ValidTags() {
            ProfileUpdateRequest request = new ProfileUpdateRequest();
            request.setNickname("Nick");
            request.setInterestTags(List.of("Food", "History", "Nature", "Music", "Culture"));

            when(userRepository.findByUuid(testUuid)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            ProfileResponse response = profileService.updateProfile(testUuid, request);

            assertThat(response.getInterestTags()).hasSize(5);
        }

        @Test
        @DisplayName("边界值：昵称为 null → 不校验昵称，仅更新其他字段")
        void shouldSkipNicknameValidationWhenNull() {
            ProfileUpdateRequest request = new ProfileUpdateRequest();
            request.setNickname(null);
            request.setBio("Updated bio only");

            when(userRepository.findByUuid(testUuid)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            ProfileResponse response = profileService.updateProfile(testUuid, request);

            assertThat(response.getBio()).isEqualTo("Updated bio only");
            assertThat(response.getNickname()).isEqualTo("TestNick"); // 未变
        }
    }

    // ========== uploadAvatar ==========

    @Nested
    @DisplayName("uploadAvatar")
    class UploadAvatar {

        @Test
        @DisplayName("校验：空文件 → 抛出 'Please upload a JPG, PNG, or WebP image'")
        void shouldRejectEmptyFile() {
            MockMultipartFile emptyFile = new MockMultipartFile(
                    "file", "avatar.jpg", "image/jpeg", new byte[0]);

            assertThatThrownBy(() -> profileService.uploadAvatar(testUuid, emptyFile))
                    .isInstanceOf(FileUploadException.class)
                    .hasMessage("Please upload a JPG, PNG, or WebP image");
        }

        @Test
        @DisplayName("校验：文件超过 2MB → 抛出 'File size must be under 2MB'")
        void shouldRejectOversizedFile() {
            byte[] largeContent = new byte[3 * 1024 * 1024]; // 3MB
            MockMultipartFile largeFile = new MockMultipartFile(
                    "file", "avatar.jpg", "image/jpeg", largeContent);

            assertThatThrownBy(() -> profileService.uploadAvatar(testUuid, largeFile))
                    .isInstanceOf(FileUploadException.class)
                    .hasMessage("File size must be under 2MB");
        }

        @Test
        @DisplayName("校验：无效 MIME 类型 → 抛出 'Please upload a JPG, PNG, or WebP image'")
        void shouldRejectInvalidMimeType() {
            MockMultipartFile pdfFile = new MockMultipartFile(
                    "file", "document.pdf", "application/pdf", new byte[]{1, 2, 3});

            assertThatThrownBy(() -> profileService.uploadAvatar(testUuid, pdfFile))
                    .isInstanceOf(FileUploadException.class)
                    .hasMessage("Please upload a JPG, PNG, or WebP image");
        }

        @Test
        @DisplayName("校验：无效扩展名 → 抛出 'Please upload a JPG, PNG, or WebP image'")
        void shouldRejectInvalidExtension() {
            MockMultipartFile badExtFile = new MockMultipartFile(
                    "file", "image.gif", "image/jpeg", new byte[]{1, 2, 3});

            assertThatThrownBy(() -> profileService.uploadAvatar(testUuid, badExtFile))
                    .isInstanceOf(FileUploadException.class)
                    .hasMessage("Please upload a JPG, PNG, or WebP image");
        }

        @Test
        @DisplayName("正常流：有效 JPG 文件 → 返回 avatarUrl 并更新用户")
        void shouldUploadValidJpgSuccessfully() {
            byte[] content = new byte[]{1, 2, 3, 4, 5};
            MockMultipartFile jpgFile = new MockMultipartFile(
                    "file", "photo.jpg", "image/jpeg", content);

            when(userRepository.findByUuid(testUuid)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            AvatarUploadResponse response = profileService.uploadAvatar(testUuid, jpgFile);

            assertThat(response.getAvatarUrl()).startsWith("/uploads/avatars/");
            assertThat(response.getAvatarUrl()).endsWith(".jpg");
            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("正常流：有效 PNG 文件 → 返回 avatarUrl")
        void shouldUploadValidPngSuccessfully() {
            byte[] content = new byte[]{1, 2, 3, 4, 5};
            MockMultipartFile pngFile = new MockMultipartFile(
                    "file", "photo.png", "image/png", content);

            when(userRepository.findByUuid(testUuid)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            AvatarUploadResponse response = profileService.uploadAvatar(testUuid, pngFile);

            assertThat(response.getAvatarUrl()).endsWith(".png");
        }

        @Test
        @DisplayName("正常流：有效 WebP 文件 → 返回 avatarUrl")
        void shouldUploadValidWebpSuccessfully() {
            byte[] content = new byte[]{1, 2, 3, 4, 5};
            MockMultipartFile webpFile = new MockMultipartFile(
                    "file", "photo.webp", "image/webp", content);

            when(userRepository.findByUuid(testUuid)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            AvatarUploadResponse response = profileService.uploadAvatar(testUuid, webpFile);

            assertThat(response.getAvatarUrl()).endsWith(".webp");
        }
    }

    // ========== getAvailableTags ==========

    @Nested
    @DisplayName("getAvailableTags")
    class GetAvailableTags {

        @Test
        @DisplayName("返回 10 项预定义标签")
        void shouldReturn10PredefinedTags() {
            List<String> tags = profileService.getAvailableTags();

            assertThat(tags).containsExactly(
                    "History", "Food", "Nature", "Photography", "Adventure",
                    "Culture", "Shopping", "Nightlife", "Architecture", "Music"
            );
            assertThat(tags).hasSize(10);
        }
    }
}
