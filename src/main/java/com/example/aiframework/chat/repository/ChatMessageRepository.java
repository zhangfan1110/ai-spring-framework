package com.example.aiframework.chat.repository;

import com.example.aiframework.chat.entity.ChatMessageEntity;
import com.example.aiframework.chat.mapper.ChatMessageMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 聊天消息仓库
 */
@Repository
public class ChatMessageRepository {
    
    @Autowired
    private ChatMessageMapper chatMessageMapper;
    
    /**
     * 保存消息
     */
    public ChatMessageEntity save(ChatMessageEntity entity) {
        if (entity.getId() == null) {
            entity.setId(java.util.UUID.randomUUID().toString());
        }
        if (entity.getCreateTime() == null) {
            entity.setCreateTime(LocalDateTime.now());
        }
        entity.setUpdateTime(LocalDateTime.now());
        
        chatMessageMapper.insert(entity);
        return entity;
    }
    
    /**
     * 批量保存消息
     */
    public int batchSave(List<ChatMessageEntity> entities) {
        for (ChatMessageEntity entity : entities) {
            save(entity);
        }
        return entities.size();
    }
    
    /**
     * 按 ID 查询消息
     */
    public ChatMessageEntity findById(String id) {
        return chatMessageMapper.selectById(id);
    }
    
    /**
     * 按会话 ID 查询所有消息
     */
    public List<ChatMessageEntity> findBySessionId(String sessionId) {
        return chatMessageMapper.selectBySessionId(sessionId);
    }
    
    /**
     * 按会话 ID 查询最近 N 条消息
     */
    public List<ChatMessageEntity> findBySessionIdLimit(String sessionId, int limit) {
        return chatMessageMapper.selectBySessionIdLimit(sessionId, limit);
    }
    
    /**
     * 删除指定会话的所有消息
     */
    public int deleteBySessionId(String sessionId) {
        return chatMessageMapper.deleteBySessionId(sessionId);
    }
    
    /**
     * 删除指定时间之前的消息
     */
    public int deleteBeforeTime(LocalDateTime beforeTime) {
        return chatMessageMapper.deleteBeforeTime(beforeTime);
    }
    
    /**
     * 搜索消息（按关键词）
     */
    public List<ChatMessageEntity> searchByKeyword(String sessionId, String keyword, int limit) {
        return chatMessageMapper.searchByKeyword(sessionId, keyword, limit);
    }
    
    /**
     * 全局搜索消息（不限会话）
     */
    public List<ChatMessageEntity> searchByKeywordGlobal(String keyword, int limit) {
        return chatMessageMapper.searchByKeywordGlobal(keyword, limit);
    }
    
    /**
     * 按角色和关键词搜索
     */
    public List<ChatMessageEntity> searchByRoleAndKeyword(
            String sessionId, String role, String keyword, int limit) {
        return chatMessageMapper.searchByRoleAndKeyword(sessionId, role, keyword, limit);
    }
    
    /**
     * 按时间范围搜索
     */
    public List<ChatMessageEntity> searchByTimeRange(
            String sessionId, LocalDateTime startTime, LocalDateTime endTime, int limit) {
        return chatMessageMapper.searchByTimeRange(sessionId, startTime, endTime, limit);
    }
}
