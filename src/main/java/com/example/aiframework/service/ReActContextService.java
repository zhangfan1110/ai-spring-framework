package com.example.aiframework.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * ReAct Agent 上下文服务
 * 支持历史对话、用户偏好、知识库等上下文信息
 */
@Service
public class ReActContextService {
    
    private static final Logger log = LoggerFactory.getLogger(ReActContextService.class);
    
    @Autowired(required = false)
    private RedisTemplate<String, String> redisTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    // 内存存储（如果没有 Redis）
    private final Map<String, AgentContext> contextStore = new HashMap<>();
    
    private static final String CONTEXT_PREFIX = "agent:context:";
    private static final long TTL_HOURS = 24;
    
    /**
     * Agent 上下文
     */
    public static class AgentContext {
        private String contextId;
        private String sessionId;
        private String userId;
        private List<ConversationMessage> conversationHistory;
        private Map<String, Object> userPreferences;
        private List<String> knowledgeBaseIds;
        private Map<String, Object> metadata;
        private long createdAt;
        private long updatedAt;
        
        public AgentContext() {
            this.conversationHistory = new ArrayList<>();
            this.userPreferences = new HashMap<>();
            this.knowledgeBaseIds = new ArrayList<>();
            this.metadata = new HashMap<>();
            this.createdAt = System.currentTimeMillis();
            this.updatedAt = System.currentTimeMillis();
        }
        
        // Getters and Setters
        public String getContextId() { return contextId; }
        public void setContextId(String contextId) { this.contextId = contextId; }
        
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        
        public List<ConversationMessage> getConversationHistory() { return conversationHistory; }
        public void setConversationHistory(List<ConversationMessage> conversationHistory) { this.conversationHistory = conversationHistory; }
        public void addConversationMessage(ConversationMessage msg) { this.conversationHistory.add(msg); }
        
        public Map<String, Object> getUserPreferences() { return userPreferences; }
        public void setUserPreferences(Map<String, Object> userPreferences) { this.userPreferences = userPreferences; }
        public void putUserPreference(String key, Object value) { this.userPreferences.put(key, value); }
        
        public List<String> getKnowledgeBaseIds() { return knowledgeBaseIds; }
        public void setKnowledgeBaseIds(List<String> knowledgeBaseIds) { this.knowledgeBaseIds = knowledgeBaseIds; }
        public void addKnowledgeBaseId(String id) { this.knowledgeBaseIds.add(id); }
        
        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
        public void putMetadata(String key, Object value) { this.metadata.put(key, value); }
        
        public long getCreatedAt() { return createdAt; }
        public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
        
        public long getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
    }
    
    /**
     * 对话消息
     */
    public static class ConversationMessage {
        private String role; // USER/AI/SYSTEM
        private String content;
        private long timestamp;
        private Map<String, Object> metadata;
        
        public ConversationMessage() {
            this.timestamp = System.currentTimeMillis();
            this.metadata = new HashMap<>();
        }
        
        public ConversationMessage(String role, String content) {
            this();
            this.role = role;
            this.content = content;
        }
        
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
        
        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    }
    
    /**
     * 创建上下文
     */
    public AgentContext createContext(String sessionId, String userId) {
        String contextId = UUID.randomUUID().toString();
        
        AgentContext context = new AgentContext();
        context.setContextId(contextId);
        context.setSessionId(sessionId);
        context.setUserId(userId);
        
        saveContext(context);
        
        log.info("创建 Agent 上下文：{}", contextId);
        return context;
    }
    
    /**
     * 获取上下文
     */
    public AgentContext getContext(String contextId) {
        if (contextId == null) {
            return null;
        }
        
        // 尝试从 Redis 获取
        if (redisTemplate != null) {
            try {
                String key = CONTEXT_PREFIX + contextId;
                String json = redisTemplate.opsForValue().get(key);
                if (json != null) {
                    return objectMapper.readValue(json, AgentContext.class);
                }
            } catch (Exception e) {
                log.warn("从 Redis 获取上下文失败：{}", contextId, e);
            }
        }
        
        // 从内存获取
        return contextStore.get(contextId);
    }
    
    /**
     * 更新上下文
     */
    public void updateContext(AgentContext context) {
        if (context == null) {
            return;
        }
        
        context.setUpdatedAt(System.currentTimeMillis());
        saveContext(context);
    }
    
