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
import java.util.Base64;
import java.util.UUID;

/**
 * TTS 语音合成服务 (Text-to-Speech)
 * 支持多种语音合成引擎
 */
@Service
public class TextToSpeechService {
    
    private static final Logger log = LoggerFactory.getLogger(TextToSpeechService.class);
    
    @Value("${tts.enabled:true}")
    private boolean ttsEnabled;
    
    @Value("${tts.provider:edge}")
    private String provider; // edge/azure/google/baidu
    
    @Value("${tts.voice:zh-CN-Shaanxi-XiaoniNeural}")
    private String voice; // 默认语音
    
    @Value("${tts.rate:1.0}")
    private double rate; // 语速 0.5-2.0
    
    @Value("${tts.pitch:1.0}")
    private double pitch; // 音调 0.5-2.0
    
    @Value("${tts.output.path:./uploads/audio/tts}")
    private String outputPath;
    
    /**
     * 文本转语音
     * @param text 要转换的文本
     * @return 生成的音频文件路径
     */
    public String synthesize(String text) {
        return synthesize(text, voice, rate, pitch);
    }
    
    /**
     * 文本转语音 (自定义参数)
     * @param text 要转换的文本
     * @param voice 语音 ID
     * @param rate 语速 (0.5-2.0)
     * @param pitch 音调 (0.5-2.0)
     * @return 生成的音频文件路径
     */
    public String synthesize(String text, String voice, double rate, double pitch) {
        log.info("TTS 语音合成：text={}, voice={}, rate={}, pitch={}", 
            text.substring(0, Math.min(50, text.length())), voice, rate, pitch);
        
        if (!ttsEnabled) {
            throw new IllegalStateException("TTS 服务未启用");
        }
        
        try {
            switch (provider) {
                case "edge":
                    return synthesizeWithEdge(text, voice, rate, pitch);
                case "azure":
                    return synthesizeWithAzure(text, voice, rate, pitch);
                case "google":
                    return synthesizeWithGoogle(text, voice, rate, pitch);
                case "baidu":
                    return synthesizeWithBaidu(text, voice, rate, pitch);
                default:
                    return synthesizeWithEdge(text, voice, rate, pitch);
            }
        } catch (Exception e) {
            log.error("TTS 合成失败：{}", e.getMessage(), e);
            throw new RuntimeException("语音合成失败：" + e.getMessage());
        }
    }
    
