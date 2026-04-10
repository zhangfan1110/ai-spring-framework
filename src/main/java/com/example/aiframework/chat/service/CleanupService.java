package com.example.aiframework.chat.service;

import com.example.aiframework.chat.repository.ChatMessageRepository;
import com.example.aiframework.chat.repository.ChatSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 定时清理服务 - 自动清理过期会话
 */
@Service
public class CleanupService {
    
    private static final Logger log = LoggerFactory.getLogger(CleanupService.class);
    
    @Autowired
    private ChatSessionRepository sessionRepository;
    
    @Autowired
    private ChatMessageRepository messageRepository;
    
    @Value("${chat.cleanup.enabled:false}")
    private boolean cleanupEnabled;
    
    @Value("${chat.cleanup.retention-days:90}")
    private int retentionDays;
    
    /**
     * 每天凌晨 2 点执行清理任务
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void scheduledCleanup() {
        if (!cleanupEnabled) {
            log.debug("清理任务未启用，跳过");
            return;
        }
        
        log.info("开始执行定时清理任务");
        
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(retentionDays);
            
            // 删除过期会话的消息
            int deletedMessages = messageRepository.deleteBeforeTime(cutoffTime);
            log.info("删除过期消息：{} 条", deletedMessages);
            
            // 删除过期会话
            int deletedSessions = sessionRepository.deleteBeforeTime(cutoffTime);
            log.info("删除过期会话：{} 个", deletedSessions);
            
            log.info("定时清理任务完成 - 删除 {} 个会话，{} 条消息", 
                deletedSessions, deletedMessages);
            
        } catch (Exception e) {
            log.error("定时清理任务失败：{}", e.getMessage(), e);
        }
    }
    
    /**
     * 手动触发清理
     */
    public void manualCleanup() {
        log.info("手动触发清理任务");
        scheduledCleanup();
    }
    
    /**
     * 清理指定会话
     */
    public void cleanupSession(String sessionId) {
        log.info("清理会话：{}", sessionId);
        messageRepository.deleteBySessionId(sessionId);
        sessionRepository.deleteById(sessionId);
    }
}