    /**
     * 添加对话历史
     */
    public void addConversationMessage(String contextId, String role, String content) {
        AgentContext context = getContext(contextId);
        if (context != null) {
            ConversationMessage message = new ConversationMessage(role, content);
            context.addConversationMessage(message);
            
            // 限制历史记录数量（最多 50 条）
            if (context.getConversationHistory().size() > 50) {
                context.getConversationHistory().remove(0);
            }
            
            updateContext(context);
        }
    }
    
    /**
     * 设置用户偏好
     */
    public void setUserPreference(String contextId, String key, Object value) {
        AgentContext context = getContext(contextId);
        if (context != null) {
            context.putUserPreference(key, value);
            updateContext(context);
        }
    }
    
    /**
     * 添加知识库
     */
    public void addKnowledgeBase(String contextId, String knowledgeBaseId) {
        AgentContext context = getContext(contextId);
        if (context != null) {
            context.addKnowledgeBaseId(knowledgeBaseId);
            updateContext(context);
        }
    }
    
    /**
     * 删除上下文
     */
    public void deleteContext(String contextId) {
        if (redisTemplate != null) {
            redisTemplate.delete(CONTEXT_PREFIX + contextId);
        }
        contextStore.remove(contextId);
        log.info("删除上下文：{}", contextId);
    }
    
    /**
     * 保存上下文
     */
    private void saveContext(AgentContext context) {
        try {
            String json = objectMapper.writeValueAsString(context);
            
            if (redisTemplate != null) {
                String key = CONTEXT_PREFIX + context.getContextId();
                redisTemplate.opsForValue().set(key, json, TTL_HOURS, TimeUnit.HOURS);
            } else {
                contextStore.put(context.getContextId(), context);
            }
            
        } catch (Exception e) {
            log.error("保存上下文失败：{}", context.getContextId(), e);
        }
    }
    
    /**
     * 构建带上下文的 Prompt
     */
    public String buildContextualPrompt(String contextId, String task, String role) {
        AgentContext context = getContext(contextId);
        StringBuilder prompt = new StringBuilder();
        
        // 1. 系统提示
        prompt.append("你是一个智能助手。\n\n");
        
        // 2. 用户偏好
        if (context != null && !context.getUserPreferences().isEmpty()) {
            prompt.append("用户偏好:\n");
            for (Map.Entry<String, Object> entry : context.getUserPreferences().entrySet()) {
                prompt.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
            prompt.append("\n");
        }
        
        // 3. 历史对话
        if (context != null && !context.getConversationHistory().isEmpty()) {
            prompt.append("历史对话:\n");
            List<ConversationMessage> history = context.getConversationHistory();
            // 只显示最近 10 条
            int start = Math.max(0, history.size() - 10);
            for (int i = start; i < history.size(); i++) {
                ConversationMessage msg = history.get(i);
                prompt.append(msg.getRole()).append(": ").append(msg.getContent()).append("\n");
            }
            prompt.append("\n");
        }
        
        // 4. 知识库引用
        if (context != null && !context.getKnowledgeBaseIds().isEmpty()) {
            prompt.append("参考知识库: ").append(String.join(", ", context.getKnowledgeBaseIds())).append("\n\n");
        }
        
        // 5. 当前任务
        prompt.append("当前任务：").append(task).append("\n");
        
        // 6. 角色设定
        if (role != null && !role.isEmpty()) {
            prompt.append("你的角色：").append(role).append("\n");
        }
        
        return prompt.toString();
    }
    
    /**
     * 清空对话历史
     */
    public void clearConversationHistory(String contextId) {
        AgentContext context = getContext(contextId);
        if (context != null) {
            context.setConversationHistory(new ArrayList<>());
            updateContext(context);
        }
    }
    
    /**
     * 获取上下文统计信息
     */
    public Map<String, Object> getContextStats(String contextId) {
        AgentContext context = getContext(contextId);
        Map<String, Object> stats = new HashMap<>();
        
        if (context == null) {
            stats.put("exists", false);
            return stats;
        }
        
        stats.put("exists", true);
        stats.put("contextId", context.getContextId());
        stats.put("sessionId", context.getSessionId());
        stats.put("userId", context.getUserId());
        stats.put("conversationHistoryCount", context.getConversationHistory().size());
        stats.put("userPreferencesCount", context.getUserPreferences().size());
        stats.put("knowledgeBaseCount", context.getKnowledgeBaseIds().size());
        stats.put("createdAt", context.getCreatedAt());
        stats.put("updatedAt", context.getUpdatedAt());
        
        return stats;
    }
}
