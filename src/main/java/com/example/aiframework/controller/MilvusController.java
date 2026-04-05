package com.example.aiframework.controller;

import com.example.aiframework.model.CollectionRequest;
import com.example.aiframework.model.SearchRequest;
import com.example.aiframework.model.VectorRecord;
import com.example.aiframework.service.MilvusService;
import com.example.aiframework.service.PdfParserService;
import com.example.aiframework.util.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Milvus 向量数据库接口
 */
@RestController
@RequestMapping("/api/milvus")
@Tag(name = "Milvus 向量数据库", description = "向量集合管理、向量搜索等操作")
public class MilvusController {
    
    private static final Logger log = LoggerFactory.getLogger(MilvusController.class);
    
    @Autowired
    private MilvusService milvusService;
    
    @Autowired
    private PdfParserService pdfParser;


    @Operation(summary = "创建集合", description = "创建新的向量集合")
    @PostMapping("/collection")
    public Result<Boolean> createCollection(@RequestBody CollectionRequest request) {
        log.info("创建集合：{}", request.getCollectionName());
        boolean success = milvusService.createCollection(request);
        return Result.success(success);
    }
    
    @Operation(summary = "删除集合", description = "删除指定的向量集合")
    @DeleteMapping("/collection/{collectionName}")
    public Result<Boolean> deleteCollection(@PathVariable String collectionName) {
        log.info("删除集合：{}", collectionName);
        boolean success = milvusService.deleteCollection(collectionName);
        return Result.success(success);
    }
    
    @Operation(summary = "获取集合列表", description = "获取所有向量集合")
    @GetMapping("/collections")
    public Result<List<String>> listCollections() {
        List<String> collections = milvusService.listCollections();
        return Result.success(collections);
    }
    
    @Operation(summary = "检查集合", description = "检查集合是否存在")
    @GetMapping("/collection/{collectionName}/exists")
    public Result<Boolean> hasCollection(@PathVariable String collectionName) {
        boolean exists = milvusService.hasCollection(collectionName);
        return Result.success(exists);
    }
    
    @Operation(summary = "集合统计", description = "获取集合统计信息")
    @GetMapping("/collection/{collectionName}/stats")
    public Result<Map<String, Object>> getCollectionStats(@PathVariable String collectionName) {
        Map<String, Object> stats = milvusService.getCollectionStats(collectionName);
        return Result.success(stats);
    }
    
    @Operation(summary = "向量搜索", description = "基于文本的向量相似度搜索")
    @PostMapping("/search")
    public Result<List<VectorRecord>> search(@RequestBody SearchRequest request) {
        log.info("向量搜索：{}, topK: {}", request.getQueryText(), request.getTopK());
        List<VectorRecord> results = milvusService.searchVectors(request);
        return Result.success(results);
    }
    
    @Operation(summary = "添加文档", description = "添加文档到向量库（自动向量化）")
    @PostMapping("/document")
    public Result<String> addDocument(@RequestParam String content) {
        log.info("添加文档到向量库，长度：{}", content.length());
        String id = milvusService.addDocument(content);
        return id != null ? Result.success(id) : Result.error("添加失败");
    }
    
    @Operation(summary = "搜索文档", description = "搜索相关文档")
    @GetMapping("/documents/search")
    public Result<List<String>> searchDocuments(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int maxResults) {
        log.info("搜索文档：{}, maxResults: {}", query, maxResults);
        List<String> results = milvusService.searchDocuments(query, maxResults);
        return Result.success(results);
    }
    
    @Operation(summary = "健康检查", description = "检查 Milvus 连接状态")
    @GetMapping("/health")
    public Result<String> health() {
        List<String> collections = milvusService.listCollections();
        return collections != null ? 
            Result.success("Milvus connected! Collections: " + collections.size()) :
            Result.error("Milvus not connected");
    }
    
    // ========== 集合管理增强 ==========
    
