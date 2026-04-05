package com.example.aiframework.tool;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/**
 * PDF 处理工具 - 支持读取、转换、合并等操作
 */
public class PdfTool implements Tool {
    
    @Override
    public String getName() {
        return "pdf";
    }
    
    @Override
    public String getDescription() {
        return "PDF 文件处理工具，支持读取内容、提取文本、PDF 转图片等操作 (需要 Apache PDFBox 依赖)";
    }
    
    @Override
    public List<ToolParameter> getParameters() {
        return Arrays.asList(
            ToolParameter.string("input", "输入 PDF 文件路径", true),
            ToolParameter.string("output", "输出文件路径", false),
            ToolParameter.string("operation", "操作类型：read/extract/merge/split/info", true),
            ToolParameter.string("pages", "页码范围，如：1-5 或 1,3,5，默认全部", false),
            ToolParameter.string("text", "要添加的文本内容 (merge 时使用)", false)
        );
    }
    
    @Override
    public ToolExecutionResult execute(List<ToolParameter> parameters) {
        String input = null;
        String output = null;
        String operation = null;
        String pages = null;
        String text = null;
        
        for (ToolParameter param : parameters) {
            String name = param.getName();
            Object value = param.getDefaultValue();
            
            if (value == null) continue;
            
            switch (name) {
                case "input":
                    input = value.toString();
                    break;
                case "output":
                    output = value.toString();
                    break;
                case "operation":
                    operation = value.toString().toLowerCase();
                    break;
                case "pages":
                    pages = value.toString();
                    break;
                case "text":
                    text = value.toString();
                    break;
            }
        }
        
        if (input == null || input.trim().isEmpty()) {
            return ToolExecutionResult.error("输入文件路径不能为空");
        }
        
        if (operation == null || operation.trim().isEmpty()) {
            return ToolExecutionResult.error("操作类型不能为空");
        }
        
        File inputFile = new File(input);
        if (!inputFile.exists()) {
            return ToolExecutionResult.error("输入文件不存在：" + input);
        }
        
        try {
            switch (operation) {
                case "read":
                case "extract":
                    return extractText(inputFile, pages);
                    
                case "info":
                    return ToolExecutionResult.success(getPdfInfo(inputFile));
                    
                case "merge":
                    if (text == null) {
                        return ToolExecutionResult.error("merge 操作需要提供要合并的 PDF 路径列表 (逗号分隔)");
                    }
                    return mergePdfs(inputFile, text, output);
                    
                case "split":
                    return splitPdf(inputFile, output, pages);
                    
                default:
                    return ToolExecutionResult.error("未知操作：" + operation);
            }
            
        } catch (Exception e) {
            String errorMsg = e.getMessage();
            if (errorMsg == null || errorMsg.isEmpty()) {
                errorMsg = e.getClass().getSimpleName();
            }
            return ToolExecutionResult.error("PDF 处理失败：" + errorMsg + "\n提示：请确保已添加 Apache PDFBox 依赖");
        }
    }
    
    /**
     * 提取 PDF 文本
     */
    private ToolExecutionResult extractText(File pdfFile, String pages) throws Exception {
        Class<?> loaderClass = Class.forName("org.apache.pdfbox.pdmodel.PDDocument");
        Object document = loaderClass.getMethod("load", File.class).invoke(null, pdfFile);
        
        Class<?> extractorClass = Class.forName("org.apache.pdfbox.text.PDFTextStripper");
        Object stripper = extractorClass.getDeclaredConstructor().newInstance();
        
        StringBuilder result = new StringBuilder();
        result.append("📄 PDF 文件：").append(pdfFile.getName()).append("\n\n");
        
        if (pages != null && !pages.trim().isEmpty()) {
            // 解析页码范围
            int[] pageNumbers = parsePageRange(pages, (Integer) loaderClass.getMethod("getNumberOfPages").invoke(document));
            for (int pageNum : pageNumbers) {
                stripper.getClass().getMethod("setStartPage", int.class).invoke(stripper, pageNum);
                stripper.getClass().getMethod("setEndPage", int.class).invoke(stripper, pageNum);
                String text = (String) extractorClass.getMethod("getText", loaderClass).invoke(stripper, document);
                result.append("=== 第 ").append(pageNum).append(" 页 ===\n");
                result.append(text).append("\n\n");
            }
        } else {
            String text = (String) extractorClass.getMethod("getText", loaderClass).invoke(stripper, document);
            result.append(text);
        }
        
        loaderClass.getMethod("close").invoke(document);
        
        // 限制输出长度
        String output = result.toString();
        if (output.length() > 10000) {
            output = output.substring(0, 10000) + "\n\n... (内容过长，仅显示前 10000 字符)";
        }
        
        return ToolExecutionResult.success(output);
    }
    
