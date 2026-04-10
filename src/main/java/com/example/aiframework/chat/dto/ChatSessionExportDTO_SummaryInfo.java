package com.example.aiframework.chat.dto;

import java.time.LocalDateTime;

/**
 * 摘要信息
 */
public class ChatSessionExportDTO_SummaryInfo {
    private String summary;
    private String keyPoints;
    private LocalDateTime createTime;
    
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getKeyPoints() { return keyPoints; }
    public void setKeyPoints(String keyPoints) { this.keyPoints = keyPoints; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
