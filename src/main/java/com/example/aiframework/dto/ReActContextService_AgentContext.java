package com.example.aiframework.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.*;

/**
 * ReAct Agent 上下文
 */
@Setter
@Getter
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

    public void addConversationMessage(ReActContextService_ConversationMessage msg) { this.conversationHistory.add(msg); }

    public void putUserPreference(String key, Object value) { this.userPreferences.put(key, value); }

    public void addKnowledgeBaseId(String id) { this.knowledgeBaseIds.add(id); }

    public void putMetadata(String key, Object value) { this.metadata.put(key, value); }

}
