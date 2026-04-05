package com.example.aiframework.dto;

/**
 * 会话导出元数据
 */
public class ChatSessionExportDTO_ExportMetadata {
    private String version;
    private java.time.LocalDateTime exportTime;
    private String exportedBy;
    
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public java.time.LocalDateTime getExportTime() { return exportTime; }
    public void setExportTime(java.time.LocalDateTime exportTime) { this.exportTime = exportTime; }
    public String getExportedBy() { return exportedBy; }
    public void setExportedBy(String exportedBy) { this.exportedBy = exportedBy; }
}