    @Operation(summary = "重命名集合", description = "重命名向量集合")
    @PostMapping("/collection/{oldName}/rename")
    public Result<Map<String, Object>> renameCollection(
            @PathVariable String oldName,
            @RequestParam String newName) {
        log.info("重命名集合：{} -> {}", oldName, newName);
        
        Map<String, Object> response = new HashMap<>();
        boolean success = milvusService.renameCollection(oldName, newName);
        
        response.put("oldName", oldName);
        response.put("newName", newName);
        response.put("success", success);
        
        return success ? Result.success(response) : Result.error("重命名失败", response);
    }
    
    @Operation(summary = "克隆集合", description = "克隆向量集合（复制结构和数据）")
    @PostMapping("/collection/{sourceName}/clone")
    public Result<Map<String, Object>> cloneCollection(
            @PathVariable String sourceName,
            @RequestParam String targetName) {
        log.info("克隆集合：{} -> {}", sourceName, targetName);
        
        Map<String, Object> response = new HashMap<>();
        boolean success = milvusService.cloneCollection(sourceName, targetName);
        
        response.put("sourceName", sourceName);
        response.put("targetName", targetName);
        response.put("success", success);
        
        return success ? Result.success(response) : Result.error("克隆失败", response);
    }
    
    @Operation(summary = "创建索引", description = "为集合创建索引以优化查询性能")
    @PostMapping("/collection/{collectionName}/index")
    public Result<Map<String, Object>> createIndex(
            @PathVariable String collectionName,
            @RequestBody(required = false) Map<String, Object> indexParams) {
        log.info("为集合创建索引：{}", collectionName);
        
        Map<String, Object> response = new HashMap<>();
        boolean success = milvusService.createIndex(collectionName, 
            indexParams != null ? indexParams : new HashMap<>());
        
        response.put("collectionName", collectionName);
        response.put("success", success);
        
        return success ? Result.success(response) : Result.error("创建索引失败", response);
    }
    
    @Operation(summary = "重建索引", description = "删除旧索引并创建新索引")
    @PostMapping("/collection/{collectionName}/index/rebuild")
    public Result<Map<String, Object>> rebuildIndex(@PathVariable String collectionName) {
        log.info("重建集合索引：{}", collectionName);
        
        Map<String, Object> response = new HashMap<>();
        boolean success = milvusService.rebuildIndex(collectionName);
        
        response.put("collectionName", collectionName);
        response.put("success", success);
        
        return success ? Result.success(response) : Result.error("重建索引失败", response);
    }
    
    @Operation(summary = "加载集合", description = "加载集合到内存以提升查询性能")
    @PostMapping("/collection/{collectionName}/load")
    public Result<Map<String, Object>> loadCollection(@PathVariable String collectionName) {
        log.info("加载集合到内存：{}", collectionName);
        
        Map<String, Object> response = new HashMap<>();
        boolean success = milvusService.loadCollection(collectionName);
        
        response.put("collectionName", collectionName);
        response.put("success", success);
        
        return success ? Result.success(response) : Result.error("加载失败", response);
    }
    
    @Operation(summary = "释放集合", description = "释放集合内存以节省资源")
    @PostMapping("/collection/{collectionName}/release")
    public Result<Map<String, Object>> releaseCollection(@PathVariable String collectionName) {
        log.info("释放集合内存：{}", collectionName);
        
        Map<String, Object> response = new HashMap<>();
        boolean success = milvusService.releaseCollection(collectionName);
        
        response.put("collectionName", collectionName);
        response.put("success", success);
        
        return success ? Result.success(response) : Result.error("释放失败", response);
    }
    
    // ========== 高级搜索 ==========
    
