package com.example.aiframework.chat.service;

import com.example.aiframework.chat.dto.ChatSessionMergeRequestDTO;
import com.example.aiframework.chat.entity.ChatMessageEntity;
import com.example.aiframework.chat.entity.ChatSessionEntity;
import com.example.aiframework.chat.repository.ChatMessageRepository;
import com.example.aiframework.chat.repository.ChatSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 会话合并服务
 */
@Service
public class SessionMergeService {
    
    private static final Logger log = LoggerFactory.getLogger(SessionMergeService.class);
    
    @Autowired
    private ChatSessionRepository sessionRepository;
    
    @Autowired
    private ChatMessageRepository messageRepository;
    
    /**
     * 合并多个会话
     */
    public ChatSessionEntity mergeSessions(ChatSessionMergeRequestDTO request) {
        log.info("合并会话 - 目标：{}, 源：{}", 
            request.getTargetSessionId(), request.getSourceSessionIds());
        
        try {
            // 获取或创建目标会话
            ChatSessionEntity targetSession = sessionRepository.findById(request.getTargetSessionId());
            boolean isNewTarget = false;
            
            if (targetSession == null) {
                // 创建新会话作为目标
                targetSession = ChatSessionEntity.builder()
                    .sessionId(java.util.UUID.randomUUID().toString())
                    .title(request.getNewTitle() != null ? request.getNewTitle() : "合并的会话")
                    .messageCount(0)
                    .lastActiveTime(LocalDateTime.now())
                    .build();
                sessionRepository.save(targetSession);
                isNewTarget = true;
            }
            
            int totalMessages = 0;
            LocalDateTime latestTime = null;
            
            // 合并每个源会话的消息
            for (String sourceSessionId : request.getSourceSessionIds()) {
                if (sourceSessionId.equals(targetSession.getSessionId())) {
                    continue; // 跳过目标会话本身
                }
                
                List<ChatMessageEntity> sourceMessages = messageRepository.findBySessionId(sourceSessionId);
                
                // 将消息复制到目标会话
                for (ChatMessageEntity sourceMsg : sourceMessages) {
                    ChatMessageEntity newMessage = new ChatMessageEntity();
                    newMessage.setId(java.util.UUID.randomUUID().toString());
                    newMessage.setSessionId(targetSession.getSessionId());
                    newMessage.setRole(sourceMsg.getRole());
                    newMessage.setContent(sourceMsg.getContent());
                    newMessage.setModel(sourceMsg.getModel());
                    newMessage.setTokens(sourceMsg.getTokens());
                    newMessage.setCreateTime(sourceMsg.getCreateTime());
                    messageRepository.save(newMessage);
                    totalMessages++;
                    
                    if (sourceMsg.getCreateTime() != null) {
                        if (latestTime == null || sourceMsg.getCreateTime().isAfter(latestTime)) {
                            latestTime = sourceMsg.getCreateTime();
                        }
                    }
                }
                
                // 删除源会话 (如果不保留)
                if (!request.isKeepSourceSessions()) {
                    messageRepository.deleteBySessionId(sourceSessionId);
                    sessionRepository.deleteById(sourceSessionId);
                    log.info("删除源会话：{}", sourceSessionId);
                }
            }
            
            // 更新目标会话
            if (!isNewTarget) {
                targetSession.setMessageCount(targetSession.getMessageCount() + totalMessages);
            } else {
                targetSession.setMessageCount(totalMessages);
            }
            
            if (latestTime != null) {
                targetSession.setLastActiveTime(latestTime);
            }
            
            if (request.getNewTitle() != null) {
                targetSession.setTitle(request.getNewTitle());
            }
            
            sessionRepository.save(targetSession);
            
            log.info("合并会话完成 - 目标：{}, 新增消息：{}", 
                targetSession.getSessionId(), totalMessages);
            
            return targetSession;
            
        } catch (Exception e) {
            log.error("合并会话失败：{}", e.getMessage(), e);
            throw new RuntimeException("合并会话失败：" + e.getMessage(), e);
        }
    }
    
    /**
     * 简化的合并方法
     */
    public ChatSessionEntity mergeSessions(String targetSessionId, List<String> sourceSessionIds) {
        ChatSessionMergeRequestDTO request = ChatSessionMergeRequestDTO.builder()
            .targetSessionId(targetSessionId)
            .sourceSessionIds(sourceSessionIds)
            .keepSourceSessions(false)
            .build();
        return mergeSessions(request);
    }
}
