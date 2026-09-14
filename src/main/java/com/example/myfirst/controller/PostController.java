package com.example.myfirst.controller;

import com.example.myfirst.common.Result;
import com.example.myfirst.dto.response.PostResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private static final List<PostResponse> FEATURED_POSTS = List.of(
            new PostResponse(1L, "Li Wei", "https://i.pravatar.cc/40?u=liwei",
                    "3 Days in Chengdu — A Food Lover's Guide",
                    "From spicy hotpot in Yulin district to the adorable giant pandas at the breeding center, Chengdu exceeded every expectation. Here's my curated 3-day itinerary covering the best food spots and must-see attractions that most travel guides miss completely.",
                    1250),
            new PostResponse(2L, "Sarah Chen", "https://i.pravatar.cc/40?u=sarahchen",
                    "Hidden Gems Along the Li River",
                    "Skip the crowded tourist boats and discover these secret spots along the Li River between Guilin and Yangshuo. I spent two weeks exploring caves, bamboo forests, and local villages that barely appear in any guidebook.",
                    876),
            new PostResponse(3L, "Marco Rossi", "https://i.pravatar.cc/40?u=marco",
                    "First-Time Visitor Tips for Xi'an",
                    "Planning your first trip to Xi'an? Here are my top tips after living here for 5 years — from the best time to visit the Terracotta Warriors to finding authentic biangbiang noodles in the Muslim Quarter.",
                    42)
    );

    @GetMapping("/featured")
    public Result<List<PostResponse>> getFeaturedPosts() {
        return Result.success(FEATURED_POSTS);
    }
}
