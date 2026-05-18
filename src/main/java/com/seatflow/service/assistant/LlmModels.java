package com.seatflow.service.assistant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * LLM API 兼容 OpenAI 格式的请求/响应模型
 */
public class LlmModels {

    @Data
    @Builder
    public static class ChatRequest {
        private String model;
        private List<Message> messages;
        private Double temperature;
        private Integer max_tokens;
        private List<FunctionDef> tools;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Message {
        private String role;
        private String content;
        private String name;
        private ToolCall tool_calls;
        private String tool_call_id;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ToolCall {
        private String id;
        private String type;
        private FunctionCall function;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FunctionCall {
        private String name;
        private String arguments;
    }

    @Data
    @Builder
    public static class FunctionDef {
        private String type;
        private FunctionSchema function;
    }

    @Data
    @Builder
    public static class FunctionSchema {
        private String name;
        private String description;
        private Map<String, Object> parameters;
    }

    @Data
    public static class ChatResponse {
        private String id;
        private String object;
        private List<Choice> choices;
        private Usage usage;
    }

    @Data
    public static class Choice {
        private Integer index;
        private Message message;
        private String finish_reason;
    }

    @Data
    public static class Usage {
        private Integer prompt_tokens;
        private Integer completion_tokens;
        private Integer total_tokens;
    }
}
