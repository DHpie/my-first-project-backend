package com.example.myfirst.controller;

import com.example.myfirst.common.Result;
import com.example.myfirst.dto.response.DestinationResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/destinations")
public class DestinationController {

    private static final List<DestinationResponse> FEATURED_DESTINATIONS = List.of(
            new DestinationResponse(1L, "Chengdu", "chengdu",
                    "Home of giant pandas and world-famous Sichuan cuisine",
                    "https://images.unsplash.com/photo-1599571234909-29ed5d1321d6?w=800&h=600&fit=crop", "Hot"),
            new DestinationResponse(2L, "Xi'an", "xi-an",
                    "Walk through history along the ancient city walls",
                    "https://images.unsplash.com/photo-1591012911207-0dbac31f37da?w=800&h=600&fit=crop", "Trending"),
            new DestinationResponse(3L, "Guilin", "guilin",
                    "Stunning karst landscapes along the Li River",
                    "https://images.unsplash.com/photo-1537531383496-f4749b8032cf?w=800&h=600&fit=crop", "Editor's Pick"),
            new DestinationResponse(4L, "Shanghai", "shanghai",
                    "Where futuristic skyline meets centuries of culture",
                    "https://images.unsplash.com/photo-1537519646099-5eb3a4f1a3f3?w=800&h=600&fit=crop", "Hot"),
            new DestinationResponse(5L, "Lhasa", "lhasa",
                    "The rooftop of the world and heart of Tibetan culture",
                    "https://images.unsplash.com/photo-1545569341-9eb8b30979d9?w=800&h=600&fit=crop", "Editor's Pick")
    );

    @GetMapping("/featured")
    public Result<List<DestinationResponse>> getFeaturedDestinations() {
        return Result.success(FEATURED_DESTINATIONS);
    }
}
