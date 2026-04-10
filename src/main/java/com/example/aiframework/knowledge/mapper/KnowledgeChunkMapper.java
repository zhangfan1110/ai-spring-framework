package com.example.aiframework.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aiframework.knowledge.entity.KnowledgeChunkEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 知识分块 Mapper
 */
@Mapper
public interface KnowledgeChunkMapper extends BaseMapper<KnowledgeChunkEntity> {

    /**
     * 查询文档的分块列表
     */
    @Select("SELECT * FROM knowledge_chunk WHERE document_id = #{docId} ORDER BY chunk_index ASC")
    List<KnowledgeChunkEntity> findByDocument(String docId);

    /**
     * 删除文档的所有分块
     */
    @Select("DELETE FROM knowledge_chunk WHERE document_id = #{docId}")
    void deleteByDocument(String docId);
}
