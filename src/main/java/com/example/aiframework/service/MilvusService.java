package com.example.aiframework.service;

import com.example.aiframework.model.CollectionRequest;
import com.example.aiframework.model.SearchRequest;
import com.example.aiframework.model.VectorRecord;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Milvus 向量数据库服务（基于 LangChain4j）
 * 支持文档分词后保存向量
 */
@Service
public class MilvusService {
    
    private static final Logger log = LoggerFactory.getLogger(MilvusService.class);
    
    @Resource
    private MilvusEmbeddingStore embeddingStore;
    
    @Resource
    private EmbeddingModel embeddingModel;
    
    @Resource
    private TextSplitterService textSplitter;
    
    @Autowired
    private MilvusNativeService nativeService;
    
    @Value("${milvus.default-collection:default_collection}")
    private String defaultCollection;
    
    /**
     * 文档元数据缓存
     */
    private final Map<String, DocumentMetadata> documentMetadataCache = new ConcurrentHashMap<>();
    
    /**
     * 文档元数据类
     */
    private static class DocumentMetadata {
        String originalId;
        String content;
        int chunkIndex;
        int totalChunks;
        LocalDateTime createdAt;
        
        DocumentMetadata(String originalId, String content, int chunkIndex, int totalChunks) {
            this.originalId = originalId;
            this.content = content;
            this.chunkIndex = chunkIndex;
            this.totalChunks = totalChunks;
            this.createdAt = LocalDateTime.now();
        }
    }
    
    /**
     * 创建集合（根据对象字段，使用原生 SDK）
     */
    public boolean createCollection(CollectionRequest request) {
        log.info("创建集合：{}", request.getCollectionName());
        
        // 使用原生 SDK 创建集合（支持自定义字段）
        boolean success = nativeService.createCollection(request);
        
        if (success) {
            log.info("集合创建成功：{}", request.getCollectionName());
        } else {
            log.error("集合创建失败：{}", request.getCollectionName());
        }
        
        return success;
    }
    
    /**
     * 删除集合
     */
    public boolean deleteCollection(String collectionName) {
        log.info("删除集合：{}", collectionName);
        return nativeService.dropCollection(collectionName);
    }
    
    /**
     * 检查集合是否存在
     */
    public boolean hasCollection(String collectionName) {
        return nativeService.hasCollection(collectionName);
    }
    
    /**
     * 获取集合列表
     */
    public List<String> listCollections() {
        List<String> collections = new ArrayList<>();
        if (embeddingStore != null) {
            collections.add(defaultCollection);
        }
        return collections;
    }
    
