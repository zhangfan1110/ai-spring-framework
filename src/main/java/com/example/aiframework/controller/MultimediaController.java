package com.example.aiframework.controller;

import com.example.aiframework.multimodal.service.ImageGenerationService;
import com.example.aiframework.multimodal.service.OcrService;
import com.example.aiframework.multimodal.service.OcrService.OcrResult;
import com.example.aiframework.multimodal.service.TextToSpeechService;
import com.example.aiframework.common.util.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 多媒体接口 - TTS/OCR/图片生成
 */
@RestController
@RequestMapping("/api/multimedia")
@Tag(name = "多媒体服务", description = "TTS 语音合成、OCR 文字识别、图片生成")
public class MultimediaController {
    
    private static final Logger log = LoggerFactory.getLogger(MultimediaController.class);
    
    @Autowired(required = false)
    private TextToSpeechService textToSpeechService;
    
    @Autowired(required = false)
    private OcrService ocrService;
    
    @Autowired(required = false)
    private ImageGenerationService imageGenerationService;
    
    // ========== TTS 语音合成 ==========
    
    @Operation(summary = "文本转语音", description = "将文本转换为语音 (MP3)")
    @PostMapping(value = "/tts/synthesize", produces = "application/json")
    public Result<Map<String, Object>> synthesize(
            @RequestBody(required = false) Map<String, Object> params) {
        log.info("TTS 语音合成");
        
        if (textToSpeechService == null) {
            return Result.error("TTS 服务未启用");
        }
        
        try {
            // 参数校验
            if (params == null) {
                return Result.error("请求参数不能为空");
            }
            
            String text = params.get("text") != null ? params.get("text").toString() : null;
            if (text == null || text.trim().isEmpty()) {
                return Result.error("text 参数不能为空");
            }
            
            boolean returnBase64 = params.get("returnBase64") != null && Boolean.parseBoolean(params.get("returnBase64").toString());
            
            // 合成语音
            String audioPath = textToSpeechService.synthesize(text);
            
            Map<String, Object> response = new HashMap<>();
            response.put("audioPath", audioPath);
            response.put("filename", new File(audioPath).getName());
            
            // 可选：返回 Base64
            if (returnBase64) {
                String base64 = textToSpeechService.audioToBase64(audioPath);
                response.put("audioBase64", "data:audio/mp3;base64," + base64);
            }
            
            return Result.success(response);
            
        } catch (Exception e) {
            log.error("TTS 合成失败：{}", e.getMessage(), e);
            return Result.error("TTS 合成失败：" + e.getMessage());
        }
    }
    
    @Operation(summary = "获取支持的语音", description = "获取 TTS 支持的语音列表")
    @GetMapping("/tts/voices")
    public Result<List<String>> getSupportedVoices() {
        log.info("获取支持的语音列表");
        
        if (textToSpeechService == null) {
            return Result.error("TTS 服务未启用");
        }
        
        return Result.success(textToSpeechService.getSupportedVoices());
    }
    
    // ========== OCR 文字识别 ==========
    
