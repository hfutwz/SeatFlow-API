package com.seatflow.service.assistant;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RAG 知识库 — 基于 TF-IDF 的简单向量检索
 * <p>
 * 知识库文档放在 classpath:knowledge/ 目录下，每个 .md 或 .txt 文件为一个知识片段。
 * 系统启动时自动加载，按段落切分为 chunk，查询时基于关键词匹配度检索最相关的片段。
 * <p>
 * 生产环境可替换为向量数据库（Milvus、Pinecone 等）+ Embedding 模型。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KnowledgeBase {

    private final ObjectMapper objectMapper;

    /** 知识片段列表 */
    private final List<KnowledgeChunk> chunks = new ArrayList<>();
    /** 关键词倒排索引 */
    private final Map<String, Set<Integer>> invertedIndex = new ConcurrentHashMap<>();
    /** IDF 值 */
    private final Map<String, Double> idf = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        try {
            loadKnowledge();
        } catch (Exception e) {
            log.warn("知识库加载失败，将使用空知识库: {}", e.getMessage());
        }
    }

    /**
     * 加载 classpath:knowledge/ 下的所有文档
     */
    private void loadKnowledge() throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath:knowledge/**/*");

        int docCount = 0;
        for (Resource resource : resources) {
            String filename = resource.getFilename();
            if (filename == null || (!filename.endsWith(".md") && !filename.endsWith(".txt"))) {
                continue;
            }

            String content = resource.getContentAsString(StandardCharsets.UTF_8);
            String source = filename.replace(".md", "").replace(".txt", "");

            // 按段落切分（双换行或 # 标题）
            String[] paragraphs = content.split("\\n\\s*\\n|(?=^#{1,3}\\s)");
            for (String paragraph : paragraphs) {
                String trimmed = paragraph.trim();
                if (trimmed.length() < 20) continue; // 跳过过短的段落

                int index = chunks.size();
                chunks.add(new KnowledgeChunk(index, source, trimmed));
                docCount++;

                // 构建倒排索引
                Set<String> tokens = tokenize(trimmed);
                for (String token : tokens) {
                    invertedIndex.computeIfAbsent(token, k -> ConcurrentHashMap.newKeySet()).add(index);
                }
            }
        }

        // 计算 IDF
        int totalDocs = chunks.size();
        for (Map.Entry<String, Set<Integer>> entry : invertedIndex.entrySet()) {
            idf.put(entry.getKey(), Math.log((double) totalDocs / (entry.getValue().size() + 1)));
        }

        log.info("知识库加载完成: {} 个文档, {} 个片段, {} 个关键词", resources.length, docCount, invertedIndex.size());
    }

    /**
     * 检索与查询最相关的知识片段
     */
    public List<String> search(String query, int topK) {
        if (chunks.isEmpty()) return List.of();

        Set<String> queryTokens = tokenize(query);
        Map<Integer, Double> scores = new HashMap<>();

        for (String token : queryTokens) {
            Set<Integer> docIds = invertedIndex.getOrDefault(token, Set.of());
            double tokenIdf = idf.getOrDefault(token, 0.0);
            for (Integer docId : docIds) {
                // TF: 该 token 在文档中出现的次数
                String docContent = chunks.get(docId).content().toLowerCase();
                long tf = countOccurrences(docContent, token);
                double tfidf = tf * tokenIdf;
                scores.merge(docId, tfidf, Double::sum);
            }
        }

        // 排序取 topK
        return scores.entrySet().stream()
                .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed())
                .limit(topK)
                .map(entry -> chunks.get(entry.getKey()).content())
                .toList();
    }

    /**
     * 简易中文分词：按字符 + 常见词切分
     */
    private Set<String> tokenize(String text) {
        Set<String> tokens = new HashSet<>();
        String lower = text.toLowerCase();

        // 英文单词
        String[] words = lower.split("[^a-zA-Z0-9\\u4e00-\\u9fff]+");
        for (String word : words) {
            if (word.isEmpty()) continue;
            tokens.add(word);
            // 中文 bigram
            if (word.matches(".*[\\u4e00-\\u9fff].*")) {
                for (int i = 0; i < word.length() - 1; i++) {
                    if (isChinese(word.charAt(i)) && isChinese(word.charAt(i + 1))) {
                        tokens.add(word.substring(i, i + 2));
                    }
                }
                // 中文 trigram
                for (int i = 0; i < word.length() - 2; i++) {
                    if (isChinese(word.charAt(i)) && isChinese(word.charAt(i + 1)) && isChinese(word.charAt(i + 2))) {
                        tokens.add(word.substring(i, i + 3));
                    }
                }
            }
        }
        return tokens;
    }

    private boolean isChinese(char c) {
        return c >= '\u4e00' && c <= '\u9fff';
    }

    private long countOccurrences(String text, String token) {
        long count = 0;
        int idx = 0;
        while ((idx = text.indexOf(token, idx)) != -1) {
            count++;
            idx += token.length();
        }
        return count;
    }

    /**
     * 获取知识库片段总数
     */
    public int size() {
        return chunks.size();
    }

    /**
     * 重新加载知识库（热更新）
     */
    public void reload() {
        chunks.clear();
        invertedIndex.clear();
        idf.clear();
        init();
    }

    private record KnowledgeChunk(int index, String source, String content) {}
}
