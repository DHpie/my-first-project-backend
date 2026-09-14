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
                    "/images/destinations/chengdu.jpg", "Hot"),
            new DestinationResponse(2L, "Xi'an", "xi-an",
                    "Walk through history along the ancient city walls",
                    "/images/destinations/xian.jpg", "Trending"),
            new DestinationResponse(3L, "Guilin", "guilin",
                    "Stunning karst landscapes along the Li River",
                    "/images/destinations/guilin.jpg", "Editor's Pick"),
            new DestinationResponse(4L, "Shanghai", "shanghai",
                    "Where futuristic skyline meets centuries of culture",
                    "/images/destinations/shanghai.jpg", "Hot"),
            new DestinationResponse(5L, "Lhasa", "lhasa",
                    "The rooftop of the world and heart of Tibetan culture",
                    "/images/destinations/lhasa.jpg", "Editor's Pick")
    );

    @GetMapping("/featured")
    public Result<List<DestinationResponse>> getFeaturedDestinations() {
        return Result.success(FEATURED_DESTINATIONS);
    }
}
