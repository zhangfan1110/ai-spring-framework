package com.example.aiframework.agent.tool;

import java.util.List;

/**
 * 工具接口
 */
public interface Tool {
    
    /**
     * 工具名称
     */
    String getName();
    
    /**
     * 工具描述
     */
    String getDescription();
    
    /**
     * 工具参数列表
     */
    List<ToolParameter> getParameters();
    
    /**
     * 执行工具
     */
    ToolExecutionResult execute(List<ToolParameter> parameters);
}
