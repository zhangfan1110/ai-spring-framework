package com.example.aiframework.service;

import java.time.LocalDateTime;

/**
 * 消息搜索参数
 */
public class MessageSearchService_SearchParams {
    private String sessionId;
    private String keyword;
    private String role; // USER/AI/SYSTEM
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private int limit = 20;
    private boolean highlight = true;
    private boolean globalSearch = false;
    
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    
    public int getLimit() { return limit; }
    public void setLimit(int limit) { this.limit = limit; }
    
    public boolean isHighlight() { return highlight; }
    public void setHighlight(boolean highlight) { this.highlight = highlight; }
    
    public boolean isGlobalSearch() { return globalSearch; }
    public void setGlobalSearch(boolean globalSearch) { this.globalSearch = globalSearch; }
}
