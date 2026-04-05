package com.example.aiframework.controller;

import com.example.aiframework.service.SpeechRecognitionService;
import com.example.aiframework.util.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * 语音识别接口
 */
@RestController
@RequestMapping("/api/speech")
@Tag(name = "语音识别", description = "ASR 语音转文字、语音命令识别等功能")
public class SpeechController {
    
    private static final Logger log = LoggerFactory.getLogger(SpeechController.class);
    
    @Autowired(required = false)
    private SpeechRecognitionService speechRecognitionService;
    
    @Operation(summary = "语音转文字", description = "将音频文件转换为文字")
    @PostMapping("/transcribe")
    public Result<Map<String, Object>> transcribe(@RequestParam("file") MultipartFile file) {
        log.info("语音转文字：{}", file.getOriginalFilename());
        
        if (speechRecognitionService == null) {
            return Result.error("语音识别服务未启用");
        }
        
        try {
            long startTime = System.currentTimeMillis();
            String result = speechRecognitionService.transcribe(file);
            long duration = System.currentTimeMillis() - startTime;
            
            Map<String, Object> response = new HashMap<>();
            response.put("text", result);
            response.put("duration", duration);
            response.put("filename", file.getOriginalFilename());
            response.put("size", file.getSize());
            
            return Result.success(response);
        } catch (Exception e) {
            log.error("语音识别失败：{}", e.getMessage(), e);
            return Result.error("识别失败：" + e.getMessage());
        }
    }
    
    @Operation(summary = "语音翻译", description = "语音识别并翻译成目标语言")
    @PostMapping("/translate")
    public Result<Map<String, Object>> translate(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "target", defaultValue = "zh") String targetLanguage) {
        log.info("语音翻译：{} -> {}", file.getOriginalFilename(), targetLanguage);
        
        if (speechRecognitionService == null) {
            return Result.error("语音识别服务未启用");
        }
        
        try {
            long startTime = System.currentTimeMillis();
            String result = speechRecognitionService.translate(file, targetLanguage);
            long duration = System.currentTimeMillis() - startTime;
            
            Map<String, Object> response = new HashMap<>();
            response.put("text", result);
            response.put("targetLanguage", targetLanguage);
            response.put("duration", duration);
            
            return Result.success(response);
        } catch (Exception e) {
            log.error("语音翻译失败：{}", e.getMessage(), e);
            return Result.error("翻译失败：" + e.getMessage());
        }
    }
    
    @Operation(summary = "语音命令识别", description = "识别语音中的命令关键词")
    @PostMapping("/command")
    public Result<Map<String, Object>> recognizeCommand(@RequestParam("file") MultipartFile file) {
        log.info("语音命令识别：{}", file.getOriginalFilename());
        
        if (speechRecognitionService == null) {
            return Result.error("语音识别服务未启用");
        }
        
        try {
            long startTime = System.currentTimeMillis();
            String command = speechRecognitionService.recognizeCommand(file);
            long duration = System.currentTimeMillis() - startTime;
            
            Map<String, Object> response = new HashMap<>();
            response.put("command", command);
            response.put("duration", duration);
            
            return Result.success(response);
        } catch (Exception e) {
            log.error("语音命令识别失败：{}", e.getMessage(), e);
            return Result.error("命令识别失败：" + e.getMessage());
        }
    }
}
