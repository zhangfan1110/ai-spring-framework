package com.example.aiframework.chat.repository;

import com.example.aiframework.chat.entity.ChatSessionTagEntity;
import com.example.aiframework.chat.mapper.ChatSessionTagMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 会话标签仓库
 */
@Repository
public class ChatSessionTagRepository {
    
    @Autowired
    private ChatSessionTagMapper chatSessionTagMapper;
    
    public ChatSessionTagEntity save(ChatSessionTagEntity entity) {
        if (entity.getId() == null) {
            entity.setId(java.util.UUID.randomUUID().toString());
        }
        if (entity.getCreateTime() == null) {
            entity.setCreateTime(LocalDateTime.now());
        }
        entity.setUpdateTime(LocalDateTime.now());
        
        if (chatSessionTagMapper.selectById(entity.getId()) == null) {
            chatSessionTagMapper.insert(entity);
        } else {
            chatSessionTagMapper.updateById(entity);
        }
        return entity;
    }
    
    public List<ChatSessionTagEntity> findBySessionId(String sessionId) {
        return chatSessionTagMapper.selectBySessionId(sessionId);
    }
    
    public int deleteBySessionId(String sessionId) {
        return chatSessionTagMapper.deleteBySessionId(sessionId);
    }
    
    public List<ChatSessionTagEntity> findByTagName(String tagName) {
        return chatSessionTagMapper.selectByTagName(tagName);
    }
    
    public List<String> findAllTags() {
        return chatSessionTagMapper.selectAllTags();
    }
}
