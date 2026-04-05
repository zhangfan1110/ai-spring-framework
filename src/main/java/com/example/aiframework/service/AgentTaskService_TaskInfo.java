package com.example.aiframework.service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Agent 任务信息
 */
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
    
    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    public String getTaskType() { return taskType; }
    public void setTaskType(String taskType) { this.taskType = taskType; }
    public Map<String, Object> getInput() { return input; }
    public void setInput(Map<String, Object> input) { this.input = input; }
    public AgentTaskService.TaskStatus getStatus() { return status; }
    public void setStatus(AgentTaskService.TaskStatus status) { this.status = status; }
    public int getProgress() { return progress; }
    public void setProgress(int progress) { this.progress = progress; }
    public String getCurrentStep() { return currentStep; }
    public void setCurrentStep(String currentStep) { this.currentStep = currentStep; }
    public Object getResult() { return result; }
    public void setResult(Object result) { this.result = result; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public List<AgentTaskService_TaskLog> getLogs() { return logs; }
    public void setLogs(List<AgentTaskService_TaskLog> logs) { this.logs = logs; }
    public void addLog(AgentTaskService_TaskLog log) { this.logs.add(log); }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}
