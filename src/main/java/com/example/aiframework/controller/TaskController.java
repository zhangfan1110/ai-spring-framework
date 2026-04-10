package com.example.aiframework.controller;

import com.example.aiframework.task.entity.AsyncTaskEntity;
import com.example.aiframework.task.service.TaskQueueService;
import com.example.aiframework.common.util.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 异步任务控制器
 */
@RestController
@RequestMapping("/api/task")
@Tag(name = "异步任务", description = "异步任务管理")
public class TaskController {

    @Autowired
    private TaskQueueService taskQueueService;

    @Operation(summary = "提交异步任务")
    @PostMapping("/submit")
    public Result<Map<String, Object>> submitTask(
            @RequestParam String type,
            @RequestBody(required = false) Map<String, Object> data,
            @RequestParam(defaultValue = "3") int priority,
            @RequestParam(defaultValue = "0") long delay,
            @RequestParam(defaultValue = "3") int maxRetry
    ) {
        if (data == null) {
            data = new HashMap<>();
        }

        String taskId = taskQueueService.submitTask(type, data, priority, delay, maxRetry);

        Map<String, Object> result = new HashMap<>();
        result.put("taskId", taskId);
        result.put("status", "PENDING");
        result.put("message", "任务已提交");

        return Result.success(result);
    }

    @Operation(summary = "查询任务状态")
    @GetMapping("/status/{taskId}")
    public Result<AsyncTaskEntity> getTaskStatus(@PathVariable String taskId) {
        AsyncTaskEntity task = taskQueueService.getTaskStatus(taskId);
        
        if (task == null) {
            return Result.error("任务不存在");
        }

        return Result.success(task);
    }

    @Operation(summary = "获取任务统计")
    @GetMapping("/stats")
    public Result<Map<String, Object>> getTaskStats() {
        Map<String, Object> stats = taskQueueService.getTaskStats();
        return Result.success(stats);
    }

    @Operation(summary = "获取队列长度")
    @GetMapping("/queue/{queueName}/size")
    public Result<Map<String, Object>> getQueueSize(@PathVariable String queueName) {
        Long size = taskQueueService.getQueueSize(queueName);
        
        Map<String, Object> result = new HashMap<>();
        result.put("queue", queueName);
        result.put("size", size);

        return Result.success(result);
    }

    @Operation(summary = "重试失败任务")
    @PostMapping("/retry/{taskId}")
    public Result<Map<String, Object>> retryTask(@PathVariable String taskId) {
        boolean retried = taskQueueService.retryTask(taskId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("taskId", taskId);
        result.put("retried", retried);
        result.put("message", retried ? "任务已重新加入队列" : "无法重试（可能超过最大重试次数）");

        return Result.success(result);
    }
}
