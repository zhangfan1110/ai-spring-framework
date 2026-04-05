package com.example.aiframework.service;

import com.example.aiframework.dto.LocalModelService_ModelResponse;
import com.example.aiframework.dto.LocalModelService_OllamaChatRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * 本地大模型服务（支持 Ollama、LM Studio 等）
 */
@Service
public class LocalModelService {
    
    private static final Logger log = LoggerFactory.getLogger(LocalModelService.class);
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Value("${langchain4j.ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;
    
    @Value("${langchain4j.ollama.model-name:deepseek-r1}")
    private String ollamaModel;
    
    @Value("${langchain4j.ollama.temperature:0.7}")
    private double temperature;
    
    @Value("${langchain4j.ollama.enabled:false}")
    private boolean ollamaEnabled;
    
    /**
     * 流式响应回调接口
     */
    public interface StreamCallback {
        void onToken(String token);
        void onComplete(String fullContent);
        void onError(Throwable error);
    }
    
    /**
     * 使用 Ollama 进行聊天
     */
    public LocalModelService_ModelResponse chat(String prompt) {
        return chat(ollamaModel, prompt, temperature);
    }
    
    /**
     * 使用指定模型聊天
     */
    public LocalModelService_ModelResponse chat(String model, String prompt, double temperature) {
        log.info("本地模型聊天 - 模型：{}, 提示词长度：{}", model, prompt.length());
        
        long startTime = System.currentTimeMillis();
        LocalModelService_ModelResponse response = new LocalModelService_ModelResponse();
        response.setMetadata(new HashMap<>());
        
        try {
            if (!ollamaEnabled) {
                throw new RuntimeException("Ollama 未启用");
            }
            
            // 构建请求
            LocalModelService_OllamaChatRequest request = new LocalModelService_OllamaChatRequest(model, prompt, temperature, false);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<LocalModelService_OllamaChatRequest> entity = new HttpEntity<>(request, headers);
            
            // 调用 Ollama API
            String url = ollamaBaseUrl + "/api/chat";
            ResponseEntity<String> result = restTemplate.postForEntity(url, entity, String.class);
            
            if (result.getStatusCode() == HttpStatus.OK) {
                JsonNode jsonNode = objectMapper.readTree(result.getBody());
                
                // 解析响应
                JsonNode messageNode = jsonNode.get("message");
                String content = messageNode != null && messageNode.get("content") != null 
                    ? messageNode.get("content").asText() : "";
                
                response.setContent(content);
                response.setModel(model);
                
                // 元数据
                response.getMetadata().put("done", jsonNode.get("done").asBoolean());
                
                JsonNode totalDurationNode = jsonNode.get("total_duration");
                if (totalDurationNode != null) {
                    response.getMetadata().put("total_duration", totalDurationNode.asLong());
                }
                
                long duration = System.currentTimeMillis() - startTime;
                response.setDuration(duration);
                
                log.info("本地模型聊天完成 - 耗时：{}ms, 响应长度：{}", duration, content.length());
            } else {
                throw new RuntimeException("Ollama 响应错误：" + result.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("本地模型聊天失败：{}", e.getMessage(), e);
            response.setContent("错误：" + e.getMessage());
            response.setDuration(System.currentTimeMillis() - startTime);
        }
        
        return response;
    }
    
    /**
     * 流式聊天
     */
    public void chatStream(String prompt, StreamCallback callback) {
        chatStream(ollamaModel, prompt, temperature, callback);
    }
    
    /**
     * 流式聊天（指定模型）
     */
    public void chatStream(String model, String prompt, double temperature, StreamCallback callback) {
        log.info("本地模型流式聊天 - 模型：{}, 提示词长度：{}", model, prompt.length());
        
        try {
            if (!ollamaEnabled) {
                throw new RuntimeException("Ollama 未启用");
            }
            
            // 构建请求
            LocalModelService_OllamaChatRequest request = new LocalModelService_OllamaChatRequest(model, prompt, temperature, true);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<LocalModelService_OllamaChatRequest> entity = new HttpEntity<>(request, headers);
            
            // 调用 Ollama 流式 API - 使用 RestTemplate 的简单方式
            String url = ollamaBaseUrl + "/api/chat";
            
            // 使用 ProcessBuilder 调用 curl 进行流式请求
            ProcessBuilder pb = new ProcessBuilder(
                "curl", "-s", "-N", "-X", "POST", url,
                "-H", "Content-Type: application/json",
                "-d", objectMapper.writeValueAsString(request)
            );
            pb.redirectErrorStream(true);
            
            Process process = pb.start();
            StringBuilder fullContent = new StringBuilder();
            
            try (Scanner scanner = new Scanner(process.getInputStream())) {
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    if (line.trim().isEmpty()) continue;
                    
                    try {
                        JsonNode jsonNode = objectMapper.readTree(line);
                        
                        JsonNode messageNode = jsonNode.get("message");
                        if (messageNode != null && messageNode.get("content") != null) {
                            String token = messageNode.get("content").asText();
                            fullContent.append(token);
                            
                            // 回调
                            callback.onToken(token);
                        }
                        
                        // 完成
                        if (jsonNode.get("done") != null && jsonNode.get("done").asBoolean()) {
                            callback.onComplete(fullContent.toString());
                            break;
                        }
                    } catch (Exception e) {
                        log.warn("解析流式响应失败：{}", e.getMessage());
                    }
                }
            }
            
            process.waitFor();
            
        } catch (Exception e) {
            log.error("本地模型流式聊天失败：{}", e.getMessage(), e);
            callback.onError(e);
        }
    }
    
    /**
     * 获取可用模型列表
     */
    public List<String> listModels() {
        log.info("获取本地模型列表");
        
        List<String> models = new ArrayList<>();
        
        try {
            if (!ollamaEnabled) {
                log.warn("Ollama 未启用");
                return models;
            }
            
            String url = ollamaBaseUrl + "/api/tags";
            ResponseEntity<String> result = restTemplate.getForEntity(url, String.class);
            
            if (result.getStatusCode() == HttpStatus.OK) {
                JsonNode jsonNode = objectMapper.readTree(result.getBody());
                JsonNode modelsNode = jsonNode.get("models");
                
                if (modelsNode != null && modelsNode.isArray()) {
                    for (JsonNode modelNode : modelsNode) {
                        String name = modelNode.get("name").asText();
                        models.add(name);
                    }
                }
                
                log.info("获取到 {} 个本地模型", models.size());
            }
            
        } catch (Exception e) {
            log.error("获取模型列表失败：{}", e.getMessage(), e);
        }
        
        return models;
    }
    
    /**
     * 检查模型是否存在
     */
    public boolean hasModel(String modelName) {
        List<String> models = listModels();
        return models.stream().anyMatch(m -> m.equals(modelName) || m.startsWith(modelName + ":"));
    }
    
    /**
     * 拉取模型
     */
    public Map<String, Object> pullModel(String modelName) {
        log.info("拉取模型：{}", modelName);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (!ollamaEnabled) {
                throw new RuntimeException("Ollama 未启用");
            }
            
            // 构建请求
            Map<String, String> pullRequest = Collections.singletonMap("name", modelName);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // 调用 Ollama API - 简化版本，只返回成功状态
            String url = ollamaBaseUrl + "/api/pull";
            
            HttpEntity<Map<String, String>> pullEntity = new HttpEntity<>(pullRequest, headers);
            
            // 非流式调用，直接返回
            restTemplate.postForEntity(url, pullEntity, String.class);
            
            result.put("status", "success");
            result.put("message", "模型拉取请求已提交：" + modelName);
            result.put("note", "拉取过程在后台进行，请稍后检查模型列表");
            
        } catch (Exception e) {
            log.error("拉取模型失败：{}", e.getMessage(), e);
            result.put("status", "error");
            result.put("message", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 删除模型
     */
    public Map<String, Object> deleteModel(String modelName) {
        log.info("删除模型：{}", modelName);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (!ollamaEnabled) {
                throw new RuntimeException("Ollama 未启用");
            }
            
            // 构建请求
            Map<String, String> request = Collections.singletonMap("name", modelName);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);
            
            // 调用 Ollama API
            String url = ollamaBaseUrl + "/api/delete";
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                result.put("status", "success");
                result.put("message", "模型已删除：" + modelName);
            } else {
                result.put("status", "error");
                result.put("message", "删除失败：" + response.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("删除模型失败：{}", e.getMessage(), e);
            result.put("status", "error");
            result.put("message", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 生成嵌入向量
     */
    public List<Float> embed(String text) {
        return embed(ollamaModel, text);
    }
    
    /**
     * 生成嵌入向量（指定模型）
     */
    public List<Float> embed(String model, String text) {
        log.info("生成本地嵌入向量 - 模型：{}, 文本长度：{}", model, text.length());
        
        try {
            if (!ollamaEnabled) {
                throw new RuntimeException("Ollama 未启用");
            }
            
            // 构建请求
            Map<String, String> request = new HashMap<>();
            request.put("model", model);
            request.put("prompt", text);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);
            
            // 调用 Ollama API
            String url = ollamaBaseUrl + "/api/embeddings";
            ResponseEntity<String> result = restTemplate.postForEntity(url, entity, String.class);
            
            if (result.getStatusCode() == HttpStatus.OK) {
                JsonNode jsonNode = objectMapper.readTree(result.getBody());
                JsonNode embeddingNode = jsonNode.get("embedding");
                
                if (embeddingNode != null && embeddingNode.isArray()) {
                    List<Float> embedding = new ArrayList<>();
                    for (JsonNode node : embeddingNode) {
                        embedding.add((float) node.asDouble());
                    }
                    return embedding;
                }
            }
            
            throw new RuntimeException("嵌入向量生成失败");
            
        } catch (Exception e) {
            log.error("生成嵌入向量失败：{}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
}
