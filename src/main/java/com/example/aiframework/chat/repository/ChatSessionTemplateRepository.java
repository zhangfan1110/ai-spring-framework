package com.example.aiframework.chat.repository;

import com.example.aiframework.chat.entity.ChatSessionTemplateEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 会话模板 Repository
 */
@Mapper
public interface ChatSessionTemplateRepository {
    
    /**
     * 根据 ID 查询
     */
    ChatSessionTemplateEntity findById(@Param("templateId") String templateId);
    
    /**
     * 查询所有模板
     */
    List<ChatSessionTemplateEntity> findAll();
    
    /**
     * 按分类查询
     */
    List<ChatSessionTemplateEntity> findByCategory(@Param("category") String category);
    
    /**
     * 查询内置模板
     */
    List<ChatSessionTemplateEntity> findBuiltInTemplates();
    
    /**
     * 查询用户自定义模板
     */
    List<ChatSessionTemplateEntity> findByCreatorId(@Param("creatorId") String creatorId);
    
    /**
     * 搜索模板
     */
    List<ChatSessionTemplateEntity> search(@Param("keyword") String keyword);
    
    /**
     * 保存
     */
    int save(ChatSessionTemplateEntity entity);
    
    /**
     * 更新
     */
    int update(ChatSessionTemplateEntity entity);
    
    /**
     * 删除
     */
    int deleteById(@Param("templateId") String templateId);
    
    /**
     * 增加使用次数
     */
    int incrementUsageCount(@Param("templateId") String templateId);
}
