package com.example.aiframework.agent.tool;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 计算器工具
 */
public class CalculatorTool implements Tool {
    
    @Override
    public String getName() {
        return "calculator";
    }
    
    @Override
    public String getDescription() {
        return "执行数学计算，支持加减乘除、幂运算等";
    }
    
    @Override
    public List<ToolParameter> getParameters() {
        return Arrays.asList(
            ToolParameter.string("expression", "数学表达式，例如：2 + 2, 10 * 5, 2^3", true)
        );
    }
    
    @Override
    public ToolExecutionResult execute(List<ToolParameter> parameters) {
        try {
            String expression = null;
            for (ToolParameter param : parameters) {
                if ("expression".equals(param.getName())) {
                    Object value = param.getDefaultValue();
                    expression = value != null ? value.toString() : null;
                    break;
                }
            }
            
            if (expression == null || expression.isEmpty()) {
                return ToolExecutionResult.error("表达式不能为空");
            }
            
            // 简单的表达式计算
            double result = evaluate(expression);
            return ToolExecutionResult.success(String.valueOf(result));
            
        } catch (Exception e) {
            return ToolExecutionResult.error("计算失败：" + e.getMessage());
        }
    }
    
    /**
     * 简单的表达式求值
     */
    private double evaluate(String expression) {
        // 替换 ^ 为 Math.pow
        expression = expression.replace("^", "**");
        
        // 使用 JavaScript 引擎计算
        try {
            javax.script.ScriptEngineManager manager = new javax.script.ScriptEngineManager();
            javax.script.ScriptEngine engine = manager.getEngineByName("JavaScript");
            Object result = engine.eval(expression);
            return ((Number) result).doubleValue();
        } catch (Exception e) {
            throw new RuntimeException("表达式解析失败：" + expression);
        }
    }
}
