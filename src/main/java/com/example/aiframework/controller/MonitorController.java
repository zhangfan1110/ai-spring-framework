package com.example.aiframework.controller;

import com.example.aiframework.system.service.PerformanceMonitorService;
import com.example.aiframework.chat.service.TokenUsageService;
import com.example.aiframework.common.util.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 监控统计接口
 */
@RestController
@RequestMapping("/api/monitor")
@Tag(name = "监控统计", description = "性能监控和 Token 使用统计")
public class MonitorController {

    @Autowired
    private TokenUsageService tokenUsageService;

    @Autowired
    private PerformanceMonitorService performanceMonitorService;

    @Operation(summary = "获取今日 Token 使用统计")
    @GetMapping("/token/today")
    public Result<Map<String, Object>> getTodayTokenStats() {
        return Result.success(tokenUsageService.getTodayStats());
    }

    @Operation(summary = "获取本周 Token 使用统计")
    @GetMapping("/token/week")
    public Result<Map<String, Object>> getWeekTokenStats() {
        return Result.success(tokenUsageService.getWeekStats());
    }

    @Operation(summary = "获取本月 Token 使用统计")
    @GetMapping("/token/month")
    public Result<Map<String, Object>> getMonthTokenStats() {
        return Result.success(tokenUsageService.getMonthStats());
    }

    @Operation(summary = "获取指定时间范围 Token 统计")
    @GetMapping("/token/stats")
    public Result<Map<String, Object>> getTokenStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate
    ) {
        return Result.success(tokenUsageService.getStats(startDate, endDate));
    }

    @Operation(summary = "按日期统计 Token 使用")
    @GetMapping("/token/by-date")
    public Result<Object> getTokenStatsByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate
    ) {
        return Result.success(tokenUsageService.getStatsByDate(startDate, endDate));
    }

    @Operation(summary = "按模型统计 Token 使用")
    @GetMapping("/token/by-model")
    public Result<Object> getTokenStatsByModel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate
    ) {
        return Result.success(tokenUsageService.getStatsByModel(startDate, endDate));
    }

    @Operation(summary = "获取调用链性能统计")
    @GetMapping("/performance/chains")
    public Result<Map<String, Object>> getChainStats() {
        return Result.success(performanceMonitorService.getAllChainStats());
    }

    @Operation(summary = "获取工具调用统计")
    @GetMapping("/performance/tools")
    public Result<Map<String, Object>> getToolStats() {
        return Result.success(performanceMonitorService.getAllToolStats());
    }

    @Operation(summary = "重置性能统计")
    @PostMapping("/performance/reset")
    public Result<String> resetStats() {
        performanceMonitorService.resetStats();
        return Result.success("性能统计已重置");
    }
    
    @Operation(summary = "获取系统信息")
    @GetMapping("/system")
    public Result<Map<String, Object>> getSystemInfo() {
        Map<String, Object> info = new HashMap<>();
        
        // JVM 信息
        info.put("javaVersion", System.getProperty("java.version"));
        info.put("osName", System.getProperty("os.name"));
        info.put("osArch", System.getProperty("os.arch"));
        info.put("availableProcessors", Runtime.getRuntime().availableProcessors());
        
        // 内存信息
        Runtime runtime = Runtime.getRuntime();
        info.put("maxMemory", runtime.maxMemory() / 1024 / 1024 + " MB");
        info.put("totalMemory", runtime.totalMemory() / 1024 / 1024 + " MB");
        info.put("freeMemory", runtime.freeMemory() / 1024 / 1024 + " MB");
        
        // 运行时间
        long uptime = java.lang.management.ManagementFactory.getRuntimeMXBean().getUptime();
        info.put("uptime", uptime / 1000);
        
        return Result.success(info);
    }
}
