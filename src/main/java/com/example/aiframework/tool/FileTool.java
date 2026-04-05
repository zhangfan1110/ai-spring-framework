package com.example.aiframework.tool;

import java.io.*;
import java.nio.file.*;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 文件操作工具
 */
public class FileTool implements Tool {
    
    @Override
    public String getName() {
        return "file";
    }
    
    @Override
    public String getDescription() {
        return "文件操作工具，支持读取、写入、删除、列出文件等";
    }
    
    @Override
    public List<ToolParameter> getParameters() {
        return Arrays.asList(
            ToolParameter.string("operation", "操作类型：read, write, delete, list, exists", true),
            ToolParameter.string("path", "文件路径", true),
            ToolParameter.string("content", "写入的内容 (write 操作需要)", false),
            ToolParameter.bool("append", "是否追加模式 (write 操作)", false)
        );
    }
    
    @Override
    public ToolExecutionResult execute(List<ToolParameter> parameters) {
        try {
            String operation = null;
            String path = null;
            String content = null;
            boolean append = false;
            
            for (ToolParameter param : parameters) {
                String paramName = param.getName();
                Object paramValue = param.getDefaultValue();
                
                switch (paramName) {
                    case "operation":
                        operation = paramValue != null ? paramValue.toString() : null;
                        break;
                    case "path":
                        path = paramValue != null ? paramValue.toString() : null;
                        break;
                    case "content":
                        content = paramValue != null ? paramValue.toString() : null;
                        break;
                    case "append":
                        if (paramValue != null) {
                            if (paramValue instanceof Boolean) {
                                append = (Boolean) paramValue;
                            } else {
                                append = Boolean.parseBoolean(paramValue.toString());
                            }
                        }
                        break;
                }
            }
            
            if (operation == null || path == null) {
                return ToolExecutionResult.error("operation 和 path 参数不能为空");
            }
            
            // 安全检查：限制在工作目录
            String workspace = System.getProperty("user.dir");
            Path targetPath = Paths.get(path).toAbsolutePath().normalize();
            if (!targetPath.startsWith(workspace)) {
                return ToolExecutionResult.error("安全限制：只能访问工作目录内的文件");
            }
            
            switch (operation.toLowerCase()) {
                case "read":
                    return readFile(targetPath);
                case "write":
                    if (content == null) {
                        return ToolExecutionResult.error("write 操作需要 content 参数");
                    }
                    return writeFile(targetPath, content, append);
                case "delete":
                    return deleteFile(targetPath);
                case "list":
                    return listFiles(targetPath);
                case "exists":
                    return checkExists(targetPath);
                default:
                    return ToolExecutionResult.error("未知操作：" + operation);
            }
            
        } catch (Exception e) {
            return ToolExecutionResult.error("文件操作失败：" + e.getMessage());
        }
    }
    
    private ToolExecutionResult readFile(Path path) throws IOException {
        if (!Files.exists(path)) {
            return ToolExecutionResult.error("文件不存在：" + path);
        }
        if (Files.isDirectory(path)) {
            return ToolExecutionResult.error("不能读取目录：" + path);
        }
        
        String content = Files.readString(path);
        return ToolExecutionResult.success("文件内容:\n" + content);
    }
    
    private ToolExecutionResult writeFile(Path path, String content, boolean append) throws IOException {
        Files.createDirectories(path.getParent());
        
        if (append) {
            Files.writeString(path, content, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } else {
            Files.writeString(path, content);
        }
        
        return ToolExecutionResult.success("文件已写入：" + path);
    }
    
    private ToolExecutionResult deleteFile(Path path) throws IOException {
        if (!Files.exists(path)) {
            return ToolExecutionResult.error("文件不存在：" + path);
        }
        
        Files.delete(path);
        return ToolExecutionResult.success("文件已删除：" + path);
    }
    
    private ToolExecutionResult listFiles(Path path) throws IOException {
        if (!Files.exists(path)) {
            return ToolExecutionResult.error("路径不存在：" + path);
        }
        if (!Files.isDirectory(path)) {
            return ToolExecutionResult.error("不是目录：" + path);
        }
        
        try (Stream<Path> stream = Files.list(path)) {
            String fileList = stream
                .map(p -> p.getFileName().toString() + (Files.isDirectory(p) ? "/" : ""))
                .collect(Collectors.joining("\n"));
            return ToolExecutionResult.success("目录内容:\n" + fileList);
        }
    }
    
    private ToolExecutionResult checkExists(Path path) {
        boolean exists = Files.exists(path);
        return ToolExecutionResult.success("文件" + (exists ? "存在" : "不存在") + ": " + path);
    }
}
