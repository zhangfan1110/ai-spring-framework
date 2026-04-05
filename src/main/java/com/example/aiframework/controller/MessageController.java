package com.example.aiframework.controller;

import com.example.aiframework.entity.ChatMessageEntity;
import com.example.aiframework.entity.ChatMessageFavoriteEntity;
import com.example.aiframework.service.FavoriteService;
import com.example.aiframework.service.MessageSearchService;
import com.example.aiframework.service.MessageSearchService.SearchResult;
import com.example.aiframework.util.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 消息管理接口
 */
@RestController
@RequestMapping("/api/messages")
@Tag(name = "消息管理", description = "消息收藏、搜索等管理功能")
public class MessageController {
    
    private static final Logger log = LoggerFactory.getLogger(MessageController.class);
    
    @Autowired(required = false)
    private FavoriteService favoriteService;
    
    @Autowired(required = false)
    private MessageSearchService searchService;
    
    // ========== 收藏管理 ==========
    
    @Operation(summary = "收藏消息", description = "收藏指定的消息")
    @PostMapping("/{messageId}/favorite")
    public Result<ChatMessageFavoriteEntity> favoriteMessage(
            @PathVariable String messageId,
            @RequestBody(required = false) Map<String, String> params) {
        log.info("收藏消息 - messageId: {}", messageId);
        if (favoriteService == null) {
            return Result.error("收藏服务未启用");
        }
        
        String note = params != null ? params.get("note") : null;
        ChatMessageFavoriteEntity favorite = favoriteService.favoriteMessage(messageId, note);
        return Result.success(favorite);
    }
    
    @Operation(summary = "获取所有收藏", description = "获取所有收藏的消息")
    @GetMapping("/favorites")
    public Result<List<ChatMessageFavoriteEntity>> getAllFavorites() {
        log.info("获取所有收藏");
        if (favoriteService == null) {
            return Result.error("收藏服务未启用");
        }
        return Result.success(favoriteService.getAllFavorites());
    }
    
    @Operation(summary = "取消收藏", description = "取消收藏指定的消息")
    @DeleteMapping("/{messageId}/favorite")
    public Result<Void> unfavoriteMessage(@PathVariable String messageId) {
        log.info("取消收藏 - messageId: {}", messageId);
        if (favoriteService == null) {
            return Result.error("收藏服务未启用");
        }
        favoriteService.unfavoriteMessage(messageId);
        return Result.success();
    }
    
    @Operation(summary = "搜索消息", description = "在指定会话中搜索消息（关键词匹配）")
    @GetMapping("/search")
    public Result<List<ChatMessageEntity>> searchMessages(
            @RequestParam String sessionId,
            @RequestParam String keyword,
            @RequestParam(defaultValue = "20") int limit) {
        log.info("搜索消息 - sessionId: {}, keyword: {}, limit: {}", sessionId, keyword, limit);
        
        if (searchService == null) {
            return Result.error("搜索服务未启用");
        }
        
        List<ChatMessageEntity> results = searchService.searchMessages(sessionId, keyword, limit);
        return Result.success(results);
    }
    
    @Operation(summary = "全局搜索消息", description = "跨会话搜索消息")
    @GetMapping("/search/global")
    public Result<List<ChatMessageEntity>> searchMessagesGlobal(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "50") int limit) {
        log.info("全局搜索消息 - keyword: {}, limit: {}", keyword, limit);
        
        if (searchService == null) {
            return Result.error("搜索服务未启用");
        }
        
        List<ChatMessageEntity> results = searchService.searchMessagesGlobal(keyword, limit);
        return Result.success(results);
    }
    
    @Operation(summary = "高级搜索", description = "支持角色过滤、时间范围、高亮等高级功能")
    @PostMapping("/search/advanced")
    public Result<List<SearchResult>> searchAdvanced(
            @RequestBody Map<String, Object> params) {
        log.info("高级搜索");
        
        if (searchService == null) {
            return Result.error("搜索服务未启用");
        }
        
        try {
            MessageSearchService.SearchParams searchParams = new MessageSearchService.SearchParams();
            
            // 解析参数
            searchParams.setSessionId((String) params.get("sessionId"));
            searchParams.setKeyword((String) params.get("keyword"));
            searchParams.setRole((String) params.get("role"));
            searchParams.setLimit(params.get("limit") != null ? 
                Integer.parseInt(params.get("limit").toString()) : 20);
            searchParams.setHighlight(params.get("highlight") == null || 
                Boolean.parseBoolean(params.get("highlight").toString()));
            searchParams.setGlobalSearch(params.get("globalSearch") != null && 
                Boolean.parseBoolean(params.get("globalSearch").toString()));
            
            // 时间范围
            if (params.get("startTime") != null) {
                searchParams.setStartTime(LocalDateTime.parse(params.get("startTime").toString()));
            }
            if (params.get("endTime") != null) {
                searchParams.setEndTime(LocalDateTime.parse(params.get("endTime").toString()));
            }
            
            List<SearchResult> results = searchService.searchAdvanced(searchParams);
            return Result.success(results);
            
        } catch (Exception e) {
            log.error("高级搜索失败：{}", e.getMessage(), e);
            return Result.error("搜索失败：" + e.getMessage());
        }
    }
    
    @Operation(summary = "搜索建议", description = "获取搜索关键词建议")
    @GetMapping("/search/suggestions")
    public Result<List<String>> getSearchSuggestions(
            @RequestParam String sessionId,
            @RequestParam String prefix,
            @RequestParam(defaultValue = "5") int limit) {
        log.info("获取搜索建议 - sessionId: {}, prefix: {}", sessionId, prefix);
        
        if (searchService == null) {
            return Result.error("搜索服务未启用");
        }
        
        List<String> suggestions = searchService.getSearchSuggestions(sessionId, prefix, limit);
        return Result.success(suggestions);
    }
    
    @Operation(summary = "统计搜索结果", description = "获取匹配的消息数量")
    @GetMapping("/search/count")
    public Result<Map<String, Object>> countSearchResults(
            @RequestParam String sessionId,
            @RequestParam String keyword) {
        log.info("统计搜索结果 - sessionId: {}, keyword: {}", sessionId, keyword);
        
        Map<String, Object> response = new HashMap<>();
        
        if (searchService == null) {
            response.put("count", 0);
            return Result.success(response);
        }
        
        int count = searchService.countSearchResults(sessionId, keyword);
        response.put("count", count);
        response.put("sessionId", sessionId);
        response.put("keyword", keyword);
        
        return Result.success(response);
    }
}
