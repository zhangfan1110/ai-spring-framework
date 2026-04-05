package com.example.aiframework.repository;

import com.example.aiframework.entity.ChatMessageFavoriteEntity;
import com.example.aiframework.mapper.ChatMessageFavoriteMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 收藏消息仓库
 */
@Repository
public class ChatMessageFavoriteRepository {
    
    @Autowired
    private ChatMessageFavoriteMapper chatMessageFavoriteMapper;
    
    public ChatMessageFavoriteEntity save(ChatMessageFavoriteEntity entity) {
        if (entity.getId() == null) {
            entity.setId(java.util.UUID.randomUUID().toString());
        }
        if (entity.getCreateTime() == null) {
            entity.setCreateTime(LocalDateTime.now());
        }
        entity.setUpdateTime(LocalDateTime.now());
        
        if (chatMessageFavoriteMapper.selectById(entity.getId()) == null) {
            chatMessageFavoriteMapper.insert(entity);
        } else {
            chatMessageFavoriteMapper.updateById(entity);
        }
        return entity;
    }
    
    public List<ChatMessageFavoriteEntity> findAll() {
        return chatMessageFavoriteMapper.selectAllOrderByTime();
    }
    
    public List<ChatMessageFavoriteEntity> findBySessionId(String sessionId) {
        return chatMessageFavoriteMapper.selectBySessionId(sessionId);
    }
    
    public int deleteByMessageId(String messageId) {
        return chatMessageFavoriteMapper.deleteByMessageId(messageId);
    }
    
    public ChatMessageFavoriteEntity findById(String id) {
        return chatMessageFavoriteMapper.selectById(id);
    }
    
    public int deleteById(String id) {
        return chatMessageFavoriteMapper.deleteById(id);
    }
}
