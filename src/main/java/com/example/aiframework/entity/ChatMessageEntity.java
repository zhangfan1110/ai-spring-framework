package com.example.aiframework.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 聊天消息实体 - 用于持久化存储
 */
@TableName("chat_messages")
public class ChatMessageEntity {
    
    /**
     * 消息 ID
     */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    
    /**
     * 会话 ID
     */
    private String sessionId;
    
    /**
     * 消息角色 (USER/AI/SYSTEM)
     */
    private String role;
    
    /**
     * 消息内容
     */
    private String content;
    
    /**
     * 模型名称
     */
    private String model;
    
    /**
     * 令牌数 (可选)
     */
    private Integer tokens;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
    
    /**
     * 是否被编辑过
     */
    private Boolean edited = false;
    
    /**
     * 编辑时间
     */
    private LocalDateTime editedAt;
    
    /**
     * 是否已删除（软删除）
     */
    private Boolean deleted = false;
    
    public ChatMessageEntity() {}
    
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    
    public Integer getTokens() { return tokens; }
    public void setTokens(Integer tokens) { this.tokens = tokens; }
    
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    
    public Boolean getEdited() { return edited; }
    public void setEdited(Boolean edited) { this.edited = edited; }
    
    public LocalDateTime getEditedAt() { return editedAt; }
    public void setEditedAt(LocalDateTime editedAt) { this.editedAt = editedAt; }
    
    public Boolean getDeleted() { return deleted; }
    public void setDeleted(Boolean deleted) { this.deleted = deleted; }
    
    public static Builder builder() { return new Builder(); }
    
    public static class Builder {
        private final ChatMessageEntity entity = new ChatMessageEntity();
        
        public Builder id(String id) { entity.setId(id); return this; }
        public Builder sessionId(String sessionId) { entity.setSessionId(sessionId); return this; }
        public Builder role(String role) { entity.setRole(role); return this; }
        public Builder content(String content) { entity.setContent(content); return this; }
        public Builder model(String model) { entity.setModel(model); return this; }
        public Builder tokens(Integer tokens) { entity.setTokens(tokens); return this; }
        public Builder createTime(LocalDateTime createTime) { entity.setCreateTime(createTime); return this; }
        public Builder updateTime(LocalDateTime updateTime) { entity.setUpdateTime(updateTime); return this; }
        public Builder edited(Boolean edited) { entity.setEdited(edited); return this; }
        public Builder editedAt(LocalDateTime editedAt) { entity.setEditedAt(editedAt); return this; }
        public Builder deleted(Boolean deleted) { entity.setDeleted(deleted); return this; }
        
        public ChatMessageEntity build() { return entity; }
    }
}
