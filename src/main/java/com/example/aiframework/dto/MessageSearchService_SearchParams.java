package com.example.aiframework.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 消息搜索参数
 */
@Setter
@Getter
public class MessageSearchService_SearchParams {
    private String sessionId;
    private String keyword;
    private String role; // USER/AI/SYSTEM
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private int limit = 20;
    private boolean highlight = true;
    private boolean globalSearch = false;

}
