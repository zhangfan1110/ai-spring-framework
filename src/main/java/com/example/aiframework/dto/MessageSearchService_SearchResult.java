package com.example.aiframework.dto;

import com.example.aiframework.entity.ChatMessageEntity;
import lombok.Getter;
import lombok.Setter;

/**
 * 消息搜索结果
 */
@Setter
@Getter
public class MessageSearchService_SearchResult {
    private ChatMessageEntity message;
    private String highlightedContent;
    private int matchCount;
    private double relevanceScore;

}
