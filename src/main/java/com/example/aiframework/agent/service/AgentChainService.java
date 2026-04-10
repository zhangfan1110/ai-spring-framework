package com.example.aiframework.agent.service;

import com.example.aiframework.agent.tool.Tool;
import com.example.aiframework.agent.tool.ToolManager;
import com.example.aiframework.agent.tool.ToolParameter;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.*;

/**
 * Agent 调用链服务
 * 负责工具调用链的生成、解析和执行
 * 
 * Controller 只负责接口参数校验和响应封装，所有业务逻辑都在 Service 中
 */
@Service
public class AgentChainService {
    
    private static final Logger log = LoggerFactory.getLogger(AgentChainService.class);
    
    @Resource
    private ChatLanguageModel chatModel;
    
    @Autowired
    private ToolManager toolManager;
    
    /**
     * 根据任务描述自动生成工具调用链
     * @param task 任务描述
     * @return 调用链结构（包含 nodes 列表）
     */
    public Map<String, Object> generateChain(String task) {
        log.info("生成调用链：task={}", task);
        
        try {
            // 使用 LLM 生成调用链
            String chainJson = generateChainWithLLM(task);
            
            // 解析 LLM 返回的 JSON
            return parseChainJson(chainJson);
            
        } catch (Exception e) {
            log.error("生成调用链失败：{}", e.getMessage(), e);
            throw new RuntimeException("调用链生成失败：" + e.getMessage(), e);
        }
    }
    
    /**
     * 使用 LLM 生成工具调用链
     */
    private String generateChainWithLLM(String task) {
        // 获取可用工具列表
        String toolsDescription = buildToolsDescription();
        
        // 构建 Prompt
        String prompt = buildChainGenerationPrompt(task, toolsDescription);
        
        log.debug("LLM 生成调用链 Prompt 长度：{}", prompt.length());
        
        // 调用 LLM
        String response = chatModel.generate(prompt);
        log.debug("LLM 响应：{}", response);
        
        // 提取 JSON 部分
        return extractJsonFromResponse(response);
    }
    
    /**
     * 构建工具描述
     */
    private String buildToolsDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append("可用工具列表:\n\n");
        
        List<Tool> tools = toolManager.getAllTools();
        for (Tool tool : tools) {
            sb.append("工具名称：").append(tool.getName()).append("\n");
            sb.append("描述：").append(tool.getDescription()).append("\n");
            sb.append("参数:\n");
            
            for (ToolParameter param : tool.getParameters()) {
                sb.append("  - ").append(param.getName())
                  .append(" (").append(param.getType()).append("): ")
                  .append(param.getDescription());
                if (param.isRequired()) {
                    sb.append(" [必需]");
                }
                sb.append("\n");
            }
            sb.append("\n");
        }
        
