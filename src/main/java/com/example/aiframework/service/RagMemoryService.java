package com.example.aiframework.service;

import com.example.aiframework.entity.ChatMessageEntity;
import com.example.aiframework.repository.ChatMessageRepository;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * RAG 记忆检索服务 - 支持混合检索、记忆压缩、重排序
 * 
 * 功能特性：
 * 1. 向量检索 - 基于语义相似度
 * 2. 关键词检索 - BM25 算法
 * 3. 混合检索 - 向量 + 关键词加权融合
 * 4. 记忆压缩 - 长对话自动摘要
 * 5. 重排序 - Cross-Encoder 精排
 */
@Service
public class RagMemoryService {
    
    private static final Logger log = LoggerFactory.getLogger(RagMemoryService.class);
    
    // ========== 依赖注入 ==========
    @Autowired(required = false)
    private EmbeddingStore<TextSegment> embeddingStore;
    
    @Autowired(required = false)
    private EmbeddingModel embeddingModel;
    
    @Autowired(required = false)
    private ChatLanguageModel chatModel;
    
    @Autowired
    private ChatMessageRepository messageRepository;
    
    // ========== 配置参数 ==========
    @Value("${rag.memory.enabled:false}")
    private boolean ragEnabled;
    
    @Value("${rag.memory.top-k:5}")
    private int topK;
    
    @Value("${rag.memory.similarity-threshold:0.7}")
    private double vectorThreshold;
    
    @Value("${rag.memory.keyword-weight:0.3}")
    private double keywordWeight; // 关键词权重，默认 30%
    
    @Value("${rag.memory.compress.enabled:false}")
    private boolean compressEnabled;
    
    @Value("${rag.memory.compress.message-threshold:20}")
    private int compressThreshold; // 消息数量达到多少时压缩
    
    @Value("${rag.memory.rerank.enabled:false}")
    private boolean rerankEnabled;
    
    @Value("${rag.memory.rerank.top-k:3}")
    private int rerankTopK; // 重排序后保留的数量
    
    // ========== 内部缓存 ==========
    /** 会话记忆缓存 (sessionId -> 已索引的消息 ID) */
    private final Map<String, List<String>> indexedMessages = new ConcurrentHashMap<>();
    
    /** 会话摘要缓存 (sessionId -> 摘要) */
    private final Map<String, String> sessionSummaries = new ConcurrentHashMap<>();
    
    /** 关键词索引 (sessionId -> 词频映射) */
    private final Map<String, Map<String, Integer>> keywordIndex = new ConcurrentHashMap<>();
    
    /** 文档缓存 (用于 BM25 计算) */
    private final Map<String, List<String>> sessionDocuments = new ConcurrentHashMap<>();
    
    // ========== 中文分词简单实现 ==========
    private static final Pattern CHINESE_PATTERN = Pattern.compile("[\\u4e00-\\u9fa5]+|[a-zA-Z0-9]+");
    
    @PostConstruct
    public void init() {
        log.info("RAG 记忆服务初始化完成");
        log.info("  - 向量检索：{}", ragEnabled ? "已启用" : "已禁用");
        log.info("  - 混合检索权重：向量 {:.0%}, 关键词 {:.0%}", 1 - keywordWeight, keywordWeight);
        log.info("  - 记忆压缩：{}", compressEnabled ? "已启用 (阈值：" + compressThreshold + "条)" : "已禁用");
        log.info("  - 重排序：{}", rerankEnabled ? "已启用 (Top " + rerankTopK + ")" : "已禁用");
    }
    
    // ==================== 核心检索方法 ====================
    
