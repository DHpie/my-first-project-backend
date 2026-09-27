package com.example.myfirst.controller;

import com.example.myfirst.common.FileUploadException;
import com.example.myfirst.common.GlobalExceptionHandler;
import com.example.myfirst.dto.request.ProfileUpdateRequest;
import com.example.myfirst.dto.response.AvatarUploadResponse;
import com.example.myfirst.dto.response.ProfileResponse;
import com.example.myfirst.service.ProfileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Profile API 契约测试")
class ProfileControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ProfileService profileService;

    @InjectMocks
    private ProfileController profileController;

    @InjectMocks
    private AvatarController avatarController;

    private final UUID testUuid = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(profileController, avatarController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private ProfileResponse buildProfileResponse() {
        return new ProfileResponse(
                testUuid.toString(),
                "testuser",
                "test@example.com",
                "TestNick",
                "A test bio",
                "/uploads/avatars/test.jpg",
                List.of("Food", "History"),
                LocalDateTime.of(2026, 1, 1, 0, 0),
                LocalDateTime.of(2026, 1, 2, 0, 0)
        );
    }

    // ========== GET /api/profile ==========

    @Nested
    @DisplayName("GET /api/profile")
    class GetProfile {

        @Test
        @DisplayName("正常流：返回 Result<ProfileResponse> 格式正确")
        void shouldReturnProfileWithResultWrapper() throws Exception {
            when(profileService.getProfile(testUuid)).thenReturn(buildProfileResponse());

            mockMvc.perform(get("/api/profile").param("uuid", testUuid.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("success"))
                    .andExpect(jsonPath("$.data.id").value(testUuid.toString()))
                    .andExpect(jsonPath("$.data.username").value("testuser"))
                    .andExpect(jsonPath("$.data.nickname").value("TestNick"))
                    .andExpect(jsonPath("$.data.bio").value("A test bio"))
                    .andExpect(jsonPath("$.data.avatarUrl").value("/uploads/avatars/test.jpg"))
                    .andExpect(jsonPath("$.data.interestTags[0]").value("Food"))
                    .andExpect(jsonPath("$.data.interestTags[1]").value("History"));
        }

        @Test
        @DisplayName("异常流：用户不存在 → 404 + Result 错误格式")
        void shouldReturn404WhenUserNotFound() throws Exception {
            when(profileService.getProfile(testUuid))
                    .thenThrow(new EntityNotFoundException("User not found with uuid: " + testUuid));

            mockMvc.perform(get("/api/profile").param("uuid", testUuid.toString()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(404))
                    .andExpect(jsonPath("$.message").value("User not found with uuid: " + testUuid));
        }

        @Test
        @DisplayName("校验：缺少 uuid 参数 → 500（GlobalExceptionHandler 未处理 MissingServletRequestParameterException）")
        void shouldReturn500WhenUuidMissing() throws Exception {
            // BUG: 缺少 uuid 时 Spring 抛出 MissingServletRequestParameterException，
            // 但 GlobalExceptionHandler 未处理该异常，落入通用 Exception 处理器返回 500
            mockMvc.perform(get("/api/profile"))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("校验：无效 uuid 格式 → 500（GlobalExceptionHandler 未处理 TypeMismatchException）")
        void shouldReturn500WhenUuidInvalid() throws Exception {
            // BUG: 无效 UUID 时 Spring 抛出 TypeMismatchException，
            // 但 GlobalExceptionHandler 未处理该异常，落入通用 Exception 处理器返回 500
            mockMvc.perform(get("/api/profile").param("uuid", "not-a-uuid"))
                    .andExpect(status().isInternalServerError());
        }
    }

    // ========== PUT /api/profile ==========

    @Nested
    @DisplayName("PUT /api/profile")
    class UpdateProfile {

        @Test
        @DisplayName("正常流：更新成功 → Result<ProfileResponse>")
        void shouldReturnUpdatedProfile() throws Exception {
            ProfileResponse updated = buildProfileResponse();
            updated.setNickname("NewNick");
            when(profileService.updateProfile(eq(testUuid), any(ProfileUpdateRequest.class))).thenReturn(updated);

            ProfileUpdateRequest request = new ProfileUpdateRequest();
            request.setNickname("NewNick");
            request.setBio("Updated bio");
            request.setInterestTags(List.of("Nature"));

            mockMvc.perform(put("/api/profile")
                            .param("uuid", testUuid.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.nickname").value("NewNick"));
        }

        @Test
        @DisplayName("校验：昵称超过 30 字符 → 400")
        void shouldReturn400WhenNicknameTooLong() throws Exception {
            ProfileUpdateRequest request = new ProfileUpdateRequest();
            request.setNickname("A".repeat(31));

            mockMvc.perform(put("/api/profile")
                            .param("uuid", testUuid.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400));
        }

        @Test
        @DisplayName("校验：简介超过 200 字符 → 400")
        void shouldReturn400WhenBioTooLong() throws Exception {
            ProfileUpdateRequest request = new ProfileUpdateRequest();
            request.setNickname("Nick");
            request.setBio("B".repeat(201));

            mockMvc.perform(put("/api/profile")
                            .param("uuid", testUuid.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400));
        }

        @Test
        @DisplayName("校验：昵称含 HTML → Service 层返回 400")
        void shouldReturn400WhenNicknameContainsHtml() throws Exception {
            when(profileService.updateProfile(eq(testUuid), any(ProfileUpdateRequest.class)))
                    .thenThrow(new IllegalArgumentException("Nickname cannot contain HTML tags"));

            ProfileUpdateRequest request = new ProfileUpdateRequest();
            request.setNickname("<b>test</b>");

            mockMvc.perform(put("/api/profile")
                            .param("uuid", testUuid.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400))
                    .andExpect(jsonPath("$.message").value("Nickname cannot contain HTML tags"));
        }

        @Test
        @DisplayName("校验：标签超量 → Service 层返回 400")
        void shouldReturn400WhenTooManyTags() throws Exception {
            when(profileService.updateProfile(eq(testUuid), any(ProfileUpdateRequest.class)))
                    .thenThrow(new IllegalArgumentException("Maximum 5 tags allowed"));

            ProfileUpdateRequest request = new ProfileUpdateRequest();
            request.setNickname("Nick");
            request.setInterestTags(List.of("A", "B", "C", "D", "E", "F"));

            mockMvc.perform(put("/api/profile")
                            .param("uuid", testUuid.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Maximum 5 tags allowed"));
        }
    }

    // ========== GET /api/profile/tags ==========

    @Nested
    @DisplayName("GET /api/profile/tags")
    class GetTags {

        @Test
        @DisplayName("返回 10 项预定义标签 + Result 包装")
        void shouldReturn10Tags() throws Exception {
            when(profileService.getAvailableTags()).thenReturn(List.of(
                    "History", "Food", "Nature", "Photography", "Adventure",
                    "Culture", "Shopping", "Nightlife", "Architecture", "Music"
            ));

            mockMvc.perform(get("/api/profile/tags"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(10))
                    .andExpect(jsonPath("$.data[0]").value("History"))
                    .andExpect(jsonPath("$.data[9]").value("Music"));
        }
    }

    // ========== POST /api/profile/avatar ==========

    @Nested
    @DisplayName("POST /api/profile/avatar")
    class UploadAvatar {

        @Test
        @DisplayName("正常流：上传成功 → Result<AvatarUploadResponse>")
        void shouldReturnAvatarUrl() throws Exception {
            when(profileService.uploadAvatar(eq(testUuid), any()))
                    .thenReturn(new AvatarUploadResponse("/uploads/avatars/test-uuid.jpg"));

            MockMultipartFile file = new MockMultipartFile(
                    "file", "photo.jpg", "image/jpeg", new byte[]{1, 2, 3});

            mockMvc.perform(multipart("/api/profile/avatar")
                            .file(file)
                            .param("uuid", testUuid.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.avatarUrl").value("/uploads/avatars/test-uuid.jpg"));
        }

        @Test
        @DisplayName("异常流：文件类型无效 → 400")
        void shouldReturn400ForInvalidFileType() throws Exception {
            when(profileService.uploadAvatar(eq(testUuid), any()))
                    .thenThrow(new FileUploadException("Please upload a JPG, PNG, or WebP image"));

            MockMultipartFile file = new MockMultipartFile(
                    "file", "doc.pdf", "application/pdf", new byte[]{1, 2, 3});

            mockMvc.perform(multipart("/api/profile/avatar")
                            .file(file)
                            .param("uuid", testUuid.toString()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400))
                    .andExpect(jsonPath("$.message").value("Please upload a JPG, PNG, or WebP image"));
        }

        @Test
        @DisplayName("异常流：文件超过 2MB → 400")
        void shouldReturn400ForOversizedFile() throws Exception {
            when(profileService.uploadAvatar(eq(testUuid), any()))
                    .thenThrow(new FileUploadException("File size must be under 2MB"));

            MockMultipartFile file = new MockMultipartFile(
                    "file", "big.jpg", "image/jpeg", new byte[100]);

            mockMvc.perform(multipart("/api/profile/avatar")
                            .file(file)
                            .param("uuid", testUuid.toString()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("File size must be under 2MB"));
        }
    }
}