    /**
     * 获取集合统计
     */
    public Map<String, Object> getCollectionStats(String collectionName) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("collectionName", collectionName);
        stats.put("exists", hasCollection(collectionName));
        stats.put("rowCount", documentMetadataCache.size());
        return stats;
    }
    
    /**
     * 向量搜索
     */
    public List<VectorRecord> searchVectors(SearchRequest request) {
        if (embeddingStore == null || embeddingModel == null) {
            log.warn("Milvus Embedding Store 或 Embedding Model 未初始化");
            return Collections.emptyList();
        }
        
        try {
            Embedding queryEmbedding = embeddingModel.embed(request.getQueryText()).content();
            
            List<EmbeddingMatch<TextSegment>> matches = embeddingStore.findRelevant(
                queryEmbedding, 
                request.getTopK() != null ? request.getTopK() : 5
            );
            
            List<VectorRecord> results = new ArrayList<>();
            for (EmbeddingMatch<TextSegment> match : matches) {
                VectorRecord record = new VectorRecord();
                record.setId(match.embeddingId());
                record.setScore(match.score());
                
                // 从元数据缓存获取原文本
                DocumentMetadata metadata = documentMetadataCache.get(match.embeddingId());
                if (metadata != null) {
                    record.setContent(metadata.content);
                    record.setChunkIndex(metadata.chunkIndex);
                    record.setTotalChunks(metadata.totalChunks);
                } else {
                    record.setContent(match.embedded() != null ? match.embedded().text() : "");
                }
                
                results.add(record);
            }
            
            log.info("搜索到 {} 条相关结果", results.size());
            return results;
            
        } catch (Exception e) {
            log.error("搜索向量失败：{}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 添加文档（带智能分词）
     */
    public String addDocument(String content) {
        return addDocumentWithSplitting(content, 500, 50, true);
    }
    
    /**
     * 添加文档（带分词）
     * 
     * ⚠️ 重要：Milvus 默认 VARCHAR 字段最大长度为 36 字符
     * 如需存储更长文本，请在 Milvus 中手动创建集合并设置更大的 max_length
     * 
     * @param content 文档内容
     * @param chunkSize 分片大小 (字符数)，建议 30 以内 (Milvus 默认限制)
     * @param overlap 重叠字符数
     * @param enableSplitting 是否启用分片
     * @return 文档 ID
     */
    public String addDocumentWithSplitting(String content, int chunkSize, int overlap, boolean enableSplitting) {
        if (embeddingStore == null || embeddingModel == null) {
            log.warn("Milvus Embedding Store 或 Embedding Model 未初始化");
            return null;
        }
        
        if (content == null || content.trim().isEmpty()) {
            log.warn("文档内容为空");
            return null;
        }
        
        try {
            String docId = UUID.randomUUID().toString();
            List<String> chunks;
            
            // ⚠️ Milvus 默认限制：文本字段最大长度 36 字符
            // 如果 chunkSize 超过 30，发出警告
            if (chunkSize > 30) {
                log.warn("⚠️ 警告：chunkSize={} 超过 Milvus 默认限制 (36 字符)，可能导致插入失败", chunkSize);
                log.warn("解决方案 1: 减小 chunkSize 到 30 以内");
                log.warn("解决方案 2: 在 Milvus 中手动创建集合，设置 VARCHAR 字段 max_length=65535");
            }
            
            if (enableSplitting && content.length() > chunkSize) {
                chunks = textSplitter.smartSplit(content, chunkSize, overlap);
                log.info("文档已分词：{} 个片段 (chunkSize={}, overlap={})", chunks.size(), chunkSize, overlap);
            } else {
                chunks = Collections.singletonList(content);
                log.info("文档未分词：整个文档作为一个片段");
            }
            
            // 确保每个片段不超过 Milvus 限制
            List<String> safeChunks = new ArrayList<>();
            for (String chunk : chunks) {
                if (chunk.length() > 30) {
                    // 进一步分割
                    List<String> subChunks = textSplitter.splitByCharacters(chunk, 30, 0);
                    safeChunks.addAll(subChunks);
                    log.warn("片段长度 {} 超过 30，已分割为 {} 个子片段", chunk.length(), subChunks.size());
                } else {
                    safeChunks.add(chunk);
                }
            }
            
            int savedCount = 0;
            for (int i = 0; i < safeChunks.size(); i++) {
                String chunk = safeChunks.get(i);
//                String chunkId = docId + "_chunk_" + i;
                String chunkId = UUID.randomUUID().toString();
                // 向量化
                Embedding embedding = embeddingModel.embed(chunk).content();

                // 创建文本片段 (带元数据)
                dev.langchain4j.data.document.Metadata metadatainfo = new dev.langchain4j.data.document.Metadata();
                TextSegment segment = TextSegment.from(chunk, metadatainfo);
                // 添加到 Milvus
                embeddingStore.add(embedding,segment);
                
                DocumentMetadata metadata = new DocumentMetadata(docId, chunk, i, safeChunks.size());
                documentMetadataCache.put(chunkId, metadata);
                
                savedCount++;
            }
            
            log.info("文档添加完成：id={}, 总片段数={}, 保存成功={}", docId, safeChunks.size(), savedCount);
            return docId;
            
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("exceeds max length")) {
                log.error("❌ Milvus 文本长度超限！错误：{}", e.getMessage());
                log.error("解决方案：");
                log.error("  1. 减小 chunkSize 参数 (当前默认值可能太大)");
                log.error("  2. 或者在 Milvus 中手动创建集合，设置更大的 max_length:");
                log.error("     ALTER COLLECTION default_collection ALTER FIELD text SET max_length 65535;");
            } else {
                log.error("添加文档失败：{}", e.getMessage(), e);
            }
            return null;
        }
    }
    
    /**
     * 搜索文档
     */
    public List<String> searchDocuments(String query, int maxResults) {
        if (embeddingStore == null || embeddingModel == null) {
            return Collections.emptyList();
        }
        
        try {
            Embedding queryEmbedding = embeddingModel.embed(query).content();
            
            List<EmbeddingMatch<TextSegment>> matches = embeddingStore.findRelevant(
                queryEmbedding, 
                maxResults
            );
            
            List<String> results = new ArrayList<>();
            for (EmbeddingMatch<TextSegment> match : matches) {
                DocumentMetadata metadata = documentMetadataCache.get(match.embeddingId());
                if (metadata != null) {
                    results.add(metadata.content);
                } else if (match.embedded() != null) {
                    results.add(match.embedded().text());
                }
            }
            
            log.info("搜索到 {} 条相关文档", results.size());
            return results;
            
        } catch (Exception e) {
            log.error("搜索文档失败：{}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    // ========== 集合管理增强 ==========
    
    /**
     * 重命名集合
     */
    public boolean renameCollection(String oldName, String newName) {
        log.info("重命名集合：{} -> {}", oldName, newName);
        
        try {
            // Milvus 不支持直接重命名，需要创建新集合并复制数据
            if (!hasCollection(oldName)) {
                log.error("原集合不存在：{}", oldName);
                return false;
            }
            
            if (hasCollection(newName)) {
                log.error("新集合已存在：{}", newName);
                return false;
            }
            
            // 使用原生服务重命名
            return nativeService.renameCollection(oldName, newName);
            
        } catch (Exception e) {
            log.error("重命名集合失败：{}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 克隆集合
     */
    public boolean cloneCollection(String sourceName, String targetName) {
        log.info("克隆集合：{} -> {}", sourceName, targetName);
        
        try {
            if (!hasCollection(sourceName)) {
                log.error("源集合不存在：{}", sourceName);
                return false;
            }
            
            if (hasCollection(targetName)) {
                log.error("目标集合已存在：{}", targetName);
                return false;
            }
            
            // 使用原生服务克隆
            return nativeService.cloneCollection(sourceName, targetName);
            
        } catch (Exception e) {
            log.error("克隆集合失败：{}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 创建索引
     */
    public boolean createIndex(String collectionName, Map<String, Object> indexParams) {
        log.info("为集合创建索引：{}", collectionName);
        
        try {
            return nativeService.createIndex(collectionName, indexParams);
            
        } catch (Exception e) {
            log.error("创建索引失败：{}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 重建索引
     */
    public boolean rebuildIndex(String collectionName) {
        log.info("重建集合索引：{}", collectionName);
        
        try {
            // 删除旧索引
            nativeService.dropIndex(collectionName);
            
            // 创建新索引
            Map<String, Object> defaultParams = new HashMap<>();
            defaultParams.put("indexType", "IVF_FLAT");
            defaultParams.put("metricType", "L2");
            defaultParams.put("params", Map.of("nlist", 1024));
            
            return nativeService.createIndex(collectionName, defaultParams);
            
        } catch (Exception e) {
            log.error("重建索引失败：{}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 加载集合到内存
     */
    public boolean loadCollection(String collectionName) {
        log.info("加载集合到内存：{}", collectionName);
        
        try {
            return nativeService.loadCollection(collectionName);
            
        } catch (Exception e) {
            log.error("加载集合失败：{}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 释放集合内存
     */
    public boolean releaseCollection(String collectionName) {
        log.info("释放集合内存：{}", collectionName);
        
        try {
            return nativeService.releaseCollection(collectionName);
            
        } catch (Exception e) {
            log.error("释放集合失败：{}", e.getMessage(), e);
            return false;
        }
    }
    
    // ========== 高级搜索 ==========
    
    /**
     * 混合搜索（向量 + 关键词）
     */
    public List<VectorRecord> hybridSearch(String collectionName, String queryText, 
                                           String keywords, int topK) {
        log.info("混合搜索 - 集合：{}, 文本：{}, 关键词：{}, topK: {}", 
            collectionName, queryText, keywords, topK);
        
        try {
            // 1. 向量搜索
            SearchRequest vectorRequest = new SearchRequest();
            vectorRequest.setQueryText(queryText);
            vectorRequest.setTopK(topK);
            List<VectorRecord> vectorResults = searchVectors(vectorRequest);
            
            // 2. 关键词过滤
            if (keywords != null && !keywords.isEmpty()) {
                List<VectorRecord> filteredResults = new ArrayList<>();
                for (VectorRecord record : vectorResults) {
                    if (record.getContent() != null && 
                        record.getContent().toLowerCase().contains(keywords.toLowerCase())) {
                        filteredResults.add(record);
                    }
                }
                log.info("关键词过滤后：{} 条结果", filteredResults.size());
                return filteredResults;
            }
            
            return vectorResults;
            
        } catch (Exception e) {
            log.error("混合搜索失败：{}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 过滤搜索（带元数据过滤）
     */
    public List<VectorRecord> filteredSearch(String collectionName, String queryText, 
                                             int topK, Map<String, Object> filters) {
        log.info("过滤搜索 - 集合：{}, 文本：{}, 过滤条件：{}", 
            collectionName, queryText, filters);
        
        try {
            // 1. 向量搜索
            SearchRequest vectorRequest = new SearchRequest();
            vectorRequest.setQueryText(queryText);
            vectorRequest.setTopK(topK * 2); // 多取一些用于过滤
            List<VectorRecord> vectorResults = searchVectors(vectorRequest);
            
            // 2. 元数据过滤
            if (filters != null && !filters.isEmpty()) {
                List<VectorRecord> filteredResults = new ArrayList<>();
                for (VectorRecord record : vectorResults) {
                    boolean matches = true;
                    
                    for (Map.Entry<String, Object> filter : filters.entrySet()) {
                        String key = filter.getKey();
                        Object value = filter.getValue();
                        
                        // 简单匹配：检查内容是否包含关键词
                        if (value instanceof String) {
                            if (record.getContent() == null || 
                                !record.getContent().contains((String) value)) {
                                matches = false;
                                break;
                            }
                        }
                    }
                    
                    if (matches) {
                        filteredResults.add(record);
                    }
                }
                
                // 限制返回数量
                return filteredResults.subList(0, Math.min(topK, filteredResults.size()));
            }
            
            return vectorResults.subList(0, Math.min(topK, vectorResults.size()));
            
        } catch (Exception e) {
            log.error("过滤搜索失败：{}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 多集合搜索
     */
    public Map<String, List<VectorRecord>> multiCollectionSearch(String queryText, 
                                                                  List<String> collectionNames, 
                                                                  int topKPerCollection) {
        log.info("多集合搜索 - 集合：{}, 文本：{}, 每集合 topK: {}", 
            collectionNames, queryText, topKPerCollection);
        
        Map<String, List<VectorRecord>> results = new HashMap<>();
        
        for (String collectionName : collectionNames) {
            try {
                SearchRequest request = new SearchRequest();
                request.setQueryText(queryText);
                request.setTopK(topKPerCollection);
                
                List<VectorRecord> collectionResults = searchVectors(request);
                results.put(collectionName, collectionResults);
                
            } catch (Exception e) {
                log.error("集合 {} 搜索失败：{}", collectionName, e.getMessage());
                results.put(collectionName, Collections.emptyList());
            }
        }
        
        return results;
    }
    
    /**
     * 合并多集合搜索结果
     */
    public List<VectorRecord> mergedMultiCollectionSearch(String queryText, 
                                                           List<String> collectionNames, 
                                                           int topKTotal) {
        log.info("合并多集合搜索 - 集合：{}, 总 topK: {}", collectionNames, topKTotal);
        
        try {
            // 1. 从每个集合搜索
            Map<String, List<VectorRecord>> allResults = multiCollectionSearch(
                queryText, collectionNames, topKTotal);
            
            // 2. 合并所有结果
            List<VectorRecord> merged = new ArrayList<>();
            for (List<VectorRecord> results : allResults.values()) {
                merged.addAll(results);
            }
            
            // 3. 按分数排序
            merged.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
            
            // 4. 去重（按内容）
            List<VectorRecord> deduped = new ArrayList<>();
            Set<String> seenContents = new HashSet<>();
            for (VectorRecord record : merged) {
                if (record.getContent() != null && 
                    !seenContents.contains(record.getContent())) {
                    deduped.add(record);
                    seenContents.add(record.getContent());
                }
            }
            
            // 5. 限制返回数量
            return deduped.subList(0, Math.min(topKTotal, deduped.size()));
            
        } catch (Exception e) {
            log.error("合并搜索结果失败：{}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 语义搜索（纯语义，无关键词匹配）
     */
    public List<VectorRecord> semanticSearch(String collectionName, String queryText, int topK) {
        log.info("语义搜索 - 集合：{}, 文本：{}", collectionName, queryText);
        
        // 语义搜索就是标准的向量搜索
        SearchRequest request = new SearchRequest();
        request.setQueryText(queryText);
        request.setTopK(topK);
        
        return searchVectors(request);
    }
}
