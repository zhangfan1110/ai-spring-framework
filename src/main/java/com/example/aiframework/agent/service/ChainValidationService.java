package com.example.aiframework.agent.service;

import com.example.aiframework.agent.tool.Tool;
import com.example.aiframework.agent.tool.ToolManager;
import com.example.aiframework.agent.tool.ToolParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 调用链验证服务
 * 验证工具调用链的合法性、完整性、可执行性
 */
@Service
public class ChainValidationService {
    
    private static final Logger log = LoggerFactory.getLogger(ChainValidationService.class);
    
    @Autowired
    private ToolManager toolManager;
    
    /**
     * 验证结果
     */
    public static class ValidationResult {
        private boolean valid;
        private List<String> errors;
        private List<String> warnings;
        private int nodeCount;
        private int estimatedDuration;
        private Map<String, Object> details;
        
        public ValidationResult() {
            this.valid = true;
            this.errors = new ArrayList<>();
            this.warnings = new ArrayList<>();
            this.details = new ConcurrentHashMap<>();
        }
        
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        
        public List<String> getErrors() { return errors; }
        public void setErrors(List<String> errors) { this.errors = errors; }
        
        public List<String> getWarnings() { return warnings; }
        public void setWarnings(List<String> warnings) { this.warnings = warnings; }
        
        public int getNodeCount() { return nodeCount; }
        public void setNodeCount(int nodeCount) { this.nodeCount = nodeCount; }
        
        public int getEstimatedDuration() { return estimatedDuration; }
        public void setEstimatedDuration(int estimatedDuration) { this.estimatedDuration = estimatedDuration; }
        
        public Map<String, Object> getDetails() { return details; }
        public void setDetails(Map<String, Object> details) { this.details = details; }
        
        public void addError(String error) {
            this.errors.add(error);
            this.valid = false;
        }
        
        public void addWarning(String warning) {
            this.warnings.add(warning);
        }
    }
    
    /**
     * 验证调用链
     * @param chain 调用链数据（包含 nodes 列表）
     * @return 验证结果
     */
    @SuppressWarnings("unchecked")
    public ValidationResult validateChain(Map<String, Object> chain) {
        log.info("验证调用链");
        
        ValidationResult result = new ValidationResult();
        
        if (chain == null) {
            result.addError("调用链不能为空");
            return result;
        }
        
        // 获取节点列表
        List<Map<String, Object>> nodes = (List<Map<String, Object>>) chain.get("nodes");
        if (nodes == null || nodes.isEmpty()) {
            result.addError("调用链不能为空，至少需要一个步骤");
            return result;
        }
        
        result.setNodeCount(nodes.size());
        
        // 1. 检查节点数量
        if (nodes.size() > 20) {
            result.addWarning("调用链过长（" + nodes.size() + " 个步骤），建议拆分为多个调用链");
        }
        
        // 2. 收集所有 stepId
        Set<String> stepIds = new HashSet<>();
        Map<String, Map<String, Object>> nodeMap = new HashMap<>();
        
        for (Map<String, Object> node : nodes) {
            String stepId = (String) node.get("stepId");
            if (stepId == null || stepId.trim().isEmpty()) {
                result.addError("存在节点缺少 stepId");
                continue;
            }
            
            if (stepIds.contains(stepId)) {
                result.addError("stepId 重复：" + stepId);
            } else {
                stepIds.add(stepId);
                nodeMap.put(stepId, node);
            }
        }
        
        // 3. 验证每个节点
        int totalEstimatedTime = 0;
        for (Map<String, Object> node : nodes) {
            ValidationResult nodeResult = validateNode(node, stepIds);
            result.getErrors().addAll(nodeResult.getErrors());
            result.getWarnings().addAll(nodeResult.getWarnings());
            
            // 估算耗时
            totalEstimatedTime += estimateNodeDuration(node);
        }
        
        result.setEstimatedDuration(totalEstimatedTime);
        
        // 4. 检查循环依赖
        if (hasCircularDependency(nodes)) {
            result.addError("检测到循环依赖，请检查 dependsOn 配置");
        }
        
        // 5. 检查孤立节点
        checkOrphanNodes(nodes, result);
        
        // 6. 填充详细信息
        result.getDetails().put("totalNodes", nodes.size());
        result.getDetails().put("validNodes", stepIds.size());
        result.getDetails().put("hasErrors", !result.getErrors().isEmpty());
        result.getDetails().put("hasWarnings", !result.getWarnings().isEmpty());
        
        log.info("验证完成：valid={}, errors={}, warnings={}", 
            result.isValid(), result.getErrors().size(), result.getWarnings().size());
        
        return result;
    }
    