    @Operation(summary = "混合搜索", description = "向量相似度 + 关键词过滤的混合搜索")
    @PostMapping("/search/hybrid")
    public Result<List<VectorRecord>> hybridSearch(@RequestBody Map<String, Object> request) {
        String collectionName = (String) request.getOrDefault("collection", "default_collection");
        String queryText = (String) request.get("queryText");
        String keywords = (String) request.get("keywords");
        int topK = ((Number) request.getOrDefault("topK", 5)).intValue();
        
        log.info("混合搜索 - 集合：{}, 文本：{}, 关键词：{}, topK: {}", 
            collectionName, queryText, keywords, topK);
        
        List<VectorRecord> results = milvusService.hybridSearch(
            collectionName, queryText, keywords, topK);
        
        return Result.success(results);
    }
    
    @Operation(summary = "过滤搜索", description = "带元数据过滤条件的向量搜索")
    @PostMapping("/search/filtered")
    public Result<List<VectorRecord>> filteredSearch(@RequestBody Map<String, Object> request) {
        String collectionName = (String) request.getOrDefault("collection", "default_collection");
        String queryText = (String) request.get("queryText");
        int topK = ((Number) request.getOrDefault("topK", 5)).intValue();
        @SuppressWarnings("unchecked")
        Map<String, Object> filters = (Map<String, Object>) request.get("filters");
        
        log.info("过滤搜索 - 集合：{}, 文本：{}, 过滤：{}", collectionName, queryText, filters);
        
        List<VectorRecord> results = milvusService.filteredSearch(
            collectionName, queryText, topK, filters);
        
        return Result.success(results);
    }
    
    @Operation(summary = "多集合搜索", description = "在多个集合中同时搜索，返回每个集合的结果")
    @PostMapping("/search/multi-collection")
    public Result<Map<String, List<VectorRecord>>> multiCollectionSearch(
            @RequestBody Map<String, Object> request) {
        String queryText = (String) request.get("queryText");
        @SuppressWarnings("unchecked")
        List<String> collections = (List<String>) request.get("collections");
        int topKPerCollection = ((Number) request.getOrDefault("topKPerCollection", 5)).intValue();
        
        log.info("多集合搜索 - 集合：{}, 文本：{}, topK: {}", collections, queryText, topKPerCollection);
        
        Map<String, List<VectorRecord>> results = milvusService.multiCollectionSearch(
            queryText, collections, topKPerCollection);
        
        return Result.success(results);
    }
    
    @Operation(summary = "合并多集合搜索", description = "在多个集合中搜索并合并结果，去重后返回")
    @PostMapping("/search/merged")
    public Result<List<VectorRecord>> mergedSearch(@RequestBody Map<String, Object> request) {
        String queryText = (String) request.get("queryText");
        @SuppressWarnings("unchecked")
        List<String> collections = (List<String>) request.get("collections");
        int topKTotal = ((Number) request.getOrDefault("topKTotal", 10)).intValue();
        
        log.info("合并多集合搜索 - 集合：{}, 文本：{}, 总 topK: {}", collections, queryText, topKTotal);
        
        List<VectorRecord> results = milvusService.mergedMultiCollectionSearch(
            queryText, collections, topKTotal);
        
        return Result.success(results);
    }
    
    @Operation(summary = "语义搜索", description = "纯语义向量搜索（无关键词匹配）")
    @PostMapping("/search/semantic")
    public Result<List<VectorRecord>> semanticSearch(@RequestBody Map<String, Object> request) {
        String collectionName = (String) request.getOrDefault("collection", "default_collection");
        String queryText = (String) request.get("queryText");
        int topK = ((Number) request.getOrDefault("topK", 5)).intValue();
        
        log.info("语义搜索 - 集合：{}, 文本：{}", collectionName, queryText);
        
        List<VectorRecord> results = milvusService.semanticSearch(
            collectionName, queryText, topK);
        
        return Result.success(results);
    }
    
