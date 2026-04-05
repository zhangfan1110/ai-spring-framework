package com.example.aiframework.controller;

import com.example.aiframework.entity.KnowledgeBaseEntity;
import com.example.aiframework.entity.KnowledgeDocumentEntity;
import com.example.aiframework.mapper.KnowledgeDocumentMapper;
import com.example.aiframework.service.KnowledgeBaseService;
import com.example.aiframework.service.KnowledgeSearchService;
import com.example.aiframework.service.KnowledgeSearchService.SearchResult;
import com.example.aiframework.util.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 知识库控制器
 */
@RestController
@RequestMapping("/api/knowledge")
@Tag(name = "知识库管理", description = "知识库和文档管理")
public class KnowledgeController {

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @Autowired
    private KnowledgeSearchService searchService;

    @Autowired
    private KnowledgeDocumentMapper documentMapper;

    private static final String UPLOAD_DIR = System.getProperty("java.io.tmpdir") + "/knowledge-base/";

    @Operation(summary = "创建知识库")
    @PostMapping("/base")
    public Result<KnowledgeBaseEntity> createKnowledgeBase(
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam(defaultValue = "PRIVATE") String type,
            @RequestParam(required = false) String createdBy
    ) {
        KnowledgeBaseEntity kb = knowledgeBaseService.createKnowledgeBase(
            name, description, type, createdBy != null ? createdBy : "system"
        );
        return Result.success(kb);
    }

    @Operation(summary = "更新知识库")
    @PutMapping("/base/{id}")
    public Result<KnowledgeBaseEntity> updateKnowledgeBase(
            @PathVariable String id,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description
    ) {
        KnowledgeBaseEntity kb = knowledgeBaseService.updateKnowledgeBase(id, name, description);
        if (kb == null) {
            return Result.error("知识库不存在");
        }
        return Result.success(kb);
    }

    @Operation(summary = "删除知识库")
    @DeleteMapping("/base/{id}")
    public Result<String> deleteKnowledgeBase(@PathVariable String id) {
        boolean deleted = knowledgeBaseService.deleteKnowledgeBase(id);
        return deleted ? Result.success("删除成功") : Result.error("删除失败");
    }

    @Operation(summary = "获取知识库详情")
    @GetMapping("/base/{id}")
    public Result<KnowledgeBaseEntity> getKnowledgeBase(@PathVariable String id) {
        KnowledgeBaseEntity kb = knowledgeBaseService.getKnowledgeBase(id);
        if (kb == null) {
            return Result.error("知识库不存在");
        }
        return Result.success(kb);
    }

    @Operation(summary = "查询知识库列表")
    @GetMapping("/base/list")
    public Result<List<KnowledgeBaseEntity>> listKnowledgeBases(
            @RequestParam(required = false) String userId
    ) {
        List<KnowledgeBaseEntity> list = knowledgeBaseService.listKnowledgeBases(
            userId != null ? userId : "system"
        );
        return Result.success(list);
    }

    @Operation(summary = "获取知识库统计")
    @GetMapping("/base/{id}/stats")
    public Result<Map<String, Object>> getKnowledgeBaseStats(@PathVariable String id) {
        Map<String, Object> stats = knowledgeBaseService.getKnowledgeBaseStats(id);
        if (stats == null) {
            return Result.error("知识库不存在");
        }
        return Result.success(stats);
    }

    @Operation(summary = "上传文档")
    @PostMapping("/base/{kbId}/document")
    public Result<KnowledgeDocumentEntity> uploadDocument(
            @PathVariable String kbId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String uploadedBy
    ) throws IOException {
        KnowledgeBaseEntity kb = knowledgeBaseService.getKnowledgeBase(kbId);
        if (kb == null) {
            return Result.error("知识库不存在");
        }

        // 保存文件
        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        Path filePath = Paths.get(UPLOAD_DIR + fileName);
        Files.createDirectories(filePath.getParent());
        file.transferTo(filePath);

        // 创建文档记录
        KnowledgeDocumentEntity doc = new KnowledgeDocumentEntity();
        doc.setId(UUID.randomUUID().toString());
        doc.setKnowledgeBaseId(kbId);
        doc.setTitle(title != null ? title : file.getOriginalFilename());
        doc.setDocType(getFileType(file.getOriginalFilename()));
        doc.setFilePath(filePath.toString());
        doc.setFileSize(file.getSize());
        doc.setVectorStatus("PENDING");
        doc.setUploadedBy(uploadedBy != null ? uploadedBy : "system");

        documentMapper.insert(doc);

        // TODO: 异步处理文档向量化

        return Result.success(doc);
    }

    @Operation(summary = "查询文档列表")
    @GetMapping("/base/{kbId}/documents")
    public Result<List<KnowledgeDocumentEntity>> listDocuments(@PathVariable String kbId) {
        List<KnowledgeDocumentEntity> list = documentMapper.findByKnowledgeBase(kbId);
        return Result.success(list);
    }

    @Operation(summary = "搜索知识")
    @GetMapping("/base/{kbId}/search")
    public Result<List<SearchResult>> searchKnowledge(
            @PathVariable String kbId,
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int topK
    ) {
        List<SearchResult> results = searchService.semanticSearch(kbId, query, topK);
        return Result.success(results);
    }

    @Operation(summary = "删除文档")
    @DeleteMapping("/document/{id}")
    public Result<String> deleteDocument(@PathVariable String id) {
        documentMapper.deleteById(id);
        // TODO: 删除对应的向量和分块
        return Result.success("删除成功");
    }

    private String getFileType(String fileName) {
        if (fileName == null) return "UNKNOWN";
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".pdf")) return "PDF";
        if (lower.endsWith(".doc") || lower.endsWith(".docx")) return "WORD";
        if (lower.endsWith(".md")) return "MD";
        if (lower.endsWith(".txt")) return "TEXT";
        return "UNKNOWN";
    }
}
