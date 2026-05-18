package com.seatflow.service.assistant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seatflow.config.AssistantProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * LLM API 客户端 — 兼容 OpenAI Chat Completions 格式
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LlmClient {

    private final AssistantProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 调用 LLM Chat API
     */
    public LlmModels.ChatResponse chat(LlmModels.ChatRequest request) {
        String apiUrl = properties.getLlm().getApiUrl();
        String apiKey = properties.getLlm().getApiKey();

        if (apiKey == null || apiKey.isBlank()) {
            throw new RuntimeException("LLM API Key 未配置，请在 application.yml 中设置 seatflow.assistant.llm.api-key");
        }

        // 设置 model
        if (request.getModel() == null) {
            request.setModel(properties.getLlm().getModel());
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        try {
            String body = objectMapper.writeValueAsString(request);
            log.debug("LLM request: {}", body.length() > 500 ? body.substring(0, 500) + "..." : body);

            HttpEntity<String> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                LlmModels.ChatResponse result = objectMapper.readValue(response.getBody(), LlmModels.ChatResponse.class);
                log.debug("LLM response tokens: {}", result.getUsage());
                return result;
            } else {
                log.error("LLM API error: status={}, body={}", response.getStatusCode(), response.getBody());
                throw new RuntimeException("LLM API 调用失败: " + response.getStatusCode());
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("LLM API 调用异常", e);
            throw new RuntimeException("LLM API 调用异常: " + e.getMessage());
        }
    }

    /**
     * 简单调用（无 Function Calling）
     */
    public String simpleChat(List<LlmModels.Message> messages) {
        LlmModels.ChatRequest request = LlmModels.ChatRequest.builder()
                .messages(messages)
                .temperature(properties.getLlm().getTemperature())
                .max_tokens(properties.getLlm().getMaxTokens())
                .build();

        LlmModels.ChatResponse response = chat(request);
        if (response.getChoices() != null && !response.getChoices().isEmpty()) {
            return response.getChoices().get(0).getMessage().getContent();
        }
        return "抱歉，AI 助手暂时无法回复。";
    }
}
