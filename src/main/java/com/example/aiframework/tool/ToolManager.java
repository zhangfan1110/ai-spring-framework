package com.example.aiframework.tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具管理器
 */
@Component
public class ToolManager {
    
    private static final Logger log = LoggerFactory.getLogger(ToolManager.class);
    
    private final Map<String, Tool> tools = new HashMap<>();
    
    public ToolManager() {
        // 注册内置工具
        registerTool(new CalculatorTool());
        registerTool(new DateTimeTool());
        registerTool(new SearchTool());
        registerTool(new FileTool());
        registerTool(new ShellTool());
        registerTool(new CodeExecutorTool());
        
        // 注册扩展工具
        registerTool(new WeatherTool());
        registerTool(new HttpTool());
        registerTool(new DatabaseTool());
        registerTool(new EmailTool());
        registerTool(new ImageTool());
        registerTool(new PdfTool());
        registerTool(new TranslationTool());
        registerTool(new NewsTool());
        registerTool(new StockTool());
    }
    
    /**
     * 注册工具
     */
    public void registerTool(Tool tool) {
        tools.put(tool.getName(), tool);
        log.info("注册工具：{}", tool.getName());
    }
    
    /**
     * 获取所有工具
     */
    public List<Tool> getAllTools() {
        return new ArrayList<>(tools.values());
    }
    
    /**
     * 根据名称获取工具
     */
    public Tool getTool(String name) {
        return tools.get(name);
    }
    
    /**
     * 执行工具
     */
    public ToolExecutionResult executeTool(String toolName, List<ToolParameter> parameters) {
        log.info("执行工具：{}, 参数：{}", toolName, parameters);
        
        Tool tool = tools.get(toolName);
        if (tool == null) {
            return ToolExecutionResult.error("未知工具：" + toolName);
        }
        
        try {
            return tool.execute(parameters);
        } catch (Exception e) {
            log.error("工具执行失败：{}", e.getMessage(), e);
            return ToolExecutionResult.error("工具执行失败：" + e.getMessage());
        }
    }
    
    /**
     * 生成工具描述 (用于 Prompt)
     */
    public String generateToolsDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append("可用工具列表:\n\n");
        
        for (Tool tool : tools.values()) {
            sb.append("工具名称：").append(tool.getName()).append("\n");
            sb.append("描述：").append(tool.getDescription()).append("\n");
            sb.append("参数:\n");
            
            for (ToolParameter param : tool.getParameters()) {
                sb.append("  - ").append(param.getName())
                  .append(" (").append(param.getType()).append(") ")
                  .append(param.getDescription());
                if (param.isRequired()) {
                    sb.append(" [必填]");
                }
                sb.append("\n");
            }
            sb.append("\n");
        }
        
        return sb.toString();
    }
}
