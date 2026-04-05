package com.example.aiframework.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 会话统计 DTO - 手动实现 getter/setter (避免 Lombok 问题)
 */
public class ChatSessionStatsDTO {
    
    private Long totalSessions;
    private Long totalMessages;
    private Long todaySessions;
    private Long todayMessages;
    private Double avgMessagesPerSession;
    private Integer maxMessagesInSession;
    private Long activeSessions;
    private Long userMessages;
    private Long aiMessages;
    
    public ChatSessionStatsDTO() {}
    
    public Long getTotalSessions() { return totalSessions; }
    public void setTotalSessions(Long totalSessions) { this.totalSessions = totalSessions; }
    
    public Long getTotalMessages() { return totalMessages; }
    public void setTotalMessages(Long totalMessages) { this.totalMessages = totalMessages; }
    
    public Long getTodaySessions() { return todaySessions; }
    public void setTodaySessions(Long todaySessions) { this.todaySessions = todaySessions; }
    
    public Long getTodayMessages() { return todayMessages; }
    public void setTodayMessages(Long todayMessages) { this.todayMessages = todayMessages; }
    
    public Double getAvgMessagesPerSession() { return avgMessagesPerSession; }
    public void setAvgMessagesPerSession(Double avgMessagesPerSession) { this.avgMessagesPerSession = avgMessagesPerSession; }
    
    public Integer getMaxMessagesInSession() { return maxMessagesInSession; }
    public void setMaxMessagesInSession(Integer maxMessagesInSession) { this.maxMessagesInSession = maxMessagesInSession; }
    
    public Long getActiveSessions() { return activeSessions; }
    public void setActiveSessions(Long activeSessions) { this.activeSessions = activeSessions; }
    
    public Long getUserMessages() { return userMessages; }
    public void setUserMessages(Long userMessages) { this.userMessages = userMessages; }
    
    public Long getAiMessages() { return aiMessages; }
    public void setAiMessages(Long aiMessages) { this.aiMessages = aiMessages; }
    
    public static Builder builder() { return new Builder(); }
    
    public static class Builder {
        private final ChatSessionStatsDTO dto = new ChatSessionStatsDTO();
        
        public Builder totalSessions(Long totalSessions) { dto.setTotalSessions(totalSessions); return this; }
        public Builder totalMessages(Long totalMessages) { dto.setTotalMessages(totalMessages); return this; }
        public Builder todaySessions(Long todaySessions) { dto.setTodaySessions(todaySessions); return this; }
        public Builder todayMessages(Long todayMessages) { dto.setTodayMessages(todayMessages); return this; }
        public Builder avgMessagesPerSession(Double avgMessagesPerSession) { dto.setAvgMessagesPerSession(avgMessagesPerSession); return this; }
        public Builder maxMessagesInSession(Integer maxMessagesInSession) { dto.setMaxMessagesInSession(maxMessagesInSession); return this; }
        public Builder activeSessions(Long activeSessions) { dto.setActiveSessions(activeSessions); return this; }
        public Builder userMessages(Long userMessages) { dto.setUserMessages(userMessages); return this; }
        public Builder aiMessages(Long aiMessages) { dto.setAiMessages(aiMessages); return this; }
        
        public ChatSessionStatsDTO build() { return dto; }
    }
}
