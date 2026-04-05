package com.example.aiframework.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 语音识别服务 - ASR (Automatic Speech Recognition)
 * 使用 Whisper 开源模型
 */
@Service
public class SpeechRecognitionService {
    
    private static final Logger log = LoggerFactory.getLogger(SpeechRecognitionService.class);
    
    @Value("${asr.enabled:true}")
    private boolean asrEnabled;
    
    @Value("${asr.provider:whisper}")
    private String provider;
    
    @Value("${asr.model:whisper-1}")
    private String model;
    
    @Value("${asr.language:zh}")
    private String language;
    
    @Value("${asr.upload.path:./uploads/audio}")
    private String uploadPath;
    
    @Value("${asr.max-size:25MB}")
    private String maxSize;
    
    @Value("${asr.allowed-types:audio/wav,audio/mp3,audio/mpeg,audio/ogg,audio/webm,audio/flac}")
    private String[] allowedTypes;
    
    /**
     * 语音转文字
     */
    public String transcribe(MultipartFile file) throws Exception {
        log.info("语音识别：{}", file.getOriginalFilename());
        
        // 验证文件
        validateAudio(file);
        
        // 保存临时文件
        Path tempFile = saveAudio(file);
        
        try {
            // 根据提供商选择识别方式
            if ("whisper-local".equals(provider)) {
                return transcribeWithLocalWhisper(tempFile);
            } else if ("whisper-api".equals(provider)) {
                return transcribeWithWhisperAPI(tempFile);
            } else {
                // 默认使用本地 Whisper
                return transcribeWithLocalWhisper(tempFile);
            }
        } finally {
            // 清理临时文件
            Files.deleteIfExists(tempFile);
        }
    }
    
    /**
     * 语音转文字 (带翻译)
     */
    public String translate(MultipartFile file, String targetLanguage) throws Exception {
        log.info("语音翻译：{} -> {}", file.getOriginalFilename(), targetLanguage);
        
        String originalText = transcribe(file);
        
        // TODO: 调用翻译服务
        return originalText;
    }
    
    /**
     * 语音命令识别
     */
    public String recognizeCommand(MultipartFile file) throws Exception {
        log.info("语音命令识别：{}", file.getOriginalFilename());
        
        String text = transcribe(file);
        
        // 提取命令关键词
        String command = extractCommand(text);
        
        log.info("识别命令：{}", command);
        return command;
    }
    
    /**
     * 使用本地 Whisper 识别
     */
    private String transcribeWithLocalWhisper(Path audioFile) throws Exception {
        log.info("使用本地 Whisper 识别：{}", audioFile);
        
        // 检查 Whisper 是否安装
        if (!isWhisperInstalled()) {
            throw new IllegalStateException("Whisper 未安装，请先安装：pip install openai-whisper");
        }
        
        // 构建命令
        ProcessBuilder pb = new ProcessBuilder(
            "whisper",
            audioFile.toString(),
            "--model", model,
            "--language", language,
            "--output_format", "txt",
            "--output_dir", uploadPath
        );
        
        pb.redirectErrorStream(true);
        Process process = pb.start();
        
        // 等待完成 (最长 5 分钟)
        if (!process.waitFor(5, TimeUnit.MINUTES)) {
            process.destroyForcibly();
            throw new RuntimeException("Whisper 识别超时");
        }
        
        // 读取输出
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        
        // 读取生成的文本文件
        String txtFile = audioFile.toString().replaceAll("\\.(wav|mp3|ogg|webm|flac)$", "") + ".txt";
        Path txtPath = Paths.get(txtFile);
        if (Files.exists(txtPath)) {
            String result = Files.readString(txtPath);
            Files.deleteIfExists(txtPath); // 清理 txt 文件
            return result;
        }
        
        return output.toString();
    }
    
    /**
     * 使用 Whisper API 识别
     */
    private String transcribeWithWhisperAPI(Path audioFile) throws Exception {
        log.info("使用 Whisper API 识别：{}", audioFile);
        
        // TODO: 集成 OpenAI Whisper API
        // 或使用其他云端 ASR 服务
        
        return "Whisper API 识别结果 (待实现)";
    }
    
    /**
     * 验证音频文件
     */
    private void validateAudio(MultipartFile file) throws IOException {
        // 检查文件类型
        String contentType = file.getContentType();
        boolean allowed = false;
        for (String type : allowedTypes) {
            if (type.equals(contentType) || contentType.endsWith(type.split("/")[1])) {
                allowed = true;
                break;
            }
        }
        
        if (!allowed) {
            throw new IllegalArgumentException("不支持的音频格式：" + contentType);
        }
        
        // 检查文件大小
        long maxSizeBytes = parseSize(maxSize);
        if (file.getSize() > maxSizeBytes) {
            throw new IllegalArgumentException("音频大小超过限制：" + 
                file.getSize() + " > " + maxSize);
        }
    }
    
    /**
     * 保存音频文件
     */
    private Path saveAudio(MultipartFile file) throws IOException {
        Path uploadDir = Paths.get(uploadPath);
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }
        
        String filename = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        Path filePath = uploadDir.resolve(filename);
        
        file.transferTo(filePath);
        
        log.info("音频已保存：{}", filePath);
        return filePath;
    }
    
    /**
     * 检查 Whisper 是否安装
     */
    private boolean isWhisperInstalled() {
        try {
            ProcessBuilder pb = new ProcessBuilder("whisper", "--version");
            Process process = pb.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 提取命令关键词
     */
    private String extractCommand(String text) {
        // 简单的命令提取逻辑
        // 可以集成 NLP 模型进行更准确的命令识别
        
        String[] commandKeywords = {
            "打开", "关闭", "启动", "停止",
            "创建", "删除", "保存", "发送",
            "搜索", "查询", "播放", "暂停"
        };
        
        for (String keyword : commandKeywords) {
            if (text.contains(keyword)) {
                return keyword;
            }
        }
        
        return text;
    }
    
    /**
     * 解析文件大小字符串
     */
    private long parseSize(String size) {
        size = size.toUpperCase();
        if (size.endsWith("MB")) {
            return Long.parseLong(size.substring(0, size.length() - 2)) * 1024 * 1024;
        } else if (size.endsWith("KB")) {
            return Long.parseLong(size.substring(0, size.length() - 2)) * 1024;
        } else if (size.endsWith("GB")) {
            return Long.parseLong(size.substring(0, size.length() - 2)) * 1024 * 1024 * 1024;
        }
        return Long.parseLong(size);
    }
}
