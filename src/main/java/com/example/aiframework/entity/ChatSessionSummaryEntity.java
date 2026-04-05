package com.example.aiframework.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 会话摘要实体 - 用于存储长对话的摘要
 */
@TableName("chat_session_summaries")
public class ChatSessionSummaryEntity {
    
    /**
     * 摘要 ID
     */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    
    /**
     * 会话 ID
     */
    private String sessionId;
    
    /**
     * 摘要内容
     */
    private String summary;
    
    /**
     * 关键信息 (JSON 格式)
     */
    private String keyPoints;
    
    /**
     * 摘要的消息范围 (起始消息 ID)
     */
    private String startMessageId;
    
    /**
     * 摘要的消息范围 (结束消息 ID)
     */
    private String endMessageId;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
    
    public ChatSessionSummaryEntity() {}
    
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    
    public String getKeyPoints() { return keyPoints; }
    public void setKeyPoints(String keyPoints) { this.keyPoints = keyPoints; }
    
    public String getStartMessageId() { return startMessageId; }
    public void setStartMessageId(String startMessageId) { this.startMessageId = startMessageId; }
    
    public String getEndMessageId() { return endMessageId; }
    public void setEndMessageId(String endMessageId) { this.endMessageId = endMessageId; }
    
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    
    public static Builder builder() { return new Builder(); }
    
    public static class Builder {
        private final ChatSessionSummaryEntity entity = new ChatSessionSummaryEntity();
        
        public Builder id(String id) { entity.setId(id); return this; }
        public Builder sessionId(String sessionId) { entity.setSessionId(sessionId); return this; }
        public Builder summary(String summary) { entity.setSummary(summary); return this; }
        public Builder keyPoints(String keyPoints) { entity.setKeyPoints(keyPoints); return this; }
        public Builder startMessageId(String startMessageId) { entity.setStartMessageId(startMessageId); return this; }
        public Builder endMessageId(String endMessageId) { entity.setEndMessageId(endMessageId); return this; }
        public Builder createTime(LocalDateTime createTime) { entity.setCreateTime(createTime); return this; }
        public Builder updateTime(LocalDateTime updateTime) { entity.setUpdateTime(updateTime); return this; }
        
        public ChatSessionSummaryEntity build() { return entity; }
    }
}
