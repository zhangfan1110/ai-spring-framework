package com.example.aiframework.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 会话导出元数据
 */
@Setter
@Getter
public class ChatSessionExportDTO_ExportMetadata {
    private String version;
    private java.time.LocalDateTime exportTime;
    private String exportedBy;

}