    /**
     * 验证单个节点
     */
    @SuppressWarnings("unchecked")
    private ValidationResult validateNode(Map<String, Object> node, Set<String> allStepIds) {
        ValidationResult result = new ValidationResult();
        
        String stepId = (String) node.get("stepId");
        String toolName = (String) node.get("toolName");
        String dependsOn = (String) node.get("dependsOn");
        Map<String, String> parameters = (Map<String, String>) node.get("parameters");
        
        // 检查工具名称
        if (toolName == null || toolName.trim().isEmpty()) {
            result.addError("节点 " + stepId + " 缺少 toolName");
            return result;
        }
        
        // 检查工具是否存在
        Tool tool = toolManager.getTool(toolName);
        if (tool == null) {
            result.addError("工具不存在：" + toolName + "（节点：" + stepId + "）");
            return result;
        }
        
        // 检查依赖关系
        if (dependsOn != null && !dependsOn.trim().isEmpty()) {
            if (!allStepIds.contains(dependsOn)) {
                result.addError("节点 " + stepId + " 依赖不存在的步骤：" + dependsOn);
            } else {
                // 检查依赖顺序（被依赖的节点必须在前面）
                int currentIndex = findNodeIndex(stepId, (List<Map<String, Object>>) null);
                int dependIndex = findNodeIndex(dependsOn, (List<Map<String, Object>>) null);
                if (currentIndex != -1 && dependIndex != -1 && currentIndex < dependIndex) {
                    result.addWarning("节点 " + stepId + " 在依赖节点 " + dependsOn + " 之前定义");
                }
            }
        }
        
        // 检查必需参数
        if (parameters != null) {
            for (ToolParameter param : tool.getParameters()) {
                if (param.isRequired() && !parameters.containsKey(param.getName())) {
                    result.addError("节点 " + stepId + " 缺少必需参数：" + param.getName());
                }
            }
        } else {
            // 检查是否有必需参数
            for (ToolParameter param : tool.getParameters()) {
                if (param.isRequired()) {
                    result.addError("节点 " + stepId + " 缺少必需参数：" + param.getName());
                }
            }
        }
        
        // 检查参数引用
        if (parameters != null) {
            for (Map.Entry<String, String> entry : parameters.entrySet()) {
                String value = entry.getValue();
                if (value != null && value.contains("${") && value.contains("}")) {
                    // 提取引用
                    String refStepId = extractStepIdFromRef(value);
                    if (refStepId != null && !allStepIds.contains(refStepId)) {
                        result.addWarning("节点 " + stepId + " 引用了不存在的步骤：" + refStepId);
                    }
                }
            }
        }
        
        return result;
    }
    
    /**
     * 检查循环依赖
     */
    @SuppressWarnings("unchecked")
    private boolean hasCircularDependency(List<Map<String, Object>> nodes) {
        Map<String, String> dependencies = new HashMap<>();
        
        for (Map<String, Object> node : nodes) {
            String stepId = (String) node.get("stepId");
            String dependsOn = (String) node.get("dependsOn");
            if (dependsOn != null && !dependsOn.trim().isEmpty()) {
                dependencies.put(stepId, dependsOn);
            }
        }
        
        // 检测环
        Set<String> visited = new HashSet<>();
        Set<String> recStack = new HashSet<>();
        
        for (String stepId : dependencies.keySet()) {
            if (hasCycle(stepId, dependencies, visited, recStack)) {
                return true;
            }
        }
        
        return false;
    }
    
