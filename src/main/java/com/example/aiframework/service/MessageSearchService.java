package com.example.aiframework.service;

import com.example.aiframework.entity.ChatMessageEntity;
import com.example.aiframework.repository.ChatMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 消息搜索服务
 * 支持多种搜索模式：关键词、全文、时间范围、角色过滤等
 */
@Service
public class MessageSearchService {
    
    private static final Logger log = LoggerFactory.getLogger(MessageSearchService.class);
    
    @Autowired
    private ChatMessageRepository messageRepository;
    
    /**
     * 搜索结果类
     */
    public static class SearchResult {
        private ChatMessageEntity message;
        private String highlightedContent;
        private int matchCount;
        private double relevanceScore;
        
        public ChatMessageEntity getMessage() { return message; }
        public void setMessage(ChatMessageEntity message) { this.message = message; }
        
        public String getHighlightedContent() { return highlightedContent; }
        public void setHighlightedContent(String highlightedContent) { this.highlightedContent = highlightedContent; }
        
        public int getMatchCount() { return matchCount; }
        public void setMatchCount(int matchCount) { this.matchCount = matchCount; }
        
        public double getRelevanceScore() { return relevanceScore; }
        public void setRelevanceScore(double relevanceScore) { this.relevanceScore = relevanceScore; }
    }
    
    /**
     * 搜索参数
     */
    public static class SearchParams {
        private String sessionId;
        private String keyword;
        private String role; // USER/AI/SYSTEM
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private int limit = 20;
        private boolean highlight = true;
        private boolean globalSearch = false;
        
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        
        public String getKeyword() { return keyword; }
        public void setKeyword(String keyword) { this.keyword = keyword; }
        
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        
        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
        
        public LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
        
        public int getLimit() { return limit; }
        public void setLimit(int limit) { this.limit = limit; }
        
        public boolean isHighlight() { return highlight; }
        public void setHighlight(boolean highlight) { this.highlight = highlight; }
        
        public boolean isGlobalSearch() { return globalSearch; }
        public void setGlobalSearch(boolean globalSearch) { this.globalSearch = globalSearch; }
    }
    
    /**
     * 搜索消息（基础搜索）
     */
    public List<ChatMessageEntity> searchMessages(String sessionId, String keyword, int limit) {
        log.info("搜索消息：sessionId={}, keyword={}, limit={}", sessionId, keyword, limit);
        
        if (keyword == null || keyword.trim().isEmpty()) {
            log.warn("搜索关键词为空");
            return Collections.emptyList();
        }
        
        return messageRepository.searchByKeyword(sessionId, keyword.trim(), limit);
    }
    
