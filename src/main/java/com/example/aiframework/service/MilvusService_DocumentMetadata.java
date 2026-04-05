package com.example.aiframework.service;

import java.time.LocalDateTime;

/**
 * 文档元数据
 */
public class MilvusService_DocumentMetadata {
    String originalId;
    String content;
    int chunkIndex;
    int totalChunks;
    LocalDateTime createdAt;
    
    public MilvusService_DocumentMetadata(String originalId, String content, int chunkIndex, int totalChunks) {
        this.originalId = originalId;
        this.content = content;
        this.chunkIndex = chunkIndex;
        this.totalChunks = totalChunks;
        this.createdAt = LocalDateTime.now();
    }
    
    public String getOriginalId() { return originalId; }
    public void setOriginalId(String originalId) { this.originalId = originalId; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public int getChunkIndex() { return chunkIndex; }
    public void setChunkIndex(int chunkIndex) { this.chunkIndex = chunkIndex; }
    
    public int getTotalChunks() { return totalChunks; }
    public void setTotalChunks(int totalChunks) { this.totalChunks = totalChunks; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
