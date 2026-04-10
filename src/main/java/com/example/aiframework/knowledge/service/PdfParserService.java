package com.example.aiframework.knowledge.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * PDF 解析服务
 * 使用 Apache PDFBox 提取 PDF 文本内容
 */
@Service
public class PdfParserService {
    
    private static final Logger log = LoggerFactory.getLogger(PdfParserService.class);
    
    /**
     * 解析 PDF 文件，提取文本内容
     * @param file PDF 文件
     * @return 提取的文本内容
     * @throws IOException 解析失败时抛出
     */
    public String parsePdf(MultipartFile file) throws IOException {
        log.info("解析 PDF 文件：{}, 大小：{} bytes", file.getOriginalFilename(), file.getSize());
        
        try (InputStream inputStream = file.getInputStream();
             PDDocument document = PDDocument.load(inputStream)) {
            
            // 检查是否加密
            if (document.isEncrypted()) {
                log.warn("PDF 文件已加密，尝试解密");
                // 尝试空密码解密
                document.setAllSecurityToBeRemoved(true);
            }
            
            // 获取页数
            int pageCount = document.getNumberOfPages();
            log.info("PDF 页数：{}", pageCount);
            
            if (pageCount == 0) {
                log.warn("PDF 文件没有页面");
                return "";
            }
            
            // 提取文本
            PDFTextStripper textStripper = new PDFTextStripper();
            textStripper.setSortByPosition(true); // 按位置排序，提高准确性
            
            // 可以设置起始页和结束页
            textStripper.setStartPage(1);
            textStripper.setEndPage(document.getNumberOfPages());
            
            String text = textStripper.getText(document);
            
            log.info("PDF 解析完成，提取到 {} 个字符", text.length());
            
            // 后处理：清理多余空白
            text = postProcessText(text);
            
            return text;
        }
    }
    
    /**
     * 解析 PDF 指定页范围
     * @param file PDF 文件
     * @param startPage 起始页（1-based）
     * @param endPage 结束页（1-based）
     * @return 提取的文本内容
     */
    public String parsePdf(MultipartFile file, int startPage, int endPage) throws IOException {
        log.info("解析 PDF 文件：{} (第 {}-{} 页)", file.getOriginalFilename(), startPage, endPage);
        
        try (InputStream inputStream = file.getInputStream();
             PDDocument document = PDDocument.load(inputStream)) {
            
            if (document.isEncrypted()) {
                document.setAllSecurityToBeRemoved(true);
            }
            
            int totalPages = document.getNumberOfPages();
            if (startPage < 1) startPage = 1;
            if (endPage > totalPages) endPage = totalPages;
            if (startPage > endPage) {
                log.warn("起始页 {} 大于结束页 {}，交换", startPage, endPage);
                int temp = startPage;
                startPage = endPage;
                endPage = temp;
            }
            
            PDFTextStripper textStripper = new PDFTextStripper();
            textStripper.setSortByPosition(true);
            textStripper.setStartPage(startPage);
            textStripper.setEndPage(endPage);
            
            String text = textStripper.getText(document);
            log.info("PDF 解析完成（第 {}-{} 页），提取到 {} 个字符", startPage, endPage, text.length());
            
            return postProcessText(text);
        }
    }
    
    /**
     * 获取 PDF 元数据
     * @param file PDF 文件
     * @return PDF 元数据
     */
    public PdfMetadata getMetadata(MultipartFile file) throws IOException {
        try (InputStream inputStream = file.getInputStream();
             PDDocument document = PDDocument.load(inputStream)) {
            
            PdfMetadata metadata = new PdfMetadata();
            metadata.setPageCount(document.getNumberOfPages());
            metadata.setAuthor(document.getDocumentInformation().getAuthor());
            metadata.setTitle(document.getDocumentInformation().getTitle());
            metadata.setSubject(document.getDocumentInformation().getSubject());
            metadata.setCreator(document.getDocumentInformation().getCreator());
            metadata.setProducer(document.getDocumentInformation().getProducer());
            metadata.setEncrypted(document.isEncrypted());
            
            return metadata;
        }
    }
    
    /**
     * 文本后处理
     * - 移除多余空白
     * - 规范化换行
     * - 移除页眉页脚（简单模式）
     */
    private String postProcessText(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        // 1. 将多个连续空格替换为单个空格
        text = text.replaceAll("[ \\t]+", " ");
        
        // 2. 将多个连续换行替换为双换行（段落分隔）
        text = text.replaceAll("(\\r?\\n\\s*){3,}", "\n\n");
        
        // 3. 移除行首行尾空白
        text = text.trim();
        
        // 4. 移除常见的页眉页脚模式（可选）
        // 例如："Page 1 of 10" 或 "- 1 -"
        text = text.replaceAll("(?m)^\\s*-?\\s*\\d+\\s*-?\\s*$", "");
        text = text.replaceAll("(?m)^\\s*Page\\s+\\d+\\s+(of\\s+\\d+)?\\s*$", "");
        
        return text;
    }
    
    /**
     * PDF 元数据类
     */
    public static class PdfMetadata {
        private int pageCount;
        private String author;
        private String title;
        private String subject;
        private String creator;
        private String producer;
        private boolean encrypted;
        
        public int getPageCount() { return pageCount; }
        public void setPageCount(int pageCount) { this.pageCount = pageCount; }
        
        public String getAuthor() { return author; }
        public void setAuthor(String author) { this.author = author; }
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }
        
        public String getCreator() { return creator; }
        public void setCreator(String creator) { this.creator = creator; }
        
        public String getProducer() { return producer; }
        public void setProducer(String producer) { this.producer = producer; }
        
        public boolean isEncrypted() { return encrypted; }
        public void setEncrypted(boolean encrypted) { this.encrypted = encrypted; }
    }
}