    /**
     * 全局搜索消息（跨会话）
     */
    public List<ChatMessageEntity> searchMessagesGlobal(String keyword, int limit) {
        log.info("全局搜索消息：keyword={}, limit={}", keyword, limit);
        
        if (keyword == null || keyword.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        return messageRepository.searchByKeywordGlobal(keyword.trim(), limit);
    }
    
    /**
     * 高级搜索（支持多种过滤条件）
     */
    public List<SearchResult> searchAdvanced(SearchParams params) {
        log.info("高级搜索：{}", params);
        
        if (params.getKeyword() == null || params.getKeyword().trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        List<ChatMessageEntity> messages;
        
        // 根据参数选择不同的搜索方式
        if (params.isGlobalSearch()) {
            // 全局搜索
            messages = messageRepository.searchByKeywordGlobal(
                params.getKeyword().trim(), 
                params.getLimit()
            );
        } else if (params.getRole() != null && !params.getRole().isEmpty()) {
            // 按角色过滤
            messages = messageRepository.searchByRoleAndKeyword(
                params.getSessionId(),
                params.getRole(),
                params.getKeyword().trim(),
                params.getLimit()
            );
        } else if (params.getStartTime() != null && params.getEndTime() != null) {
            // 按时间范围搜索
            messages = messageRepository.searchByTimeRange(
                params.getSessionId(),
                params.getStartTime(),
                params.getEndTime(),
                params.getLimit()
            );
        } else {
            // 基础搜索
            messages = messageRepository.searchByKeyword(
                params.getSessionId(),
                params.getKeyword().trim(),
                params.getLimit()
            );
        }
        
        // 处理搜索结果（高亮、评分）
        List<SearchResult> results = new ArrayList<>();
        for (ChatMessageEntity message : messages) {
            SearchResult result = new SearchResult();
            result.setMessage(message);
            
            if (params.isHighlight()) {
                result.setHighlightedContent(highlightKeyword(message.getContent(), params.getKeyword()));
            } else {
                result.setHighlightedContent(message.getContent());
            }
            
            result.setMatchCount(countMatches(message.getContent(), params.getKeyword()));
            result.setRelevanceScore(calculateRelevance(message, params.getKeyword()));
            
            results.add(result);
        }
        
        // 按相关度排序
        results.sort((a, b) -> Double.compare(b.getRelevanceScore(), a.getRelevanceScore()));
        
        log.info("搜索完成，找到 {} 条结果", results.size());
        return results;
    }
    
    /**
     * 高亮关键词
     */
    public String highlightKeyword(String content, String keyword) {
        if (content == null || keyword == null) {
            return content;
        }
        
        // 不区分大小写的高亮
        Pattern pattern = Pattern.compile(Pattern.quote(keyword), Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(content);
        
        return matcher.replaceAll("<mark>$0</mark>");
    }
    
    /**
     * 统计关键词匹配次数
     */
    private int countMatches(String content, String keyword) {
        if (content == null || keyword == null) {
            return 0;
        }
        
        Pattern pattern = Pattern.compile(Pattern.quote(keyword), Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(content);
        
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        
        return count;
    }
    
    /**
     * 计算相关度评分
     * 基于：匹配次数、内容长度、消息角色、时间新鲜度
     */
    private double calculateRelevance(ChatMessageEntity message, String keyword) {
        double score = 0.0;
        
        String content = message.getContent();
        if (content == null) {
            return 0.0;
        }
        
        // 1. 匹配次数（权重 40%）
        int matchCount = countMatches(content, keyword);
        score += matchCount * 10.0;
        
        // 2. 匹配密度（权重 30%）
        double density = (double) matchCount / content.length() * 100;
        score += Math.min(density * 5, 30.0);
        
        // 3. AI 回复权重更高（权重 20%）
        if ("AI".equals(message.getRole())) {
            score += 20.0;
        }
        
        // 4. 时间新鲜度（权重 10%）
        if (message.getCreateTime() != null) {
            long daysDiff = java.time.Duration.between(
                message.getCreateTime(), 
                LocalDateTime.now()
            ).toDays();
            score += Math.max(0, 10.0 - daysDiff);
        }
        
        return score;
    }
    
    /**
     * 获取搜索建议（基于历史消息）
     */
    public List<String> getSearchSuggestions(String sessionId, String prefix, int limit) {
        log.info("获取搜索建议：sessionId={}, prefix={}", sessionId, prefix);
        
        if (prefix == null || prefix.length() < 2) {
            return Collections.emptyList();
        }
        
        // 简单实现：从最近消息中提取包含前缀的关键词
        List<ChatMessageEntity> recentMessages = messageRepository.findBySessionIdLimit(sessionId, 100);
        
        Set<String> suggestions = new LinkedHashSet<>();
        for (ChatMessageEntity message : recentMessages) {
            String content = message.getContent();
            if (content != null && content.toLowerCase().contains(prefix.toLowerCase())) {
                // 提取包含前缀的词语
                Pattern pattern = Pattern.compile("\\b" + Pattern.quote(prefix) + "\\w*", Pattern.CASE_INSENSITIVE);
                Matcher matcher = pattern.matcher(content);
                while (matcher.find() && suggestions.size() < limit) {
                    suggestions.add(matcher.group());
                }
            }
        }
        
        return new ArrayList<>(suggestions).subList(0, Math.min(limit, suggestions.size()));
    }
    
    /**
     * 统计搜索结果数量
     */
    public int countSearchResults(String sessionId, String keyword) {
        List<ChatMessageEntity> results = messageRepository.searchByKeyword(sessionId, keyword, 1000);
        return results.size();
    }
}
