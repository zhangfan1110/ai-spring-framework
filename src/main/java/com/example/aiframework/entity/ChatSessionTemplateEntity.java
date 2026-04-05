package com.example.aiframework.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 会话模板实体 - 预设场景模板
 */
@TableName("chat_session_templates")
public class ChatSessionTemplateEntity {
    
    /**
     * 模板 ID
     */
    @TableId(type = IdType.ASSIGN_UUID)
    private String templateId;
    
    /**
     * 模板名称
     */
    private String templateName;
    
    /**
     * 模板描述
     */
    private String description;
    
    /**
     * 模板分类 (learning/coding/writing/analysis/roleplay/other)
     */
    private String category;
    
    /**
     * 图标/Emoji
     */
    private String icon;
    
    /**
     * 预设提示词 (System Prompt)
     */
    private String systemPrompt;
    
    /**
     * 开场白 (可选)
     */
    private String openingMessage;
    
    /**
     * 建议问题列表 (JSON 数组)
     */
    private String suggestedQuestions;
    
    /**
     * 默认模型配置
     */
    private String defaultModel;
    
    /**
     * 温度参数
     */
    private Double temperature;
    
    /**
     * 最大上下文长度
     */
    private Integer maxContextLength;
    
    /**
     * 是否内置模板
     */
    private Boolean isBuiltIn;
    
    /**
     * 使用次数
     */
    private Integer usageCount;
    
    /**
     * 创建者 ID
     */
    private String creatorId;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
    
    /**
     * 是否已禁用
     */
    private Boolean disabled;
    
    public ChatSessionTemplateEntity() {
        this.isBuiltIn = false;
        this.usageCount = 0;
        this.disabled = false;
        this.temperature = 0.7;
    }
    
    // Getters and Setters
    public String getTemplateId() { return templateId; }
    public void setTemplateId(String templateId) { this.templateId = templateId; }
    
    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    
    public String getSystemPrompt() { return systemPrompt; }
    public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }
    
    public String getOpeningMessage() { return openingMessage; }
    public void setOpeningMessage(String openingMessage) { this.openingMessage = openingMessage; }
    
    public String getSuggestedQuestions() { return suggestedQuestions; }
    public void setSuggestedQuestions(String suggestedQuestions) { this.suggestedQuestions = suggestedQuestions; }
    
    public String getDefaultModel() { return defaultModel; }
    public void setDefaultModel(String defaultModel) { this.defaultModel = defaultModel; }
    
    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }
    
    public Integer getMaxContextLength() { return maxContextLength; }
    public void setMaxContextLength(Integer maxContextLength) { this.maxContextLength = maxContextLength; }
    
    public Boolean getIsBuiltIn() { return isBuiltIn; }
    public void setIsBuiltIn(Boolean builtIn) { isBuiltIn = builtIn; }
    
    public Integer getUsageCount() { return usageCount; }
    public void setUsageCount(Integer usageCount) { this.usageCount = usageCount; }
    
    public String getCreatorId() { return creatorId; }
    public void setCreatorId(String creatorId) { this.creatorId = creatorId; }
    
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    
    public Boolean getDisabled() { return disabled; }
    public void setDisabled(Boolean disabled) { this.disabled = disabled; }
    
    /**
     * 增加使用次数
     */
    public void incrementUsageCount() {
        this.usageCount = (this.usageCount == null ? 0 : this.usageCount) + 1;
    }
    
    public static Builder builder() { return new Builder(); }
    
    public static class Builder {
        private final ChatSessionTemplateEntity entity = new ChatSessionTemplateEntity();
        
        public Builder templateId(String templateId) { entity.setTemplateId(templateId); return this; }
        public Builder templateName(String templateName) { entity.setTemplateName(templateName); return this; }
        public Builder description(String description) { entity.setDescription(description); return this; }
        public Builder category(String category) { entity.setCategory(category); return this; }
        public Builder icon(String icon) { entity.setIcon(icon); return this; }
        public Builder systemPrompt(String systemPrompt) { entity.setSystemPrompt(systemPrompt); return this; }
        public Builder openingMessage(String openingMessage) { entity.setOpeningMessage(openingMessage); return this; }
        public Builder suggestedQuestions(String suggestedQuestions) { entity.setSuggestedQuestions(suggestedQuestions); return this; }
        public Builder defaultModel(String defaultModel) { entity.setDefaultModel(defaultModel); return this; }
        public Builder temperature(Double temperature) { entity.setTemperature(temperature); return this; }
        public Builder maxContextLength(Integer maxContextLength) { entity.setMaxContextLength(maxContextLength); return this; }
        public Builder isBuiltIn(Boolean builtIn) { entity.setIsBuiltIn(builtIn); return this; }
        public Builder usageCount(Integer usageCount) { entity.setUsageCount(usageCount); return this; }
        public Builder creatorId(String creatorId) { entity.setCreatorId(creatorId); return this; }
        public Builder createTime(LocalDateTime createTime) { entity.setCreateTime(createTime); return this; }
        public Builder updateTime(LocalDateTime updateTime) { entity.setUpdateTime(updateTime); return this; }
        public Builder disabled(Boolean disabled) { entity.setDisabled(disabled); return this; }
        
        public ChatSessionTemplateEntity build() { return entity; }
    }
}
