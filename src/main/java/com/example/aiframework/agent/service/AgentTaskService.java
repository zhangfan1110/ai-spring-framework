package com.example.aiframework.agent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aiframework.agent.entity.AgentTaskEntity;
import com.example.aiframework.agent.entity.AgentTaskLogEntity;
import com.example.aiframework.agent.mapper.AgentTaskLogMapper;
import com.example.aiframework.agent.mapper.AgentTaskMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Agent 异步任务服务（数据库持久化版本）
 */
@Service
public class AgentTaskService {
    
    private static final Logger log = LoggerFactory.getLogger(AgentTaskService.class);
    
    @Autowired
    private ReActAgentService agentService;
    
    @Autowired
    private AgentTaskMapper taskMapper;
    
    @Autowired
    private AgentTaskLogMapper logMapper;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    // 内存缓存（用于实时状态更新）
    private final Map<String, TaskInfo> taskCache = new ConcurrentHashMap<>();
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * 任务状态枚举
     */
    public enum TaskStatus {
        PENDING("等待中"),
        RUNNING("执行中"),
        COMPLETED("已完成"),
        FAILED("失败"),
        CANCELLED("已取消");
        
        private final String description;
        
        TaskStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() { return description; }
    }
    
    /**
     * 任务信息
     */
    public static class TaskInfo {
        private String taskId;
        private String taskType;
        private Map<String, Object> input;
        private TaskStatus status;
        private int progress;
        private String currentStep;
        private Object result;
        private String errorMessage;
        private List<TaskLog> logs;
        private LocalDateTime createdAt;
        private LocalDateTime startedAt;
        private LocalDateTime completedAt;
        
        public TaskInfo() {
            this.logs = new ArrayList<>();
        }
        
        // Getters and Setters
        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }
        public String getTaskType() { return taskType; }
        public void setTaskType(String taskType) { this.taskType = taskType; }
        public Map<String, Object> getInput() { return input; }
        public void setInput(Map<String, Object> input) { this.input = input; }
        public TaskStatus getStatus() { return status; }
        public void setStatus(TaskStatus status) { this.status = status; }
        public int getProgress() { return progress; }
        public void setProgress(int progress) { this.progress = progress; }
        public String getCurrentStep() { return currentStep; }
        public void setCurrentStep(String currentStep) { this.currentStep = currentStep; }
        public Object getResult() { return result; }
        public void setResult(Object result) { this.result = result; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public List<TaskLog> getLogs() { return logs; }
        public void setLogs(List<TaskLog> logs) { this.logs = logs; }
        public void addLog(TaskLog log) { this.logs.add(log); }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public LocalDateTime getStartedAt() { return startedAt; }
        public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
        public LocalDateTime getCompletedAt() { return completedAt; }
        public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    }
    
    /**
     * 任务日志
     */
    public static class TaskLog {
        private String logType;
        private String content;
        private Map<String, Object> metadata;
        private LocalDateTime timestamp;
        
        public TaskLog() {
            this.timestamp = LocalDateTime.now();
        }
        
        public String getLogType() { return logType; }
        public void setLogType(String logType) { this.logType = logType; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }
    
    /**
     * 异步执行 ReAct 任务
     */
    public String executeReActAsync(String task, String role) {
        String taskId = UUID.randomUUID().toString();
        
        try {
            // 创建任务记录
            AgentTaskEntity entity = new AgentTaskEntity();
            entity.setId(taskId);
            entity.setTaskType("REACT");
            entity.setStatus(TaskStatus.PENDING.name());
            entity.setProgress(0);
            entity.setCurrentStep("等待执行...");
            
            Map<String, Object> input = new HashMap<>();
            input.put("task", task);
            input.put("role", role);
            entity.setTaskInput(objectMapper.writeValueAsString(input));
            
            taskMapper.insert(entity);
            
            // 同时更新缓存
            TaskInfo taskInfo = new TaskInfo();
            taskInfo.setTaskId(taskId);
            taskInfo.setTaskType("REACT");
            taskInfo.setStatus(TaskStatus.PENDING);
            taskInfo.setProgress(0);
            taskInfo.setCurrentStep("等待执行...");
            taskInfo.setInput(input);
            taskInfo.setCreatedAt(LocalDateTime.now());
            taskCache.put(taskId, taskInfo);
            
            log.info("创建异步任务：{}", taskId);
            
            // 异步执行
            executeTaskAsync(taskId, task, role);
            
            return taskId;
            
        } catch (Exception e) {
            log.error("创建异步任务失败：{}", taskId, e);
            throw new RuntimeException("创建任务失败：" + e.getMessage(), e);
        }
    }
    
