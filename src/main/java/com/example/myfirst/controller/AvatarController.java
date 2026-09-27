package com.example.myfirst.controller;

import com.example.myfirst.common.Result;
import com.example.myfirst.dto.response.AvatarUploadResponse;
import com.example.myfirst.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class AvatarController {

    private final ProfileService profileService;

    @PostMapping("/avatar")
    public Result<AvatarUploadResponse> uploadAvatar(
            @RequestParam("uuid") UUID userUuid,
            @RequestParam("file") MultipartFile file) {
        AvatarUploadResponse response = profileService.uploadAvatar(userUuid, file);
        return Result.success(response);
    }
}
