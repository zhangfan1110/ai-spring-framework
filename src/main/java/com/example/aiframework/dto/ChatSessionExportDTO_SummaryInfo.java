package com.example.aiframework.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 摘要信息
 */
@Setter
@Getter
public class ChatSessionExportDTO_SummaryInfo {
    private String summary;
    private String keyPoints;
    private java.time.LocalDateTime createTime;

}
