package com.example.aiframework.controller;

import com.example.aiframework.chat.dto.ChatSessionStatsDTO;
import com.example.aiframework.chat.service.CleanupService;
import com.example.aiframework.chat.service.SessionStatsService;
import com.example.aiframework.common.util.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 系统管理接口
 */
@RestController
@RequestMapping("/api/system")
@Tag(name = "系统管理", description = "系统统计、清理、健康检查等功能")
public class SystemController {
    
    private static final Logger log = LoggerFactory.getLogger(SystemController.class);
    
    @Autowired(required = false)
    private SessionStatsService sessionStatsService;
    
    @Autowired(required = false)
    private CleanupService cleanupService;
    
    // ========== 统计信息 ==========
    
    @Operation(summary = "获取统计信息", description = "获取系统总体统计信息")
    @GetMapping("/stats")
    public Result<ChatSessionStatsDTO> getStats() {
        log.info("获取统计信息");
        if (sessionStatsService == null) {
            return Result.error("统计服务未启用");
        }
        ChatSessionStatsDTO stats = sessionStatsService.getOverallStats();
        return Result.success(stats);
    }
    
    @Operation(summary = "获取会话统计", description = "获取指定会话的统计信息")
    @GetMapping("/stats/{sessionId}")
    public Result<ChatSessionStatsDTO> getSessionStats(@PathVariable String sessionId) {
        log.info("获取会话统计：{}", sessionId);
        if (sessionStatsService == null) {
            return Result.error("统计服务未启用");
        }
        ChatSessionStatsDTO stats = sessionStatsService.getSessionStats(sessionId);
        return Result.success(stats);
    }
    
    // ========== 系统信息 ==========
    
    @Operation(summary = "系统信息", description = "获取系统运行信息")
    @GetMapping("/info")
    public Result<Map<String, Object>> getSystemInfo() {
        Map<String, Object> info = new HashMap<>();
        
        // JVM 信息
        info.put("javaVersion", System.getProperty("java.version"));
        info.put("javaVendor", System.getProperty("java.vendor"));
        info.put("osName", System.getProperty("os.name"));
        info.put("osArch", System.getProperty("os.arch"));
        
        // 内存信息
        Runtime runtime = Runtime.getRuntime();
        info.put("maxMemory", runtime.maxMemory() / 1024 / 1024 + " MB");
        info.put("totalMemory", runtime.totalMemory() / 1024 / 1024 + " MB");
        info.put("freeMemory", runtime.freeMemory() / 1024 / 1024 + " MB");
        info.put("availableProcessors", runtime.availableProcessors());
        
        // 运行时间
        long uptime = java.lang.management.ManagementFactory.getRuntimeMXBean().getUptime();
        info.put("uptime", formatUptime(uptime));
        
        return Result.success(info);
    }
    
    // ========== 清理任务 ==========
    
    @Operation(summary = "手动触发清理", description = "手动执行定时清理任务")
    @PostMapping("/cleanup/run")
    public Result<Void> runCleanup() {
        log.info("手动触发清理任务");
        if (cleanupService == null) {
            return Result.error("清理服务未启用");
        }
        cleanupService.manualCleanup();
        return Result.success();
    }
    
    @Operation(summary = "清理会话", description = "删除指定会话")
    @DeleteMapping("/cleanup/{sessionId}")
    public Result<Void> cleanupSession(@PathVariable String sessionId) {
        log.info("清理会话：{}", sessionId);
        if (cleanupService == null) {
            return Result.error("清理服务未启用");
        }
        cleanupService.cleanupSession(sessionId);
        return Result.success();
    }
    
    /**
     * 格式化运行时间
     */
    private String formatUptime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        return String.format("%d 天 %d 小时 %d 分钟 %d 秒", 
            days, hours % 24, minutes % 60, seconds % 60);
    }
}
