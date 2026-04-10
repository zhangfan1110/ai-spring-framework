package com.example.aiframework.agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.*;

/**
 * 调用链优化建议服务
 * 分析调用链结构，给出优化建议
 */
@Service
public class ChainOptimizationService {
    
    private static final Logger log = LoggerFactory.getLogger(ChainOptimizationService.class);
    
    @Resource
    private ChatLanguageModel chatModel;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * 优化建议
     */
    public static class OptimizationSuggestion {
        private String type; // PERFORMANCE/RELIABILITY/COST/COMPLEXITY
        private String severity; // HIGH/MEDIUM/LOW
        private String description;
        private String suggestion;
        private int estimatedImprovement; // 百分比
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getSuggestion() { return suggestion; }
        public void setSuggestion(String suggestion) { this.suggestion = suggestion; }
        
        public int getEstimatedImprovement() { return estimatedImprovement; }
        public void setEstimatedImprovement(int estimatedImprovement) { this.estimatedImprovement = estimatedImprovement; }
    }
    
    /**
     * 优化分析报告
     */
    public static class OptimizationReport {
        private int originalSteps;
        private int optimizedSteps;
        private int originalDuration;
        private int optimizedDuration;
        private List<OptimizationSuggestion> suggestions;
        private String summary;
        private Map<String, Object> details;
        
        public int getOriginalSteps() { return originalSteps; }
        public void setOriginalSteps(int originalSteps) { this.originalSteps = originalSteps; }
        
        public int getOptimizedSteps() { return optimizedSteps; }
        public void setOptimizedSteps(int optimizedSteps) { this.optimizedSteps = optimizedSteps; }
        
        public int getOriginalDuration() { return originalDuration; }
        public void setOriginalDuration(int originalDuration) { this.originalDuration = originalDuration; }
        
        public int getOptimizedDuration() { return optimizedDuration; }
        public void setOptimizedDuration(int optimizedDuration) { this.optimizedDuration = optimizedDuration; }
        
        public List<OptimizationSuggestion> getSuggestions() { return suggestions; }
        public void setSuggestions(List<OptimizationSuggestion> suggestions) { this.suggestions = suggestions; }
        
        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }
        
