package com.example.aiframework.service;

import dev.langchain4j.data.image.Image;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.UUID;

/**
 * 视觉识别服务 - 图片识别和处理
 */
@Service
public class VisionService {
    
    private static final Logger log = LoggerFactory.getLogger(VisionService.class);
    
    @Resource
    private ChatLanguageModel chatModel;
    
    @Value("${vision.upload.path:./uploads/images}")
    private String uploadPath;
    
    @Value("${vision.max-size:10MB}")
    private String maxSize;
    
    @Value("${vision.allowed-types:image/jpeg,image/png,image/gif,image/webp}")
    private String[] allowedTypes;
    
    /**
     * 识别图片内容
     */
    public String recognizeImage(MultipartFile file, String prompt) throws IOException {
        log.info("识别图片：{}", file.getOriginalFilename());
        
        // 验证文件
        validateImage(file);
        
        // 保存图片
        String savedPath = saveImage(file);
        
        // 转换为 Base64
        byte[] imageBytes = file.getBytes();
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);
        
        // 构建消息
        Image image = Image.builder()
            .base64Data(base64Image)
            .mimeType(file.getContentType())
            .build();
        
        String userPrompt = prompt != null && !prompt.isEmpty() 
            ? prompt 
            : "请描述这张图片的内容";
        
        UserMessage userMessage = UserMessage.from(
            ImageContent.from(image),
            TextContent.from(userPrompt)
        );
        
        // 调用视觉模型
        dev.langchain4j.model.output.Response<AiMessage> response = chatModel.generate(userMessage);
        AiMessage aiMessage = response.content();
        
        log.info("图片识别完成");
        return aiMessage.text();
    }
    
    /**
     * OCR 文字提取
     */
    public String extractText(MultipartFile file) throws IOException {
        log.info("OCR 文字提取：{}", file.getOriginalFilename());
        
        String prompt = "请提取这张图片中的所有文字内容，保持原有格式。如果是表格，请用 Markdown 表格格式输出。";
        return recognizeImage(file, prompt);
    }
    
    /**
     * 生成图片描述
     */
    public String generateDescription(MultipartFile file) throws IOException {
        log.info("生成图片描述：{}", file.getOriginalFilename());
        
        String prompt = "请详细描述这张图片，包括：\n" +
            "1. 主要物体和人物\n" +
            "2. 场景和环境\n" +
            "3. 颜色和光线\n" +
            "4. 任何文字信息\n" +
            "5. 你的理解和推断";
        
        return recognizeImage(file, prompt);
    }
    
    /**
     * 物体识别
     */
    public String detectObjects(MultipartFile file) throws IOException {
        log.info("物体识别：{}", file.getOriginalFilename());
        
        String prompt = "请识别这张图片中的所有物体，列出：\n" +
            "1. 物体名称\n" +
            "2. 位置（左上右下等）\n" +
            "3. 大小（大/中/小）\n" +
            "4. 颜色\n" +
            "请用列表格式输出。";
        
        return recognizeImage(file, prompt);
    }
    
    /**
     * 场景理解
     */
    public String understandScene(MultipartFile file) throws IOException {
        log.info("场景理解：{}", file.getOriginalFilename());
        
        String prompt = "请理解这张图片的场景：\n" +
            "1. 这是什么地方？\n" +
            "2. 发生了什么？\n" +
            "3. 人物在做什么？\n" +
            "4. 时间可能是什么时候？\n" +
            "5. 整体氛围如何？";
        
        return recognizeImage(file, prompt);
    }
    
    /**
     * 验证图片文件
     */
    private void validateImage(MultipartFile file) throws IOException {
        // 检查文件类型
        String contentType = file.getContentType();
        boolean allowed = false;
        for (String type : allowedTypes) {
            if (type.equals(contentType)) {
                allowed = true;
                break;
            }
        }
        
        if (!allowed) {
            throw new IllegalArgumentException("不支持的图片类型：" + contentType);
        }
        
        // 检查文件大小
        long maxSizeBytes = parseSize(maxSize);
        if (file.getSize() > maxSizeBytes) {
            throw new IllegalArgumentException("图片大小超过限制：" + 
                file.getSize() + " > " + maxSize);
        }
    }
    
    /**
     * 保存图片
     */
    private String saveImage(MultipartFile file) throws IOException {
        Path uploadDir = Paths.get(uploadPath);
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }
        
        String filename = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        Path filePath = uploadDir.resolve(filename);
        
        file.transferTo(filePath);
        
        log.info("图片已保存：{}", filePath);
        return filePath.toString();
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
