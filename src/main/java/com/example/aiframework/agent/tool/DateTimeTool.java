package com.example.aiframework.agent.tool;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

/**
 * 日期时间工具
 */
public class DateTimeTool implements Tool {
    
    @Override
    public String getName() {
        return "datetime";
    }
    
    @Override
    public String getDescription() {
        return "获取当前日期和时间，或进行日期格式化";
    }
    
    @Override
    public List<ToolParameter> getParameters() {
        return Arrays.asList(
            ToolParameter.string("format", "日期格式，例如：yyyy-MM-dd HH:mm:ss", false)
        );
    }
    
    @Override
    public ToolExecutionResult execute(List<ToolParameter> parameters) {
        try {
            String format = "yyyy-MM-dd HH:mm:ss";
            for (ToolParameter param : parameters) {
                if ("format".equals(param.getName()) && param.getDefaultValue() != null) {
                    format = param.getDefaultValue().toString();
                    break;
                }
            }
            
            LocalDateTime now = LocalDateTime.now();
            String formatted = now.format(DateTimeFormatter.ofPattern(format));
            
            return ToolExecutionResult.success(formatted);
            
        } catch (Exception e) {
            return ToolExecutionResult.error("日期格式化失败：" + e.getMessage());
        }
    }
}
