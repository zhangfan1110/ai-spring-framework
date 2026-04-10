package com.example.aiframework.chat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 会话分享实体 - 公开分享链接
 */
@TableName("chat_session_shares")
public class ChatSessionShareEntity {
    
    /**
     * 分享 ID
     */
    @TableId(type = IdType.ASSIGN_UUID)
    private String shareId;
    
    /**
     * 会话 ID
     */
    private String sessionId;
    
    /**
     * 分享令牌 (公开访问 token)
     */
    private String shareToken;
    
    /**
     * 分享标题 (可自定义)
     */
    private String shareTitle;
    
    /**
     * 分享描述
     */
    private String shareDescription;
    
    /**
     * 是否公开 (false=仅链接可访问，true=公开广场)
     */
    private Boolean isPublic;
    
    /**
     * 访问密码 (可选)
     */
    private String accessPassword;
    
    /**
     * 过期时间 (null=永久)
     */
    private LocalDateTime expireTime;
    
    /**
     * 访问次数
     */
    private Integer viewCount;
    
    /**
     * 最大访问次数 (null=无限制)
     */
    private Integer maxViews;
    
    /**
     * 创建者 ID
     */
    private String creatorId;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 是否已禁用
     */
    private Boolean disabled;
    
    public ChatSessionShareEntity() {
        this.isPublic = false;
        this.viewCount = 0;
        this.disabled = false;
    }
    
    // Getters and Setters
    public String getShareId() { return shareId; }
    public void setShareId(String shareId) { this.shareId = shareId; }
    
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    public String getShareToken() { return shareToken; }
    public void setShareToken(String shareToken) { this.shareToken = shareToken; }
    
    public String getShareTitle() { return shareTitle; }
    public void setShareTitle(String shareTitle) { this.shareTitle = shareTitle; }
    
    public String getShareDescription() { return shareDescription; }
    public void setShareDescription(String shareDescription) { this.shareDescription = shareDescription; }
    
    public Boolean getIsPublic() { return isPublic; }
    public void setIsPublic(Boolean isPublic) { this.isPublic = isPublic; }
    
    public String getAccessPassword() { return accessPassword; }
    public void setAccessPassword(String accessPassword) { this.accessPassword = accessPassword; }
    
    public LocalDateTime getExpireTime() { return expireTime; }
    public void setExpireTime(LocalDateTime expireTime) { this.expireTime = expireTime; }
    
    public Integer getViewCount() { return viewCount; }
    public void setViewCount(Integer viewCount) { this.viewCount = viewCount; }
    
    public Integer getMaxViews() { return maxViews; }
    public void setMaxViews(Integer maxViews) { this.maxViews = maxViews; }
    
    public String getCreatorId() { return creatorId; }
    public void setCreatorId(String creatorId) { this.creatorId = creatorId; }
    
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    
    public Boolean getDisabled() { return disabled; }
    public void setDisabled(Boolean disabled) { this.disabled = disabled; }
    
    /**
     * 检查是否已过期
     */
    public boolean isExpired() {
        if (expireTime == null) return false;
        return LocalDateTime.now().isAfter(expireTime);
    }
    
    /**
     * 检查是否达到最大访问次数
     */
    public boolean isMaxViewsReached() {
        if (maxViews == null) return false;
        return viewCount >= maxViews;
    }
    
    /**
     * 检查分享是否可用
     */
    public boolean isAvailable() {
        return !disabled && !isExpired() && !isMaxViewsReached();
    }
    
    /**
     * 增加访问次数
     */
    public void incrementViewCount() {
        this.viewCount = (this.viewCount == null ? 0 : this.viewCount) + 1;
    }
    
    public static Builder builder() { return new Builder(); }
    
    public static class Builder {
        private final ChatSessionShareEntity entity = new ChatSessionShareEntity();
        
        public Builder shareId(String shareId) { entity.setShareId(shareId); return this; }
        public Builder sessionId(String sessionId) { entity.setSessionId(sessionId); return this; }
        public Builder shareToken(String shareToken) { entity.setShareToken(shareToken); return this; }
        public Builder shareTitle(String shareTitle) { entity.setShareTitle(shareTitle); return this; }
        public Builder shareDescription(String shareDescription) { entity.setShareDescription(shareDescription); return this; }
        public Builder isPublic(Boolean isPublic) { entity.setIsPublic(isPublic); return this; }
        public Builder accessPassword(String accessPassword) { entity.setAccessPassword(accessPassword); return this; }
        public Builder expireTime(LocalDateTime expireTime) { entity.setExpireTime(expireTime); return this; }
        public Builder viewCount(Integer viewCount) { entity.setViewCount(viewCount); return this; }
        public Builder maxViews(Integer maxViews) { entity.setMaxViews(maxViews); return this; }
        public Builder creatorId(String creatorId) { entity.setCreatorId(creatorId); return this; }
        public Builder createTime(LocalDateTime createTime) { entity.setCreateTime(createTime); return this; }
        public Builder disabled(Boolean disabled) { entity.setDisabled(disabled); return this; }
        
        public ChatSessionShareEntity build() { return entity; }
    }
}
