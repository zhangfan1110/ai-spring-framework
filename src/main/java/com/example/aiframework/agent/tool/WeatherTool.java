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
 * 天气查询工具 - 基于 wttr.in 免费天气 API
 */
public class WeatherTool implements Tool {
    
    private static final String WEATHER_API = "https://wttr.in/";
    
    @Override
    public String getName() {
        return "weather";
    }
    
    @Override
    public String getDescription() {
        return "查询指定城市的天气预报，支持中文城市名";
    }
    
    @Override
    public List<ToolParameter> getParameters() {
        return Arrays.asList(
            ToolParameter.string("city", "城市名称，如：北京、上海、New York", true),
            ToolParameter.string("days", "预报天数 (1-3)，默认 1", false)
        );
    }
    
    @Override
    public ToolExecutionResult execute(List<ToolParameter> parameters) {
        String city = null;
        int days = 1;
        
        for (ToolParameter param : parameters) {
            String name = param.getName();
            Object value = param.getDefaultValue();
            
            if ("city".equals(name) && value != null) {
                city = value.toString();
            } else if ("days".equals(name) && value != null) {
                try {
                    days = Math.max(1, Math.min(3, Integer.parseInt(value.toString())));
                } catch (NumberFormatException e) {
                    days = 1;
                }
            }
        }
        
        if (city == null || city.trim().isEmpty()) {
            return ToolExecutionResult.error("城市名称不能为空");
        }
        
        try {
            // 构建 API URL
            String encodedCity = URLEncoder.encode(city.trim(), StandardCharsets.UTF_8);
            String urlStr = WEATHER_API + encodedCity + "?format=j1&num_of_days=" + days;
            
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("User-Agent", "WeatherTool/1.0");
            
            int status = conn.getResponseCode();
            if (status != 200) {
                return ToolExecutionResult.error("天气 API 请求失败，状态码：" + status);
            }
            
            // 读取响应
            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }
            
            // 解析 JSON 响应
            String weatherInfo = parseWeatherResponse(response.toString(), city, days);
            
            conn.disconnect();
            return ToolExecutionResult.success(weatherInfo);
            
        } catch (Exception e) {
            return ToolExecutionResult.error("天气查询失败：" + e.getMessage());
        }
    }
    
    /**
     * 解析天气 API 响应
     */
    private String parseWeatherResponse(String json, String city, int days) {
        try {
            // 简单解析 JSON 提取关键信息
            String currentTemp = extractJsonValue(json, "temp_C");
            String weatherDesc = extractJsonValue(json, "weatherDesc");
            String humidity = extractJsonValue(json, "humidity");
            String windSpeed = extractJsonValue(json, "windspeedKmph");
            String feelsLike = extractJsonValue(json, "FeelsLikeC");
            
            StringBuilder sb = new StringBuilder();
            sb.append("📍 城市：").append(city).append("\n");
            sb.append("🌡️ 当前温度：").append(currentTemp).append("°C");
            if (!feelsLike.isEmpty()) {
                sb.append(" (体感：").append(feelsLike).append("°C)");
            }
            sb.append("\n");
            sb.append("🌤️ 天气：").append(weatherDesc).append("\n");
            sb.append("💧 湿度：").append(humidity).append("%\n");
            sb.append("💨 风速：").append(windSpeed).append(" km/h\n");
            
            return sb.toString();
            
        } catch (Exception e) {
            return "天气数据解析失败，原始数据：" + json.substring(0, Math.min(200, json.length()));
        }
    }
    
    /**
     * 简单提取 JSON 值
     */
    private String extractJsonValue(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*\"?([^,\"\\}]+)\"?";
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(pattern).matcher(json);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }
}