    /**
     * 使用 Edge TTS (免费)
     */
    private String synthesizeWithEdge(String text, String voice, double rate, double pitch) throws Exception {
        log.info("使用 Edge TTS 合成：voice={}, rate={}, pitch={}", voice, rate, pitch);
        
        // 检查 edge-tts 是否安装
        if (!isEdgeTtsInstalled()) {
            throw new IllegalStateException("edge-tts 未安装，请运行：pip install edge-tts");
        }
        
        // 生成输出文件名
        String filename = UUID.randomUUID().toString() + ".mp3";
        Path outputPath = Paths.get(this.outputPath, filename);
        Files.createDirectories(outputPath.getParent());
        
        // 构建命令 - 关键：修复 rate 和 pitch 格式
        ProcessBuilder pb = new ProcessBuilder(
            "edge-tts",
            "--voice", voice,
            "--text", text,
            "--rate", formatRate(rate),
            "--pitch", formatPitch(pitch),
            "--write-media", outputPath.toString()
        );
        
        pb.redirectErrorStream(true);
        Process process = pb.start();
        
        // 等待完成
        if (!process.waitFor(2, java.util.concurrent.TimeUnit.MINUTES)) {
            process.destroyForcibly();
            throw new RuntimeException("TTS 合成超时");
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
        
        if (!Files.exists(outputPath)) {
            throw new RuntimeException("TTS 合成失败：" + output.toString());
        }
        
        log.info("TTS 合成完成：{}", outputPath);
        return outputPath.toString();
    }
    
    /**
     * 使用 Azure TTS
     */
    private String synthesizeWithAzure(String text, String voice, double rate, double pitch) throws Exception {
        log.info("使用 Azure TTS 合成");
        
        // TODO: 集成 Azure Cognitive Services TTS API
        // 需要配置 AZURE_TTS_KEY 和 AZURE_TTS_REGION
        
        String apiKey = System.getenv("AZURE_TTS_KEY");
        String region = System.getenv("AZURE_TTS_REGION");
        
        if (apiKey == null || region == null) {
            throw new IllegalStateException("未配置 Azure TTS API Key 和 Region");
        }
        
        // 构建请求
        URL url = new URL("https://" + region + ".tts.speech.microsoft.com/cognitiveservices/v1");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Ocp-Apim-Subscription-Key", apiKey);
        conn.setRequestProperty("Content-Type", "application/ssml+xml");
        conn.setRequestProperty("X-Microsoft-OutputFormat", "audio-16khz-128kbitrate-mono-mp3");
        conn.setDoOutput(true);
        
        // SSML 请求体
        String ssml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<speak version=\"1.0\" xmlns=\"http://www.w3.org/2001/10/synthesis\" xml:lang=\"zh-CN\">\n" +
            "  <voice name=\"" + voice + "\">\n" +
            "    <prosody rate=\"" + rate + "\" pitch=\"" + pitch + "\">" + text + "</prosody>\n" +
            "  </voice>\n" +
            "</speak>";
        
        try (OutputStream os = conn.getOutputStream()) {
            os.write(ssml.getBytes("UTF-8"));
        }
        
        int status = conn.getResponseCode();
        if (status == 200) {
            // 保存音频
            String filename = UUID.randomUUID().toString() + ".mp3";
            Path outputPath = Paths.get(this.outputPath, filename);
            Files.createDirectories(outputPath.getParent());
            
            try (InputStream is = conn.getInputStream()) {
                Files.copy(is, outputPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            
            return outputPath.toString();
        } else {
            throw new RuntimeException("Azure TTS 请求失败：" + status);
        }
    }
    
    /**
     * 使用 Google TTS
     */
    private String synthesizeWithGoogle(String text, String voice, double rate, double pitch) throws Exception {
        log.info("使用 Google TTS 合成");
        
        // TODO: 集成 Google Cloud TTS API
        
        throw new UnsupportedOperationException("Google TTS 待实现");
    }
    
    /**
     * 使用百度 TTS
     */
    private String synthesizeWithBaidu(String text, String voice, double rate, double pitch) throws Exception {
        log.info("使用百度 TTS 合成");
        
        // TODO: 集成百度语音合成 API
        
        throw new UnsupportedOperationException("百度 TTS 待实现");
    }
    
    /**
     * 格式化语速参数
     * edge-tts 要求：+X% 或 -X%，必须有正负号
     */
    private String formatRate(double rate) {
        // rate=1.0 表示正常速度，转换为 +0%
        int percent = (int) Math.round((rate - 1.0) * 100);
        return (percent >= 0 ? "+" : "") + percent + "%";
    }
    
    /**
     * 格式化音调参数
     * edge-tts 要求：+XHz 或 -XHz，必须有正负号
     */
    private String formatPitch(double pitch) {
        // pitch=1.0 表示正常音调，转换为 +0Hz
        int hz = (int) Math.round((pitch - 1.0) * 100);
        return (hz >= 0 ? "+" : "") + hz + "Hz";
    }
    
    /**
     * 检查 edge-tts 是否安装
     */
    private boolean isEdgeTtsInstalled() {
        try {
            ProcessBuilder pb = new ProcessBuilder("edge-tts", "--version");
            Process process = pb.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            log.debug("edge-tts 检查失败：{}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 获取支持的语音列表
     */
    public java.util.List<String> getSupportedVoices() {
        // 常用中文语音
        return java.util.Arrays.asList(
            "zh-CN-XiaoxiaoNeural",      // 女声，温暖
            "zh-CN-YunxiNeural",         // 男声，沉稳
            "zh-CN-YunyangNeural",       // 男声，专业
            "zh-CN-XiaoyiNeural",        // 女声，活泼
            "zh-CN-Liaoning-XiaobeiNeural", // 东北话
            "zh-CN-Shaanxi-XiaoniNeural",   // 陕西话
            "zh-HK-HiuMaanNeural",       // 粤语女声
            "zh-TW-HsiaoChenNeural"      // 台湾女声
        );
    }
    
    /**
     * 音频转 Base64 (用于前端播放)
     */
    public String audioToBase64(String audioPath) throws IOException {
        Path path = Paths.get(audioPath);
        byte[] audioBytes = Files.readAllBytes(path);
        return Base64.getEncoder().encodeToString(audioBytes);
    }
}
