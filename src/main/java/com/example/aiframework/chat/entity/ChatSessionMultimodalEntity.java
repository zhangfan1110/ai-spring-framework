package com.example.aiframework.chat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 多模态会话消息实体 - 支持图片/语音/文件
 */
@TableName("chat_messages_multimodal")
public class ChatSessionMultimodalEntity {
    
    /**
     * 附件 ID
     */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    
    /**
     * 消息 ID
     */
    private String messageId;
    
    /**
     * 会话 ID
     */
    private String sessionId;
    
    /**
     * 附件类型 (image/audio/video/file)
     */
    private String attachmentType;
    
    /**
     * 文件 URL/路径
     */
    private String fileUrl;
    
    /**
     * 文件原始名称
     */
    private String fileName;
    
    /**
     * 文件大小 (字节)
     */
    private Long fileSize;
    
    /**
     * MIME 类型
     */
    private String mimeType;
    
    /**
     * 文件描述/说明
     */
    private String description;
    
    /**
     * 转写文本 (语音/图片 OCR)
     */
    private String transcribedText;
    
    /**
     * 处理状态 (pending/processing/completed/failed)
     */
    private String processStatus;
    
    /**
     * 处理错误信息
     */
    private String processError;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    public ChatSessionMultimodalEntity() {
        this.processStatus = "pending";
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }
    
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    public String getAttachmentType() { return attachmentType; }
    public void setAttachmentType(String attachmentType) { this.attachmentType = attachmentType; }
    
    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
    
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    
    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getTranscribedText() { return transcribedText; }
    public void setTranscribedText(String transcribedText) { this.transcribedText = transcribedText; }
    
    public String getProcessStatus() { return processStatus; }
    public void setProcessStatus(String processStatus) { this.processStatus = processStatus; }
    
    public String getProcessError() { return processError; }
    public void setProcessError(String processError) { this.processError = processError; }
    
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    
    public static Builder builder() { return new Builder(); }
    
    public static class Builder {
        private final ChatSessionMultimodalEntity entity = new ChatSessionMultimodalEntity();
        
        public Builder id(String id) { entity.setId(id); return this; }
        public Builder messageId(String messageId) { entity.setMessageId(messageId); return this; }
        public Builder sessionId(String sessionId) { entity.setSessionId(sessionId); return this; }
        public Builder attachmentType(String attachmentType) { entity.setAttachmentType(attachmentType); return this; }
        public Builder fileUrl(String fileUrl) { entity.setFileUrl(fileUrl); return this; }
        public Builder fileName(String fileName) { entity.setFileName(fileName); return this; }
        public Builder fileSize(Long fileSize) { entity.setFileSize(fileSize); return this; }
        public Builder mimeType(String mimeType) { entity.setMimeType(mimeType); return this; }
        public Builder description(String description) { entity.setDescription(description); return this; }
        public Builder transcribedText(String transcribedText) { entity.setTranscribedText(transcribedText); return this; }
        public Builder processStatus(String processStatus) { entity.setProcessStatus(processStatus); return this; }
        public Builder processError(String processError) { entity.setProcessError(processError); return this; }
        public Builder createTime(LocalDateTime createTime) { entity.setCreateTime(createTime); return this; }
        
        public ChatSessionMultimodalEntity build() { return entity; }
    }
}