    /**
     * 异步执行任务
     */
    @Async
    public CompletableFuture<String> executeTaskAsync(String taskId, String task, String role) {
        log.info("开始异步执行任务：{}", taskId);
        
        try {
            // 更新状态为运行中
            updateTaskStatus(taskId, TaskStatus.RUNNING, 10, "开始执行...");
            
            // 添加日志
            addLog(taskId, "INFO", "任务开始执行", null);
            
            // 执行 ReAct 任务
            String result;
            if (role != null && !role.isEmpty()) {
                ReActAgentService.AgentRole agentRole = ReActAgentService.AgentRole.valueOf(role.toUpperCase());
                result = agentService.executeWithRole(task, agentRole);
            } else {
                result = agentService.execute(task);
            }
            
            // 执行完成
            updateTaskStatus(taskId, TaskStatus.COMPLETED, 100, "执行完成");
            saveResult(taskId, result);
            addLog(taskId, "INFO", "任务执行完成", null);
            
            log.info("异步任务完成：{}", taskId);
            
        } catch (Exception e) {
            log.error("异步任务失败：{}", taskId, e);
            
            updateTaskStatus(taskId, TaskStatus.FAILED, 0, "执行失败");
            saveError(taskId, e.getMessage());
            addLog(taskId, "ERROR", "任务执行失败：" + e.getMessage(), null);
        }
        
        return CompletableFuture.completedFuture(taskId);
    }
    
    /**
     * 获取任务状态
     */
    public TaskInfo getTaskStatus(String taskId) {
        // 先从缓存获取
        TaskInfo cached = taskCache.get(taskId);
        if (cached != null) {
            return cached;
        }
        
        // 从数据库获取
        try {
            AgentTaskEntity entity = taskMapper.selectById(taskId);
            if (entity == null) {
                return null;
            }
            
            TaskInfo taskInfo = new TaskInfo();
            taskInfo.setTaskId(entity.getId());
            taskInfo.setTaskType(entity.getTaskType());
            taskInfo.setStatus(TaskStatus.valueOf(entity.getStatus()));
            taskInfo.setProgress(entity.getProgress());
            taskInfo.setCurrentStep(entity.getCurrentStep());
            taskInfo.setCreatedAt(entity.getCreatedAt());
            taskInfo.setStartedAt(entity.getStartedAt());
            taskInfo.setCompletedAt(entity.getCompletedAt());
            
            // 解析输入
            if (entity.getTaskInput() != null) {
                taskInfo.setInput(objectMapper.readValue(
                    entity.getTaskInput(),
                    new TypeReference<Map<String, Object>>() {}
                ));
            }
            
            // 解析结果
            if (entity.getResult() != null) {
                taskInfo.setResult(objectMapper.readValue(
                    entity.getResult(),
                    new TypeReference<Object>() {}
                ));
            }
            
            taskInfo.setErrorMessage(entity.getErrorMessage());
            
            // 获取日志
            List<AgentTaskLogEntity> logEntities = logMapper.selectByTaskId(taskId, 50);
            for (AgentTaskLogEntity logEntity : logEntities) {
                TaskLog taskLog = new TaskLog();
                taskLog.setLogType(logEntity.getLogType());
                taskLog.setContent(logEntity.getContent());
                if (logEntity.getMetadata() != null) {
                    taskLog.setMetadata(objectMapper.readValue(
                        logEntity.getMetadata(),
                        new TypeReference<Map<String, Object>>() {}
                    ));
                }
                taskLog.setTimestamp(logEntity.getCreatedAt());
                taskInfo.addLog(taskLog);
            }
            
            // 更新缓存
            taskCache.put(taskId, taskInfo);
            
            return taskInfo;
            
        } catch (Exception e) {
            log.error("获取任务状态失败：{}", taskId, e);
            return null;
        }
    }
    
    /**
     * 获取任务列表
     */
    public List<Map<String, Object>> getTaskList(String status, int limit) {
        List<Map<String, Object>> tasks = new ArrayList<>();
        
        try {
            List<AgentTaskEntity> entities = taskMapper.selectTaskList(status, limit);
            
            for (AgentTaskEntity entity : entities) {
                Map<String, Object> task = new HashMap<>();
                task.put("id", entity.getId());
                task.put("taskType", entity.getTaskType());
                task.put("status", entity.getStatus());
                task.put("progress", entity.getProgress());
                task.put("currentStep", entity.getCurrentStep());
                task.put("createdAt", entity.getCreatedAt());
                task.put("completedAt", entity.getCompletedAt());
                tasks.add(task);
            }
            
        } catch (Exception e) {
            log.error("获取任务列表失败", e);
        }
        
        return tasks;
    }
    
    /**
     * 取消任务
     */
    public boolean cancelTask(String taskId) {
        try {
            TaskInfo taskInfo = getTaskStatus(taskId);
            if (taskInfo != null && taskInfo.getStatus() == TaskStatus.RUNNING) {
                updateTaskStatus(taskId, TaskStatus.CANCELLED, 0, "任务已取消");
                addLog(taskId, "INFO", "任务已取消", null);
                log.info("取消任务：{}", taskId);
                return true;
            }
            return false;
            
        } catch (Exception e) {
            log.error("取消任务失败：{}", taskId, e);
            return false;
        }
    }
    
