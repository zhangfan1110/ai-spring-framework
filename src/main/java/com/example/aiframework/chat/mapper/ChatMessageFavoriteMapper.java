package com.example.aiframework.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aiframework.chat.entity.ChatMessageFavoriteEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 收藏消息 Mapper
 */
@Mapper
public interface ChatMessageFavoriteMapper extends BaseMapper<ChatMessageFavoriteEntity> {
    
    List<ChatMessageFavoriteEntity> selectAllOrderByTime();
    
    List<ChatMessageFavoriteEntity> selectBySessionId(@Param("sessionId") String sessionId);
    
    int deleteByMessageId(@Param("messageId") String messageId);
}
