package com.seatflow.controller;

import com.seatflow.common.result.Result;
import com.seatflow.dto.request.ChatRequest;
import com.seatflow.dto.response.ChatResponse;
import com.seatflow.service.AssistantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/assistant")
@RequiredArgsConstructor
public class AssistantController {

    private final AssistantService assistantService;

    @PostMapping("/chat")
    public Result<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        return Result.ok(assistantService.chat(request));
    }
}
