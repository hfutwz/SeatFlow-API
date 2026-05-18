package com.seatflow.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "seatflow.assistant")
public class AssistantProperties {

    /**
     * LLM API 配置
     */
    private LlmConfig llm = new LlmConfig();

    /**
     * RAG 知识库配置
     */
    private RagConfig rag = new RagConfig();

    @Data
    public static class LlmConfig {
        /** API 端点（兼容 OpenAI 格式） */
        private String apiUrl = "https://open.bigmodel.cn/api/paas/v4/chat/completions";
        /** API Key */
        private String apiKey = "";
        /** 模型名称 */
        private String model = "glm-4-flash";
        /** 温度参数 */
        private double temperature = 0.7;
        /** 最大输出 token 数 */
        private int maxTokens = 1024;
    }

    @Data
    public static class RagConfig {
        /** 是否启用 RAG */
        private boolean enabled = true;
        /** 知识库文档目录（classpath 下的路径） */
        private String knowledgePath = "knowledge/";
        /** 检索返回的最大文档片段数 */
        private int topK = 3;
        /** 相似度阈值 */
        private double similarityThreshold = 0.3;
    }
}
