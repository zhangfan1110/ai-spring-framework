package com.example.aiframework.chat.repository;

import com.example.aiframework.chat.entity.ChatSessionShareEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 会话分享 Repository
 */
@Mapper
public interface ChatSessionShareRepository {
    
    /**
     * 根据 ID 查询
     */
    ChatSessionShareEntity findById(@Param("shareId") String shareId);
    
    /**
     * 根据分享令牌查询
     */
    ChatSessionShareEntity findByShareToken(@Param("shareToken") String shareToken);
    
    /**
     * 根据会话 ID 查询所有分享
     */
    List<ChatSessionShareEntity> findBySessionId(@Param("sessionId") String sessionId);
    
    /**
     * 查询用户的分享列表
     */
    List<ChatSessionShareEntity> findByCreatorId(@Param("creatorId") String creatorId);
    
    /**
     * 查询公开分享
     */
    List<ChatSessionShareEntity> findPublicShares(@Param("limit") int limit, @Param("offset") int offset);
    
    /**
     * 保存
     */
    int save(ChatSessionShareEntity entity);
    
    /**
     * 更新
     */
    int update(ChatSessionShareEntity entity);
    
    /**
     * 删除
     */
    int deleteById(@Param("shareId") String shareId);
    
    /**
     * 增加访问次数
     */
    int incrementViewCount(@Param("shareId") String shareId);
    
    /**
     * 禁用/启用
     */
    int setDisabled(@Param("shareId") String shareId, @Param("disabled") boolean disabled);
}
