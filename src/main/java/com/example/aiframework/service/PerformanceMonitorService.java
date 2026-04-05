package com.example.aiframework.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 性能监控服务
 */
@Slf4j
@Service
public class PerformanceMonitorService {

    /**
     * 调用链执行时间统计
     */
    private final Map<String, AtomicLong> chainExecutionTimes = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> chainExecutionCounts = new ConcurrentHashMap<>();

    /**
     * 工具调用统计
     */
    private final Map<String, AtomicLong> toolCallCounts = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> toolSuccessCounts = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> toolFailCounts = new ConcurrentHashMap<>();

    /**
     * 记录调用链执行时间
     */
    public void recordChainExecution(String chainId, long executionTimeMs) {
        chainExecutionTimes.computeIfAbsent(chainId, k -> new AtomicLong(0))
                .addAndGet(executionTimeMs);
        chainExecutionCounts.computeIfAbsent(chainId, k -> new AtomicLong(0))
                .incrementAndGet();

        log.debug("记录调用链执行：chainId={}, executionTime={}ms", chainId, executionTimeMs);
    }

    /**
     * 记录工具调用
     */
    public void recordToolCall(String toolName, boolean success) {
        toolCallCounts.computeIfAbsent(toolName, k -> new AtomicLong(0))
                .incrementAndGet();

        if (success) {
            toolSuccessCounts.computeIfAbsent(toolName, k -> new AtomicLong(0))
                    .incrementAndGet();
        } else {
            toolFailCounts.computeIfAbsent(toolName, k -> new AtomicLong(0))
                    .incrementAndGet();
        }

        log.debug("记录工具调用：toolName={}, success={}", toolName, success);
    }

    /**
     * 获取调用链平均执行时间
     */
    public Map<String, Object> getChainStats(String chainId) {
        AtomicLong totalTime = chainExecutionTimes.get(chainId);
        AtomicLong count = chainExecutionCounts.get(chainId);

        if (totalTime == null || count == null || count.get() == 0) {
            return null;
        }

        return Map.of(
                "chainId", chainId,
                "totalCount", count.get(),
                "totalTimeMs", totalTime.get(),
                "avgTimeMs", totalTime.get() / count.get()
        );
    }

    /**
     * 获取所有调用链统计
     */
    public Map<String, Object> getAllChainStats() {
        Map<String, Object> result = new ConcurrentHashMap<>();
        chainExecutionTimes.forEach((chainId, totalTime) -> {
            result.put(chainId, getChainStats(chainId));
        });
        return result;
    }

    /**
     * 获取工具调用统计
     */
    public Map<String, Object> getToolStats(String toolName) {
        AtomicLong totalCalls = toolCallCounts.get(toolName);
        AtomicLong successCalls = toolSuccessCounts.get(toolName);
        AtomicLong failCalls = toolFailCounts.get(toolName);

        if (totalCalls == null) {
            return null;
        }

        long success = successCalls != null ? successCalls.get() : 0;
        long fail = failCalls != null ? failCalls.get() : 0;
        double successRate = totalCalls.get() > 0 ? (double) success / totalCalls.get() * 100 : 0;

        return Map.of(
                "toolName", toolName,
                "totalCalls", totalCalls.get(),
                "successCalls", success,
                "failCalls", fail,
                "successRate", String.format("%.2f%%", successRate)
        );
    }

    /**
     * 获取所有工具统计
     */
    public Map<String, Object> getAllToolStats() {
        Map<String, Object> result = new ConcurrentHashMap<>();
        toolCallCounts.forEach((toolName, totalCalls) -> {
            result.put(toolName, getToolStats(toolName));
        });
        return result;
    }

    /**
     * 重置统计
     */
    public void resetStats() {
        chainExecutionTimes.clear();
        chainExecutionCounts.clear();
        toolCallCounts.clear();
        toolSuccessCounts.clear();
        toolFailCounts.clear();
        log.info("性能统计已重置");
    }
}
