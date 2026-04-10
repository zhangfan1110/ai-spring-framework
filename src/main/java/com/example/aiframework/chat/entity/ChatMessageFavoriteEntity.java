package com.example.aiframework.chat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 收藏消息实体
 */
@TableName("chat_message_favorites")
public class ChatMessageFavoriteEntity {
    
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    
    private String messageId;
    
    private String sessionId;
    
    private String content;
    
    private String note;
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
    
    public ChatMessageFavoriteEntity() {}
    
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }
    
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    
    public static Builder builder() { return new Builder(); }
    
    public static class Builder {
        private final ChatMessageFavoriteEntity entity = new ChatMessageFavoriteEntity();
        
        public Builder id(String id) { entity.setId(id); return this; }
        public Builder messageId(String messageId) { entity.setMessageId(messageId); return this; }
        public Builder sessionId(String sessionId) { entity.setSessionId(sessionId); return this; }
        public Builder content(String content) { entity.setContent(content); return this; }
        public Builder note(String note) { entity.setNote(note); return this; }
        public Builder createTime(LocalDateTime createTime) { entity.setCreateTime(createTime); return this; }
        public Builder updateTime(LocalDateTime updateTime) { entity.setUpdateTime(updateTime); return this; }
        
        public ChatMessageFavoriteEntity build() { return entity; }
    }
}
