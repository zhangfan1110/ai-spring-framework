package com.example.aiframework.multimodal.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * 图片生成服务
 * 支持 DALL-E 3 和 Stable Diffusion
 */
@Service
public class ImageGenerationService {
    
    private static final Logger log = LoggerFactory.getLogger(ImageGenerationService.class);
    
    @Value("${image-gen.enabled:true}")
    private boolean imageGenEnabled;
    
    @Value("${image-gen.provider:dall-e-3}")
    private String provider; // dall-e-3/stable-diffusion/midjourney
    
    @Value("${image-gen.output.path:./uploads/generated}")
    private String outputPath;
    
    @Value("${image-gen.size:1024x1024}")
    private String size; // 256x256, 512x512, 1024x1024, 1792x1024, 1024x1792
    
    @Value("${image-gen.quality:standard}")
    private String quality; // standard/hd
    
    @Value("${image-gen.style:vivid}")
    private String style; // vivid/natural
    
    /**
     * 根据描述生成图片
     * @param prompt 图片描述
     * @return 生成的图片路径
     */
    public String generate(String prompt) {
        return generate(prompt, size, quality, style);
    }
    
    /**
     * 根据描述生成图片 (自定义参数)
     * @param prompt 图片描述
     * @param size 尺寸
     * @param quality 质量
     * @param style 风格
     * @return 生成的图片路径
     */
    public String generate(String prompt, String size, String quality, String style) {
        log.info("图片生成：prompt={}, size={}, quality={}, style={}", 
            prompt.substring(0, Math.min(50, prompt.length())), size, quality, style);
        
        if (!imageGenEnabled) {
            throw new IllegalStateException("图片生成服务未启用");
        }
        
        try {
            switch (provider) {
                case "dall-e-3":
                    return generateWithDalle3(prompt, size, quality, style);
                case "dall-e-2":
                    return generateWithDalle2(prompt, size);
                case "stable-diffusion":
                    return generateWithSD(prompt, size);
                case "midjourney":
                    return generateWithMidjourney(prompt);
                default:
                    return generateWithDalle3(prompt, size, quality, style);
            }
        } catch (Exception e) {
            log.error("图片生成失败：{}", e.getMessage(), e);
            throw new RuntimeException("图片生成失败：" + e.getMessage());
        }
    }
    
    /**
     * 使用 DALL-E 3 生成
     */
    private String generateWithDalle3(String prompt, String size, String quality, String style) throws Exception {
        log.info("使用 DALL-E 3 生成图片");
        
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null) {
            throw new IllegalStateException("未配置 OPENAI_API_KEY 环境变量");
        }
        
        // 构建请求
        URL url = new URL("https://api.openai.com/v1/images/generations");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(60000);
        conn.setReadTimeout(60000);
        
        // 请求体
        String json = String.format(
            "{\"model\": \"dall-e-3\", \"prompt\": \"%s\", \"n\": 1, \"size\": \"%s\", \"quality\": \"%s\", \"style\": \"%s\"}",
            escapeJson(prompt), size, quality, style
        );
        
        try (OutputStream os = conn.getOutputStream()) {
            os.write(json.getBytes("UTF-8"));
        }
        