        public Map<String, Object> getDetails() { return details; }
        public void setDetails(Map<String, Object> details) { this.details = details; }
    }
    
    /**
     * 分析并优化调用链
     */
    @SuppressWarnings("unchecked")
    public OptimizationReport analyzeAndOptimize(Map<String, Object> chain) {
        log.info("分析调用链优化建议");
        
        OptimizationReport report = new OptimizationReport();
        report.setSuggestions(new ArrayList<>());
        report.setDetails(new HashMap<>());
        
        List<Map<String, Object>> nodes = (List<Map<String, Object>>) chain.get("nodes");
        if (nodes == null || nodes.isEmpty()) {
            report.setSummary("调用链为空");
            return report;
        }
        
        report.setOriginalSteps(nodes.size());
        report.setOriginalDuration(estimateDuration(nodes));
        
        // 1. 静态分析
        analyzeStructure(nodes, report);
        
        // 2. LLM 分析
        analyzeWithLLM(chain, report);
        
        // 3. 计算优化后的指标
        calculateOptimizedMetrics(nodes, report);
        
        // 4. 生成总结
        generateSummary(report);
        
        log.info("优化分析完成：{} 条建议", report.getSuggestions().size());
        return report;
    }
    
    /**
     * 静态分析
     */
    private void analyzeStructure(List<Map<String, Object>> nodes, OptimizationReport report) {
        // 分析 1: 检查可并行的步骤
        analyzeParallelism(nodes, report);
        
        // 分析 2: 检查冗余步骤
        analyzeRedundancy(nodes, report);
        
        // 分析 3: 检查依赖关系
        analyzeDependencies(nodes, report);
        
        // 分析 4: 检查工具选择
        analyzeToolSelection(nodes, report);
    }
    
    /**
     * 分析并行执行机会
     */
    private void analyzeParallelism(List<Map<String, Object>> nodes, OptimizationReport report) {
        Map<String, String> dependencies = new HashMap<>();
        for (Map<String, Object> node : nodes) {
            String stepId = (String) node.get("stepId");
            String dependsOn = (String) node.get("dependsOn");
            if (dependsOn != null && !dependsOn.trim().isEmpty()) {
                dependencies.put(stepId, dependsOn);
            }
        }
        
        // 找出没有依赖的节点（可以并行）
        List<String> independentNodes = new ArrayList<>();
        for (Map<String, Object> node : nodes) {
            String stepId = (String) node.get("stepId");
            if (!dependencies.containsKey(stepId)) {
                independentNodes.add(stepId);
            }
        }
        
        if (independentNodes.size() > 1) {
            OptimizationSuggestion suggestion = new OptimizationSuggestion();
            suggestion.setType("PERFORMANCE");
            suggestion.setSeverity("MEDIUM");
            suggestion.setDescription(independentNodes.size() + " 个步骤没有依赖关系，可以并行执行");
            suggestion.setSuggestion("将步骤 " + String.join(", ", independentNodes) + " 标记为 parallel=true");
            suggestion.setEstimatedImprovement((independentNodes.size() - 1) * 20);
            report.getSuggestions().add(suggestion);
        }
    }
    
    /**
     * 分析冗余步骤
     */
    private void analyzeRedundancy(List<Map<String, Object>> nodes, OptimizationReport report) {
        // 检查连续的同类型工具
        Map<String, Integer> toolCount = new HashMap<>();
        for (Map<String, Object> node : nodes) {
            String toolName = (String) node.get("toolName");
            toolCount.put(toolName, toolCount.getOrDefault(toolName, 0) + 1);
        }
        
        for (Map.Entry<String, Integer> entry : toolCount.entrySet()) {
            if (entry.getValue() >= 3) {
                OptimizationSuggestion suggestion = new OptimizationSuggestion();
                suggestion.setType("COMPLEXITY");
                suggestion.setSeverity("LOW");
                suggestion.setDescription("工具 " + entry.getKey() + " 被使用了 " + entry.getValue() + " 次");
                suggestion.setSuggestion("考虑合并为一次调用，使用批量处理");
                suggestion.setEstimatedImprovement(10);
                report.getSuggestions().add(suggestion);
            }
        }
    }
    
    /**
     * 分析依赖关系
     */
    private void analyzeDependencies(List<Map<String, Object>> nodes, OptimizationReport report) {
        // 检查长依赖链
        Map<String, Integer> depth = new HashMap<>();
        for (Map<String, Object> node : nodes) {
            String stepId = (String) node.get("stepId");
            String dependsOn = (String) node.get("dependsOn");
            
            if (dependsOn == null) {
                depth.put(stepId, 0);
            } else {
                depth.put(stepId, depth.getOrDefault(dependsOn, 0) + 1);
            }
        }
        
        int maxDepth = depth.values().stream().max(Integer::compareTo).orElse(0);
        if (maxDepth >= 5) {
            OptimizationSuggestion suggestion = new OptimizationSuggestion();
            suggestion.setType("RELIABILITY");
            suggestion.setSeverity("HIGH");
            suggestion.setDescription("依赖链过长（深度：" + maxDepth + "）");
            suggestion.setSuggestion("考虑拆分调用链，或重新设计步骤顺序");
            suggestion.setEstimatedImprovement(30);
            report.getSuggestions().add(suggestion);
        }
    }
    
    /**
     * 分析工具选择
     */
    private void analyzeToolSelection(List<Map<String, Object>> nodes, OptimizationReport report) {
        // 检查是否有更优的工具选择
        for (Map<String, Object> node : nodes) {
            String toolName = (String) node.get("toolName");
            
            if ("shell".equals(toolName)) {
                Map<String, String> params = (Map<String, String>) node.get("parameters");
                String command = params != null ? params.get("command") : "";
                
                if (command != null && (command.contains("curl") || command.contains("wget"))) {
                    OptimizationSuggestion suggestion = new OptimizationSuggestion();
                    suggestion.setType("RELIABILITY");
                    suggestion.setSeverity("MEDIUM");
                    suggestion.setDescription("使用 Shell 命令进行 HTTP 请求");
                    suggestion.setSuggestion("建议使用 http 工具，更可靠且易于调试");
                    suggestion.setEstimatedImprovement(15);
                    report.getSuggestions().add(suggestion);
                }
            }
        }
    }
    
    /**
     * 使用 LLM 分析
     */
    private void analyzeWithLLM(Map<String, Object> chain, OptimizationReport report) {
        try {
            String prompt = buildOptimizationPrompt(chain);
            String response = chatModel.generate(prompt);
            
            // 解析 LLM 的建议（简化处理）
            if (response.contains("建议") || response.contains("优化")) {
                OptimizationSuggestion suggestion = new OptimizationSuggestion();
                suggestion.setType("COST");
                suggestion.setSeverity("LOW");
                suggestion.setDescription("LLM 分析建议");
                suggestion.setSuggestion(response.substring(0, Math.min(200, response.length())));
                suggestion.setEstimatedImprovement(5);
                report.getSuggestions().add(suggestion);
            }
            
        } catch (Exception e) {
            log.warn("LLM 分析失败：{}", e.getMessage());
        }
    }
    
    /**
     * 构建 LLM Prompt
     */
    private String buildOptimizationPrompt(Map<String, Object> chain) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个工作流优化专家。请分析以下工具调用链，给出优化建议。\n\n");
        sb.append("调用链结构:\n");
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> nodes = (List<Map<String, Object>>) chain.get("nodes");
        for (Map<String, Object> node : nodes) {
            sb.append("- ").append(node.get("stepId")).append(": ")
              .append(node.get("toolName")).append("\n");
        }
        
        sb.append("\n请从以下角度分析:\n");
        sb.append("1. 是否有可以并行的步骤？\n");
        sb.append("2. 是否有冗余的步骤？\n");
        sb.append("3. 依赖关系是否合理？\n");
        sb.append("4. 工具选择是否最优？\n");
        sb.append("5. 如何减少执行时间？\n\n");
        sb.append("请给出具体建议:");
        
        return sb.toString();
    }
    
    /**
     * 估算执行时间
     */
    private int estimateDuration(List<Map<String, Object>> nodes) {
        int total = 0;
        for (Map<String, Object> node : nodes) {
            String toolName = (String) node.get("toolName");
            total += getToolDuration(toolName);
        }
        return total;
    }
    
    /**
     * 获取工具执行时间
     */
    private int getToolDuration(String toolName) {
        switch (toolName != null ? toolName : "") {
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
     * 计算优化后的指标
     */
    private void calculateOptimizedMetrics(List<Map<String, Object>> nodes, OptimizationReport report) {
        int parallelizableSteps = 0;
        int removableSteps = 0;
        
        for (OptimizationSuggestion suggestion : report.getSuggestions()) {
            if (suggestion.getType().equals("PERFORMANCE")) {
                parallelizableSteps += suggestion.getEstimatedImprovement() / 20;
            }
            if (suggestion.getType().equals("COMPLEXITY")) {
                removableSteps += 1;
            }
        }
        
        report.setOptimizedSteps(nodes.size() - removableSteps);
        report.setOptimizedDuration(report.getOriginalDuration() - (parallelizableSteps * 2000));
    }
    
    /**
     * 生成总结
     */
    private void generateSummary(OptimizationReport report) {
        int highSeverity = (int) report.getSuggestions().stream()
            .filter(s -> "HIGH".equals(s.getSeverity()))
            .count();
        
        if (highSeverity > 0) {
            report.setSummary("发现 " + highSeverity + " 个严重问题，建议优先优化");
        } else if (!report.getSuggestions().isEmpty()) {
            report.setSummary("发现 " + report.getSuggestions().size() + " 个优化点，预计提升 " + 
                calculateTotalImprovement(report) + "% 性能");
        } else {
            report.setSummary("调用链结构良好，无需优化");
        }
    }
    
    /**
     * 计算总体改进
     */
    private int calculateTotalImprovement(OptimizationReport report) {
        if (report.getOriginalDuration() == 0) {
            return 0;
        }
        int improvement = report.getOriginalDuration() - report.getOptimizedDuration();
        return (improvement * 100) / report.getOriginalDuration();
    }
}
