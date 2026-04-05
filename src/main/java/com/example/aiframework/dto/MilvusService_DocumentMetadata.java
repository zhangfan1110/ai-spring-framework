package com.example.aiframework.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 文档元数据
 */
@Setter
@Getter
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

}
