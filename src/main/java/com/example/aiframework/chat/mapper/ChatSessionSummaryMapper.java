package com.example.aiframework.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aiframework.chat.entity.ChatSessionSummaryEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 会话摘要 Mapper
 */
@Mapper
public interface ChatSessionSummaryMapper extends BaseMapper<ChatSessionSummaryEntity> {
    
    /**
     * 按会话 ID 查询摘要
     */
    ChatSessionSummaryEntity selectBySessionId(@Param("sessionId") String sessionId);
    
    /**
     * 查询会话的所有摘要 (按时间排序)
     */
    List<ChatSessionSummaryEntity> selectBySessionIdOrderByTime(@Param("sessionId") String sessionId);
    
    /**
     * 删除指定会话的摘要
     */
    int deleteBySessionId(@Param("sessionId") String sessionId);
}
