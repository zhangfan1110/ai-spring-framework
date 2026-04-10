package com.example.aiframework.controller;

import com.example.aiframework.chat.dto.ChatSessionExportDTO;
import com.example.aiframework.chat.dto.ChatSessionMergeRequestDTO;
import com.example.aiframework.chat.dto.ChatSessionStatsDTO;
import com.example.aiframework.chat.entity.ChatMessageEntity;
import com.example.aiframework.chat.entity.ChatSessionEntity;
import com.example.aiframework.chat.entity.ChatSessionShareEntity;
import com.example.aiframework.chat.entity.ChatSessionTagEntity;
import com.example.aiframework.chat.entity.ChatSessionTemplateEntity;
import com.example.aiframework.chat.service.*;
import com.example.aiframework.chat.service.SessionShareService.ShareConfig;
import com.example.aiframework.common.util.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

/**
 * 会话管理接口
 */
@RestController
@RequestMapping("/api/sessions")
@Tag(name = "会话管理", description = "会话导出、导入、合并、标签、分享、模板等管理功能")
public class SessionController {
    
    private static final Logger log = LoggerFactory.getLogger(SessionController.class);
    
    @Autowired(required = false)
    private ExportImportService exportImportService;
    
    @Autowired(required = false)
    private SessionMergeService sessionMergeService;
    
    @Autowired(required = false)
    private SessionTagService sessionTagService;
    
    @Autowired(required = false)
    private CleanupService cleanupService;
    
    @Autowired(required = false)
    private RedisSessionCacheService redisSessionCacheService;
    
    @Autowired(required = false)
    private SessionShareService sessionShareService;
    
    @Autowired(required = false)
    private SessionTemplateService sessionTemplateService;
    
    // ========== 会话分享 ==========
    
    @Operation(summary = "创建会话分享", description = "生成会话的公开分享链接")
    @PostMapping("/{sessionId}/share")
    public Result<ChatSessionShareEntity> createShare(
            @PathVariable String sessionId,
            @RequestBody Map<String, Object> params,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        log.info("创建会话分享 - sessionId: {}", sessionId);
        
        if (sessionShareService == null) {
            return Result.error("分享服务未启用");
        }
        
        ShareConfig config = new ShareConfig();
        config.title(params.get("shareTitle") != null ? params.get("shareTitle").toString() : null);
        config.description(params.get("shareDescription") != null ? params.get("shareDescription").toString() : null);
        
        if (params.containsKey("isPublic")) {
            config.publicShare(Boolean.parseBoolean(params.get("isPublic").toString()));
        }
        
        if (params.containsKey("accessPassword")) {
            config.password(params.get("accessPassword").toString());
        }
        
        if (params.containsKey("expireDays")) {
            config.expireDays(Integer.parseInt(params.get("expireDays").toString()));
        }
        
        if (params.containsKey("maxViews")) {
            config.maxViews(Integer.parseInt(params.get("maxViews").toString()));
        }
        
        String creatorId = userId != null ? userId : "anonymous";
        ChatSessionShareEntity share = sessionShareService.createShare(sessionId, creatorId, config);
        
        return Result.success(share);
    }
    