    private boolean hasCycle(String stepId, Map<String, String> dependencies, 
                            Set<String> visited, Set<String> recStack) {
        if (recStack.contains(stepId)) {
            return true;
        }
        
        if (visited.contains(stepId)) {
            return false;
        }
        
        visited.add(stepId);
        recStack.add(stepId);
        
        String dependOn = dependencies.get(stepId);
        if (dependOn != null && hasCycle(dependOn, dependencies, visited, recStack)) {
            return true;
        }
        
        recStack.remove(stepId);
        return false;
    }
    
    /**
     * 检查孤立节点
     */
    @SuppressWarnings("unchecked")
    private void checkOrphanNodes(List<Map<String, Object>> nodes, ValidationResult result) {
        if (nodes.size() <= 1) {
            return;
        }
        
        Set<String> referencedSteps = new HashSet<>();
        
        // 收集所有被引用的步骤
        for (Map<String, Object> node : nodes) {
            String dependsOn = (String) node.get("dependsOn");
            if (dependsOn != null && !dependsOn.trim().isEmpty()) {
                referencedSteps.add(dependsOn);
            }
            
            // 检查参数引用
            Map<String, String> parameters = (Map<String, String>) node.get("parameters");
            if (parameters != null) {
                for (String value : parameters.values()) {
                    if (value != null && value.contains("${") && value.contains("}")) {
                        String refStepId = extractStepIdFromRef(value);
                        if (refStepId != null) {
                            referencedSteps.add(refStepId);
                        }
                    }
                }
            }
        }
        
        // 检查是否有节点既不被依赖也不依赖别人（除了第一个和最后一个）
        for (int i = 1; i < nodes.size() - 1; i++) {
            Map<String, Object> node = nodes.get(i);
            String stepId = (String) node.get("stepId");
            String dependsOn = (String) node.get("dependsOn");
            
            if ((dependsOn == null || dependsOn.trim().isEmpty()) && 
                !referencedSteps.contains(stepId)) {
                result.addWarning("节点 " + stepId + " 可能是孤立的，既没有依赖也没有被依赖");
            }
        }
    }
    
    /**
     * 估算节点执行耗时
     */
    private int estimateNodeDuration(Map<String, Object> node) {
        String toolName = (String) node.get("toolName");
        if (toolName == null) return 1000;
        
        // 根据工具类型估算
        switch (toolName) {
            case "calculator":
            case "datetime":
                return 100;
            case "file":
            case "shell":
                return 2000;
            case "http":
            case "search":
            case "weather":
            case "news":
            case "stock":
                return 3000;
            case "code_executor":
                return 5000;
            case "pdf":
            case "image":
                return 5000;
            default:
                return 1000;
        }
    }
    
    /**
     * 从引用中提取 stepId
     */
    private String extractStepIdFromRef(String value) {
        if (value == null) return null;
        
        int start = value.indexOf("${");
        int end = value.indexOf("_result}", start);
        
        if (start != -1 && end != -1) {
            return value.substring(start + 2, end);
        }
        
        return null;
    }
    
    /**
     * 查找节点索引
     */
    private int findNodeIndex(String stepId, List<Map<String, Object>> nodes) {
        if (nodes == null) return -1;
        
        for (int i = 0; i < nodes.size(); i++) {
            Map<String, Object> node = nodes.get(i);
            String id = (String) node.get("stepId");
            if (stepId.equals(id)) {
                return i;
            }
        }
        
        return -1;
    }
    
    /**
     * 获取工具列表（用于前端展示）
     */
    public List<Map<String, Object>> getAvailableTools() {
        List<Map<String, Object>> tools = new ArrayList<>();
        
        for (Tool tool : toolManager.getAllTools()) {
            Map<String, Object> toolInfo = new HashMap<>();
            toolInfo.put("name", tool.getName());
            toolInfo.put("description", tool.getDescription());
            
            List<Map<String, Object>> params = new ArrayList<>();
            for (ToolParameter param : tool.getParameters()) {
                Map<String, Object> paramInfo = new HashMap<>();
                paramInfo.put("name", param.getName());
                paramInfo.put("type", param.getType());
                paramInfo.put("description", param.getDescription());
                paramInfo.put("required", param.isRequired());
                paramInfo.put("defaultValue", param.getDefaultValue());
                params.add(paramInfo);
            }
            toolInfo.put("parameters", params);
            
            tools.add(toolInfo);
        }
        
        return tools;
    }
}
