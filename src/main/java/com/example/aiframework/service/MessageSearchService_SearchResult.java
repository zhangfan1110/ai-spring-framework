package com.example.aiframework.service;

import com.example.aiframework.entity.ChatMessageEntity;

/**
 * 消息搜索结果
 */
public class MessageSearchService_SearchResult {
    private ChatMessageEntity message;
    private String highlightedContent;
    private int matchCount;
    private double relevanceScore;
    
    public ChatMessageEntity getMessage() { return message; }
    public void setMessage(ChatMessageEntity message) { this.message = message; }
    
    public String getHighlightedContent() { return highlightedContent; }
    public void setHighlightedContent(String highlightedContent) { this.highlightedContent = highlightedContent; }
    
    public int getMatchCount() { return matchCount; }
    public void setMatchCount(int matchCount) { this.matchCount = matchCount; }
    
    public double getRelevanceScore() { return relevanceScore; }
    public void setRelevanceScore(double relevanceScore) { this.relevanceScore = relevanceScore; }
}
