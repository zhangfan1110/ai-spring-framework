package com.example.aiframework.chat.repository;

import com.example.aiframework.chat.entity.ChatSessionSummaryEntity;
import com.example.aiframework.chat.mapper.ChatSessionSummaryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 会话摘要仓库
 */
@Repository
public class ChatSessionSummaryRepository {
    
    @Autowired
    private ChatSessionSummaryMapper chatSessionSummaryMapper;
    
    /**
     * 保存摘要
     */
    public ChatSessionSummaryEntity save(ChatSessionSummaryEntity entity) {
        if (entity.getId() == null) {
            entity.setId(java.util.UUID.randomUUID().toString());
        }
        if (entity.getCreateTime() == null) {
            entity.setCreateTime(LocalDateTime.now());
        }
        entity.setUpdateTime(LocalDateTime.now());
        
        if (chatSessionSummaryMapper.selectById(entity.getId()) == null) {
            chatSessionSummaryMapper.insert(entity);
        } else {
            chatSessionSummaryMapper.updateById(entity);
        }
        return entity;
    }
    
    /**
     * 按会话 ID 查询最新摘要
     */
    public ChatSessionSummaryEntity findBySessionId(String sessionId) {
        return chatSessionSummaryMapper.selectBySessionId(sessionId);
    }
    
    /**
     * 查询会话的所有摘要
     */
    public List<ChatSessionSummaryEntity> findBySessionIdOrderByTime(String sessionId) {
        return chatSessionSummaryMapper.selectBySessionIdOrderByTime(sessionId);
    }
    
    /**
     * 删除会话的摘要
     */
    public int deleteBySessionId(String sessionId) {
        return chatSessionSummaryMapper.deleteBySessionId(sessionId);
    }
}
