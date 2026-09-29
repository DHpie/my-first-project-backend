package com.example.myfirst.controller;

import com.example.myfirst.common.Result;
import com.example.myfirst.dto.request.ChatRequest;
import com.example.myfirst.dto.response.ChatResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Deprecated
@RestController
@RequestMapping("/api/ai")
public class AiChatController {

    @PostMapping("/chat")
    public Result<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        String message = request.getMessage().toLowerCase();
        String reply = generateMockReply(message);
        return Result.success(new ChatResponse(reply));
    }

    private String generateMockReply(String message) {
        if (message.contains("beijing")) {
            return "Beijing is a must-visit! Check out the Great Wall at Mutianyu, the Forbidden City, and don't miss the local Peking duck. I recommend trying the street food at Wangfujing Snack Street.";
        }
        if (message.contains("food") || message.contains("eat")) {
            return "China has an incredible food scene! Try Sichuan hotpot in Chengdu, dim sum in Guangzhou, and Peking duck in Beijing. Each region has its own unique flavors and specialties.";
        }
        if (message.contains("shanghai")) {
            return "Shanghai is a fascinating blend of old and new! Visit the Bund for stunning skyline views, explore Yu Garden for traditional Chinese architecture, and enjoy the vibrant art scene in M50.";
        }
        if (message.contains("chengdu")) {
            return "Chengdu is famous for its laid-back lifestyle and amazing food! Visit the Giant Panda Breeding Research Base, stroll through Jinli Ancient Street, and of course, try authentic Sichuan cuisine.";
        }
        if (message.contains("great wall") || message.contains("wall")) {
            return "The Great Wall is one of the most iconic landmarks in China! The Mutianyu section near Beijing is less crowded and offers beautiful views. Consider visiting during autumn for the best scenery.";
        }
        return "China is full of amazing destinations and rich cultural experiences! I can help you plan your trip — try asking about specific cities like Beijing, Shanghai, or Chengdu, or ask about food, attractions, and travel tips!";
    }
}
