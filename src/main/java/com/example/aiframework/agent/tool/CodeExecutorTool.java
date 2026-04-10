package com.example.aiframework.agent.tool;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.Arrays;
import java.util.List;

/**
 * 代码执行工具
 */
public class CodeExecutorTool implements Tool {
    
    @Override
    public String getName() {
        return "code";
    }
    
    @Override
    public String getDescription() {
        return "执行代码片段，支持 JavaScript 和 Python";
    }
    
    @Override
    public List<ToolParameter> getParameters() {
        return Arrays.asList(
            ToolParameter.string("language", "编程语言：javascript, python", true),
            ToolParameter.string("code", "要执行的代码", true)
        );
    }
    
    @Override
    public ToolExecutionResult execute(List<ToolParameter> parameters) {
        try {
            String language = null;
            String code = null;
            
            for (ToolParameter param : parameters) {
                String paramName = param.getName();
                Object paramValue = param.getDefaultValue();
                
                switch (paramName) {
                    case "language":
                        language = paramValue != null ? paramValue.toString() : null;
                        break;
                    case "code":
                        code = paramValue != null ? paramValue.toString() : null;
                        break;
                }
            }
            
            if (language == null || code == null) {
                return ToolExecutionResult.error("language 和 code 参数不能为空");
            }
            
            switch (language.toLowerCase()) {
                case "javascript":
                case "js":
                    return executeJavaScript(code);
                case "python":
                case "py":
                    return executePython(code);
                default:
                    return ToolExecutionResult.error("不支持的语言：" + language);
            }
            
        } catch (Exception e) {
            return ToolExecutionResult.error("代码执行失败：" + e.getMessage());
        }
    }
    
    private ToolExecutionResult executeJavaScript(String code) {
        try {
            ScriptEngineManager manager = new ScriptEngineManager();
            ScriptEngine engine = manager.getEngineByName("JavaScript");
            
            Object result = engine.eval(code);
            
            return ToolExecutionResult.success("JavaScript 执行结果:\n" + result);
            
        } catch (Exception e) {
            return ToolExecutionResult.error("JavaScript 执行失败：" + e.getMessage());
        }
    }
    
    private ToolExecutionResult executePython(String code) {
        try {
            // 尝试使用 Python 引擎 (如果安装了 GraalPython)
            ScriptEngineManager manager = new ScriptEngineManager();
            ScriptEngine engine = manager.getEngineByName("python");
            
            if (engine != null) {
                Object result = engine.eval(code);
                return ToolExecutionResult.success("Python 执行结果:\n" + result);
            } else {
                // 尝试使用系统 Python
                ProcessBuilder pb = new ProcessBuilder("python3", "-c", code);
                pb.redirectErrorStream(true);
                Process process = pb.start();
                
                StringBuilder output = new StringBuilder();
                try (java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                    }
                }
                
                int exitCode = process.waitFor();
                if (exitCode == 0) {
                    return ToolExecutionResult.success("Python 执行结果:\n" + output);
                } else {
                    return ToolExecutionResult.error("Python 执行失败:\n" + output);
                }
            }
            
        } catch (Exception e) {
            return ToolExecutionResult.error("Python 执行失败：" + e.getMessage());
        }
    }
}
