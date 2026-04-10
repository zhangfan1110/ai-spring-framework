package com.example.aiframework.agent.tool;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * 终端指令执行工具
 */
public class ShellTool implements Tool {
    
    // 安全白名单命令
    private static final List<String> ALLOWED_COMMANDS = Arrays.asList(
        "ls", "dir", "pwd", "cd",
        "cat", "head", "tail", "grep", "find",
        "echo", "date", "whoami", "uname",
        "mkdir", "touch", "cp", "mv", "rm",
        "git", "npm", "mvn", "java", "javac",
        "python", "python3", "node"
    );
    
    // 危险命令黑名单
    private static final List<String> BLOCKED_COMMANDS = Arrays.asList(
        "rm -rf /", "rm -rf /*", "dd if=/dev/zero",
        "mkfs", "fdisk", "chmod -R 777",
        "wget", "curl", "nc", "netcat",
        "sudo", "su", "passwd", "useradd"
    );
    
    @Override
    public String getName() {
        return "shell";
    }
    
    @Override
    public String getDescription() {
        return "执行终端命令，支持常用 Linux/Mac 命令";
    }
    
    @Override
    public List<ToolParameter> getParameters() {
        return Arrays.asList(
            ToolParameter.string("command", "要执行的命令", true),
            ToolParameter.number("timeout", "超时时间 (秒)，默认 30", false),
            ToolParameter.bool("safeMode", "安全模式 (默认 true)", false)
        );
    }
    
    @Override
    public ToolExecutionResult execute(List<ToolParameter> parameters) {
        try {
            String command = null;
            int timeout = 30;
            boolean safeMode = true;
            
            for (ToolParameter param : parameters) {
                String paramName = param.getName();
                Object paramValue = param.getDefaultValue();
                
                switch (paramName) {
                    case "command":
                        command = paramValue != null ? paramValue.toString() : null;
                        break;
                    case "timeout":
                        if (paramValue != null) {
                            if (paramValue instanceof Number) {
                                timeout = ((Number) paramValue).intValue();
                            } else {
                                try {
                                    timeout = Integer.parseInt(paramValue.toString());
                                } catch (NumberFormatException e) {
                                    timeout = 30;
                                }
                            }
                        }
                        break;
                    case "safeMode":
                        if (paramValue != null) {
                            if (paramValue instanceof Boolean) {
                                safeMode = (Boolean) paramValue;
                            } else {
                                safeMode = Boolean.parseBoolean(paramValue.toString());
                            }
                        }
                        break;
                }
            }
            
            if (command == null || command.isEmpty()) {
                return ToolExecutionResult.error("命令不能为空");
            }
            
            // 安全检查
            if (safeMode && !isCommandSafe(command)) {
                return ToolExecutionResult.error("安全限制：不允许执行该命令");
            }
            
            // 执行命令
            ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);
            pb.directory(new File(System.getProperty("user.dir")));
            pb.redirectErrorStream(true);
            
            Process process = pb.start();
            
            // 读取输出
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            
            // 等待完成
            if (!process.waitFor(timeout, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return ToolExecutionResult.error("命令执行超时 (" + timeout + "秒)");
            }
            
            int exitCode = process.exitValue();
            String result = output.toString();
            
            if (exitCode == 0) {
                return ToolExecutionResult.success("执行成功:\n" + result);
            } else {
                return ToolExecutionResult.error("执行失败 (退出码：" + exitCode + "):\n" + result);
            }
            
        } catch (Exception e) {
            return ToolExecutionResult.error("命令执行失败：" + e.getMessage());
        }
    }
    
    /**
     * 检查命令是否安全
     */
    private boolean isCommandSafe(String command) {
        // 检查黑名单
        for (String blocked : BLOCKED_COMMANDS) {
            if (command.contains(blocked)) {
                return false;
            }
        }
        
        // 检查管道和重定向
        if (command.contains("|") || command.contains(">") || command.contains("&&")) {
            // 允许简单的管道，如 ls | grep
            String[] parts = command.split("[|&]");
            for (String part : parts) {
                if (!isCommandSafe(part.trim())) {
                    return false;
                }
            }
            return true;
        }
        
        // 检查白名单
        String baseCommand = command.split("\\s+")[0];
        return ALLOWED_COMMANDS.contains(baseCommand);
    }
}
