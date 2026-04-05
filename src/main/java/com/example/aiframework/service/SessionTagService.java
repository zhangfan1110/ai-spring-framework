package com.example.aiframework.service;

import com.example.aiframework.entity.ChatSessionTagEntity;
import com.example.aiframework.repository.ChatSessionTagRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 会话标签服务
 */
@Service
public class SessionTagService {
    
    private static final Logger log = LoggerFactory.getLogger(SessionTagService.class);
    
    @Autowired
    private ChatSessionTagRepository tagRepository;
    
    /**
     * 为会话添加标签
     */
    public ChatSessionTagEntity addTag(String sessionId, String tagName, String tagColor) {
        log.info("为会话添加标签 - sessionId: {}, tagName: {}, tagColor: {}", sessionId, tagName, tagColor);
        
        ChatSessionTagEntity tag = ChatSessionTagEntity.builder()
            .sessionId(sessionId)
            .tagName(tagName)
            .tagColor(tagColor != null ? tagColor : "#1890ff")
            .build();
        
        return tagRepository.save(tag);
    }
    
    /**
     * 获取会话的所有标签
     */
    public List<ChatSessionTagEntity> getSessionTags(String sessionId) {
        return tagRepository.findBySessionId(sessionId);
    }
    
    /**
     * 删除会话的标签
     */
    public void removeTag(String sessionId, String tagName) {
        log.info("删除会话标签 - sessionId: {}, tagName: {}", sessionId, tagName);
        
        List<ChatSessionTagEntity> tags = tagRepository.findBySessionId(sessionId);
        for (ChatSessionTagEntity tag : tags) {
            if (tag.getTagName().equals(tagName)) {
                tagRepository.deleteBySessionId(tag.getId());
            }
        }
    }
    
    /**
     * 删除会话的所有标签
     */
    public void removeAllTags(String sessionId) {
        tagRepository.deleteBySessionId(sessionId);
    }
    
    /**
     * 获取所有标签
     */
    public List<String> getAllTags() {
        return tagRepository.findAllTags();
    }
    
    /**
     * 根据标签查找会话
     */
    public List<String> getSessionIdsByTag(String tagName) {
        List<ChatSessionTagEntity> tags = tagRepository.findByTagName(tagName);
        return tags.stream()
            .map(ChatSessionTagEntity::getSessionId)
            .collect(java.util.stream.Collectors.toList());
    }
}
