package com.example.aiframework.model;

/**
 * 向量记录模型
 */
public class VectorRecord {
    
    private String id;
    private String docId;
    private String content;
    private Object metadata;
    private Double score;
    private Integer chunkIndex;      // 片段索引（第几个片段）
    private Integer totalChunks;     // 总片段数
    
    public VectorRecord() {}
    
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getDocId() { return docId; }
    public void setDocId(String docId) { this.docId = docId; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public Object getMetadata() { return metadata; }
    public void setMetadata(Object metadata) { this.metadata = metadata; }
    
    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }
    
    public Integer getChunkIndex() { return chunkIndex; }
    public void setChunkIndex(Integer chunkIndex) { this.chunkIndex = chunkIndex; }
    
    public Integer getTotalChunks() { return totalChunks; }
    public void setTotalChunks(Integer totalChunks) { this.totalChunks = totalChunks; }
}
