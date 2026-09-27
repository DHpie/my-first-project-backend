package com.example.myfirst.service;

import com.example.myfirst.dto.request.ProfileUpdateRequest;
import com.example.myfirst.dto.response.AvatarUploadResponse;
import com.example.myfirst.dto.response.ProfileResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface ProfileService {

    ProfileResponse getProfile(UUID userUuid);

    ProfileResponse updateProfile(UUID userUuid, ProfileUpdateRequest request);

    AvatarUploadResponse uploadAvatar(UUID userUuid, MultipartFile file);

    List<String> getAvailableTags();
}
