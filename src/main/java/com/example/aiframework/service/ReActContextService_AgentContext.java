package com.example.aiframework.service;

import java.util.*;

/**
 * ReAct Agent 上下文
 */
public class ReActContextService_AgentContext {
    private String contextId;
    private String sessionId;
    private String userId;
    private List<ReActContextService_ConversationMessage> conversationHistory;
    private Map<String, Object> userPreferences;
    private List<String> knowledgeBaseIds;
    private Map<String, Object> metadata;
    private long createdAt;
    private long updatedAt;
    
    public ReActContextService_AgentContext() {
        this.conversationHistory = new ArrayList<>();
        this.userPreferences = new HashMap<>();
        this.knowledgeBaseIds = new ArrayList<>();
        this.metadata = new HashMap<>();
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }
    
    public String getContextId() { return contextId; }
    public void setContextId(String contextId) { this.contextId = contextId; }
    
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public List<ReActContextService_ConversationMessage> getConversationHistory() { return conversationHistory; }
    public void setConversationHistory(List<ReActContextService_ConversationMessage> conversationHistory) { this.conversationHistory = conversationHistory; }
    public void addConversationMessage(ReActContextService_ConversationMessage msg) { this.conversationHistory.add(msg); }
    
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