    /**
     * 中断任务（可恢复）
     */
    public boolean pauseTask(String taskId) {
        try {
            TaskInfo taskInfo = getTaskStatus(taskId);
            if (taskInfo != null && taskInfo.getStatus() == TaskStatus.RUNNING) {
                updateTaskStatus(taskId, TaskStatus.PENDING, taskInfo.getProgress(), "任务已暂停");
                addLog(taskId, "INFO", "任务已暂停，可随时恢复", null);
                log.info("中断任务：{}", taskId);
                return true;
            }
            return false;
            
        } catch (Exception e) {
            log.error("中断任务失败：{}", taskId, e);
            return false;
        }
    }
    
    /**
     * 恢复任务
     */
    public boolean resumeTask(String taskId) {
        try {
            TaskInfo taskInfo = getTaskStatus(taskId);
            if (taskInfo != null && taskInfo.getStatus() == TaskStatus.PENDING) {
                // 重新异步执行
                Map<String, Object> input = taskInfo.getInput();
                String task = (String) input.get("task");
                String role = (String) input.get("role");
                
                updateTaskStatus(taskId, TaskStatus.RUNNING, taskInfo.getProgress(), "任务恢复执行");
                addLog(taskId, "INFO", "任务恢复执行", null);
                
                executeTaskAsync(taskId, task, role);
                log.info("恢复任务：{}", taskId);
                return true;
            }
            return false;
            
        } catch (Exception e) {
            log.error("恢复任务失败：{}", taskId, e);
            return false;
        }
    }
    
    /**
     * 更新任务状态
     */
    private void updateTaskStatus(String taskId, TaskStatus status, int progress, String currentStep) {
        try {
            AgentTaskEntity entity = new AgentTaskEntity();
            entity.setId(taskId);
            entity.setStatus(status.name());
            entity.setProgress(progress);
            entity.setCurrentStep(currentStep);
            
            if (status == TaskStatus.RUNNING) {
                entity.setStartedAt(LocalDateTime.now());
            } else if (status == TaskStatus.COMPLETED || status == TaskStatus.FAILED || status == TaskStatus.CANCELLED) {
                entity.setCompletedAt(LocalDateTime.now());
            }
            
            taskMapper.updateById(entity);
            
            // 更新缓存
            TaskInfo taskInfo = taskCache.get(taskId);
            if (taskInfo != null) {
                taskInfo.setStatus(status);
                taskInfo.setProgress(progress);
                taskInfo.setCurrentStep(currentStep);
            }
            
        } catch (Exception e) {
            log.error("更新任务状态失败：{}", taskId, e);
        }
    }
    
    /**
     * 保存结果
     */
    private void saveResult(String taskId, String result) {
        try {
            AgentTaskEntity entity = new AgentTaskEntity();
            entity.setId(taskId);
            entity.setResult(objectMapper.writeValueAsString(result));
            taskMapper.updateById(entity);
            
            // 更新缓存
            TaskInfo taskInfo = taskCache.get(taskId);
            if (taskInfo != null) {
                taskInfo.setResult(result);
            }
            
        } catch (Exception e) {
            log.error("保存结果失败：{}", taskId, e);
        }
    }
    
    /**
     * 保存错误
     */
    private void saveError(String taskId, String errorMessage) {
        try {
            AgentTaskEntity entity = new AgentTaskEntity();
            entity.setId(taskId);
            entity.setErrorMessage(errorMessage);
            taskMapper.updateById(entity);
            
            // 更新缓存
            TaskInfo taskInfo = taskCache.get(taskId);
            if (taskInfo != null) {
                taskInfo.setErrorMessage(errorMessage);
            }
            
        } catch (Exception e) {
            log.error("保存错误失败：{}", taskId, e);
        }
    }
    
    /**
     * 添加日志
     */
    public void addLog(String taskId, String logType, String content, Map<String, Object> metadata) {
        try {
            AgentTaskLogEntity logEntity = new AgentTaskLogEntity();
            logEntity.setId(UUID.randomUUID().toString());
            logEntity.setTaskId(taskId);
            logEntity.setLogType(logType);
            logEntity.setContent(content);
            if (metadata != null) {
                logEntity.setMetadata(objectMapper.writeValueAsString(metadata));
            }
            
            logMapper.insert(logEntity);
            
            // 更新缓存
            TaskInfo taskInfo = taskCache.get(taskId);
            if (taskInfo != null) {
                TaskLog taskLog = new TaskLog();
                taskLog.setLogType(logType);
                taskLog.setContent(content);
                taskLog.setMetadata(metadata);
                taskInfo.addLog(taskLog);
                
                // 限制日志数量
                if (taskInfo.getLogs().size() > 100) {
                    taskInfo.getLogs().remove(0);
                }
            }
            
        } catch (Exception e) {
            log.error("添加日志失败：{}", taskId, e);
        }
    }
    
    /**
     * 定时清理旧任务（每小时执行一次）
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void cleanupOldTasks() {
        try {
            String beforeTime = LocalDateTime.now().minusHours(24).format(DATE_FORMATTER);
            int deleted = taskMapper.deleteOldTasks(beforeTime);
            log.info("清理旧任务完成，删除 {} 条记录", deleted);
        } catch (Exception e) {
            log.error("清理旧任务失败", e);
        }
    }
}
