package com.example.aiframework.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aiframework.chat.entity.ChatMessageEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 聊天消息 Mapper
 */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessageEntity> {
    
    /**
     * 按会话 ID 查询消息列表
     */
    List<ChatMessageEntity> selectBySessionId(@Param("sessionId") String sessionId);
    
    /**
     * 按会话 ID 查询最近 N 条消息
     */
    List<ChatMessageEntity> selectBySessionIdLimit(@Param("sessionId") String sessionId, @Param("limit") int limit);
    
    /**
     * 删除指定会话的所有消息
     */
    int deleteBySessionId(@Param("sessionId") String sessionId);
    
    /**
     * 删除指定时间之前的消息
     */
    int deleteBeforeTime(@Param("beforeTime") LocalDateTime beforeTime);
    
    /**
     * 搜索消息（按关键词匹配内容）
     */
    List<ChatMessageEntity> searchByKeyword(
        @Param("sessionId") String sessionId,
        @Param("keyword") String keyword,
        @Param("limit") int limit
    );
    
    /**
     * 搜索消息（按关键词匹配内容，不限会话）
     */
    List<ChatMessageEntity> searchByKeywordGlobal(
        @Param("keyword") String keyword,
        @Param("limit") int limit
    );
    
    /**
     * 按角色和关键词搜索
     */
    List<ChatMessageEntity> searchByRoleAndKeyword(
        @Param("sessionId") String sessionId,
        @Param("role") String role,
        @Param("keyword") String keyword,
        @Param("limit") int limit
    );
    
    /**
     * 按时间范围搜索
     */
    List<ChatMessageEntity> searchByTimeRange(
        @Param("sessionId") String sessionId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime,
        @Param("limit") int limit
    );
}
