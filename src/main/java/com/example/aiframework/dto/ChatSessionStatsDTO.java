package com.example.aiframework.dto;

import lombok.*;

/**
 * 会话统计 DTO - 手动实现 getter/setter (避免 Lombok 问题)
 */
@Setter
@Getter
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
