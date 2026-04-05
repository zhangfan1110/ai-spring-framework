package com.example.aiframework.dto;

/**
 * 消息信息
 */
public class ChatSessionExportDTO_MessageInfo {
    private String id;
    private String role;
    private String content;
    private String model;
    private Integer tokens;
    private java.time.LocalDateTime createTime;
    
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public Integer getTokens() { return tokens; }
    public void setTokens(Integer tokens) { this.tokens = tokens; }
    public java.time.LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(java.time.LocalDateTime createTime) { this.createTime = createTime; }
}
