package com.example.aiframework.controller;

import com.example.aiframework.entity.ChatMessageEntity;
import com.example.aiframework.entity.ChatSessionEntity;
import com.example.aiframework.entity.ChatSessionSummaryEntity;
import com.example.aiframework.model.ChatRequest;
import com.example.aiframework.model.ChatResponse;
import com.example.aiframework.model.MessageEditRequest;
import com.example.aiframework.service.ChatService;
import com.example.aiframework.service.RagMemoryService;
import com.example.aiframework.service.SessionSummaryService;
import com.example.aiframework.util.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;

/**
 * AI 聊天接口
 */
@RestController
@RequestMapping("/api/ai")
@Tag(name = "AI 聊天", description = "与 AI 进行对话，支持会话记忆和 RAG")
public class AiController {
    
    private static final Logger log = LoggerFactory.getLogger(AiController.class);
    
    @Autowired
    private ChatService chatService;
    
    @Autowired(required = false)
    private SessionSummaryService sessionSummaryService;
    
    @Autowired(required = false)
    private RagMemoryService ragMemoryService;
    
    @Operation(summary = "聊天", description = "与 AI 进行对话，支持会话记忆")
    @PostMapping("/chat")
    public Result<ChatResponse> chat(@RequestBody ChatRequest request) {
        log.info("收到聊天请求：{}", request.getMessage());
        ChatResponse response = chatService.chat(request);
        return Result.success(response);
    }
    
    @Operation(summary = "清空会话", description = "清空指定会话的历史记录")
    @DeleteMapping("/session/{sessionId}")
    public Result<Void> clearSession(@PathVariable String sessionId) {
        log.info("清空会话：{}", sessionId);
        chatService.clearSession(sessionId);
        return Result.success();
    }
    
    @Operation(summary = "获取会话历史", description = "获取指定会话的完整历史消息")
    @GetMapping("/session/{sessionId}/history")
    public Result<List<ChatMessageEntity>> getSessionHistory(@PathVariable String sessionId) {
        log.info("获取会话历史：{}", sessionId);
        List<ChatMessageEntity> history = chatService.getSessionHistory(sessionId);
        return Result.success(history);
    }
    
    @Operation(summary = "获取会话列表", description = "获取所有会话列表 (按活跃度排序)")
    @GetMapping("/sessions")
    public Result<List<ChatSessionEntity>> getAllSessions() {
        log.info("获取所有会话列表");
        List<ChatSessionEntity> sessions = chatService.getAllSessions();
        return Result.success(sessions);
    }
    
    @Operation(summary = "获取最近会话", description = "获取最近活跃的 N 个会话")
    @GetMapping("/sessions/recent")
    public Result<List<ChatSessionEntity>> getRecentSessions(
            @RequestParam(defaultValue = "10") int limit) {
        log.info("获取最近会话，limit: {}", limit);
        List<ChatSessionEntity> sessions = chatService.getRecentSessions(limit);
        return Result.success(sessions);
    }
    
    @Operation(summary = "获取会话摘要", description = "获取指定会话的 AI 生成摘要")
    @GetMapping("/session/{sessionId}/summary")
    public Result<ChatSessionSummaryEntity> getSessionSummary(@PathVariable String sessionId) {
        log.info("获取会话摘要：{}", sessionId);
        if (sessionSummaryService == null) {
            return Result.error("会话摘要服务未启用");
        }
        ChatSessionSummaryEntity summary = sessionSummaryService.getSummary(sessionId);
        return Result.success(summary);
    }
    
    @Operation(summary = "生成会话摘要", description = "手动触发 AI 生成会话摘要")
    @PostMapping("/session/{sessionId}/summary")
    public Result<ChatSessionSummaryEntity> generateSessionSummary(@PathVariable String sessionId) {
        log.info("生成会话摘要：{}", sessionId);
        if (sessionSummaryService == null) {
            return Result.error("会话摘要服务未启用");
        }
        ChatSessionSummaryEntity summary = sessionSummaryService.generateSummary(sessionId);
        return Result.success(summary);
    }
    