    @Operation(summary = "OCR 文字识别", description = "从图片中提取文字")
    @PostMapping("/ocr/recognize")
    public Result<Map<String, Object>> recognize(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "lang", defaultValue = "chi_sim+eng") String lang) {
        log.info("OCR 文字识别：file={}, lang={}", file.getOriginalFilename(), lang);
        
        if (ocrService == null) {
            return Result.error("OCR 服务未启用");
        }
        
        try {
            long startTime = System.currentTimeMillis();
            String text = ocrService.recognize(file, lang);
            long duration = System.currentTimeMillis() - startTime;
            
            Map<String, Object> response = new HashMap<>();
            response.put("text", text);
            response.put("language", lang);
            response.put("duration", duration);
            response.put("filename", file.getOriginalFilename());
            response.put("charCount", text != null ? text.length() : 0);
            
            if (text == null || text.trim().isEmpty()) {
                log.warn("OCR 识别结果为空，可能原因：1.图片质量差 2.语言包未安装 3.图片中没有文字");
                response.put("warning", "识别结果为空，请检查图片质量或语言包配置");
            }
            
            return Result.success(response);
            
        } catch (Exception e) {
            log.error("OCR 识别失败：{}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("provider", System.getenv("OCR_PROVIDER"));
            errorResponse.put("lang", lang);
            errorResponse.put("hint", "请检查：1.Tesseract 是否安装 2.语言包是否正确 3.图片是否清晰");
            return Result.success(errorResponse);
        }
    }
    
    @Operation(summary = "OCR 结构化识别", description = "提取文字及位置信息")
    @PostMapping("/ocr/structured")
    public Result<OcrResult> recognizeStructured(
            @RequestParam("file") MultipartFile file) {
        log.info("OCR 结构化识别：{}", file.getOriginalFilename());
        
        if (ocrService == null) {
            return Result.error("OCR 服务未启用");
        }
        
        try {
            OcrResult result = ocrService.recognizeWithPosition(file);
            return Result.success(result);
        } catch (Exception e) {
            log.error("OCR 结构化识别失败：{}", e.getMessage(), e);
            return Result.error("识别失败：" + e.getMessage());
        }
    }
    
    // ========== 图片生成 ==========
    
    @Operation(summary = "AI 图片生成", description = "根据文字描述生成图片")
    @PostMapping("/image/generate")
    public Result<Map<String, Object>> generateImage(
            @RequestBody Map<String, Object> params) {
        log.info("AI 图片生成");
        
        if (imageGenerationService == null) {
            return Result.error("图片生成服务未启用");
        }
        
        try {
            String prompt = params.get("prompt").toString();
            String size = params.get("size") != null ? params.get("size").toString() : "1024x1024";
            String quality = params.get("quality") != null ? params.get("quality").toString() : "standard";
            String style = params.get("style") != null ? params.get("style").toString() : "vivid";
            
            long startTime = System.currentTimeMillis();
            String imagePath = imageGenerationService.generate(prompt, size, quality, style);
            long duration = System.currentTimeMillis() - startTime;
            
            Map<String, Object> response = new HashMap<>();
            response.put("imagePath", imagePath);
            response.put("filename", new File(imagePath).getName());
            response.put("prompt", prompt);
            response.put("size", size);
            response.put("duration", duration);
            
            // 可选：返回 Base64
            if (Boolean.parseBoolean(params.get("returnBase64").toString())) {
                byte[] imageBytes = Files.readAllBytes(Paths.get(imagePath));
                String base64 = java.util.Base64.getEncoder().encodeToString(imageBytes);
                response.put("imageBase64", "data:image/png;base64," + base64);
            }
            
            return Result.success(response);
            
        } catch (Exception e) {
            log.error("图片生成失败：{}", e.getMessage(), e);
            return Result.error("图片生成失败：" + e.getMessage());
        }
    }
    
    @Operation(summary = "图片变体", description = "基于现有图片生成变体")
    @PostMapping("/image/variation")
    public Result<Map<String, Object>> createVariation(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "size", defaultValue = "1024x1024") String size,
            @RequestParam(value = "quality", defaultValue = "standard") String quality) {
        log.info("创建图片变体：{}, size={}", file.getOriginalFilename(), size);
        
        if (imageGenerationService == null) {
            return Result.error("图片生成服务未启用");
        }
        
        try {
            // 保存临时文件
            Path tempFile = saveUploadFile(file);
            
            try {
                long startTime = System.currentTimeMillis();
                String imagePath = imageGenerationService.createVariation(tempFile.toString(), size, quality);
                long duration = System.currentTimeMillis() - startTime;
                
                Map<String, Object> response = new HashMap<>();
                response.put("imagePath", imagePath);
                response.put("filename", new File(imagePath).getName());
                response.put("originalFilename", file.getOriginalFilename());
                response.put("size", size);
                response.put("duration", duration);
                
                return Result.success(response);
                
            } finally {
                // 清理临时文件
                Files.deleteIfExists(tempFile);
            }
            
        } catch (Exception e) {
            log.error("图片变体失败：{}", e.getMessage(), e);
            return Result.error("变体失败：" + e.getMessage());
        }
    }
    
    @Operation(summary = "图片编辑", description = "编辑/修改图片（Inpainting）")
    @PostMapping("/image/edit")
    public Result<Map<String, Object>> editImage(
            @RequestParam("image") MultipartFile imageFile,
            @RequestParam(value = "mask", required = false) MultipartFile maskFile,
            @RequestParam("prompt") String prompt,
            @RequestParam(value = "size", defaultValue = "1024x1024") String size) {
        log.info("编辑图片：{}, mask={}, prompt={}", imageFile.getOriginalFilename(), 
            maskFile != null ? maskFile.getOriginalFilename() : "null", prompt);
        
        if (imageGenerationService == null) {
            return Result.error("图片生成服务未启用");
        }
        
        try {
            // 保存临时文件
            Path tempImage = saveUploadFile(imageFile);
            Path tempMask = null;
            
            try {
                if (maskFile != null && !maskFile.isEmpty()) {
                    tempMask = saveUploadFile(maskFile);
                }
                
                long startTime = System.currentTimeMillis();
                String imagePath = imageGenerationService.editImage(
                    tempImage.toString(), 
                    tempMask != null ? tempMask.toString() : null, 
                    prompt
                );
                long duration = System.currentTimeMillis() - startTime;
                
                Map<String, Object> response = new HashMap<>();
                response.put("imagePath", imagePath);
                response.put("filename", new File(imagePath).getName());
                response.put("originalFilename", imageFile.getOriginalFilename());
                response.put("prompt", prompt);
                response.put("hasMask", tempMask != null);
                response.put("size", size);
                response.put("duration", duration);
                
                return Result.success(response);
                
            } finally {
                // 清理临时文件
                Files.deleteIfExists(tempImage);
                if (tempMask != null) {
                    Files.deleteIfExists(tempMask);
                }
            }
            
        } catch (Exception e) {
            log.error("图片编辑失败：{}", e.getMessage(), e);
            return Result.error("编辑失败：" + e.getMessage());
        }
    }
    
    @Operation(summary = "OCR 诊断", description = "检查 OCR 服务配置")
    @GetMapping("/ocr/diagnose")
    public Result<Map<String, Object>> diagnose() {
        log.info("OCR 诊断");
        
        Map<String, Object> info = new HashMap<>();
        
        if (ocrService == null) {
            info.put("enabled", false);
            return Result.success(info);
        }
        
        info.put("enabled", true);
        
        // 检查 Tesseract
        try {
            ProcessBuilder pb = new ProcessBuilder("tesseract", "--version");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            
            process.waitFor();
            info.put("tesseractInstalled", process.exitValue() == 0);
            info.put("tesseractVersion", output.toString().trim());
            
            // 检查语言包
            ProcessBuilder pb2 = new ProcessBuilder("tesseract", "--list-langs");
            pb2.redirectErrorStream(true);
            Process process2 = pb2.start();
            
            StringBuilder langs = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process2.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    langs.append(line).append("\n");
                }
            }
            
            process2.waitFor();
            info.put("languagePacks", langs.toString().trim());
            info.put("hasChiSim", langs.toString().contains("chi_sim"));
            info.put("hasEng", langs.toString().contains("eng"));
            
        } catch (Exception e) {
            info.put("tesseractInstalled", false);
            info.put("error", e.getMessage());
        }
        
        // 检查 Python OCR
        info.put("pythonInstalled", isPythonInstalled());
        info.put("paddleOCRInstalled", isPaddleOCRInstalled());
        info.put("easyOCRInstalled", isEasyOCRInstalled());
        
        return Result.success(info);
    }
    
    private boolean isPythonInstalled() {
        try {
            ProcessBuilder pb = new ProcessBuilder("python3", "--version");
            return pb.start().waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean isPaddleOCRInstalled() {
        try {
            ProcessBuilder pb = new ProcessBuilder("python3", "-c", "from paddleocr import PaddleOCR");
            return pb.start().waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean isEasyOCRInstalled() {
        try {
            ProcessBuilder pb = new ProcessBuilder("python3", "-c", "import easyocr");
            return pb.start().waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 保存上传的文件到临时目录
     */
    private Path saveUploadFile(MultipartFile file) throws IOException {
        Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"), "multimedia-uploads");
        Files.createDirectories(tempDir);
        
        String filename = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        Path filePath = tempDir.resolve(filename);
        
        file.transferTo(filePath);
        return filePath;
    }
}
