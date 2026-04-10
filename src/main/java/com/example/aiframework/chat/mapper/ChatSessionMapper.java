package com.example.aiframework.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aiframework.chat.entity.ChatSessionEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 会话 Mapper
 */
@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSessionEntity> {
    
    /**
     * 查询所有会话 (按最后活跃时间排序)
     */
    List<ChatSessionEntity> selectAllOrderByLastActive();
    
    /**
     * 查询最近活跃的会话
     */
    List<ChatSessionEntity> selectRecentSessions(@Param("limit") int limit);
    
    /**
     * 删除指定会话
     */
    int deleteBySessionId(@Param("sessionId") String sessionId);
    
    /**
     * 删除指定时间之前的会话
     */
    int deleteBeforeTime(@Param("beforeTime") LocalDateTime beforeTime);
}