    /**
     * 【混合检索】检索相关记忆 - 向量 + 关键词融合
     * 
     * @param sessionId 会话 ID
     * @param query 查询文本
     * @return 相关的历史消息列表
     */
    public List<String> retrieveRelevantMemories(String sessionId, String query) {
        if (!ragEnabled) {
            return new ArrayList<>();
        }
        
        log.info("RAG 混合检索：sessionId={}, query={}", sessionId, query);
        
        try {
            List<RankedMatch> allMatches = new ArrayList<>();
            
            // 1. 向量检索
            if (embeddingStore != null && embeddingModel != null) {
                List<EmbeddingMatch<TextSegment>> vectorMatches = doVectorSearch(sessionId, query);
                for (EmbeddingMatch<TextSegment> match : vectorMatches) {
                    allMatches.add(new RankedMatch(match, "vector"));
                }
                log.info("向量检索到 {} 条匹配", vectorMatches.size());
            }
            
            // 2. 关键词检索 (BM25)
            List<KeywordMatch> keywordMatches = doKeywordSearch(sessionId, query);
            for (KeywordMatch match : keywordMatches) {
                allMatches.add(new RankedMatch(match));
            }
            log.info("关键词检索到 {} 条匹配", keywordMatches.size());
            
            // 3. 融合排序 (RRF + 分数加权)
            List<RankedMatch> fused = fuseResults(allMatches, query);
            
            // 4. 重排序 (可选)
            if (rerankEnabled && chatModel != null && !fused.isEmpty()) {
                fused = rerankResults(query, fused);
            }
            
            // 5. 提取文本
            List<String> results = fused.stream()
                .limit(rerankEnabled ? rerankTopK : topK)
                .map(m -> m.getSegment().text())
                .collect(Collectors.toList());
            
            log.info("RAG 最终返回 {} 条记忆", results.size());
            return results;
            
        } catch (Exception e) {
            log.error("RAG 检索失败：{}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 向量检索
     */
    private List<EmbeddingMatch<TextSegment>> doVectorSearch(String sessionId, String query) {
        try {
            Embedding queryEmbedding = embeddingModel.embed(query).content();
            List<EmbeddingMatch<TextSegment>> matches = embeddingStore.findRelevant(queryEmbedding, topK * 2);
            
            // 过滤会话和阈值
            return matches.stream()
                .filter(m -> {
                    TextSegment segment = m.embedded();
                    if (segment == null) return false;
                    String matchSessionId = segment.metadata().getString("sessionId");
                    return (sessionId.equals(matchSessionId) || matchSessionId == null)
                        && m.score() >= vectorThreshold;
                })
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            log.warn("向量检索失败：{}", e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * 关键词检索 (BM25 简化版)
     */
    private List<KeywordMatch> doKeywordSearch(String sessionId, String query) {
        List<String> docs = sessionDocuments.getOrDefault(sessionId, new ArrayList<>());
        if (docs.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 分词
        List<String> queryTokens = tokenize(query);
        if (queryTokens.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 计算 BM25 分数
        List<KeywordMatch> results = new ArrayList<>();
        for (int i = 0; i < docs.size(); i++) {
            double score = calculateBM25(docs.get(i), queryTokens);
            if (score > 0) {
                results.add(new KeywordMatch(i, docs.get(i), score));
            }
        }
        
        // 排序
        results.sort((a, b) -> Double.compare(b.score, a.score));
        return results.stream().limit(topK * 2).collect(Collectors.toList());
    }
    
    /**
     * 简单中文分词
     */
    private List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        var matcher = CHINESE_PATTERN.matcher(text.toLowerCase());
        while (matcher.find()) {
            String token = matcher.group();
            if (token.length() >= 2) { // 过滤单字
                tokens.add(token);
            }
        }
        return tokens;
    }
    
    /**
     * 简化 BM25 评分
     */
    private double calculateBM25(String doc, List<String> queryTokens) {
        Map<String, Integer> docFreq = keywordIndex.computeIfAbsent(doc, k -> {
            Map<String, Integer> freq = new HashMap<>();
            for (String token : tokenize(doc)) {
                freq.put(token, freq.getOrDefault(token, 0) + 1);
            }
            return freq;
        });
        
        double score = 0;
        double k1 = 1.5; // BM25 参数
        double b = 0.75;
        
        for (String token : queryTokens) {
            int tf = docFreq.getOrDefault(token, 0);
            if (tf > 0) {
                // 简化 IDF (假设所有词 IDF=1)
                double idf = 1.0;
                double docLen = docFreq.values().stream().mapToInt(Integer::intValue).sum();
                double avgDocLen = 50; // 假设平均文档长度
                
                score += idf * (tf * (k1 + 1)) / (tf + k1 * (1 - b + b * docLen / avgDocLen));
            }
        }
        
        return score;
    }
    
    /**
     * 结果融合 (RRF + 分数加权)
     */
    private List<RankedMatch> fuseResults(List<RankedMatch> matches, String query) {
        // 按来源分组
        Map<String, List<RankedMatch>> bySource = matches.stream()
            .collect(Collectors.groupingBy(RankedMatch::getSource));
        
        List<RankedMatch> fused = new ArrayList<>();
        
        // 计算融合分数
        for (RankedMatch match : matches) {
            double finalScore;
            
            if ("vector".equals(match.getSource())) {
                // 向量分数归一化
                double vectorScore = match.getEmbeddingMatch().score();
                
                // 查找对应的关键词分数
                double keywordScore = findKeywordScore(match.getSegment().text(), bySource);
                
                // 加权融合
                finalScore = (1 - keywordWeight) * vectorScore + keywordWeight * keywordScore;
            } else {
                // 关键词分数归一化
                double keywordScore = normalizeKeywordScore(match.getKeywordMatch().score);
                
                // 查找对应的向量分数
                double vectorScore = findVectorScore(match.getSegment(), bySource);
                
                // 加权融合
                finalScore = keywordWeight * keywordScore + (1 - keywordWeight) * vectorScore;
            }
            
            match.setFinalScore(finalScore);
            fused.add(match);
        }
        
        // 按最终分数排序
        fused.sort((a, b) -> Double.compare(b.getFinalScore(), a.getFinalScore()));
        return fused;
    }
    
    private double findKeywordScore(String text, Map<String, List<RankedMatch>> bySource) {
        List<RankedMatch> keywordMatches = bySource.getOrDefault("keyword", new ArrayList<>());
        for (RankedMatch m : keywordMatches) {
            if (m.getKeywordMatch() != null && m.getKeywordMatch().text.equals(text)) {
                return normalizeKeywordScore(m.getKeywordMatch().score);
            }
        }
        return 0;
    }
    
    private double findVectorScore(TextSegment segment, Map<String, List<RankedMatch>> bySource) {
        List<RankedMatch> vectorMatches = bySource.getOrDefault("vector", new ArrayList<>());
        for (RankedMatch m : vectorMatches) {
            if (m.getEmbeddingMatch() != null && m.getEmbeddingMatch().embedded().equals(segment)) {
                return m.getEmbeddingMatch().score();
            }
        }
        return 0;
    }
    
    private double normalizeKeywordScore(double rawScore) {
        // 简单归一化到 0-1
        return Math.min(1.0, rawScore / 10.0);
    }
    
    /**
     * 重排序 (使用 LLM 作为 Cross-Encoder)
     */
    private List<RankedMatch> rerankResults(String query, List<RankedMatch> candidates) {
        log.info("开始重排序 {} 条候选结果", candidates.size());
        
        try {
            // 构建重排序 Prompt
            StringBuilder prompt = new StringBuilder();
            prompt.append("请对以下候选文本与查询的相关性进行评分 (0-10 分，10 分最相关)。\n\n");
            prompt.append("查询：").append(query).append("\n\n");
            prompt.append("候选文本:\n");
            
            for (int i = 0; i < candidates.size(); i++) {
                prompt.append("[").append(i).append("] ").append(candidates.get(i).getSegment().text()).append("\n");
            }
            
            prompt.append("\n请按格式输出评分 (只输出 JSON 数组，如：[8, 5, 9, 3, 7]):\n");
            
            // 调用 LLM 评分
            String response = chatModel.generate(prompt.toString());
            List<Double> scores = parseScores(response, candidates.size());
            
            // 应用评分
            for (int i = 0; i < Math.min(candidates.size(), scores.size()); i++) {
                candidates.get(i).setFinalScore(scores.get(i) / 10.0);
            }
            
            // 重新排序
            candidates.sort((a, b) -> Double.compare(b.getFinalScore(), a.getFinalScore()));
            
            log.info("重排序完成");
            return candidates;
            
        } catch (Exception e) {
            log.warn("重排序失败，使用原始排序：{}", e.getMessage());
            return candidates;
        }
    }
    
    /**
     * 解析 LLM 返回的评分
     */
    private List<Double> parseScores(String response, int expectedCount) {
        List<Double> scores = new ArrayList<>();
        
        // 提取 JSON 数组
        int start = response.indexOf("[");
        int end = response.lastIndexOf("]");
        
        if (start != -1 && end != -1 && end > start) {
            String arrayStr = response.substring(start, end + 1);
            String[] parts = arrayStr.replaceAll("[\\[\\]\\s]", "").split(",");
            
            for (String part : parts) {
                try {
                    scores.add(Double.parseDouble(part.trim()));
                } catch (NumberFormatException e) {
                    scores.add(5.0); // 默认分
                }
            }
        }
        
        // 补齐默认分
        while (scores.size() < expectedCount) {
            scores.add(5.0);
        }
        
        return scores;
    }
    
    // ==================== 记忆压缩 ====================
    
    /**
     * 索引消息 (带压缩检测)
     */
    public void indexMessage(ChatMessageEntity message) {
        if (!ragEnabled) {
            return;
        }
        
        String sessionId = message.getSessionId();
        
        // 检查是否需要压缩
        if (compressEnabled) {
            List<String> indexed = indexedMessages.computeIfAbsent(sessionId, k -> new ArrayList<>());
            indexed.add(message.getId());
            
            if (indexed.size() >= compressThreshold) {
                log.info("会话 {} 消息数达到 {}，触发记忆压缩", sessionId, indexed.size());
                compressSession(sessionId);
                indexed.clear();
                return;
            }
        }
        
        // 正常索引
        doIndexMessage(message);
    }
    
    /**
     * 压缩会话历史 (生成摘要)
     */
    public void compressSession(String sessionId) {
        if (!compressEnabled || chatModel == null) {
            return;
        }
        
        try {
            List<ChatMessageEntity> history = messageRepository.findBySessionId(sessionId);
            if (history.size() < compressThreshold) {
                return;
            }
            
            // 构建对话历史
            StringBuilder conversation = new StringBuilder();
            for (ChatMessageEntity msg : history) {
                String role = "user".equals(msg.getRole()) ? "用户" : "助手";
                conversation.append(role).append(": ").append(msg.getContent()).append("\n");
            }
            
            // 调用 LLM 生成摘要
            String summary = generateSummary(conversation.toString());
            sessionSummaries.put(sessionId, summary);
            
            log.info("会话 {} 压缩完成，摘要长度：{} 字", sessionId, summary.length());
            
            // 将摘要作为特殊记忆索引
            ChatMessageEntity summaryMsg = new ChatMessageEntity();
            summaryMsg.setId("summary_" + sessionId);
            summaryMsg.setSessionId(sessionId);
            summaryMsg.setRole("system");
            summaryMsg.setContent("[对话摘要] " + summary);
            summaryMsg.setCreateTime(LocalDateTime.now());
            
            doIndexMessage(summaryMsg);
            
        } catch (Exception e) {
            log.error("会话压缩失败：{}", e.getMessage(), e);
        }
    }
    
    /**
     * 生成对话摘要
     */
    private String generateSummary(String conversation) {
        String prompt = "请总结以下对话的核心内容，包括：\n" +
                       "1. 用户的主要需求/问题\n" +
                       "2. 已解决的关键点\n" +
                       "3. 待处理的事项\n" +
                       "4. 重要事实/偏好\n\n" +
                       "要求：简洁明了，200 字以内。\n\n" +
                       "对话内容:\n" + conversation;
        
        return chatModel.generate(prompt);
    }
    
    /**
     * 普通索引消息
     */
    private void doIndexMessage(ChatMessageEntity message) {
        if (embeddingStore == null || embeddingModel == null) {
            return;
        }
        
        try {
            // 更新关键词索引
            String sessionId = message.getSessionId();
            sessionDocuments.computeIfAbsent(sessionId, k -> new ArrayList<>()).add(message.getContent());
            keywordIndex.remove(message.getContent()); // 清除缓存，强制重新计算
            
            // 生成向量
            Embedding embedding = embeddingModel.embed(message.getContent()).content();
            
            // 创建文本片段 (带元数据)
            dev.langchain4j.data.document.Metadata metadata = new dev.langchain4j.data.document.Metadata();
            metadata.put("sessionId", message.getSessionId());
            metadata.put("messageId", message.getId());
            metadata.put("role", message.getRole());
            metadata.put("timestamp", message.getCreateTime().toString());
            
            TextSegment segment = TextSegment.from(message.getContent(), metadata);
            
            // 添加到向量存储
            embeddingStore.add(embedding, segment);
            
            log.debug("索引消息：{}", message.getId());
            
        } catch (Exception e) {
            log.error("索引消息失败：{}", e.getMessage(), e);
        }
    }
    
    // ==================== 辅助方法 ====================
    
    /**
     * 批量索引会话历史
     */
    public void indexSessionHistory(String sessionId) {
        if (!ragEnabled) {
            return;
        }
        
        List<ChatMessageEntity> history = messageRepository.findBySessionId(sessionId);
        for (ChatMessageEntity message : history) {
            doIndexMessage(message);
        }
        
        log.info("批量索引会话 {} 的 {} 条消息", sessionId, history.size());
    }
    
    /**
     * 获取会话摘要
     */
    public String getSessionSummary(String sessionId) {
        return sessionSummaries.getOrDefault(sessionId, null);
    }
    
    /**
     * 构建 RAG 增强的提示词 (带摘要)
     */
    public String buildRagPrompt(String sessionId, String query, List<String> relevantMemories) {
        StringBuilder prompt = new StringBuilder();
        
        // 1. 先添加会话摘要 (如果有)
        String summary = getSessionSummary(sessionId);
        if (summary != null && !summary.isEmpty()) {
            prompt.append("【历史对话摘要】\n");
            prompt.append(summary).append("\n\n");
        }
        
        // 2. 添加相关记忆
        if (relevantMemories != null && !relevantMemories.isEmpty()) {
            prompt.append("【相关历史记忆】\n");
            for (String memory : relevantMemories) {
                prompt.append("- ").append(memory).append("\n");
            }
            prompt.append("\n");
        }
        
        // 3. 添加当前查询
        prompt.append("请基于以上信息回答用户的问题：\n\n");
        prompt.append(query);
        
        return prompt.toString();
    }
    
    // ==================== 内部类 ====================
    
    /**
     * 关键词匹配结果
     */
    static class KeywordMatch {
        int docId;
        String text;
        double score;
        
        KeywordMatch(int docId, String text, double score) {
            this.docId = docId;
            this.text = text;
            this.score = score;
        }
    }
    
    /**
     * 统一排序匹配结果
     */
    static class RankedMatch {
        private EmbeddingMatch<TextSegment> embeddingMatch;
        private KeywordMatch keywordMatch;
        private String source;
        private double finalScore;
        
        RankedMatch(EmbeddingMatch<TextSegment> match, String source) {
            this.embeddingMatch = match;
            this.source = source;
        }
        
        RankedMatch(KeywordMatch match) {
            this.keywordMatch = match;
            this.source = "keyword";
        }
        
        TextSegment getSegment() {
            return embeddingMatch != null ? embeddingMatch.embedded() 
                : TextSegment.from(keywordMatch.text);
        }
        
        String getSource() {
            return source;
        }
        
        double getFinalScore() {
            return finalScore;
        }
        
        void setFinalScore(double score) {
            this.finalScore = score;
        }
        
        EmbeddingMatch<TextSegment> getEmbeddingMatch() {
            return embeddingMatch;
        }
        
        KeywordMatch getKeywordMatch() {
            return keywordMatch;
        }
    }
}
