package com.example.myfirst.service.impl;

import com.example.myfirst.common.FileUploadException;
import com.example.myfirst.dto.request.ProfileUpdateRequest;
import com.example.myfirst.dto.response.AvatarUploadResponse;
import com.example.myfirst.dto.response.ProfileResponse;
import com.example.myfirst.entity.User;
import com.example.myfirst.mapper.UserMapper;
import com.example.myfirst.repository.UserRepository;
import com.example.myfirst.service.ProfileService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private static final long MAX_FILE_SIZE = 2 * 1024 * 1024; // 2MB
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp"
    );
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final List<String> AVAILABLE_TAGS = List.of(
            "History", "Food", "Nature", "Photography", "Adventure",
            "Culture", "Shopping", "Nightlife", "Architecture", "Music"
    );

    private final UserRepository userRepository;

    @Value("${app.upload.dir:uploads/avatars}")
    private String uploadDir;

    @Override
    @Transactional(readOnly = true)
    public ProfileResponse getProfile(UUID userUuid) {
        User user = findUserByUuid(userUuid);
        return toProfileResponse(user);
    }

    @Override
    @Transactional
    public ProfileResponse updateProfile(UUID userUuid, ProfileUpdateRequest request) {
        User user = findUserByUuid(userUuid);

        // 校验昵称不能为空白
        if (request.getNickname() != null && request.getNickname().isBlank()) {
            throw new IllegalArgumentException("Nickname is required");
        }

        // 校验昵称不含 HTML 标签
        if (request.getNickname() != null && containsHtml(request.getNickname())) {
            throw new IllegalArgumentException("Nickname cannot contain HTML tags");
        }

        // 校验兴趣标签数量
        if (request.getInterestTags() != null && request.getInterestTags().size() > 5) {
            throw new IllegalArgumentException("Maximum 5 tags allowed");
        }

        // 校验兴趣标签是否在预定义列表中
        if (request.getInterestTags() != null) {
            for (String tag : request.getInterestTags()) {
                if (!AVAILABLE_TAGS.contains(tag)) {
                    throw new IllegalArgumentException("Invalid interest tag: " + tag);
                }
            }
        }

        // 更新字段
        if (request.getNickname() != null) {
            user.setNickname(request.getNickname());
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }
        if (request.getInterestTags() != null) {
            user.setInterestTags(UserMapper.serializeTags(request.getInterestTags()));
        }

        User updated = userRepository.save(user);
        return toProfileResponse(updated);
    }

    @Override
    @Transactional
    public AvatarUploadResponse uploadAvatar(UUID userUuid, MultipartFile file) {
        // 校验文件是否为空
        if (file.isEmpty()) {
            throw new FileUploadException("Please upload a JPG, PNG, or WebP image");
        }

        // 校验文件大小
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new FileUploadException("File size must be under 2MB");
        }

        // 校验 MIME 类型
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new FileUploadException("Please upload a JPG, PNG, or WebP image");
        }

        // 校验文件扩展名
        String originalFilename = file.getOriginalFilename();
        String extension = getExtension(originalFilename);
        if (extension == null || !ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new FileUploadException("Please upload a JPG, PNG, or WebP image");
        }

        // 生成 UUID 文件名
        String newFilename = UUID.randomUUID() + "." + extension.toLowerCase();

        // 确保上传目录存在
        Path uploadPath = Paths.get(uploadDir);
        try {
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // 保存文件
            Path filePath = uploadPath.resolve(newFilename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // 更新用户头像 URL
            User user = findUserByUuid(userUuid);
            String oldAvatarUrl = user.getAvatarUrl();

            String avatarUrl = "/uploads/avatars/" + newFilename;
            user.setAvatarUrl(avatarUrl);
            userRepository.save(user);

            // 异步清理旧头像
            if (oldAvatarUrl != null && !oldAvatarUrl.isBlank()) {
                deleteOldAvatar(oldAvatarUrl);
            }

            return new AvatarUploadResponse(avatarUrl);

        } catch (IOException e) {
            log.error("Failed to upload avatar", e);
            throw new FileUploadException("Failed to upload avatar");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getAvailableTags() {
        return AVAILABLE_TAGS;
    }

    private User findUserByUuid(UUID userUuid) {
        return userRepository.findByUuid(userUuid)
                .orElseThrow(() -> new EntityNotFoundException("User not found with uuid: " + userUuid));
    }

    private ProfileResponse toProfileResponse(User user) {
        return new ProfileResponse(
                user.getUuid().toString(),
                user.getUsername(),
                user.getEmail(),
                user.getNickname(),
                user.getBio(),
                user.getAvatarUrl(),
                UserMapper.deserializeTags(user.getInterestTags()),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return null;
        }
        return filename.substring(filename.lastIndexOf('.') + 1);
    }

    private void deleteOldAvatar(String oldAvatarUrl) {
        try {
            // 从 URL 中提取文件路径
            String filename = oldAvatarUrl.substring(oldAvatarUrl.lastIndexOf('/') + 1);
            Path oldFilePath = Paths.get(uploadDir).resolve(filename);
            if (Files.exists(oldFilePath)) {
                Files.delete(oldFilePath);
                log.info("Deleted old avatar: {}", oldFilePath);
            }
        } catch (IOException e) {
            log.warn("Failed to delete old avatar: {}", oldAvatarUrl, e);
        }
    }

    private boolean containsHtml(String text) {
        return text.matches(".*<[^>]+>.*");
    }
}
