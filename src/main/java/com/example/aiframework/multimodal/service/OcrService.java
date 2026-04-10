package com.example.aiframework.multimodal.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * OCR 文字识别服务
 * 支持多种 OCR 引擎
 */
@Service
public class OcrService {
    
    private static final Logger log = LoggerFactory.getLogger(OcrService.class);
    
    @Value("${ocr.enabled:true}")
    private boolean ocrEnabled;
    
    @Value("${ocr.provider:tesseract}")
    private String provider; // tesseract/paddle/easyocr/azure
    
    @Value("${ocr.lang:chi_sim+eng}")
    private String lang; // 语言
    
    @Value("${ocr.output.path:./uploads/ocr}")
    private String outputPath;
    
    /**
     * OCR 文字识别
     * @param imageFile 图片文件
     * @return 识别的文字
     */
    public String recognize(MultipartFile imageFile) {
        return recognize(imageFile, lang);
    }
    
    /**
     * OCR 文字识别 (指定语言)
     * @param imageFile 图片文件
     * @param lang 语言 (chi_sim 简体中文/chi_tra 繁体中文/eng 英文/jpn 日文/kor 韩文)
     * @return 识别的文字
     */
    public String recognize(MultipartFile imageFile, String lang) {
        log.info("OCR 文字识别：file={}, lang={}", imageFile.getOriginalFilename(), lang);
        
        if (!ocrEnabled) {
            throw new IllegalStateException("OCR 服务未启用");
        }
        
        try {
            // 验证图片
            validateImage(imageFile);
            
            // 保存临时文件
            Path tempFile = saveImage(imageFile);
            
            try {
                switch (provider) {
                    case "tesseract":
                        return recognizeWithTesseract(tempFile, lang);
                    case "paddle":
                        return recognizeWithPaddleOCR(tempFile, lang);
                    case "easyocr":
                        return recognizeWithEasyOCR(tempFile, lang);
                    case "azure":
                        return recognizeWithAzure(tempFile);
                    default:
                        return recognizeWithTesseract(tempFile, lang);
                }
            } finally {
                // 清理临时文件
                Files.deleteIfExists(tempFile);
            }
            
        } catch (Exception e) {
            log.error("OCR 识别失败：{}", e.getMessage(), e);
            throw new RuntimeException("OCR 识别失败：" + e.getMessage());
        }
    }
    
