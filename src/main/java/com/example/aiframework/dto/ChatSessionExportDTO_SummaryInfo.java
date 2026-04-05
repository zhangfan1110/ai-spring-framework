package com.example.aiframework.dto;

/**
 * 摘要信息
 */
public class ChatSessionExportDTO_SummaryInfo {
    private String summary;
    private String keyPoints;
    private java.time.LocalDateTime createTime;
    
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getKeyPoints() { return keyPoints; }
    public void setKeyPoints(String keyPoints) { this.keyPoints = keyPoints; }
    public java.time.LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(java.time.LocalDateTime createTime) { this.createTime = createTime; }
}