    @Operation(summary = "RAG 检索", description = "基于向量相似度检索相关记忆")
    @GetMapping("/rag/search")
    public Result<List<String>> ragSearch(
            @RequestParam String sessionId,
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int topK) {
        log.info("RAG 检索 - sessionId: {}, query: {}", sessionId, query);
        if (ragMemoryService == null) {
            return Result.error("RAG 服务未启用");
        }
        List<String> memories = ragMemoryService.retrieveRelevantMemories(sessionId, query);
        return Result.success(memories);
    }
    
    @Operation(summary = "健康检查", description = "检查 AI 服务状态")
    @GetMapping(value = "/health", produces = "application/json;charset=UTF-8")
    public Result<String> health() {
        return Result.success("AI Service is running! 🦐");
    }
    
    // ========== 流式输出 ==========
    
    @Operation(summary = "流式聊天", description = "使用 SSE 流式输出 AI 回复，实时显示生成过程")
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody ChatRequest request) {
        log.info("流式聊天请求：{}", request.getMessage());
        
        // 设置超时时间（0 表示永不过期）
        SseEmitter emitter = new SseEmitter(0L);
        
        // 异步执行流式聊天
        new Thread(() -> {
            try {
                chatService.chatStream(request, token -> {
                    try {
                        // 发送每个 token
                        emitter.send(SseEmitter.event()
                                .name("message")
                                .data(token));
                    } catch (IOException e) {
                        log.error("发送 SSE 消息失败", e);
                        emitter.completeWithError(e);
                    }
                });
                
                // 完成
                emitter.send(SseEmitter.event()
                        .name("complete")
                        .data("[DONE]"));
                emitter.complete();
                
            } catch (Exception e) {
                log.error("流式聊天错误", e);
                emitter.completeWithError(e);
            }
        }).start();
        
        // 处理完成/错误回调
        emitter.onCompletion(() -> log.info("SSE 连接完成"));
        emitter.onTimeout(() -> {
            log.warn("SSE 连接超时");
            emitter.complete();
        });
        emitter.onError(e -> log.error("SSE 连接错误", e));
        
        return emitter;
    }
    
    // ========== 消息编辑删除 ==========
    
    @Operation(summary = "编辑消息", description = "编辑已发送的消息（用户或 AI 消息）")
    @PutMapping("/message/{messageId}")
    public Result<ChatMessageEntity> editMessage(
            @PathVariable String messageId,
            @RequestBody MessageEditRequest request) {
        log.info("编辑消息：{}", messageId);
        
        try {
            ChatMessageEntity updatedMessage = chatService.editMessage(messageId, request.getContent());
            return Result.success(updatedMessage);
            
        } catch (RuntimeException e) {
            log.error("编辑消息失败：{}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }
    
    @Operation(summary = "删除消息", description = "删除单条消息（软删除）")
    @DeleteMapping("/message/{messageId}")
    public Result<Void> deleteMessage(@PathVariable String messageId) {
        log.info("删除消息：{}", messageId);
        
        try {
            chatService.deleteMessage(messageId);
            return Result.success();
            
        } catch (RuntimeException e) {
            log.error("删除消息失败：{}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }
    
    // ========== 重新生成 ==========
    
    @Operation(summary = "重新生成回复", description = "基于用户消息重新生成 AI 回复")
    @PostMapping("/message/{messageId}/regenerate")
    public Result<ChatResponse> regenerateResponse(@PathVariable String messageId) {
        log.info("重新生成回复：{}", messageId);
        
        try {
            ChatResponse response = chatService.regenerateResponse(messageId);
            return Result.success(response);
            
        } catch (RuntimeException e) {
            log.error("重新生成失败：{}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }
}