    /**
     * 使用 Tesseract OCR
     */
    private String recognizeWithTesseract(Path imageFile, String lang) throws Exception {
        log.info("使用 Tesseract OCR 识别：file={}, lang={}", imageFile, lang);
        
        // 检查 Tesseract 是否安装
        if (!isTesseractInstalled()) {
            throw new IllegalStateException("Tesseract 未安装，请运行：brew install tesseract (macOS) 或 apt install tesseract-ocr (Linux)");
        }
        
        // 检查语言包
        List<String> availableLangs = getAvailableLanguagePacks();
        log.info("已安装的语言包：{}", availableLangs);
        
        for (String l : lang.split("\\+")) {
            if (!availableLangs.contains(l)) {
                log.error("语言包未安装：{}，可用的语言包：{}", l, availableLangs);
                throw new IllegalStateException("语言包 '" + l + "' 未安装，请先安装：brew install tesseract-lang (macOS)");
            }
        }
        
        // 检查图片文件
        if (!Files.exists(imageFile)) {
            throw new IllegalArgumentException("图片文件不存在：" + imageFile);
        }
        
        // 构建命令 - 使用 stdout 直接输出，不生成文件
        List<String> command = new ArrayList<>();
        command.add("tesseract");
        command.add(imageFile.toString());
        command.add("stdout");  // 直接输出到 stdout
        command.add("-l");
        command.add(lang);
        command.add("--psm");
        command.add("6");  // 假设是统一块文本
        command.add("-c");
        command.add("tessedit_write_images=0"); // 禁用调试输出
        
        log.info("执行命令：{}", String.join(" ", command));
        
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        
        // 读取输出（包括错误信息）
        StringBuilder output = new StringBuilder();
        StringBuilder error = new StringBuilder();
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                log.debug("Tesseract 输出：{}", line);
            }
        }
        
        try (BufferedReader errReader = new BufferedReader(
                new InputStreamReader(process.getErrorStream()))) {
            String line;
            while ((line = errReader.readLine()) != null) {
                error.append(line).append("\n");
                log.warn("Tesseract 错误：{}", line);
            }
        }
        
        // 等待完成
        if (!process.waitFor(2, TimeUnit.MINUTES)) {
            process.destroyForcibly();
            throw new RuntimeException("OCR 识别超时");
        }
        
        log.info("Tesseract 执行完成，退出码：{}, 输出长度：{}", process.exitValue(), output.length());
        
        // 检查退出码
        if (process.exitValue() != 0) {
            log.error("Tesseract 执行失败，退出码：{}", process.exitValue());
            log.error("错误输出：{}", error.toString());
            throw new RuntimeException("Tesseract 执行失败 (退出码=" + process.exitValue() + ")：" + error.toString());
        }
        
        String result = output.toString().trim();
        
        if (result.isEmpty()) {
            log.warn("Tesseract 未识别到任何文字");
            log.warn("图片路径：{}", imageFile);
            log.warn("语言：{}", lang);
            log.warn("错误输出：{}", error.toString());
            return ""; // 返回空字符串，不抛异常
        }
        
        // 后处理：过滤 Tesseract 诊断信息和明显误识别
        result = postProcessOcrResult(result);
        
        log.info("OCR 识别成功，识别到 {} 个字符", result.length());
        return result;
    }
    
    /**
     * OCR 结果后处理：过滤诊断信息和明显误识别
     * @param result 原始 OCR 结果
     * @return 清理后的结果
     */
    private String postProcessOcrResult(String result) {
        String cleaned = result;
        
        // 1. 过滤 Tesseract 诊断信息行
        cleaned = cleaned.replaceAll("(?m)^Estimating resolution as \\d+\\s*$", "");
        cleaned = cleaned.replaceAll("(?m)^Warning[:\\s]*.*$", "");
        cleaned = cleaned.replaceAll("(?m)^Tesseract.*$", "");
        
        // 2. 过滤明显的误识别模式（代码注释场景）
        // [kk, [k, fie 等通常是 /* 的误识别
        cleaned = cleaned.replaceAll("(?m)^\\[k+k?\\s*$", "");
        cleaned = cleaned.replaceAll("(?m)^fie\\s*$", "");
        
        // 3. 过滤只有标点符号的行（通常是误识别）
        cleaned = cleaned.replaceAll("(?m)^[\\[\\]{}()/\\\\*]+$", "");
        
        // 4. 过滤 t/BERR 等明显乱码
        cleaned = cleaned.replaceAll("(?m)^t/\\s*$", "");
        cleaned = cleaned.replaceAll("(?m)^\\d+%?\\s+Tesseract\\s+BERR\\s*$", "");
        
        // 5. 清理空行（多个连续空行合并为一个）
        cleaned = cleaned.replaceAll("\\n{3,}", "\n\n");
        
        // 6.  trim 每行的前后空格
        StringBuilder finalResult = new StringBuilder();
        for (String line : cleaned.split("\\n")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                if (!finalResult.isEmpty()) {
                    finalResult.append("\n");
                }
                finalResult.append(trimmed);
            }
        }
        
        return finalResult.toString().trim();
    }
    
    /**
     * 使用 PaddleOCR (推荐，中文效果好)
     */
    private String recognizeWithPaddleOCR(Path imageFile, String lang) throws Exception {
        log.info("使用 PaddleOCR 识别");
        
        // 检查 PaddleOCR 是否安装
        if (!isPaddleOCRInstalled()) {
            throw new IllegalStateException("PaddleOCR 未安装，请运行：pip install paddlepaddle paddleocr");
        }
        
        // 创建 Python 脚本
        Path scriptFile = createPaddleOCRScript(imageFile, lang);
        
        try {
            ProcessBuilder pb = new ProcessBuilder("python3", scriptFile.toString());
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            // 等待完成
            if (!process.waitFor(2, TimeUnit.MINUTES)) {
                process.destroyForcibly();
                throw new RuntimeException("OCR 识别超时");
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
            
            return output.toString().trim();
            
        } finally {
            Files.deleteIfExists(scriptFile);
        }
    }
    
    /**
     * 使用 EasyOCR
     */
    private String recognizeWithEasyOCR(Path imageFile, String lang) throws Exception {
        log.info("使用 EasyOCR 识别");
        
        // 检查 EasyOCR 是否安装
        if (!isEasyOCRInstalled()) {
            throw new IllegalStateException("EasyOCR 未安装，请运行：pip install easyocr");
        }
        
        // 映射语言
        String easyOcrLang = mapToEasyOCRLang(lang);
        
        // 创建 Python 脚本
        Path scriptFile = createEasyOCRScript(imageFile, easyOcrLang);
        
        try {
            ProcessBuilder pb = new ProcessBuilder("python3", scriptFile.toString());
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            if (!process.waitFor(2, TimeUnit.MINUTES)) {
                process.destroyForcibly();
                throw new RuntimeException("OCR 识别超时");
            }
            
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            
            return output.toString().trim();
            
        } finally {
            Files.deleteIfExists(scriptFile);
        }
    }
    
    /**
     * 使用 Azure OCR
     */
    private String recognizeWithAzure(Path imageFile) throws Exception {
        log.info("使用 Azure OCR 识别");
        
        // TODO: 集成 Azure Computer Vision OCR API
        
        String apiKey = System.getenv("AZURE_VISION_KEY");
        String endpoint = System.getenv("AZURE_VISION_ENDPOINT");
        
        if (apiKey == null || endpoint == null) {
            throw new IllegalStateException("未配置 Azure Vision API Key 和 Endpoint");
        }
        
        // 调用 Azure API
        throw new UnsupportedOperationException("Azure OCR 待实现");
    }
    
    /**
     * 创建 PaddleOCR Python 脚本
     */
    private Path createPaddleOCRScript(Path imageFile, String lang) throws IOException {
        String script = 
            "from paddleocr import PaddleOCR\n" +
            "import sys\n" +
            "\n" +
            "ocr = PaddleOCR(use_angle_cls=True, lang='" + (lang.contains("chi") ? "ch" : "en") + "')\n" +
            "img_path = '" + imageFile.toString() + "'\n" +
            "result = ocr.ocr(img_path, cls=True)\n" +
            "\n" +
            "for line in result:\n" +
            "    if line:\n" +
            "        for item in line:\n" +
            "            print(item[1][0])\n";
        
        Path scriptFile = Paths.get(outputPath, "paddle_ocr_" + UUID.randomUUID() + ".py");
        Files.createDirectories(scriptFile.getParent());
        Files.writeString(scriptFile, script);
        
        return scriptFile;
    }
    
    /**
     * 创建 EasyOCR Python 脚本
     */
    private Path createEasyOCRScript(Path imageFile, String lang) throws IOException {
        String script = 
            "import easyocr\n" +
            "import sys\n" +
            "\n" +
            "reader = easyocr.Reader(['" + lang + "'], gpu=False)\n" +
            "result = reader.readtext('" + imageFile.toString() + "')\n" +
            "\n" +
            "for detection in result:\n" +
            "    print(detection[1])\n";
        
        Path scriptFile = Paths.get(outputPath, "easyocr_" + UUID.randomUUID() + ".py");
        Files.createDirectories(scriptFile.getParent());
        Files.writeString(scriptFile, script);
        
        return scriptFile;
    }
    
    /**
     * 映射语言代码到 EasyOCR
     */
    private String mapToEasyOCRLang(String tesseractLang) {
        Map<String, String> langMap = new HashMap<>();
        langMap.put("chi_sim", "ch_sim");
        langMap.put("chi_tra", "ch_tra");
        langMap.put("eng", "en");
        langMap.put("jpn", "ja");
        langMap.put("kor", "ko");
        
        return langMap.getOrDefault(tesseractLang, "en");
    }
    
    /**
     * 验证图片
     */
    private void validateImage(MultipartFile file) throws IOException {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("请上传图片文件");
        }
        
        long maxSize = 20 * 1024 * 1024; // 20MB
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("图片大小超过限制 (20MB)");
        }
    }
    
    /**
     * 保存图片
     */
    private Path saveImage(MultipartFile file) throws IOException {
        Path uploadDir = Paths.get(outputPath);
        Files.createDirectories(uploadDir);
        
        String filename = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        Path filePath = uploadDir.resolve(filename);
        
        file.transferTo(filePath);
        return filePath;
    }
    
    /**
     * 检查 Tesseract 是否安装
     */
    private boolean isTesseractInstalled() {
        try {
            ProcessBuilder pb = new ProcessBuilder("tesseract", "--version");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            // 读取版本信息
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line = reader.readLine();
                log.info("Tesseract 版本：{}", line);
            }
            
            return process.waitFor() == 0;
        } catch (Exception e) {
            log.debug("Tesseract 检查失败：{}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 获取已安装的语言包列表
     */
    private List<String> getAvailableLanguagePacks() {
        List<String> langs = new ArrayList<>();
        try {
            ProcessBuilder pb = new ProcessBuilder("tesseract", "--list-langs");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // 跳过标题行
                    if (!line.startsWith("List") && !line.isEmpty()) {
                        langs.add(line.trim());
                    }
                }
            }
            
            process.waitFor();
            log.info("已安装的语言包：{}", langs);
            
        } catch (Exception e) {
            log.warn("语言包检查失败：{}", e.getMessage());
        }
        return langs;
    }
    
    /**
     * 检查 PaddleOCR 是否安装
     */
    private boolean isPaddleOCRInstalled() {
        try {
            ProcessBuilder pb = new ProcessBuilder("python3", "-c", "from paddleocr import PaddleOCR");
            Process process = pb.start();
            return process.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 检查 EasyOCR 是否安装
     */
    private boolean isEasyOCRInstalled() {
        try {
            ProcessBuilder pb = new ProcessBuilder("python3", "-c", "import easyocr");
            Process process = pb.start();
            return process.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 结构化 OCR 结果 (带位置信息)
     * 使用 Tesseract 输出 XML 格式获取位置信息
     */
    public OcrResult recognizeWithPosition(MultipartFile imageFile) {
        log.info("OCR 结构化识别：{}", imageFile.getOriginalFilename());
        
        OcrResult result = new OcrResult();
        
        try {
            // 验证图片
            validateImage(imageFile);
            
            // 保存临时文件
            Path tempFile = saveImage(imageFile);
            
            try {
                // 检查 Tesseract 是否安装
                if (!isTesseractInstalled()) {
                    throw new IllegalStateException("Tesseract 未安装");
                }
                
                // 使用 hOCR 格式输出（带位置信息）
                List<String> command = new ArrayList<>();
                command.add("tesseract");
                command.add(tempFile.toString());
                command.add("stdout");
                command.add("-l");
                command.add(lang);
                command.add("hocr");  // 输出 hOCR 格式（带位置信息）
                command.add("--psm");
                command.add("6");
                command.add("-c");
                command.add("tessedit_write_images=0"); // 禁用调试输出
                
                log.info("执行 hOCR 命令：{}", String.join(" ", command));
                
                ProcessBuilder pb = new ProcessBuilder(command);
                pb.redirectErrorStream(true);
                Process process = pb.start();
                
                // 读取 hOCR 输出
                StringBuilder hocrOutput = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        hocrOutput.append(line).append("\n");
                    }
                }
                
                // 等待完成
                if (!process.waitFor(2, TimeUnit.MINUTES)) {
                    process.destroyForcibly();
                    throw new RuntimeException("OCR 识别超时");
                }
                
                if (process.exitValue() != 0) {
                    throw new RuntimeException("Tesseract 执行失败，退出码：" + process.exitValue());
                }
                
                // 解析 hOCR 输出，提取文字和位置
                parseHocrOutput(hocrOutput.toString(), result);
                
                // 后处理：过滤诊断信息和误识别
                result.setText(postProcessOcrResult(result.getText()));
                if (result.getRegions() != null) {
                    result.getRegions().removeIf(r -> {
                        String text = r.getText();
                        // 过滤诊断信息
                        if (text.matches("(?i)^Estimating resolution as \\d+$")) return true;
                        if (text.matches("(?i)^Warning[:\\s]*.*$")) return true;
                        if (text.matches("(?i)^Tesseract.*$")) return true;
                        // 过滤误识别
                        if (text.matches("^\\[k+k?$")) return true;
                        if (text.matches("^fie$")) return true;
                        if (text.matches("^[\\[\\]{}()/\\\\*]+$")) return true;
                        if (text.matches("^t/?$")) return true;
                        return false;
                    });
                    // 重新构建文本
                    StringBuilder cleanedText = new StringBuilder();
                    for (TextRegion region : result.getRegions()) {
                        cleanedText.append(region.getText()).append("\n");
                    }
                    result.setText(cleanedText.toString().trim());
                }
                
                log.info("OCR 结构化识别成功，识别到 {} 个文本区域", 
                    result.getRegions() != null ? result.getRegions().size() : 0);
                
            } finally {
                // 清理临时文件
                Files.deleteIfExists(tempFile);
            }
            
        } catch (Exception e) {
            log.error("OCR 结构化识别失败：{}", e.getMessage(), e);
            result.setSuccess(false);
            result.setText("识别失败：" + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 解析 hOCR 输出
     * hOCR 格式示例：
     * <span class='ocrx_word' id='word_1_1' title='bbox 100 200 150 220; x_size 20; x_conf 95'>文字</span>
     */
    private void parseHocrOutput(String hocr, OcrResult result) {
        List<TextRegion> regions = new ArrayList<>();
        StringBuilder textBuilder = new StringBuilder();
        
        // 匹配 hOCR 的 word 级别 span 标签
        // 格式：<span class='ocrx_word' ... title='bbox x1 y1 x2 y2; ...'>文字</span>
        java.util.regex.Pattern wordPattern = java.util.regex.Pattern.compile(
            "<span[^>]*class=['\"]ocrx_word['\"][^>]*title=['\"]bbox\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)[^>]*>([^<]*)</span>",
            java.util.regex.Pattern.CASE_INSENSITIVE
        );
        
        java.util.regex.Matcher matcher = wordPattern.matcher(hocr);
        
        while (matcher.find()) {
            int x1 = Integer.parseInt(matcher.group(1));
            int y1 = Integer.parseInt(matcher.group(2));
            int x2 = Integer.parseInt(matcher.group(3));
            int y2 = Integer.parseInt(matcher.group(4));
            String text = matcher.group(5).trim();
            
            if (!text.isEmpty()) {
                TextRegion region = new TextRegion();
                region.setBbox(new int[]{x1, y1, x2, y2});
                region.setText(text);
                region.setConfidence(1.0);
                
                regions.add(region);
                textBuilder.append(text).append("\n");
            }
        }
        
        // 如果没有找到 word 级别的，尝试 line 级别
        if (regions.isEmpty()) {
            java.util.regex.Pattern linePattern = java.util.regex.Pattern.compile(
                "<span[^>]*class=['\"]ocr_line['\"][^>]*title=['\"]bbox\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)[^>]*>([^<]*)</span>",
                java.util.regex.Pattern.CASE_INSENSITIVE
            );
            
            matcher = linePattern.matcher(hocr);
            while (matcher.find()) {
                int x1 = Integer.parseInt(matcher.group(1));
                int y1 = Integer.parseInt(matcher.group(2));
                int x2 = Integer.parseInt(matcher.group(3));
                int y2 = Integer.parseInt(matcher.group(4));
                String text = matcher.group(5).trim();
                
                if (!text.isEmpty()) {
                    TextRegion region = new TextRegion();
                    region.setBbox(new int[]{x1, y1, x2, y2});
                    region.setText(text);
                    region.setConfidence(1.0);
                    
                    regions.add(region);
                    textBuilder.append(text).append("\n");
                }
            }
        }
        
        result.setText(textBuilder.toString());
        result.setRegions(regions);
        result.setSuccess(true);
    }
    
    /**
     * OCR 结果类
     */
    public static class OcrResult {
        private String text;
        private List<TextRegion> regions;
        private boolean success;
        
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        
        public List<TextRegion> getRegions() { return regions; }
        public void setRegions(List<TextRegion> regions) { this.regions = regions; }
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
    }
    
    /**
     * 文字区域
     */
    public static class TextRegion {
        private String text;
        private int[] bbox; // [x1, y1, x2, y2]
        private double confidence;
        
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        
        public int[] getBbox() { return bbox; }
        public void setBbox(int[] bbox) { this.bbox = bbox; }
        
        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }
    }
}
