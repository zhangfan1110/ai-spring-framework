package com.example.aiframework.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 会话合并请求 DTO - 手动实现 getter/setter
 */
@Setter
@Getter
public class ChatSessionMergeRequestDTO {
    
    private String targetSessionId;
    private List<String> sourceSessionIds;
    private boolean keepSourceSessions;
    private String newTitle;
    
    public ChatSessionMergeRequestDTO() {}

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
