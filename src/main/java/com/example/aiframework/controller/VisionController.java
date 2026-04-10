package com.example.aiframework.controller;

import com.example.aiframework.multimodal.service.VisionService;
import com.example.aiframework.common.util.Result;
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
 * 视觉识别接口
 */
@RestController
@RequestMapping("/api/vision")
@Tag(name = "视觉识别", description = "图片识别、OCR、物体识别等功能")
public class VisionController {
    
    private static final Logger log = LoggerFactory.getLogger(VisionController.class);
    
    @Autowired(required = false)
    private VisionService visionService;
    
    @Operation(summary = "识别图片", description = "识别图片内容，可自定义提示词")
    @PostMapping("/recognize")
    public Result<Map<String, Object>> recognizeImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "prompt", required = false) String prompt) {
        log.info("识别图片：{}", file.getOriginalFilename());
        
        if (visionService == null) {
            return Result.error("视觉识别服务未启用");
        }
        
        try {
            long startTime = System.currentTimeMillis();
            String result = visionService.recognizeImage(file, prompt);
            long duration = System.currentTimeMillis() - startTime;
            
            Map<String, Object> response = new HashMap<>();
            response.put("result", result);
            response.put("duration", duration);
            response.put("filename", file.getOriginalFilename());
            response.put("size", file.getSize());
            
            return Result.success(response);
        } catch (Exception e) {
            log.error("图片识别失败：{}", e.getMessage(), e);
            return Result.error("识别失败：" + e.getMessage());
        }
    }
    
    @Operation(summary = "OCR 文字提取", description = "提取图片中的文字内容")
    @PostMapping("/ocr")
    public Result<Map<String, Object>> ocr(@RequestParam("file") MultipartFile file) {
        log.info("OCR 文字提取：{}", file.getOriginalFilename());
        
        if (visionService == null) {
            return Result.error("视觉识别服务未启用");
        }
        
        try {
            long startTime = System.currentTimeMillis();
            String result = visionService.extractText(file);
            long duration = System.currentTimeMillis() - startTime;
            
            Map<String, Object> response = new HashMap<>();
            response.put("text", result);
            response.put("duration", duration);
            response.put("filename", file.getOriginalFilename());
            
            return Result.success(response);
        } catch (Exception e) {
            log.error("OCR 失败：{}", e.getMessage(), e);
            return Result.error("OCR 失败：" + e.getMessage());
        }
    }
    
    @Operation(summary = "生成图片描述", description = "AI 生成图片的详细描述")
    @PostMapping("/describe")
    public Result<Map<String, Object>> describeImage(@RequestParam("file") MultipartFile file) {
        log.info("生成图片描述：{}", file.getOriginalFilename());
        
        if (visionService == null) {
            return Result.error("视觉识别服务未启用");
        }
        
        try {
            long startTime = System.currentTimeMillis();
            String result = visionService.generateDescription(file);
            long duration = System.currentTimeMillis() - startTime;
            
            Map<String, Object> response = new HashMap<>();
            response.put("description", result);
            response.put("duration", duration);
            
            return Result.success(response);
        } catch (Exception e) {
            log.error("生成描述失败：{}", e.getMessage(), e);
            return Result.error("生成描述失败：" + e.getMessage());
        }
    }
    
    @Operation(summary = "物体识别", description = "识别图片中的物体")
    @PostMapping("/detect")
    public Result<Map<String, Object>> detectObjects(@RequestParam("file") MultipartFile file) {
        log.info("物体识别：{}", file.getOriginalFilename());
        
        if (visionService == null) {
            return Result.error("视觉识别服务未启用");
        }
        
        try {
            long startTime = System.currentTimeMillis();
            String result = visionService.detectObjects(file);
            long duration = System.currentTimeMillis() - startTime;
            
            Map<String, Object> response = new HashMap<>();
            response.put("objects", result);
            response.put("duration", duration);
            
            return Result.success(response);
        } catch (Exception e) {
            log.error("物体识别失败：{}", e.getMessage(), e);
            return Result.error("物体识别失败：" + e.getMessage());
        }
    }
    
    @Operation(summary = "场景理解", description = "理解图片场景和情境")
    @PostMapping("/scene")
    public Result<Map<String, Object>> understandScene(@RequestParam("file") MultipartFile file) {
        log.info("场景理解：{}", file.getOriginalFilename());
        
        if (visionService == null) {
            return Result.error("视觉识别服务未启用");
        }
        
        try {
            long startTime = System.currentTimeMillis();
            String result = visionService.understandScene(file);
            long duration = System.currentTimeMillis() - startTime;
            
            Map<String, Object> response = new HashMap<>();
            response.put("scene", result);
            response.put("duration", duration);
            
            return Result.success(response);
        } catch (Exception e) {
            log.error("场景理解失败：{}", e.getMessage(), e);
            return Result.error("场景理解失败：" + e.getMessage());
        }
    }
}
