package com.example.aiframework.agent.tool;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * 翻译工具 - 支持多语言翻译 (使用免费翻译 API)
 */
public class TranslationTool implements Tool {
    
    // 支持的语言代码
    private static final String[] SUPPORTED_LANGS = {
        "zh", "en", "ja", "ko", "fr", "de", "es", "it", "ru", "pt", "ar", "th", "vi"
    };
    
    @Override
    public String getName() {
        return "translate";
    }
    
    @Override
    public String getDescription() {
        return "文本翻译工具，支持中文、英文、日文、韩文、法语、德语等 13 种语言互译";
    }
    
    @Override
    public List<ToolParameter> getParameters() {
        return Arrays.asList(
            ToolParameter.string("text", "要翻译的文本内容", true),
            ToolParameter.string("from", "源语言代码 (zh/en/ja/ko/fr/de/es/it/ru/pt/ar/th/vi)，默认自动检测", false),
            ToolParameter.string("to", "目标语言代码，默认 en", false)
        );
    }
    
    @Override
    public ToolExecutionResult execute(List<ToolParameter> parameters) {
        String text = null;
        String from = "auto";
        String to = "en";
        
        for (ToolParameter param : parameters) {
            String name = param.getName();
            Object value = param.getDefaultValue();
            
            if (value == null) continue;
            
            switch (name) {
                case "text":
                    text = value.toString();
                    break;
                case "from":
                    from = value.toString().toLowerCase();
                    break;
                case "to":
                    to = value.toString().toLowerCase();
                    break;
            }
        }
        
        if (text == null || text.trim().isEmpty()) {
            return ToolExecutionResult.error("要翻译的文本不能为空");
        }
        
        // 验证语言代码
        if (!"auto".equals(from) && !isValidLang(from)) {
            return ToolExecutionResult.error("不支持的源语言：" + from + "，支持的语言：" + String.join(",", SUPPORTED_LANGS));
        }
        if (!isValidLang(to)) {
            return ToolExecutionResult.error("不支持的目标语言：" + to + "，支持的语言：" + String.join(",", SUPPORTED_LANGS));
        }
        
        try {
            // 使用 MyMemory 翻译 API (免费，无需 key)
            String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8);
            String apiUrl = "https://api.mymemory.translated.net/get?q=" + encodedText + 
                           "&langpair=" + from + "|" + to;
            
            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("User-Agent", "TranslationTool/1.0");
            
            int status = conn.getResponseCode();
            if (status != 200) {
                return ToolExecutionResult.error("翻译 API 请求失败，状态码：" + status);
            }
            
            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }
            
            // 解析响应
            String translatedText = parseTranslationResponse(response.toString());
            
            conn.disconnect();
            
            StringBuilder result = new StringBuilder();
            result.append("🌐 翻译结果\n\n");
            result.append("源语言：").append(getLangName(from)).append("\n");
            result.append("目标语言：").append(getLangName(to)).append("\n\n");
            result.append("原文:\n").append(text).append("\n\n");
            result.append("译文:\n").append(translatedText);
            
            return ToolExecutionResult.success(result.toString());
            
        } catch (Exception e) {
            return ToolExecutionResult.error("翻译失败：" + e.getMessage());
        }
    }
    
    /**
     * 解析翻译 API 响应
     */
    private String parseTranslationResponse(String json) {
        try {
            // 提取 responseData.translatedText 字段
            int startIdx = json.indexOf("\"translatedText\"");
            if (startIdx == -1) {
                return "无法解析翻译结果";
            }
            
            startIdx = json.indexOf(":", startIdx) + 1;
            startIdx = json.indexOf("\"", startIdx) + 1;
            int endIdx = json.indexOf("\"", startIdx);
            
            String translated = json.substring(startIdx, endIdx);
            
            // 处理 HTML 实体
            translated = translated.replace("&quot;", "\"")
                                  .replace("&amp;", "&")
                                  .replace("&lt;", "<")
                                  .replace("&gt;", ">")
                                  .replace("&#39;", "'");
            
            return translated;
            
        } catch (Exception e) {
            return "解析失败：" + e.getMessage();
        }
    }
    
    private boolean isValidLang(String lang) {
        for (String supported : SUPPORTED_LANGS) {
            if (supported.equals(lang)) return true;
        }
        return false;
    }
    
    private String getLangName(String code) {
        switch (code) {
            case "zh": return "中文";
            case "en": return "英语";
            case "ja": return "日语";
            case "ko": return "韩语";
            case "fr": return "法语";
            case "de": return "德语";
            case "es": return "西班牙语";
            case "it": return "意大利语";
            case "ru": return "俄语";
            case "pt": return "葡萄牙语";
            case "ar": return "阿拉伯语";
            case "th": return "泰语";
            case "vi": return "越南语";
            case "auto": return "自动检测";
            default: return code;
        }
    }
}
