package com.example.aiframework.service;

import com.example.aiframework.entity.AsyncTaskEntity;
import com.example.aiframework.mapper.AsyncTaskMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * 任务执行器服务
 */
@Service
public class TaskExecutorService {

    private static final Logger log = LoggerFactory.getLogger(TaskExecutorService.class);

    @Autowired
    private TaskQueueService taskQueueService;

    @Autowired
    private AsyncTaskMapper asyncTaskMapper;

    @Autowired
    private ObjectMapper objectMapper;

    // 线程池配置
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
    private final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();

    /**
     * 启动任务处理器
     */
    @Scheduled(fixedDelay = 1000)
    public void processTasks() {
        try {
            // 处理延迟任务
            processDelayTasks();

            // 处理普通任务
            AsyncTaskEntity task = taskQueueService.pollTask("default");
            if (task != null) {
                executeTask(task);
            }
        } catch (Exception e) {
            log.error("Error processing task", e);
        }
    }

    /**
     * 处理延迟任务
     */
    private void processDelayTasks() {
        List<String> readyTaskIds = taskQueueService.processDelayTasks();
        for (String taskId : readyTaskIds) {
            AsyncTaskEntity task = asyncTaskMapper.selectById(taskId);
            if (task != null && "PENDING".equals(task.getStatus())) {
                taskQueueService.removeDelayTask(taskId);
                
                // 重新加入普通队列
                String queueKey = "task:queue:default";
                executorService.execute(() -> {
                    // 这里简化处理，实际应该通过 TaskQueueService
                });
            }
        }
    }

    /**
     * 执行任务
     */
    private void executeTask(AsyncTaskEntity task) {
        log.info("Executing task: {} type: {}", task.getId(), task.getTaskType());

        executorService.submit(() -> {
            try {
                Map<String, Object> data = objectMapper.readValue(
                    task.getTaskData(), 
                    Map.class
                );

                // 根据任务类型执行不同的处理器
                String result = executeByType(task.getTaskType(), data);

                taskQueueService.updateTaskStatus(task.getId(), "SUCCESS", result, null);
                log.info("Task completed: {} result: {}", task.getId(), result);

            } catch (Exception e) {
                log.error("Task failed: {}", task.getId(), e);
                handleError(task, e);
            }
        });
    }

    /**
     * 根据任务类型执行
     */
    private String executeByType(String taskType, Map<String, Object> data) throws Exception {
        switch (taskType) {
            case "CHAT_RESPONSE":
                return handleChatResponse(data);
            
            case "SESSION_SUMMARY":
                return handleSessionSummary(data);
            
            case "TITLE_GENERATION":
                return handleTitleGeneration(data);
            
            case "MEMORY_COMPRESSION":
                return handleMemoryCompression(data);
            
            case "EMAIL_SEND":
                return handleEmailSend(data);
            
            case "FILE_PROCESS":
                return handleFileProcess(data);
            
            default:
                log.warn("Unknown task type: {}", taskType);
                return "Unknown task type";
        }
    }

    /**
     * 处理聊天响应任务
     */
    private String handleChatResponse(Map<String, Object> data) {
        // 模拟异步聊天响应处理
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "Chat response processed";
    }

    /**
     * 处理会话摘要任务
     */
    private String handleSessionSummary(Map<String, Object> data) {
        String sessionId = (String) data.get("sessionId");
        log.info("Generating summary for session: {}", sessionId);
        return "Summary generated for session: " + sessionId;
    }

    /**
     * 处理标题生成任务
     */
    private String handleTitleGeneration(Map<String, Object> data) {
        String sessionId = (String) data.get("sessionId");
        log.info("Generating title for session: {}", sessionId);
        return "Title generated for session: " + sessionId;
    }

    /**
     * 处理记忆压缩任务
     */
    private String handleMemoryCompression(Map<String, Object> data) {
        String sessionId = (String) data.get("sessionId");
        log.info("Compressing memory for session: {}", sessionId);
        return "Memory compressed for session: " + sessionId;
    }

    /**
     * 处理邮件发送任务
     */
    private String handleEmailSend(Map<String, Object> data) {
        String to = (String) data.get("to");
        String subject = (String) data.get("subject");
        log.info("Sending email to: {} subject: {}", to, subject);
        return "Email sent to: " + to;
    }

    /**
     * 处理文件处理任务
     */
    private String handleFileProcess(Map<String, Object> data) {
        String filePath = (String) data.get("filePath");
        log.info("Processing file: {}", filePath);
        return "File processed: " + filePath;
    }

    /**
     * 处理错误
     */
    private void handleError(AsyncTaskEntity task, Exception e) {
        if (task.getRetryCount() < task.getMaxRetry()) {
            // 重试
            boolean retried = taskQueueService.retryTask(task.getId());
            if (retried) {
                log.info("Task {} will be retried (attempt {}/{})", 
                    task.getId(), task.getRetryCount() + 1, task.getMaxRetry());
                return;
            }
        }

        // 重试失败，标记为失败
        taskQueueService.updateTaskStatus(
            task.getId(), 
            "FAILED", 
            null, 
            e.getMessage()
        );
    }

    /**
     * 清理超时任务
     */
    @Scheduled(fixedDelay = 60000) // 每分钟执行一次
    public void cleanupTimeoutTasks() {
        LocalDateTime timeoutThreshold = LocalDateTime.now().minusMinutes(30);
        List<AsyncTaskEntity> timeoutTasks = asyncTaskMapper.findTimeoutTasks(timeoutThreshold);

        for (AsyncTaskEntity task : timeoutTasks) {
            log.warn("Task timeout: {} type: {} started: {}", 
                task.getId(), task.getTaskType(), task.getStartTime());
            
            // 尝试重试或标记失败
            handleError(task, new TimeoutException("Task execution timeout"));
        }
    }

    /**
     * 关闭线程池
     */
    public void shutdown() {
        executorService.shutdown();
        scheduledExecutor.shutdown();
        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
