package com.example.aiframework.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aiframework.knowledge.entity.KnowledgeBaseEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 知识库 Mapper
 */
@Mapper
public interface KnowledgeBaseMapper extends BaseMapper<KnowledgeBaseEntity> {

    /**
     * 查询用户的知识库列表
     */
    @Select("SELECT * FROM knowledge_base WHERE created_by = #{userId} OR type = 'PUBLIC' ORDER BY create_time DESC")
    List<KnowledgeBaseEntity> findByUser(String userId);

    /**
     * 统计知识库数量
     */
    @Select("SELECT COUNT(*) FROM knowledge_base WHERE created_by = #{userId}")
    int countByUser(String userId);
}
