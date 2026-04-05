package com.example.aiframework.dto;

import com.example.aiframework.service.AgentTaskService;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Agent 任务信息
 */
@Setter
@Getter
public class AgentTaskService_TaskInfo {
    private String taskId;
    private String taskType;
    private Map<String, Object> input;
    private AgentTaskService.TaskStatus status;
    private int progress;
    private String currentStep;
    private Object result;
    private String errorMessage;
    private List<AgentTaskService_TaskLog> logs;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    
    public AgentTaskService_TaskInfo() {
        this.logs = new ArrayList<>();
    }

    public void addLog(AgentTaskService_TaskLog log) { this.logs.add(log); }

}
