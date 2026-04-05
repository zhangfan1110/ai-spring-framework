package com.example.aiframework.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aiframework.entity.TokenUsageEntity;
import com.example.aiframework.mapper.TokenUsageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Token 使用统计服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenUsageService {

    private final TokenUsageMapper tokenUsageMapper;

    /**
     * 记录一次 Token 使用
     */
    public void recordUsage(String userId, String sessionId, String model,
                           int inputTokens, int outputTokens, String callType,
                           String status, String errorMessage, long responseTime) {
        TokenUsageEntity entity = new TokenUsageEntity();
        entity.setUserId(userId);
        entity.setSessionId(sessionId);
        entity.setModel(model);
        entity.setInputTokens(inputTokens);
        entity.setOutputTokens(outputTokens);
        entity.setTotalTokens(inputTokens + outputTokens);
        entity.setCallType(callType);
        entity.setStatus(status);
        entity.setErrorMessage(errorMessage);
        entity.setResponseTime(responseTime);

        tokenUsageMapper.insert(entity);
        log.debug("记录 Token 使用：userId={}, model={}, totalTokens={}", userId, model, entity.getTotalTokens());
    }

    /**
     * 获取今日 Token 使用统计
     */
    public Map<String, Object> getTodayStats() {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = LocalDateTime.now();
        return getStats(start, end);
    }

    /**
     * 获取本周 Token 使用统计
     */
    public Map<String, Object> getWeekStats() {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.minusDays(today.getDayOfWeek().getValue() - 1).atStartOfDay();
        LocalDateTime end = LocalDateTime.now();
        return getStats(start, end);
    }

    /**
     * 获取本月 Token 使用统计
     */
    public Map<String, Object> getMonthStats() {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime end = LocalDateTime.now();
        return getStats(start, end);
    }

    /**
     * 获取指定时间范围的统计
     */
    public Map<String, Object> getStats(LocalDateTime startDate, LocalDateTime endDate) {
        Map<String, Object> stats = new HashMap<>();

        // 总 Token 数
        LambdaQueryWrapper<TokenUsageEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.between(TokenUsageEntity::getCreateTime, startDate, endDate);
        wrapper.eq(TokenUsageEntity::getStatus, "SUCCESS");

        List<TokenUsageEntity> records = tokenUsageMapper.selectList(wrapper);

        int totalInput = 0;
        int totalOutput = 0;
        int totalCount = 0;

        for (TokenUsageEntity record : records) {
            totalInput += record.getInputTokens();
            totalOutput += record.getOutputTokens();
            totalCount++;
        }

        stats.put("totalInputTokens", totalInput);
        stats.put("totalOutputTokens", totalOutput);
        stats.put("totalTokens", totalInput + totalOutput);
        stats.put("callCount", totalCount);
        stats.put("startDate", startDate);
        stats.put("endDate", endDate);

        return stats;
    }

    /**
     * 按日期统计（用于图表）
     */
    public List<Map<String, Object>> getStatsByDate(LocalDateTime startDate, LocalDateTime endDate) {
        return tokenUsageMapper.countByDate(startDate, endDate);
    }

    /**
     * 按模型统计
     */
    public List<Map<String, Object>> getStatsByModel(LocalDateTime startDate, LocalDateTime endDate) {
        return tokenUsageMapper.countByModel(startDate, endDate);
    }

    /**
     * 估算成本（基于 Token 数）
     */
    public Map<String, Double> estimateCost(int totalTokens, String model) {
        Map<String, Double> cost = new HashMap<>();

        // 不同模型的定价（每 1000 tokens）
        double pricePer1kTokens = 0.002; // 默认价格

        if (model != null) {
            if (model.contains("qwen-turbo")) {
                pricePer1kTokens = 0.002;
            } else if (model.contains("qwen-plus")) {
                pricePer1kTokens = 0.004;
            } else if (model.contains("qwen-max")) {
                pricePer1kTokens = 0.012;
            } else if (model.contains("gpt-4")) {
                pricePer1kTokens = 0.03;
            } else if (model.contains("gpt-3.5")) {
                pricePer1kTokens = 0.002;
            }
        }

        double estimatedCost = (totalTokens / 1000.0) * pricePer1kTokens;
        cost.put("estimatedCost", estimatedCost);
        cost.put("pricePer1kTokens", pricePer1kTokens);

        return cost;
    }
}