        return sb.toString();
    }
    
    /**
     * 构建调用链生成 Prompt
     */
    private String buildChainGenerationPrompt(String task, String toolsDescription) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个智能工作流规划师。请根据用户任务，规划一个工具调用链。\n\n");
        sb.append("任务：").append(task).append("\n\n");
        sb.append(toolsDescription);
        sb.append("\n请按以下 JSON 格式返回调用链（只返回 JSON，不要其他内容）：\n\n");
        sb.append("{\n");
        sb.append("  \"description\": \"调用链描述\",\n");
        sb.append("  \"nodes\": [\n");
        sb.append("    {\n");
        sb.append("      \"stepId\": \"step1\",\n");
        sb.append("      \"toolName\": \"工具名称\",\n");
        sb.append("      \"parameters\": {\"参数名\": \"参数值\"},\n");
        sb.append("      \"dependsOn\": null,\n");
        sb.append("      \"parallel\": false\n");
        sb.append("    }\n");
        sb.append("  ]\n");
        sb.append("}\n\n");
        sb.append("规则：\n");
        sb.append("1. stepId 必须唯一，按 step1, step2, step3 命名\n");
        sb.append("2. dependsOn 填写依赖的前置 stepId，没有依赖则为 null\n");
        sb.append("3. 可以使用 ${stepId_result} 引用前置步骤的结果\n");
        sb.append("4. parallel 标记是否可以并行执行（独立步骤可设为 true）\n");
        sb.append("5. 只使用上面列出的可用工具\n");
        sb.append("6. 步骤数量控制在 3-8 个\n\n");
        sb.append("开始生成：");
        
        return sb.toString();
    }
    
    /**
     * 从 LLM 响应中提取 JSON
     */
    private String extractJsonFromResponse(String response) {
        // 尝试提取 ```json ... ``` 包裹的内容
        int startIdx = response.indexOf("```json");
        if (startIdx == -1) {
            startIdx = response.indexOf("{");
        } else {
            startIdx = response.indexOf("{", startIdx);
        }
        
        if (startIdx == -1) {
            log.warn("无法从响应中提取 JSON: {}", response);
            throw new RuntimeException("LLM 返回格式错误，未找到 JSON");
        }
        
        int endIdx = response.lastIndexOf("}");
        if (endIdx == -1 || endIdx <= startIdx) {
            log.warn("JSON 结束标记缺失：{}", response);
            throw new RuntimeException("LLM 返回格式错误，JSON 不完整");
        }
        
        return response.substring(startIdx, endIdx + 1);
    }
    
    /**
     * 解析调用链 JSON
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> parseChainJson(String json) {
        Map<String, Object> result = new HashMap<>();
        
        // 提取 description
        result.put("description", extractStringValue(json, "description"));
        
        // 提取 nodes
        List<Map<String, Object>> nodes = new ArrayList<>();
        int nodesStart = json.indexOf("\"nodes\"");
        if (nodesStart != -1) {
            int arrayStart = json.indexOf("[", nodesStart);
            int arrayEnd = findMatchingBracket(json, arrayStart);
            
            String nodesJson = json.substring(arrayStart + 1, arrayEnd);
            
            // 解析每个节点
            int nodeStart = 0;
            while ((nodeStart = nodesJson.indexOf("{", nodeStart)) != -1) {
                int nodeEnd = findMatchingBracket(nodesJson, nodeStart);
                String nodeJson = nodesJson.substring(nodeStart, nodeEnd + 1);
                
                Map<String, Object> node = parseNodeJson(nodeJson);
                nodes.add(node);
                
                nodeStart = nodeEnd + 1;
            }
        }
        
        result.put("nodes", nodes);
        return result;
    }
    
    /**
     * 解析单个节点 JSON
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseNodeJson(String json) {
        Map<String, Object> node = new HashMap<>();
        
        node.put("stepId", extractStringValue(json, "stepId"));
        node.put("toolName", extractStringValue(json, "toolName"));
        
        String dependsOn = extractStringValue(json, "dependsOn");
        node.put("dependsOn", "null".equals(dependsOn) ? null : dependsOn);
        
        String parallelStr = extractStringValue(json, "parallel");
        node.put("parallel", "true".equals(parallelStr));
        
        // 提取 parameters
        Map<String, String> parameters = new HashMap<>();
        int paramsStart = json.indexOf("\"parameters\"");
        if (paramsStart != -1) {
            int objStart = json.indexOf("{", paramsStart);
            int objEnd = findMatchingBracket(json, objStart);
            String paramsJson = json.substring(objStart + 1, objEnd);
            
            // 简单解析键值对
            String[] pairs = paramsJson.split(",");
            for (String pair : pairs) {
                String[] kv = pair.split(":");
                if (kv.length >= 2) {
                    String key = kv[0].trim().replace("\"", "");
                    String value = kv[1].trim().replace("\"", "");
                    parameters.put(key, value);
                }
            }
        }
        node.put("parameters", parameters);
        
        return node;
    }
    
    /**
     * 提取 JSON 字符串值
     */
    private String extractStringValue(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIdx = json.indexOf(searchKey);
        if (keyIdx == -1) return null;
        
        int valueStart = json.indexOf(":", keyIdx) + 1;
        
        // 跳过空白
        while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart))) {
            valueStart++;
        }
        
        if (valueStart >= json.length()) return null;
        
        // 字符串值
        if (json.charAt(valueStart) == '"') {
            valueStart++;
            int valueEnd = json.indexOf("\"", valueStart);
            return valueEnd > valueStart ? json.substring(valueStart, valueEnd) : null;
        }
        
        // null 或布尔值
        int valueEnd = valueStart;
        while (valueEnd < json.length() && json.charAt(valueEnd) != ',' && json.charAt(valueEnd) != '}') {
            valueEnd++;
        }
        return json.substring(valueStart, valueEnd).trim();
    }
    
    /**
     * 查找匹配的括号位置
     */
    private int findMatchingBracket(String json, int startIdx) {
        if (startIdx < 0 || startIdx >= json.length()) {
            return json.length() - 1;
        }
        
        if (json.charAt(startIdx) == '{') {
            int count = 1;
            for (int i = startIdx + 1; i < json.length(); i++) {
                if (json.charAt(i) == '{') count++;
                else if (json.charAt(i) == '}') {
                    count--;
                    if (count == 0) return i;
                }
            }
        } else if (json.charAt(startIdx) == '[') {
            int count = 1;
            for (int i = startIdx + 1; i < json.length(); i++) {
                if (json.charAt(i) == '[') count++;
                else if (json.charAt(i) == ']') {
                    count--;
                    if (count == 0) return i;
                }
            }
        }
        return json.length() - 1;
    }
}
