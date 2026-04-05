package com.example.aiframework.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aiframework.entity.ChatSessionTagEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 会话标签 Mapper
 */
@Mapper
public interface ChatSessionTagMapper extends BaseMapper<ChatSessionTagEntity> {
    
    List<ChatSessionTagEntity> selectBySessionId(@Param("sessionId") String sessionId);
    
    int deleteBySessionId(@Param("sessionId") String sessionId);
    
    List<ChatSessionTagEntity> selectByTagName(@Param("tagName") String tagName);
    
    List<String> selectAllTags();
}