    @Operation(summary = "访问分享链接", description = "通过分享令牌访问会话")
    @GetMapping("/share/{shareToken}")
    public Result<Map<String, Object>> accessShare(
            @PathVariable String shareToken,
            @RequestParam(value = "password", required = false) String password) {
        log.info("访问分享链接 - shareToken: {}", shareToken);
        
        if (sessionShareService == null) {
            return Result.error("分享服务未启用");
        }
        
        ChatSessionShareEntity share = sessionShareService.getShareByToken(shareToken);
        if (share == null) {
            return Result.error("分享链接不存在或已失效");
        }
        
        // 验证密码
        if (!sessionShareService.verifyPassword(share, password)) {
            return Result.error("访问密码错误");
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("share", share);
        result.put("sessionId", share.getSessionId());
        
        return Result.success(result);
    }
    
    @Operation(summary = "获取会话的分享列表", description = "获取指定会话的所有分享链接")
    @GetMapping("/{sessionId}/shares")
    public Result<List<ChatSessionShareEntity>> getSessionShares(@PathVariable String sessionId) {
        log.info("获取会话分享列表 - sessionId: {}", sessionId);
        
        if (sessionShareService == null) {
            return Result.error("分享服务未启用");
        }
        
        return Result.success(sessionShareService.getSessionShares(sessionId));
    }
    
    @Operation(summary = "删除分享", description = "删除指定的分享链接")
    @DeleteMapping("/share/{shareId}")
    public Result<Void> deleteShare(@PathVariable String shareId) {
        log.info("删除分享 - shareId: {}", shareId);
        
        if (sessionShareService == null) {
            return Result.error("分享服务未启用");
        }
        
        sessionShareService.deleteShare(shareId);
        return Result.success();
    }
    
    @Operation(summary = "禁用/启用分享", description = "禁用或启用分享链接")
    @PatchMapping("/share/{shareId}/disable")
    public Result<Void> disableShare(
            @PathVariable String shareId,
            @RequestParam boolean disable) {
        log.info("{}分享 - shareId: {}", disable ? "禁用" : "启用", shareId);
        
        if (sessionShareService == null) {
            return Result.error("分享服务未启用");
        }
        
        sessionShareService.disableShare(shareId);
        return Result.success();
    }
    
    @Operation(summary = "公开分享广场", description = "获取所有公开分享的会话")
    @GetMapping("/shares/public")
    public Result<List<ChatSessionShareEntity>> getPublicShares(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        log.info("获取公开分享广场 - limit: {}, offset: {}", limit, offset);
        
        if (sessionShareService == null) {
            return Result.error("分享服务未启用");
        }
        
        return Result.success(sessionShareService.getPublicShares(limit, offset));
    }
    
    // ========== 会话模板 ==========
    
    @Operation(summary = "获取所有模板", description = "获取所有可用的会话模板")
    @GetMapping("/templates")
    public Result<List<ChatSessionTemplateEntity>> getAllTemplates() {
        log.info("获取所有模板");
        
        if (sessionTemplateService == null) {
            return Result.error("模板服务未启用");
        }
        
        return Result.success(sessionTemplateService.getAllTemplates());
    }
    
    @Operation(summary = "按分类获取模板", description = "获取指定分类的模板")
    @GetMapping("/templates/category/{category}")
    public Result<List<ChatSessionTemplateEntity>> getTemplatesByCategory(
            @PathVariable String category) {
        log.info("获取分类模板 - category: {}", category);
        
        if (sessionTemplateService == null) {
            return Result.error("模板服务未启用");
        }
        
        return Result.success(sessionTemplateService.getTemplatesByCategory(category));
    }
    
    @Operation(summary = "搜索模板", description = "根据关键词搜索模板")
    @GetMapping("/templates/search")
    public Result<List<ChatSessionTemplateEntity>> searchTemplates(
            @RequestParam String keyword) {
        log.info("搜索模板 - keyword: {}", keyword);
        
        if (sessionTemplateService == null) {
            return Result.error("模板服务未启用");
        }
        
        return Result.success(sessionTemplateService.searchTemplates(keyword));
    }
    
    @Operation(summary = "使用模板创建会话", description = "基于模板创建新会话")
    @PostMapping("/templates/{templateId}/use")
    public Result<ChatSessionEntity> useTemplate(
            @PathVariable String templateId,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        log.info("使用模板创建会话 - templateId: {}", templateId);
        
        if (sessionTemplateService == null) {
            return Result.error("模板服务未启用");
        }
        
        String creatorId = userId != null ? userId : "anonymous";
        ChatSessionEntity session = sessionTemplateService.createSessionFromTemplate(templateId, creatorId);
        
        if (session == null) {
            return Result.error("模板不存在或已禁用");
        }
        
        return Result.success(session);
    }
    
    @Operation(summary = "创建自定义模板", description = "将当前会话保存为模板")
    @PostMapping("/{sessionId}/template")
    public Result<ChatSessionTemplateEntity> createTemplate(
            @PathVariable String sessionId,
            @RequestBody Map<String, String> params,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        log.info("创建自定义模板 - sessionId: {}", sessionId);
        
        if (sessionTemplateService == null) {
            return Result.error("模板服务未启用");
        }
        
        String name = params.get("name");
        String description = params.get("description");
        String category = params.get("category");
        
        if (name == null || name.isEmpty()) {
            return Result.error("模板名称不能为空");
        }
        
        String creatorId = userId != null ? userId : "anonymous";
        ChatSessionTemplateEntity template = sessionTemplateService.createTemplate(
            name, description, category, null, creatorId);
        
        return Result.success(template);
    }
    
    @Operation(summary = "删除模板", description = "删除自定义模板")
    @DeleteMapping("/templates/{templateId}")
    public Result<Void> deleteTemplate(
            @PathVariable String templateId,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        log.info("删除模板 - templateId: {}", templateId);
        
        if (sessionTemplateService == null) {
            return Result.error("模板服务未启用");
        }
        
        String creatorId = userId != null ? userId : "anonymous";
        sessionTemplateService.deleteTemplate(templateId, creatorId);
        
        return Result.success();
    }
    
    // ========== 原有功能 (导出/导入/标签等) ==========
    
    @Operation(summary = "导出会话", description = "导出会话为 JSON 格式")
    @GetMapping("/{sessionId}/export")
    public Result<ChatSessionExportDTO> exportSession(@PathVariable String sessionId) {
        log.info("导出会话：{}", sessionId);
        if (exportImportService == null) {
            return Result.error("导出服务未启用");
        }
        ChatSessionExportDTO exportDTO = exportImportService.exportSession(sessionId);
        return Result.success(exportDTO);
    }
    
    @Operation(summary = "导入会话", description = "从 JSON 文件导入会话")
    @PostMapping("/import")
    public Result<ChatSessionEntity> importSession(@RequestParam("file") MultipartFile file) {
        log.info("导入会话文件：{}", file.getOriginalFilename());
        if (exportImportService == null) {
            return Result.error("导入服务未启用");
        }
        try {
            ChatSessionEntity session = exportImportService.importSessionFromFile(file);
            return Result.success(session);
        } catch (Exception e) {
            return Result.error("导入失败：" + e.getMessage());
        }
    }
    
    @Operation(summary = "批量删除会话", description = "批量删除多个会话")
    @DeleteMapping("/batch")
    public Result<Map<String, Object>> deleteSessionsBatch(@RequestBody List<String> sessionIds) {
        log.info("批量删除会话：{} 个", sessionIds.size());
        
        Map<String, Object> result = new HashMap<>();
        int successCount = 0;
        int failCount = 0;
        List<String> failedIds = new ArrayList<>();
        
        for (String sessionId : sessionIds) {
            try {
                if (cleanupService != null) {
                    cleanupService.cleanupSession(sessionId);
                }
                successCount++;
            } catch (Exception e) {
                log.error("删除会话失败：{} - {}", sessionId, e.getMessage());
                failCount++;
                failedIds.add(sessionId);
            }
        }
        
        result.put("successCount", successCount);
        result.put("failCount", failCount);
        result.put("failedIds", failedIds);
        
        return Result.success(result);
    }
    
    @Operation(summary = "合并会话", description = "合并多个会话到一个目标会话")
    @PostMapping("/merge")
    public Result<ChatSessionEntity> mergeSessions(@RequestBody ChatSessionMergeRequestDTO request) {
        log.info("合并会话 - 目标：{}, 源：{}", request.getTargetSessionId(), request.getSourceSessionIds());
        if (sessionMergeService == null) {
            return Result.error("合并服务未启用");
        }
        try {
            ChatSessionEntity session = sessionMergeService.mergeSessions(request);
            return Result.success(session);
        } catch (Exception e) {
            return Result.error("合并失败：" + e.getMessage());
        }
    }
    
    @Operation(summary = "添加会话标签", description = "为会话添加标签")
    @PostMapping("/{sessionId}/tag")
    public Result<ChatSessionTagEntity> addTag(
            @PathVariable String sessionId,
            @RequestBody Map<String, String> params) {
        log.info("添加会话标签 - sessionId: {}", sessionId);
        if (sessionTagService == null) {
            return Result.error("标签服务未启用");
        }
        
        String tagName = params.get("tagName");
        String tagColor = params.get("tagColor");
        
        if (tagName == null || tagName.isEmpty()) {
            return Result.error("标签名不能为空");
        }
        
        ChatSessionTagEntity tag = sessionTagService.addTag(sessionId, tagName, tagColor);
        return Result.success(tag);
    }
    
    @Operation(summary = "获取会话标签", description = "获取会话的所有标签")
    @GetMapping("/{sessionId}/tags")
    public Result<List<ChatSessionTagEntity>> getSessionTags(@PathVariable String sessionId) {
        log.info("获取会话标签 - sessionId: {}", sessionId);
        if (sessionTagService == null) {
            return Result.error("标签服务未启用");
        }
        return Result.success(sessionTagService.getSessionTags(sessionId));
    }
    
    @Operation(summary = "删除会话标签", description = "删除会话的指定标签")
    @DeleteMapping("/{sessionId}/tag/{tagName}")
    public Result<Void> removeTag(
            @PathVariable String sessionId,
            @PathVariable String tagName) {
        log.info("删除会话标签 - sessionId: {}, tagName: {}", sessionId, tagName);
        if (sessionTagService == null) {
            return Result.error("标签服务未启用");
        }
        sessionTagService.removeTag(sessionId, tagName);
        return Result.success();
    }
    
    @Operation(summary = "获取所有标签", description = "获取系统中的所有标签")
    @GetMapping("/tags")
    public Result<List<String>> getAllTags() {
        log.info("获取所有标签");
        if (sessionTagService == null) {
            return Result.error("标签服务未启用");
        }
        return Result.success(sessionTagService.getAllTags());
    }
    
    @Operation(summary = "清空 Redis 缓存", description = "清空所有 Redis 会话缓存")
    @DeleteMapping("/cache/clear")
    public Result<Void> clearCache() {
        log.info("清空 Redis 缓存");
        if (redisSessionCacheService == null) {
            return Result.error("Redis 缓存服务未启用");
        }
        redisSessionCacheService.clearAll();
        return Result.success();
    }
    
    @Operation(summary = "获取缓存状态", description = "获取 Redis 缓存中的会话数量")
    @GetMapping("/cache/stats")
    public Result<Map<String, Object>> getCacheStats() {
        log.info("获取缓存状态");
        if (redisSessionCacheService == null) {
            return Result.error("Redis 缓存服务未启用");
        }
        
        Map<String, Object> stats = new HashMap<>();
        try {
            Set<String> keys = redisSessionCacheService.getAllSessionKeys();
            int sessionCount = keys != null ? keys.size() : 0;
            
            stats.put("sessionCount", sessionCount);
            stats.put("enabled", true);
            stats.put("prefix", "chat:session:");
            
            return Result.success(stats);
        } catch (Exception e) {
            stats.put("enabled", false);
            stats.put("error", e.getMessage());
            return Result.success(stats);
        }
    }
    
    @Operation(summary = "删除会话缓存", description = "删除指定会话的 Redis 缓存")
    @DeleteMapping("/{sessionId}/cache")
    public Result<Void> deleteSessionCache(@PathVariable String sessionId) {
        log.info("删除会话缓存：{}", sessionId);
        if (redisSessionCacheService == null) {
            return Result.error("Redis 缓存服务未启用");
        }
        redisSessionCacheService.deleteSession(sessionId);
        return Result.success();
    }
}
