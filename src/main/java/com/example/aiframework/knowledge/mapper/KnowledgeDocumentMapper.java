package com.example.aiframework.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aiframework.knowledge.entity.KnowledgeDocumentEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 知识文档 Mapper
 */
@Mapper
public interface KnowledgeDocumentMapper extends BaseMapper<KnowledgeDocumentEntity> {

    /**
     * 查询知识库的文档列表
     */
    @Select("SELECT * FROM knowledge_document WHERE knowledge_base_id = #{kbId} ORDER BY create_time DESC")
    List<KnowledgeDocumentEntity> findByKnowledgeBase(String kbId);

    /**
     * 查询待向量化的文档
     */
    @Select("SELECT * FROM knowledge_document WHERE vector_status = 'PENDING' LIMIT #{limit}")
    List<KnowledgeDocumentEntity> findPendingVectorization(int limit);

    /**
     * 统计文档数量
     */
    @Select("SELECT COUNT(*) FROM knowledge_document WHERE knowledge_base_id = #{kbId}")
    int countByKnowledgeBase(String kbId);
}
