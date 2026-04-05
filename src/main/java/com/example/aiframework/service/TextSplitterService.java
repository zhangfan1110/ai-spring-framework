package com.example.aiframework.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文本分词/切片服务
 * 将长文档分割成多个片段，便于向量化和检索
 */
@Service
public class TextSplitterService {
    
    /**
     * 按字符数分割文本（简单方法）
     * @param text 原始文本
     * @param chunkSize 每个片段的最大字符数
     * @param overlap 片段之间的重叠字符数
     * @return 分割后的文本片段列表
     */
    public List<String> splitByCharacters(String text, int chunkSize, int overlap) {
        if (text == null || text.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<String> chunks = new ArrayList<>();
        int start = 0;
        int textLength = text.length();
        
        while (start < textLength) {
            int end = Math.min(start + chunkSize, textLength);
            
            // 尝试在句子边界处分割
            if (end < textLength) {
                end = findSentenceBoundary(text, start, end);
            }
            
            String chunk = text.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }
            
            start = end - overlap;
            if (start < 0) start = 0;
        }
        
        return chunks;
    }
    
    /**
     * 按句子分割文本
     * @param text 原始文本
     * @param chunkSize 每个片段的最大句子数
     * @return 分割后的文本片段列表
     */
    public List<String> splitBySentences(String text, int chunkSize) {
        if (text == null || text.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 句子分割正则（支持中英文）
        Pattern sentencePattern = Pattern.compile(
            "[。！？.!?]+|[\r\n]+"
        );
        
        String[] sentences = sentencePattern.split(text);
        List<String> chunks = new ArrayList<>();
        
        List<String> currentChunk = new ArrayList<>();
        int sentenceCount = 0;
        
        for (String sentence : sentences) {
            String trimmed = sentence.trim();
            if (trimmed.isEmpty()) continue;
            
            currentChunk.add(trimmed);
            sentenceCount++;
            
            if (sentenceCount >= chunkSize) {
                chunks.add(String.join("。", currentChunk) + "。");
                currentChunk.clear();
                sentenceCount = 0;
            }
        }
        
        // 处理剩余的句子
        if (!currentChunk.isEmpty()) {
            chunks.add(String.join("。", currentChunk) + "。");
        }
        
        return chunks;
    }
    
    /**
     * 按段落分割文本
     * @param text 原始文本
     * @return 分割后的段落列表
     */
    public List<String> splitByParagraphs(String text) {
        if (text == null || text.isEmpty()) {
            return new ArrayList<>();
        }
        
        String[] paragraphs = text.split("\\n\\s*\\n");
        List<String> result = new ArrayList<>();
        
        for (String paragraph : paragraphs) {
            String trimmed = paragraph.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        
        return result;
    }
    
    /**
     * 智能分割（推荐方法）
     * 结合段落、句子和字符数进行分割
     * @param text 原始文本
     * @param chunkSize 每个片段的最大字符数
     * @param overlap 片段之间的重叠字符数
     * @return 分割后的文本片段列表
     */
    public List<String> smartSplit(String text, int chunkSize, int overlap) {
        if (text == null || text.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<String> chunks = new ArrayList<>();
        
        // 1. 先按段落分割
        List<String> paragraphs = splitByParagraphs(text);
        
        StringBuilder currentChunk = new StringBuilder();
        
        for (String paragraph : paragraphs) {
            // 如果当前段落 + 已有内容超过限制
            if (currentChunk.length() + paragraph.length() > chunkSize) {
                // 如果当前 chunk 不为空，先保存
                if (currentChunk.length() > 0) {
                    chunks.add(currentChunk.toString());
                    currentChunk = new StringBuilder();
                }
                
                // 如果单个段落就超过限制，继续分割
                if (paragraph.length() > chunkSize) {
                    List<String> subChunks = splitByCharacters(paragraph, chunkSize, overlap);
                    chunks.addAll(subChunks);
                } else {
                    currentChunk.append(paragraph);
                }
            } else {
                if (currentChunk.length() > 0) {
                    currentChunk.append("\n\n");
                }
                currentChunk.append(paragraph);
            }
        }
        
        // 添加最后一个 chunk
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString());
        }
        
        return chunks;
    }
    
    /**
     * 查找句子边界
     */
    private int findSentenceBoundary(String text, int start, int end) {
        // 从后往前找句子结束符
        for (int i = end - 1; i > start; i--) {
            char c = text.charAt(i);
            if (c == '。' || c == '.' || c == '！' || c == '!' || 
                c == '？' || c == '?' || c == '\n') {
                return i + 1;
            }
        }
        return end;
    }
    
    /**
     * 计算建议的分片大小
     * 基于 Embedding 模型的最大 token 数
     * @param modelType 模型类型
     * @return 建议的字符数（中文）
     */
    public int suggestChunkSize(String modelType) {
        switch (modelType) {
            case "text_embedding": // 智谱 AI
                return 500; // 智谱支持 2048 tokens，中文约 500-800 字符
            case "text-embedding-v2": // 通义千问
                return 500;
            case "text-embedding-3-small": // OpenAI
                return 600;
            default:
                return 500;
        }
    }
}
