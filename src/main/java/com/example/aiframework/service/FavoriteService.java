package com.example.aiframework.service;

import com.example.aiframework.entity.ChatMessageEntity;
import com.example.aiframework.entity.ChatMessageFavoriteEntity;
import com.example.aiframework.repository.ChatMessageFavoriteRepository;
import com.example.aiframework.repository.ChatMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 收藏消息服务
 */
@Service
public class FavoriteService {
    
    private static final Logger log = LoggerFactory.getLogger(FavoriteService.class);
    
    @Autowired
    private ChatMessageFavoriteRepository favoriteRepository;
    
    @Autowired
    private ChatMessageRepository messageRepository;
    
    /**
     * 收藏消息
     */
    public ChatMessageFavoriteEntity favoriteMessage(String messageId, String note) {
        log.info("收藏消息 - messageId: {}", messageId);
        
        ChatMessageEntity message = messageRepository.findById(messageId);
        if (message == null) {
            throw new IllegalArgumentException("消息不存在");
        }
        
        ChatMessageFavoriteEntity favorite = ChatMessageFavoriteEntity.builder()
            .messageId(messageId)
            .sessionId(message.getSessionId())
            .content(message.getContent())
            .note(note)
            .build();
        
        return favoriteRepository.save(favorite);
    }
    
    /**
     * 获取所有收藏
     */
    public List<ChatMessageFavoriteEntity> getAllFavorites() {
        return favoriteRepository.findAll();
    }
    
    /**
     * 获取会话的收藏
     */
    public List<ChatMessageFavoriteEntity> getSessionFavorites(String sessionId) {
        return favoriteRepository.findBySessionId(sessionId);
    }
    
    /**
     * 取消收藏
     */
    public void unfavoriteMessage(String messageId) {
        log.info("取消收藏 - messageId: {}", messageId);
        favoriteRepository.deleteByMessageId(messageId);
    }
    
    /**
     * 更新收藏备注
     */
    public ChatMessageFavoriteEntity updateNote(String favoriteId, String note) {
        ChatMessageFavoriteEntity favorite = favoriteRepository.findById(favoriteId);
        if (favorite != null) {
            favorite.setNote(note);
            return favoriteRepository.save(favorite);
        }
        return null;
    }
}
