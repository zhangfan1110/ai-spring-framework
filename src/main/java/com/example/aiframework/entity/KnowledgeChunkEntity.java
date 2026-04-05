package com.example.aiframework.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

/**
 * 知识文档分块实体
 */
@TableName("knowledge_chunk")
public class KnowledgeChunkEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 文档 ID
     */
    private String documentId;

    /**
     * 知识库 ID
     */
    private String knowledgeBaseId;

    /**
     * 分块内容
     */
    private String content;

    /**
     * 分块索引
     */
    private Integer chunkIndex;

    /**
     * 向量 ID（Milvus 中的 ID）
     */
    private String vectorId;

    /**
     * 元数据（JSON）
     */
    private String metadata;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }
    
    public String getKnowledgeBaseId() { return knowledgeBaseId; }
    public void setKnowledgeBaseId(String knowledgeBaseId) { this.knowledgeBaseId = knowledgeBaseId; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public Integer getChunkIndex() { return chunkIndex; }
    public void setChunkIndex(Integer chunkIndex) { this.chunkIndex = chunkIndex; }
    
    public String getVectorId() { return vectorId; }
    public void setVectorId(String vectorId) { this.vectorId = vectorId; }
    
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
    
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
