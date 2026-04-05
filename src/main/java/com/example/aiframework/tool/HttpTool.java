package com.example.aiframework.tool;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * HTTP 请求工具 - 发送 GET/POST/PUT/DELETE 请求
 */
public class HttpTool implements Tool {
    
    @Override
    public String getName() {
        return "http";
    }
    
    @Override
    public String getDescription() {
        return "发送 HTTP 请求，支持 GET/POST/PUT/DELETE 方法，可自定义请求头和请求体";
    }
    
    @Override
    public List<ToolParameter> getParameters() {
        return Arrays.asList(
            ToolParameter.string("url", "请求 URL", true),
            ToolParameter.string("method", "HTTP 方法 (GET/POST/PUT/DELETE)，默认 GET", false),
            ToolParameter.string("headers", "请求头 (JSON 格式)，如：{\"Content-Type\":\"application/json\"}", false),
            ToolParameter.string("body", "请求体 (POST/PUT 时使用)", false),
            ToolParameter.number("timeout", "超时时间 (秒)，默认 30", false)
        );
    }
    
    @Override
    public ToolExecutionResult execute(List<ToolParameter> parameters) {
        String urlStr = null;
        String method = "GET";
        String headers = null;
        String body = null;
        int timeout = 30;
        
        for (ToolParameter param : parameters) {
            String name = param.getName();
            Object value = param.getDefaultValue();
            
            if (value == null) continue;
            
            switch (name) {
                case "url":
                    urlStr = value.toString();
                    break;
                case "method":
                    method = value.toString().toUpperCase();
                    break;
                case "headers":
                    headers = value.toString();
                    break;
                case "body":
                    body = value.toString();
                    break;
                case "timeout":
                    try {
                        timeout = Integer.parseInt(value.toString());
                    } catch (NumberFormatException e) {
                        timeout = 30;
                    }
                    break;
            }
        }
        
        if (urlStr == null || urlStr.trim().isEmpty()) {
            return ToolExecutionResult.error("URL 不能为空");
        }
        
        // 安全校验 URL
        if (!urlStr.startsWith("http://") && !urlStr.startsWith("https://")) {
            return ToolExecutionResult.error("URL 必须以 http:// 或 https:// 开头");
        }
        
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            
            conn.setRequestMethod(method);
            conn.setConnectTimeout(timeout * 1000);
            conn.setReadTimeout(timeout * 1000);
            conn.setRequestProperty("User-Agent", "HttpTool/1.0");
            conn.setRequestProperty("Accept", "*/*");
            
            // 设置自定义请求头
            if (headers != null && !headers.trim().isEmpty()) {
                parseAndSetHeaders(conn, headers);
            }
            
            // 设置请求体 (POST/PUT)
            if (("POST".equals(method) || "PUT".equals(method)) && body != null) {
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json");
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = body.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
            }
            
            int status = conn.getResponseCode();
            String response = readResponse(conn, status);
            
            conn.disconnect();
            
            StringBuilder result = new StringBuilder();
            result.append("状态码：").append(status).append("\n");
            result.append("响应头：\n");
            conn.getHeaderFields().forEach((k, v) -> {
                if (k != null) {
                    result.append("  ").append(k).append(": ").append(String.join(", ", v)).append("\n");
                }
            });
            result.append("\n响应体:\n").append(response);
            
            return ToolExecutionResult.success(result.toString());
            
        } catch (Exception e) {
            return ToolExecutionResult.error("HTTP 请求失败：" + e.getMessage());
        }
    }
    
    /**
     * 解析并设置请求头
     */
    private void parseAndSetHeaders(HttpURLConnection conn, String headersJson) {
        try {
            // 简单解析 JSON 格式的请求头
            String cleaned = headersJson.replaceAll("[{}\"]", "");
            String[] pairs = cleaned.split(",");
            for (String pair : pairs) {
                String[] kv = pair.split(":", 2);
                if (kv.length == 2) {
                    String key = kv[0].trim();
                    String value = kv[1].trim();
                    conn.setRequestProperty(key, value);
                }
            }
        } catch (Exception e) {
            // 忽略解析错误
        }
    }
    
    /**
     * 读取响应
     */
    private String readResponse(HttpURLConnection conn, int status) {
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                    status >= 400 ? conn.getErrorStream() : conn.getInputStream(),
                    StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line).append("\n");
            }
        } catch (Exception e) {
            return "读取响应失败：" + e.getMessage();
        }
        return response.toString();
    }
}
