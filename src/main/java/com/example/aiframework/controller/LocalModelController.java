package com.example.aiframework.controller;

import com.example.aiframework.system.service.LocalModelService;
import com.example.aiframework.system.dto.LocalModelService_ModelResponse;
import com.example.aiframework.system.service.LocalModelService.StreamCallback;
import com.example.aiframework.common.util.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 本地大模型接口（Ollama、LM Studio 等）
 */
@RestController
@RequestMapping("/api/local-model")
@Tag(name = "本地大模型", description = "本地模型调用（Ollama、LM Studio 等）")
public class LocalModelController {
    
    private static final Logger log = LoggerFactory.getLogger(LocalModelController.class);
    
    @Autowired
    private LocalModelService localModelService;
    
    @Operation(summary = "聊天", description = "使用本地模型进行对话")
    @PostMapping("/chat")
    public Result<Map<String, Object>> chat(@RequestBody Map<String, Object> request) {
        log.info("本地模型聊天请求");
        
        try {
            String prompt = (String) request.get("prompt");
            String model = (String) request.getOrDefault("model", "deepseek-r1");
            double temperature = ((Number) request.getOrDefault("temperature", 0.7)).doubleValue();
            
            if (prompt == null || prompt.isEmpty()) {
                return Result.error("提示词不能为空");
            }
            
            LocalModelService_ModelResponse response = localModelService.chat(model, prompt, temperature);
            
            Map<String, Object> result = new HashMap<>();
            result.put("content", response.getContent());
            result.put("model", response.getModel());
            result.put("duration", response.getDuration());
            result.put("metadata", response.getMetadata());
            
            return Result.success(result);
            
        } catch (Exception e) {
            log.error("本地模型聊天失败：{}", e.getMessage(), e);
            return Result.error("聊天失败：" + e.getMessage());
        }
    }
    
    @Operation(summary = "流式聊天", description = "使用 SSE 流式输出本地模型响应")
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody Map<String, Object> request) {
        log.info("本地模型流式聊天请求");
        
        // 设置超时时间（0 表示永不过期）
        SseEmitter emitter = new SseEmitter(0L);
        
        String prompt = (String) request.get("prompt");
        String model = (String) request.getOrDefault("model", "deepseek-r1");
        double temperature = ((Number) request.getOrDefault("temperature", 0.7)).doubleValue();
        
        if (prompt == null || prompt.isEmpty()) {
            emitter.completeWithError(new RuntimeException("提示词不能为空"));
            return emitter;
        }
        
        // 异步执行流式聊天
        new Thread(() -> {
            try {
                localModelService.chatStream(model, prompt, temperature, new StreamCallback() {
                    @Override
                    public void onToken(String token) {
                        try {
                            emitter.send(SseEmitter.event()
                                    .name("token")
                                    .data(token));
                        } catch (IOException e) {
                            log.error("发送 SSE token 失败", e);
                        }
                    }
                    
                    @Override
                    public void onComplete(String fullContent) {
                        try {
                            emitter.send(SseEmitter.event()
                                    .name("complete")
                                    .data(fullContent));
                            emitter.complete();
                        } catch (IOException e) {
                            log.error("发送 SSE 完成信号失败", e);
                            emitter.completeWithError(e);
                        }
                    }
                    
                    @Override
                    public void onError(Throwable error) {
                        log.error("流式聊天错误", error);
                        emitter.completeWithError(error);
                    }
                });
            } catch (Exception e) {
                log.error("流式聊天异常", e);
                emitter.completeWithError(e);
            }
        }).start();
        
        // 处理回调
        emitter.onCompletion(() -> log.info("SSE 连接完成"));
        emitter.onTimeout(() -> {
            log.warn("SSE 连接超时");
            emitter.complete();
        });
        emitter.onError(e -> log.error("SSE 连接错误", e));
        
        return emitter;
    }
    
    @Operation(summary = "获取模型列表", description = "获取本地可用的模型列表")
    @GetMapping("/models")
    public Result<List<String>> listModels() {
        log.info("获取本地模型列表");
        
        List<String> models = localModelService.listModels();
        return Result.success(models);
    }
    
    @Operation(summary = "检查模型", description = "检查指定模型是否存在")
    @GetMapping("/model/{modelName}/exists")
    public Result<Map<String, Object>> hasModel(@PathVariable String modelName) {
        log.info("检查模型：{}", modelName);
        
        boolean exists = localModelService.hasModel(modelName);
        
        Map<String, Object> result = new HashMap<>();
        result.put("modelName", modelName);
        result.put("exists", exists);
        
        return Result.success(result);
    }
    
    @Operation(summary = "拉取模型", description = "从 Ollama 仓库拉取模型")
    @PostMapping("/model/pull")
    public Result<Map<String, Object>> pullModel(@RequestBody Map<String, String> request) {
        log.info("拉取模型：{}", request.get("name"));
        
        String modelName = request.get("name");
        if (modelName == null || modelName.isEmpty()) {
            return Result.error("模型名称不能为空");
        }
        
        Map<String, Object> result = localModelService.pullModel(modelName);
        return "success".equals(result.get("status")) ? 
            Result.success(result) : Result.error("拉取失败", result);
    }
    
    @Operation(summary = "删除模型", description = "删除本地模型")
    @DeleteMapping("/model/{modelName}")
    public Result<Map<String, Object>> deleteModel(@PathVariable String modelName) {
        log.info("删除模型：{}", modelName);
        
        Map<String, Object> result = localModelService.deleteModel(modelName);
        return "success".equals(result.get("status")) ? 
            Result.success(result) : Result.error("删除失败", result);
    }
    
    @Operation(summary = "生成嵌入向量", description = "使用本地模型生成文本嵌入向量")
    @PostMapping("/embeddings")
    public Result<Map<String, Object>> embeddings(@RequestBody Map<String, Object> request) {
        log.info("生成本地嵌入向量");
        
        try {
            String text = (String) request.get("text");
            String model = (String) request.getOrDefault("model", "deepseek-r1");
            
            if (text == null || text.isEmpty()) {
                return Result.error("文本不能为空");
            }
            
            List<Float> embedding = localModelService.embed(model, text);
            
            Map<String, Object> result = new HashMap<>();
            result.put("embedding", embedding);
            result.put("model", model);
            result.put("dimension", embedding.size());
            
            return Result.success(result);
            
        } catch (Exception e) {
            log.error("生成嵌入向量失败：{}", e.getMessage(), e);
            return Result.error("生成失败：" + e.getMessage());
        }
    }
    
    @Operation(summary = "健康检查", description = "检查本地模型服务状态")
    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        log.info("本地模型健康检查");
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            List<String> models = localModelService.listModels();
            result.put("status", "connected");
            result.put("modelCount", models.size());
            result.put("models", models);
            
            return Result.success(result);
            
        } catch (Exception e) {
            result.put("status", "disconnected");
            result.put("error", e.getMessage());
            
            return Result.error("连接失败", result);
        }
    }
}
