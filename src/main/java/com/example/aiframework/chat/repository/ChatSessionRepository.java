package com.example.aiframework.chat.repository;

import com.example.aiframework.chat.entity.ChatSessionEntity;
import com.example.aiframework.chat.mapper.ChatSessionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 会话仓库
 */
@Repository
public class ChatSessionRepository {
    
    @Autowired
    private ChatSessionMapper chatSessionMapper;
    
    /**
     * 保存会话
     */
    public ChatSessionEntity save(ChatSessionEntity entity) {
        if (entity.getCreateTime() == null) {
            entity.setCreateTime(LocalDateTime.now());
        }
        entity.setUpdateTime(LocalDateTime.now());
        
        if (chatSessionMapper.selectById(entity.getSessionId()) == null) {
            chatSessionMapper.insert(entity);
        } else {
            chatSessionMapper.updateById(entity);
        }
        return entity;
    }
    
    /**
     * 按 ID 查询会话
     */
    public ChatSessionEntity findById(String sessionId) {
        return chatSessionMapper.selectById(sessionId);
    }
    
    /**
     * 查询所有会话
     */
    public List<ChatSessionEntity> findAll() {
        return chatSessionMapper.selectAllOrderByLastActive();
    }
    
    /**
     * 查询最近活跃的会话
     */
    public List<ChatSessionEntity> findRecentSessions(int limit) {
        return chatSessionMapper.selectRecentSessions(limit);
    }
    
    /**
     * 删除会话
     */
    public int deleteById(String sessionId) {
        return chatSessionMapper.deleteBySessionId(sessionId);
    }
    
    /**
     * 删除指定时间之前的会话
     */
    public int deleteBeforeTime(LocalDateTime beforeTime) {
        return chatSessionMapper.deleteBeforeTime(beforeTime);
    }
}