    /**
     * 获取 PDF 信息
     */
    private String getPdfInfo(File pdfFile) throws Exception {
        Class<?> loaderClass = Class.forName("org.apache.pdfbox.pdmodel.PDDocument");
        Object document = loaderClass.getMethod("load", File.class).invoke(null, pdfFile);
        Object catalog = loaderClass.getMethod("getDocumentCatalog").invoke(document);
        Object docInfo = loaderClass.getMethod("getDocumentInformation").invoke(document);
        
        StringBuilder sb = new StringBuilder();
        sb.append("📄 PDF 信息\n");
        sb.append("文件名：").append(pdfFile.getName()).append("\n");
        sb.append("文件大小：").append(formatFileSize(pdfFile.length())).append("\n");
        sb.append("页数：").append(loaderClass.getMethod("getNumberOfPages").invoke(document)).append("\n");
        
        // 尝试获取元数据
        try {
            String title = (String) docInfo.getClass().getMethod("getTitle").invoke(docInfo);
            String author = (String) docInfo.getClass().getMethod("getAuthor").invoke(docInfo);
            String subject = (String) docInfo.getClass().getMethod("getSubject").invoke(docInfo);
            
            if (title != null && !title.isEmpty()) sb.append("标题：").append(title).append("\n");
            if (author != null && !author.isEmpty()) sb.append("作者：").append(author).append("\n");
            if (subject != null && !subject.isEmpty()) sb.append("主题：").append(subject).append("\n");
        } catch (Exception e) {
            sb.append("元数据：不可用\n");
        }
        
        loaderClass.getMethod("close").invoke(document);
        return sb.toString();
    }
    
    /**
     * 合并 PDF
     */
    private ToolExecutionResult mergePdfs(File firstPdf, String otherPdfsStr, String outputPath) throws Exception {
        Class<?> mergerClass = Class.forName("org.apache.pdfbox.multipdf.PDFMergerUtility");
        Object merger = mergerClass.getDeclaredConstructor().newInstance();
        
        String[] pdfPaths = otherPdfsStr.split(",");
        String destPath = outputPath != null ? outputPath : "merged_output.pdf";
        
        merger.getClass().getMethod("setDestinationFileName", String.class).invoke(merger, destPath);
        merger.getClass().getMethod("addSource", File.class).invoke(merger, firstPdf);
        
        for (String path : pdfPaths) {
            File pdfFile = new File(path.trim());
            if (pdfFile.exists()) {
                merger.getClass().getMethod("addSource", File.class).invoke(merger, pdfFile);
            }
        }
        
        merger.getClass().getMethod("mergeDocuments", InputStream.class).invoke(merger, (Object) null);
        
        return ToolExecutionResult.success("PDF 合并完成!\n输出：" + destPath);
    }
    
    /**
     * 分割 PDF
     */
    private ToolExecutionResult splitPdf(File pdfFile, String outputDir, String pages) throws Exception {
        String outDir = outputDir != null ? outputDir : pdfFile.getParent();
        
        Class<?> loaderClass = Class.forName("org.apache.pdfbox.pdmodel.PDDocument");
        Class<?> splitterClass = Class.forName("org.apache.pdfbox.multipdf.Splitter");
        
        Object document = loaderClass.getMethod("load", File.class).invoke(null, pdfFile);
        Object splitter = splitterClass.getDeclaredConstructor().newInstance();
        
        List<?> splitDocs = (List<?>) splitterClass.getMethod("split", loaderClass).invoke(splitter, document);
        
        int count = 0;
        for (Object splitDoc : splitDocs) {
            String splitPath = outDir + "/" + pdfFile.getName().replace(".pdf", "_page_" + (++count) + ".pdf");
            loaderClass.getMethod("save", String.class).invoke(splitDoc, splitPath);
            loaderClass.getMethod("close").invoke(splitDoc);
        }
        
        loaderClass.getMethod("close").invoke(document);
        
        return ToolExecutionResult.success("PDF 分割完成!\n输出目录：" + outDir + "\n共分割为 " + count + " 个文件");
    }
    
    private int[] parsePageRange(String range, int totalPages) {
        // 简单解析页码范围，如 "1-5" 或 "1,3,5"
        List<String> parts = Arrays.asList(range.split(","));
        return parts.stream().mapToInt(p -> {
            if (p.contains("-")) {
                String[] r = p.split("-");
                return Integer.parseInt(r[0].trim());
            }
            return Integer.parseInt(p.trim());
        }).toArray();
    }
    
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
    }
}
