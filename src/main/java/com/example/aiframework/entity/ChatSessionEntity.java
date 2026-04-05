package com.example.aiframework.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 会话实体 - 用于管理会话元数据
 */
@TableName("chat_sessions")
public class ChatSessionEntity {
    
    /**
     * 会话 ID
     */
    @TableId(type = IdType.ASSIGN_UUID)
    private String sessionId;
    
    /**
     * 会话标题 (可由 AI 自动生成)
     */
    private String title;
    
    /**
     * 消息数量
     */
    private Integer messageCount;
    
    /**
     * 最后活跃时间
     */
    private LocalDateTime lastActiveTime;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
    
    public ChatSessionEntity() {}
    
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public Integer getMessageCount() { return messageCount; }
    public void setMessageCount(Integer messageCount) { this.messageCount = messageCount; }
    
    public LocalDateTime getLastActiveTime() { return lastActiveTime; }
    public void setLastActiveTime(LocalDateTime lastActiveTime) { this.lastActiveTime = lastActiveTime; }
    
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    
    public static Builder builder() { return new Builder(); }
    
    public static class Builder {
        private final ChatSessionEntity entity = new ChatSessionEntity();
        
        public Builder sessionId(String sessionId) { entity.setSessionId(sessionId); return this; }
        public Builder title(String title) { entity.setTitle(title); return this; }
        public Builder messageCount(Integer messageCount) { entity.setMessageCount(messageCount); return this; }
        public Builder lastActiveTime(LocalDateTime lastActiveTime) { entity.setLastActiveTime(lastActiveTime); return this; }
        public Builder createTime(LocalDateTime createTime) { entity.setCreateTime(createTime); return this; }
        public Builder updateTime(LocalDateTime updateTime) { entity.setUpdateTime(updateTime); return this; }
        
        public ChatSessionEntity build() { return entity; }
    }
}
