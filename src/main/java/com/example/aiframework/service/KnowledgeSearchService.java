package com.example.aiframework.service;

import com.example.aiframework.entity.KnowledgeChunkEntity;
import com.example.aiframework.mapper.KnowledgeChunkMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 知识检索服务
 */
@Service
public class KnowledgeSearchService {

    @Autowired
    private KnowledgeChunkMapper chunkMapper;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 语义搜索
     * 
     * @param knowledgeBaseId 知识库 ID
     * @param query 查询文本
     * @param topK 返回数量
     * @return 搜索结果
     */
    public List<SearchResult> semanticSearch(String knowledgeBaseId, String query, int topK) {
        // TODO: 使用 Milvus 进行向量相似度搜索
        // 这里先实现基于数据库的简单搜索
        
        List<KnowledgeChunkEntity> chunks = chunkMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<KnowledgeChunkEntity>()
                .eq(KnowledgeChunkEntity::getKnowledgeBaseId, knowledgeBaseId)
                .like(KnowledgeChunkEntity::getContent, query)
                .last("LIMIT " + topK)
        );

        List<SearchResult> results = new ArrayList<>();
        for (KnowledgeChunkEntity chunk : chunks) {
            SearchResult result = new SearchResult();
            result.setChunkId(chunk.getId());
            result.setContent(chunk.getContent());
            result.setScore(0.8); // 模拟分数
            
            if (chunk.getMetadata() != null) {
                try {
                    Map<String, Object> metadata = objectMapper.readValue(
                        chunk.getMetadata(), 
                        Map.class
                    );
                    result.setMetadata(metadata);
                } catch (Exception e) {
                    // Ignore
                }
            }

            results.add(result);
        }

        return results;
    }

    /**
     * 混合搜索（语义 + 关键词）
     */
    public List<SearchResult> hybridSearch(String knowledgeBaseId, String query, int topK) {
        return semanticSearch(knowledgeBaseId, query, topK);
    }

    /**
     * 搜索结果
     */
    public static class SearchResult {
        private String chunkId;
        private String content;
        private Double score;
        private Map<String, Object> metadata;

        public String getChunkId() { return chunkId; }
        public void setChunkId(String chunkId) { this.chunkId = chunkId; }
        
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        
        public Double getScore() { return score; }
        public void setScore(Double score) { this.score = score; }
        
        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    }
}
