package com.example.aiframework.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * Agent 任务日志实体
 */
@TableName("agent_task_logs")
public class AgentTaskLogEntity {
    
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String taskId;
    private String logType;
    private String content;
    private String metadata;
    private LocalDateTime createdAt;
    
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    
    public String getLogType() { return logType; }
    public void setLogType(String logType) { this.logType = logType; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
