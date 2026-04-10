package com.example.aiframework.chat.dto;

import java.util.List;

/**
 * 会话合并请求 DTO
 */
public class ChatSessionMergeRequestDTO {
    
    private String targetSessionId;
    private List<String> sourceSessionIds;
    private boolean keepSourceSessions;
    private String newTitle;
    
    public ChatSessionMergeRequestDTO() {}

    public String getTargetSessionId() { return targetSessionId; }
    public void setTargetSessionId(String targetSessionId) { this.targetSessionId = targetSessionId; }
    
    public List<String> getSourceSessionIds() { return sourceSessionIds; }
    public void setSourceSessionIds(List<String> sourceSessionIds) { this.sourceSessionIds = sourceSessionIds; }
    
    public boolean isKeepSourceSessions() { return keepSourceSessions; }
    public void setKeepSourceSessions(boolean keepSourceSessions) { this.keepSourceSessions = keepSourceSessions; }
    
    public String getNewTitle() { return newTitle; }
    public void setNewTitle(String newTitle) { this.newTitle = newTitle; }

    public static Builder builder() { return new Builder(); }
    
    public static class Builder {
        private final ChatSessionMergeRequestDTO dto = new ChatSessionMergeRequestDTO();
        
        public Builder targetSessionId(String targetSessionId) { dto.setTargetSessionId(targetSessionId); return this; }
        public Builder sourceSessionIds(List<String> sourceSessionIds) { dto.setSourceSessionIds(sourceSessionIds); return this; }
        public Builder keepSourceSessions(boolean keepSourceSessions) { dto.setKeepSourceSessions(keepSourceSessions); return this; }
        public Builder newTitle(String newTitle) { dto.setNewTitle(newTitle); return this; }
        
        public ChatSessionMergeRequestDTO build() { return dto; }
    }
}