    @Operation(summary = "导入文档", description = "上传文档文件（TXT/MD/PDF/DOCX），自动分词后生成向量并存储到向量库")
    @PostMapping("/import")
    public Result<Map<String, Object>> importDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "collection", defaultValue = "default_collection") String collection,
            @RequestParam(value = "chunkSize", defaultValue = "500") int chunkSize,
            @RequestParam(value = "overlap", defaultValue = "50") int overlap,
            @RequestParam(value = "enableSplitting", defaultValue = "true") boolean enableSplitting) {
        log.info("导入文档：{}, collection={}, chunkSize={}, overlap={}", 
            file.getOriginalFilename(), collection, chunkSize, overlap);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 验证文件
            String filename = file.getOriginalFilename();
            if (filename == null) {
                return Result.error("文件名不能为空");
            }
            
            String lowerFilename = filename.toLowerCase();
            boolean isPdf = lowerFilename.endsWith(".pdf");
            boolean isWord = lowerFilename.endsWith(".doc") || lowerFilename.endsWith(".docx");
            boolean isText = lowerFilename.endsWith(".txt") || lowerFilename.endsWith(".md");
            
            if (!isPdf && !isWord && !isText) {
                return Result.error("仅支持 TXT、MD、PDF、DOCX 格式文件");
            }
            
            // 读取文件内容
            String content;
            if (isPdf) {
                // 使用 PDFBox 解析 PDF
                content = pdfParser.parsePdf(file);
                response.put("fileType", "PDF");
            } else if (isWord) {
                // Word 文档解析
                content = readWordContent(file);
                response.put("fileType", "Word");
            } else {
                // 纯文本
                content = readTextContent(file);
                response.put("fileType", "Text");
            }
            
            if (content == null || content.trim().isEmpty()) {
                return Result.error("文件内容为空");
            }
            
            // 添加到向量库
            long startTime = System.currentTimeMillis();
            String docId = milvusService.addDocumentWithSplitting(content, chunkSize, overlap, enableSplitting);
            long duration = System.currentTimeMillis() - startTime;
            
            if (docId == null) {
                return Result.error("文档导入失败，请查看日志");
            }
            
            // 如果是 PDF，添加元数据
            if (isPdf) {
                try {
                    PdfParserService.PdfMetadata metadata = pdfParser.getMetadata(file);
                    response.put("pdfPageCount", metadata.getPageCount());
                    response.put("pdfTitle", metadata.getTitle());
                    response.put("pdfAuthor", metadata.getAuthor());
                } catch (Exception e) {
                    log.warn("获取 PDF 元数据失败：{}", e.getMessage());
                }
            }
            
            response.put("documentId", docId);
            response.put("filename", filename);
            response.put("collection", collection);
            response.put("charCount", content.length());
            response.put("chunkSize", chunkSize);
            response.put("overlap", overlap);
            response.put("enableSplitting", enableSplitting);
            response.put("duration", duration);
            response.put("message", "文档已成功导入并生成向量");
            
            return Result.success(response);
            
        } catch (Exception e) {
            log.error("导入文档失败：{}", e.getMessage(), e);
            response.put("error", e.getMessage());
            return Result.error("导入失败：" + e.getMessage());
        }
    }
    
    @Operation(summary = "获取 PDF 元数据", description = "解析 PDF 文件并返回元数据信息（页数、作者、标题等）")
    @PostMapping("/pdf/metadata")
    public Result<Map<String, Object>> getPdfMetadata(
            @RequestParam("file") MultipartFile file) {
        log.info("获取 PDF 元数据：{}", file.getOriginalFilename());
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            String filename = file.getOriginalFilename();
            if (filename == null || !filename.toLowerCase().endsWith(".pdf")) {
                return Result.error("仅支持 PDF 格式文件");
            }
            
            PdfParserService.PdfMetadata metadata = pdfParser.getMetadata(file);
            
            response.put("filename", filename);
            response.put("pageCount", metadata.getPageCount());
            response.put("title", metadata.getTitle());
            response.put("author", metadata.getAuthor());
            response.put("subject", metadata.getSubject());
            response.put("creator", metadata.getCreator());
            response.put("producer", metadata.getProducer());
            response.put("encrypted", metadata.isEncrypted());
            
            return Result.success(response);
            
        } catch (Exception e) {
            log.error("获取 PDF 元数据失败：{}", e.getMessage(), e);
            return Result.error("获取元数据失败：" + e.getMessage());
        }
    }
    
    @Operation(summary = "批量导入文档", description = "上传多个文档文件，批量导入到向量库")
    @PostMapping("/import/batch")
    public Result<Map<String, Object>> importBatchDocuments(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "collection", defaultValue = "default_collection") String collection,
            @RequestParam(value = "chunkSize", defaultValue = "500") int chunkSize,
            @RequestParam(value = "overlap", defaultValue = "50") int overlap) {
        log.info("批量导入文档：{} 个文件", files.size());
        
        Map<String, Object> response = new HashMap<>();
        List<String> successFiles = new ArrayList<>();
        List<String> failedFiles = new ArrayList<>();
        
        long startTime = System.currentTimeMillis();
        
        for (MultipartFile file : files) {
            try {
                String filename = file.getOriginalFilename();
                if (filename == null) continue;
                
                String lowerFilename = filename.toLowerCase();
                boolean isPdf = lowerFilename.endsWith(".pdf");
                boolean isWord = lowerFilename.endsWith(".doc") || lowerFilename.endsWith(".docx");
                boolean isText = lowerFilename.endsWith(".txt") || lowerFilename.endsWith(".md");
                
                if (!isPdf && !isWord && !isText) {
                    failedFiles.add(filename + " (不支持的格式)");
                    continue;
                }
                
                String content;
                if (isPdf) {
                    content = pdfParser.parsePdf(file);
                } else if (isWord) {
                    content = readWordContent(file);
                } else {
                    content = readTextContent(file);
                }
                
                if (content == null || content.trim().isEmpty()) {
                    failedFiles.add(filename + " (文件内容为空)");
                    continue;
                }
                
                String docId = milvusService.addDocumentWithSplitting(content, chunkSize, overlap, true);
                if (docId != null) {
                    successFiles.add(filename);
                } else {
                    failedFiles.add(filename);
                }
                
            } catch (Exception e) {
                log.error("导入文件失败：{}", file.getOriginalFilename(), e);
                failedFiles.add(file.getOriginalFilename() + ": " + e.getMessage());
            }
        }
        
        long duration = System.currentTimeMillis() - startTime;
        
        response.put("totalFiles", files.size());
        response.put("successCount", successFiles.size());
        response.put("failedCount", failedFiles.size());
        response.put("successFiles", successFiles);
        response.put("failedFiles", failedFiles);
        response.put("duration", duration);
        response.put("collection", collection);
        
        if (failedFiles.isEmpty()) {
            return Result.success(response);
        } else {
            response.put("partialFailure", true);
            return Result.error("部分文件导入失败", response);
        }
    }
    
    /**
     * 读取文本文件内容
     */
    private String readTextContent(MultipartFile file) throws Exception {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            return content.toString();
        }
    }
    
    /**
     * 读取 Word 文件内容（DOCX）
     * 支持解析段落和表格
     */
    private String readWordContent(MultipartFile file) throws IOException {
        try (InputStream inputStream = file.getInputStream();
             XWPFDocument document = new XWPFDocument(inputStream)) {
            
            StringBuilder content = new StringBuilder();
            
            // 读取段落
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                String text = paragraph.getText();
                if (!text.isEmpty()) {
                    content.append(text).append("\n");
                }
            }
            
            // 读取表格内容
            for (XWPFTable table : document.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    List<XWPFTableCell> cells = row.getTableCells();
                    StringBuilder rowText = new StringBuilder();
                    for (XWPFTableCell cell : cells) {
                        rowText.append(cell.getText()).append("\t");
                    }
                    content.append(rowText.toString().trim()).append("\n");
                }
            }
            
            log.info("Word 文档解析完成，提取到 {} 个字符", content.length());
            return content.toString().trim();
        }
    }
}
