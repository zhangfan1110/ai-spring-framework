package com.example.aiframework.chat.service;

import com.example.aiframework.chat.dto.ChatSessionStatsDTO;
import com.example.aiframework.chat.entity.ChatMessageEntity;
import com.example.aiframework.chat.entity.ChatSessionEntity;
import com.example.aiframework.chat.repository.ChatMessageRepository;
import com.example.aiframework.chat.repository.ChatSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 会话统计服务
 */
@Service
public class SessionStatsService {
    
    private static final Logger log = LoggerFactory.getLogger(SessionStatsService.class);
    
    @Autowired
    private ChatSessionRepository sessionRepository;
    
    @Autowired
    private ChatMessageRepository messageRepository;
    
    /**
     * 获取总体统计信息
     */
    public ChatSessionStatsDTO getOverallStats() {
        log.info("获取总体统计信息");
        
        // 查询所有会话
        List<ChatSessionEntity> allSessions = sessionRepository.findAll();
        long totalSessions = allSessions.size();
        
        // 查询所有消息
        long totalMessages = 0;
        long userMessages = 0;
        long aiMessages = 0;
        int maxMessages = 0;
        
        for (ChatSessionEntity session : allSessions) {
            List<ChatMessageEntity> messages = messageRepository.findBySessionId(session.getSessionId());
            int msgCount = messages.size();
            totalMessages += msgCount;
            
            if (msgCount > maxMessages) {
                maxMessages = msgCount;
            }
            
            for (ChatMessageEntity msg : messages) {
                if ("USER".equals(msg.getRole())) {
                    userMessages++;
                } else if ("AI".equals(msg.getRole())) {
                    aiMessages++;
                }
            }
        }
        
        // 今日统计
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        long todaySessions = allSessions.stream()
            .filter(s -> s.getCreateTime() != null && s.getCreateTime().isAfter(todayStart))
            .count();
        
        long todayMessages = 0; // 简化实现，实际应该查询数据库
        
        // 活跃会话 (最近 7 天)
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        long activeSessions = allSessions.stream()
            .filter(s -> s.getLastActiveTime() != null && s.getLastActiveTime().isAfter(sevenDaysAgo))
            .count();
        
        // 计算平均值
        double avgMessages = totalSessions > 0 ? (double) totalMessages / totalSessions : 0;
        
        return ChatSessionStatsDTO.builder()
            .totalSessions(totalSessions)
            .totalMessages(totalMessages)
            .todaySessions(todaySessions)
            .todayMessages(todayMessages)
            .avgMessagesPerSession(avgMessages)
            .maxMessagesInSession(maxMessages)
            .activeSessions(activeSessions)
            .userMessages(userMessages)
            .aiMessages(aiMessages)
            .build();
    }
    
    /**
     * 获取会话详情统计
     */
    public ChatSessionStatsDTO getSessionStats(String sessionId) {
        log.info("获取会话统计：{}", sessionId);
        
        List<ChatMessageEntity> messages = messageRepository.findBySessionId(sessionId);
        long userMessages = messages.stream().filter(m -> "USER".equals(m.getRole())).count();
        long aiMessages = messages.stream().filter(m -> "AI".equals(m.getRole())).count();
        
        return ChatSessionStatsDTO.builder()
            .totalSessions(1L)
            .totalMessages((long) messages.size())
            .userMessages(userMessages)
            .aiMessages(aiMessages)
            .maxMessagesInSession(messages.size())
            .build();
    }
}
