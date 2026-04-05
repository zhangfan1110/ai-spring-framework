package com.example.aiframework.service;

import com.example.aiframework.entity.AsyncTaskEntity;
import com.example.aiframework.mapper.AsyncTaskMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 任务队列服务
 */
@Service
public class TaskQueueService {

    @Autowired
    private AsyncTaskMapper asyncTaskMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String TASK_QUEUE_PREFIX = "task:queue:";
    private static final String TASK_DELAY_PREFIX = "task:delay:";

    /**
     * 提交异步任务
     */
    public String submitTask(String taskType, Map<String, Object> data, int priority, long delaySeconds, int maxRetry) {
        String taskId = UUID.randomUUID().toString();

        AsyncTaskEntity task = new AsyncTaskEntity();
        task.setId(taskId);
        task.setTaskType(taskType);
        task.setTaskData(toJson(data));
        task.setStatus("PENDING");
        task.setPriority(priority);
        task.setDelaySeconds(delaySeconds > 0 ? delaySeconds : null);
        task.setMaxRetry(maxRetry);
        task.setRetryCount(0);

        if (delaySeconds > 0) {
            // 延迟任务
            task.setCreateTime(LocalDateTime.now().plusSeconds(delaySeconds));
            asyncTaskMapper.insert(task);
            
            // 添加到延迟队列
            redisTemplate.opsForZSet().add(
                TASK_DELAY_PREFIX,
                taskId,
                System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(delaySeconds)
            );
        } else {
            // 立即执行
            task.setCreateTime(LocalDateTime.now());
            asyncTaskMapper.insert(task);
            
            // 添加到任务队列
            String queueKey = TASK_QUEUE_PREFIX + "default";
            redisTemplate.opsForList().leftPush(queueKey, taskId);
        }

        return taskId;
    }

    /**
     * 从队列获取任务
     */
    public AsyncTaskEntity pollTask(String queue) {
        String queueKey = TASK_QUEUE_PREFIX + queue;
        String taskId = (String) redisTemplate.opsForList().rightPop(queueKey);
        
        if (taskId == null) {
            return null;
        }

        AsyncTaskEntity task = asyncTaskMapper.selectById(taskId);
        if (task != null) {
            task.setStatus("RUNNING");
            task.setStartTime(LocalDateTime.now());
            asyncTaskMapper.updateById(task);
        }

        return task;
    }

    /**
     * 处理延迟任务
     */
    public List<String> processDelayTasks() {
        long now = System.currentTimeMillis();
        return redisTemplate.opsForZSet()
            .rangeByScore(TASK_DELAY_PREFIX, 0, now)
            .stream()
            .map(obj -> (String) obj)
            .toList();
    }

    /**
     * 移除延迟任务
     */
    public void removeDelayTask(String taskId) {
        redisTemplate.opsForZSet().remove(TASK_DELAY_PREFIX, taskId);
    }

    /**
     * 更新任务状态
     */
    public void updateTaskStatus(String taskId, String status, String result, String errorMessage) {
        AsyncTaskEntity task = asyncTaskMapper.selectById(taskId);
        if (task == null) {
            return;
        }

        task.setStatus(status);
        task.setResult(result);
        task.setErrorMessage(errorMessage);
        
        if ("SUCCESS".equals(status) || "FAILED".equals(status)) {
            task.setEndTime(LocalDateTime.now());
        }

        asyncTaskMapper.updateById(task);
    }

    /**
     * 重试任务
     */
    public boolean retryTask(String taskId) {
        AsyncTaskEntity task = asyncTaskMapper.selectById(taskId);
        if (task == null || task.getRetryCount() >= task.getMaxRetry()) {
            return false;
        }

        task.setRetryCount(task.getRetryCount() + 1);
        task.setStatus("PENDING");
        task.setErrorMessage(null);
        task.setStartTime(null);
        asyncTaskMapper.updateById(task);

        // 重新加入队列
        String queueKey = TASK_QUEUE_PREFIX + "default";
        redisTemplate.opsForList().leftPush(queueKey, taskId);

        return true;
    }

    /**
     * 获取任务状态
     */
    public AsyncTaskEntity getTaskStatus(String taskId) {
        return asyncTaskMapper.selectById(taskId);
    }

    /**
     * 获取队列长度
     */
    public Long getQueueSize(String queue) {
        String queueKey = TASK_QUEUE_PREFIX + queue;
        return redisTemplate.opsForList().size(queueKey);
    }

    /**
     * 获取任务统计
     */
    public Map<String, Object> getTaskStats() {
        Map<String, Object> stats = new HashMap<>();
        
        List<AsyncTaskMapper.TaskCount> statusCounts = asyncTaskMapper.countByStatus();
        for (AsyncTaskMapper.TaskCount count : statusCounts) {
            stats.put(count.getStatus().toLowerCase(), count.getCount());
        }

        stats.put("delayQueueSize", redisTemplate.opsForZSet().size(TASK_DELAY_PREFIX));
        stats.put("defaultQueueSize", getQueueSize("default"));

        return stats;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }

    private <T> T fromJson(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            return null;
        }
    }
}
