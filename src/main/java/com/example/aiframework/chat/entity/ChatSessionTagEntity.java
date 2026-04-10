package com.example.aiframework.chat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 会话标签实体
 */
@TableName("chat_session_tags")
public class ChatSessionTagEntity {
    
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    
    private String sessionId;
    
    private String tagName;
    
    private String tagColor;
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
    
    public ChatSessionTagEntity() {}
    
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    public String getTagName() { return tagName; }
    public void setTagName(String tagName) { this.tagName = tagName; }
    
    public String getTagColor() { return tagColor; }
    public void setTagColor(String tagColor) { this.tagColor = tagColor; }
    
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    
    public static Builder builder() { return new Builder(); }
    
    public static class Builder {
        private final ChatSessionTagEntity entity = new ChatSessionTagEntity();
        
        public Builder id(String id) { entity.setId(id); return this; }
        public Builder sessionId(String sessionId) { entity.setSessionId(sessionId); return this; }
        public Builder tagName(String tagName) { entity.setTagName(tagName); return this; }
        public Builder tagColor(String tagColor) { entity.setTagColor(tagColor); return this; }
        public Builder createTime(LocalDateTime createTime) { entity.setCreateTime(createTime); return this; }
        public Builder updateTime(LocalDateTime updateTime) { entity.setUpdateTime(updateTime); return this; }
        
        public ChatSessionTagEntity build() { return entity; }
    }
}