        int status = conn.getResponseCode();
        if (status == 200) {
            // 解析响应
            String response = readResponse(conn);
            String imageUrl = extractImageUrl(response);
            
            // 下载图片
            return downloadImage(imageUrl);
        } else {
            String error = readResponse(conn);
            throw new RuntimeException("DALL-E 3 请求失败：" + status + " - " + error);
        }
    }
    
    /**
     * 使用 DALL-E 2 生成
     */
    private String generateWithDalle2(String prompt, String size) throws Exception {
        log.info("使用 DALL-E 2 生成图片");
        
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null) {
            throw new IllegalStateException("未配置 OPENAI_API_KEY 环境变量");
        }
        
        URL url = new URL("https://api.openai.com/v1/images/generations");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        
        String json = String.format(
            "{\"model\": \"dall-e-2\", \"prompt\": \"%s\", \"n\": 1, \"size\": \"%s\"}",
            escapeJson(prompt), size
        );
        
        try (OutputStream os = conn.getOutputStream()) {
            os.write(json.getBytes("UTF-8"));
        }
        
        int status = conn.getResponseCode();
        if (status == 200) {
            String response = readResponse(conn);
            String imageUrl = extractImageUrl(response);
            return downloadImage(imageUrl);
        } else {
            throw new RuntimeException("DALL-E 2 请求失败：" + status);
        }
    }
    
    /**
     * 使用 Stable Diffusion 生成 (本地或 API)
     */
    private String generateWithSD(String prompt, String size) throws Exception {
        log.info("使用 Stable Diffusion 生成图片");
        
        // 检查是否是本地 SD
        String sdEndpoint = System.getenv("SD_WEBUI_URL");
        
        if (sdEndpoint != null) {
            // 使用本地 Stable Diffusion WebUI
            return generateWithSDWebUI(prompt, size, sdEndpoint);
        } else {
            // 使用 Stability AI API
            return generateWithStabilityAI(prompt, size);
        }
    }
    
    /**
     * 使用 Stable Diffusion WebUI (本地)
     */
    private String generateWithSDWebUI(String prompt, String size, String endpoint) throws Exception {
        String[] dims = size.split("x");
        int width = Integer.parseInt(dims[0]);
        int height = Integer.parseInt(dims[1]);
        
        URL url = new URL(endpoint + "/sdapi/v1/txt2img");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        
        String json = String.format(
            "{\"prompt\": \"%s\", \"width\": %d, \"height\": %d, \"steps\": 20}",
            escapeJson(prompt), width, height
        );
        
        try (OutputStream os = conn.getOutputStream()) {
            os.write(json.getBytes("UTF-8"));
        }
        
        int status = conn.getResponseCode();
        if (status == 200) {
            String response = readResponse(conn);
            // 解析 base64 图片
            String base64 = extractBase64Image(response);
            return saveBase64Image(base64);
        } else {
            throw new RuntimeException("SD WebUI 请求失败：" + status);
        }
    }
    
    /**
     * 使用 Stability AI API
     */
    private String generateWithStabilityAI(String prompt, String size) throws Exception {
        log.info("使用 Stability AI API 生成图片");
        
        String apiKey = System.getenv("STABILITY_API_KEY");
        if (apiKey == null) {
            throw new IllegalStateException("未配置 STABILITY_API_KEY 环境变量");
        }
        
        // TODO: 集成 Stability AI API
        
        throw new UnsupportedOperationException("Stability AI API 待实现");
    }
    
    /**
     * 使用 Midjourney (通过 Discord 机器人)
     */
    private String generateWithMidjourney(String prompt) throws Exception {
        log.info("使用 Midjourney 生成图片");
        
        // TODO: 集成 Midjourney (需要通过 Discord API 或第三方服务)
        
        throw new UnsupportedOperationException("Midjourney 待实现");
    }
    
    /**
     * 下载图片
     */
    private String downloadImage(String imageUrl) throws IOException {
        URL url = new URL(imageUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        
        String filename = UUID.randomUUID().toString() + ".png";
        Path outputPath = Paths.get(this.outputPath, filename);
        Files.createDirectories(outputPath.getParent());
        
        try (InputStream is = conn.getInputStream()) {
            Files.copy(is, outputPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
        
        log.info("图片已保存：{}", outputPath);
        return outputPath.toString();
    }
    
    /**
     * 保存 Base64 图片
     */
    private String saveBase64Image(String base64) throws IOException {
        String filename = UUID.randomUUID().toString() + ".png";
        Path outputPath = Paths.get(this.outputPath, filename);
        Files.createDirectories(outputPath.getParent());
        
        byte[] imageBytes = Base64.getDecoder().decode(base64);
        Files.write(outputPath, imageBytes);
        
        return outputPath.toString();
    }
    
    /**
     * 读取响应
     */
    private String readResponse(HttpURLConnection conn) throws IOException {
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }
        return response.toString();
    }
    
    /**
     * 提取图片 URL
     */
    private String extractImageUrl(String json) {
        // 简单解析 JSON 提取 url 字段
        int startIdx = json.indexOf("\"url\"");
        if (startIdx == -1) return null;
        
        startIdx = json.indexOf(":", startIdx) + 1;
        startIdx = json.indexOf("\"", startIdx) + 1;
        int endIdx = json.indexOf("\"", startIdx);
        
        return json.substring(startIdx, endIdx);
    }
    
    /**
     * 提取 Base64 图片
     */
    private String extractBase64Image(String json) {
        int startIdx = json.indexOf("\"images\"");
        if (startIdx == -1) return null;
        
        startIdx = json.indexOf("[", startIdx);
        startIdx = json.indexOf("\"", startIdx) + 1;
        int endIdx = json.indexOf("\"", startIdx);
        
        return json.substring(startIdx, endIdx);
    }
    
    /**
     * JSON 转义
     */
    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
    
    /**
     * 图片变体 - 基于原图生成相似但不同的变体
     * @param imagePath 原图路径
     * @return 变体图片路径
     */
    public String createVariation(String imagePath) {
        return createVariation(imagePath, size, "standard");
    }
    
    /**
     * 图片变体 - 基于原图生成相似但不同的变体
     * @param imagePath 原图路径
     * @param size 尺寸
     * @param quality 质量
     * @return 变体图片路径
     */
    public String createVariation(String imagePath, String size, String quality) {
        log.info("创建图片变体：{}, size={}", imagePath, size);
        
        if (!imageGenEnabled) {
            throw new IllegalStateException("图片生成服务未启用");
        }
        
        try {
            switch (provider) {
                case "stable-diffusion":
                    return createVariationWithSD(imagePath, size);
                case "dall-e-3":
                case "dall-e-2":
                default:
                    return createVariationWithDalle(imagePath, size);
            }
        } catch (Exception e) {
            log.error("图片变体失败：{}", e.getMessage(), e);
            throw new RuntimeException("图片变体失败：" + e.getMessage());
        }
    }
    
    /**
     * 使用 DALL-E 创建变体
     */
    private String createVariationWithDalle(String imagePath, String size) throws Exception {
        log.info("使用 DALL-E 创建图片变体");
        
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null) {
            throw new IllegalStateException("未配置 OPENAI_API_KEY 环境变量");
        }
        
        // 检查图片大小（DALL-E 变体要求 < 4MB）
        Path imageFile = Paths.get(imagePath);
        long fileSize = Files.size(imageFile);
        if (fileSize > 4 * 1024 * 1024) {
            throw new IllegalArgumentException("图片大小超过 4MB 限制，当前：" + (fileSize / 1024 / 1024) + "MB");
        }
        
        URL url = new URL("https://api.openai.com/v1/images/variations");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setDoOutput(true);
        conn.setConnectTimeout(60000);
        conn.setReadTimeout(60000);
        
        // 构建 multipart 请求
        String boundary = "----WebKitFormBoundary" + UUID.randomUUID().toString().replace("-", "");
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        
        try (OutputStream os = conn.getOutputStream()) {
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(os, "UTF-8"), true);
            
            // 图片文件
            writer.append("--").append(boundary).append("\r\n");
            writer.append("Content-Disposition: form-data; name=\"image\"; filename=\"").append(imageFile.getFileName().toString()).append("\"\r\n");
            writer.append("Content-Type: image/png\r\n\r\n");
            writer.flush();
            Files.copy(imageFile, os);
            os.flush();
            writer.append("\r\n");
            
            // 尺寸参数
            writer.append("--").append(boundary).append("\r\n");
            writer.append("Content-Disposition: form-data; name=\"size\"\r\n\r\n");
            writer.append(size);
            writer.append("\r\n");
            
            // n 参数（生成数量）
            writer.append("--").append(boundary).append("\r\n");
            writer.append("Content-Disposition: form-data; name=\"n\"\r\n\r\n");
            writer.append("1");
            writer.append("\r\n");
            
            // 结束边界
            writer.append("--").append(boundary).append("--\r\n");
            writer.flush();
            writer.close();
        }
        
        int status = conn.getResponseCode();
        if (status == 200) {
            String response = readResponse(conn);
            String imageUrl = extractImageUrl(response);
            return downloadImage(imageUrl);
        } else {
            String error = readResponse(conn);
            throw new RuntimeException("DALL-E 变体请求失败：" + status + " - " + error);
        }
    }
    
    /**
     * 使用 Stable Diffusion 创建变体 (img2img)
     */
    private String createVariationWithSD(String imagePath, String size) throws Exception {
        log.info("使用 Stable Diffusion 创建图片变体 (img2img)");
        
        String sdEndpoint = System.getenv("SD_WEBUI_URL");
        if (sdEndpoint == null) {
            throw new IllegalStateException("未配置 SD_WEBUI_URL 环境变量");
        }
        
        // 读取原图并转换为 base64
        Path imageFile = Paths.get(imagePath);
        byte[] imageBytes = Files.readAllBytes(imageFile);
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);
        
        String[] dims = size.split("x");
        int width = Integer.parseInt(dims[0]);
        int height = Integer.parseInt(dims[1]);
        
        URL url = new URL(sdEndpoint + "/sdapi/v1/img2img");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        
        // img2img 需要 prompt，使用通用描述
        String json = String.format(
            "{\"init_images\": [\"%s\"], \"prompt\": \"high quality variation\", \"width\": %d, \"height\": %d, \"denoising_strength\": 0.7, \"steps\": 20}",
            base64Image, width, height
        );
        
        try (OutputStream os = conn.getOutputStream()) {
            os.write(json.getBytes("UTF-8"));
        }
        
        int status = conn.getResponseCode();
        if (status == 200) {
            String response = readResponse(conn);
            String base64 = extractBase64Image(response);
            return saveBase64Image(base64);
        } else {
            throw new RuntimeException("SD WebUI img2img 请求失败：" + status);
        }
    }
    
    /**
     * 图片编辑 - 使用掩码指定编辑区域
     * @param imagePath 原图路径
     * @param maskPath 掩码图片路径（白色区域为编辑区）
     * @param prompt 编辑描述
     * @return 编辑后的图片路径
     */
    public String editImage(String imagePath, String maskPath, String prompt) {
        log.info("编辑图片：{}, mask={}, prompt={}", imagePath, maskPath, prompt);
        
        if (!imageGenEnabled) {
            throw new IllegalStateException("图片生成服务未启用");
        }
        
        try {
            switch (provider) {
                case "stable-diffusion":
                    return editImageWithSD(imagePath, maskPath, prompt);
                case "dall-e-3":
                case "dall-e-2":
                default:
                    return editImageWithDalle(imagePath, maskPath, prompt);
            }
        } catch (Exception e) {
            log.error("图片编辑失败：{}", e.getMessage(), e);
            throw new RuntimeException("图片编辑失败：" + e.getMessage());
        }
    }
    
    /**
     * 使用 DALL-E 编辑图片（Inpainting）
     */
    private String editImageWithDalle(String imagePath, String maskPath, String prompt) throws Exception {
        log.info("使用 DALL-E 编辑图片 (Inpainting)");
        
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null) {
            throw new IllegalStateException("未配置 OPENAI_API_KEY 环境变量");
        }
        
        Path imageFile = Paths.get(imagePath);
        Path maskFile = maskPath != null ? Paths.get(maskPath) : null;
        
        // 检查图片大小
        if (Files.size(imageFile) > 4 * 1024 * 1024) {
            throw new IllegalArgumentException("图片大小超过 4MB 限制");
        }
        if (maskFile != null && Files.size(maskFile) > 4 * 1024 * 1024) {
            throw new IllegalArgumentException("掩码图片大小超过 4MB 限制");
        }
        
        URL url = new URL("https://api.openai.com/v1/images/edits");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setDoOutput(true);
        conn.setConnectTimeout(60000);
        conn.setReadTimeout(60000);
        
        // 构建 multipart 请求
        String boundary = "----WebKitFormBoundary" + UUID.randomUUID().toString().replace("-", "");
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        
        try (OutputStream os = conn.getOutputStream()) {
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(os, "UTF-8"), true);
            
            // 图片文件
            writer.append("--").append(boundary).append("\r\n");
            writer.append("Content-Disposition: form-data; name=\"image\"; filename=\"").append(imageFile.getFileName().toString()).append("\"\r\n");
            writer.append("Content-Type: image/png\r\n\r\n");
            writer.flush();
            Files.copy(imageFile, os);
            os.flush();
            writer.append("\r\n");
            
            // 掩码文件（可选）
            if (maskFile != null && Files.exists(maskFile)) {
                writer.append("--").append(boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"mask\"; filename=\"").append(maskFile.getFileName().toString()).append("\"\r\n");
                writer.append("Content-Type: image/png\r\n\r\n");
                writer.flush();
                Files.copy(maskFile, os);
                os.flush();
                writer.append("\r\n");
            }
            
            // 编辑描述
            writer.append("--").append(boundary).append("\r\n");
            writer.append("Content-Disposition: form-data; name=\"prompt\"\r\n\r\n");
            writer.append(prompt);
            writer.append("\r\n");
            
            // 尺寸参数
            writer.append("--").append(boundary).append("\r\n");
            writer.append("Content-Disposition: form-data; name=\"size\"\r\n\r\n");
            writer.append(size);
            writer.append("\r\n");
            
            // n 参数
            writer.append("--").append(boundary).append("\r\n");
            writer.append("Content-Disposition: form-data; name=\"n\"\r\n\r\n");
            writer.append("1");
            writer.append("\r\n");
            
            // 结束边界
            writer.append("--").append(boundary).append("--\r\n");
            writer.flush();
            writer.close();
        }
        
        int status = conn.getResponseCode();
        if (status == 200) {
            String response = readResponse(conn);
            String imageUrl = extractImageUrl(response);
            return downloadImage(imageUrl);
        } else {
            String error = readResponse(conn);
            throw new RuntimeException("DALL-E 编辑请求失败：" + status + " - " + error);
        }
    }
    
    /**
     * 使用 Stable Diffusion 编辑图片 (Inpainting)
     */
    private String editImageWithSD(String imagePath, String maskPath, String prompt) throws Exception {
        log.info("使用 Stable Diffusion 编辑图片 (Inpainting)");
        
        String sdEndpoint = System.getenv("SD_WEBUI_URL");
        if (sdEndpoint == null) {
            throw new IllegalStateException("未配置 SD_WEBUI_URL 环境变量");
        }
        
        // 读取原图和掩码
        Path imageFile = Paths.get(imagePath);
        byte[] imageBytes = Files.readAllBytes(imageFile);
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);
        
        String base64Mask = null;
        if (maskPath != null && Files.exists(Paths.get(maskPath))) {
            byte[] maskBytes = Files.readAllBytes(Paths.get(maskPath));
            base64Mask = Base64.getEncoder().encodeToString(maskBytes);
        }
        
        String[] dims = size.split("x");
        int width = Integer.parseInt(dims[0]);
        int height = Integer.parseInt(dims[1]);
        
        URL url = new URL(sdEndpoint + "/sdapi/v1/inpaint");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        
        String json;
        if (base64Mask != null) {
            json = String.format(
                "{\"init_images\": [\"%s\"], \"mask\": \"%s\", \"prompt\": \"%s\", \"width\": %d, \"height\": %d, \"steps\": 20}",
                base64Image, base64Mask, escapeJson(prompt), width, height
            );
        } else {
            json = String.format(
                "{\"init_images\": [\"%s\"], \"prompt\": \"%s\", \"width\": %d, \"height\": %d, \"steps\": 20}",
                base64Image, escapeJson(prompt), width, height
            );
        }
        
        try (OutputStream os = conn.getOutputStream()) {
            os.write(json.getBytes("UTF-8"));
        }
        
        int status = conn.getResponseCode();
        if (status == 200) {
            String response = readResponse(conn);
            String base64 = extractBase64Image(response);
            return saveBase64Image(base64);
        } else {
            throw new RuntimeException("SD WebUI inpaint 请求失败：" + status);
        }
    }
    
    /**
     * 图片放大 - 使用 SD 放大
     * @param imagePath 原图路径
     * @return 放大后的图片路径
     */
    public String upscaleImage(String imagePath) {
        return upscaleImage(imagePath, 2);
    }
    
    /**
     * 图片放大
     * @param imagePath 原图路径
     * @param scale 放大倍数
     * @return 放大后的图片路径
     */
    public String upscaleImage(String imagePath, int scale) {
        log.info("放大图片：{}, scale={}", imagePath, scale);
        
        if (!imageGenEnabled) {
            throw new IllegalStateException("图片生成服务未启用");
        }
        
        try {
            String sdEndpoint = System.getenv("SD_WEBUI_URL");
            if (sdEndpoint == null) {
                throw new IllegalStateException("未配置 SD_WEBUI_URL 环境变量");
            }
            
            // 读取原图
            Path imageFile = Paths.get(imagePath);
            byte[] imageBytes = Files.readAllBytes(imageFile);
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            
            URL url = new URL(sdEndpoint + "/sdapi/v1/extra-single-image");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            
            String json = String.format(
                "{\"image\": \"%s\", \"resize_mode\": 0, \"upscaling_resize\": %d, \"upscaler_1\": \"ESRGAN_4x\"}",
                base64Image, scale
            );
            
            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes("UTF-8"));
            }
            
            int status = conn.getResponseCode();
            if (status == 200) {
                String response = readResponse(conn);
                // 解析 SD 放大返回的 base64
                int startIdx = response.indexOf("\"image\"");
                if (startIdx != -1) {
                    startIdx = response.indexOf(":", startIdx) + 1;
                    startIdx = response.indexOf("\"", startIdx) + 1;
                    int endIdx = response.indexOf("\"", startIdx);
                    String base64 = response.substring(startIdx, endIdx);
                    return saveBase64Image(base64);
                }
                throw new RuntimeException("无法解析放大结果");
            } else {
                throw new RuntimeException("SD WebUI 放大请求失败：" + status);
            }
            
        } catch (Exception e) {
            log.error("图片放大失败：{}", e.getMessage(), e);
            throw new RuntimeException("图片放大失败：" + e.getMessage());
        }
    }
}
