package com.example.myfirst.controller;

import com.example.myfirst.common.Result;
import com.example.myfirst.dto.request.ProfileUpdateRequest;
import com.example.myfirst.dto.response.ProfileResponse;
import com.example.myfirst.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    public Result<ProfileResponse> getProfile(@RequestParam("uuid") UUID userUuid) {
        ProfileResponse response = profileService.getProfile(userUuid);
        return Result.success(response);
    }

    @PutMapping
    public Result<ProfileResponse> updateProfile(
            @RequestParam("uuid") UUID userUuid,
            @Valid @RequestBody ProfileUpdateRequest request) {
        ProfileResponse response = profileService.updateProfile(userUuid, request);
        return Result.success(response);
    }

    @GetMapping("/tags")
    public Result<List<String>> getAvailableTags() {
        List<String> tags = profileService.getAvailableTags();
        return Result.success(tags);
    }
}
