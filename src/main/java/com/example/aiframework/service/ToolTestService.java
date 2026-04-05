package com.example.aiframework.service;

import com.example.aiframework.tool.Tool;
import com.example.aiframework.tool.ToolExecutionResult;
import com.example.aiframework.tool.ToolManager;
import com.example.aiframework.tool.ToolParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 工具执行测试服务
 * 支持工具功能测试、性能基准测试、压力测试
 */
@Service
public class ToolTestService {
    
    private static final Logger log = LoggerFactory.getLogger(ToolTestService.class);
    
    @Autowired
    private ToolManager toolManager;
    
    /**
     * 测试结果
     */
    public static class TestResult {
        private String toolName;
        private boolean success;
        private String message;
        private Object output;
        private long duration;
        private Map<String, Object> metrics;
        
        public String getToolName() { return toolName; }
        public void setToolName(String toolName) { this.toolName = toolName; }
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public Object getOutput() { return output; }
        public void setOutput(Object output) { this.output = output; }
        
        public long getDuration() { return duration; }
        public void setDuration(long duration) { this.duration = duration; }
        
        public Map<String, Object> getMetrics() { return metrics; }
        public void setMetrics(Map<String, Object> metrics) { this.metrics = metrics; }
    }
    
    /**
     * 基准测试报告
     */
    public static class BenchmarkReport {
        private String toolName;
        private int iterations;
        private long totalTime;
        private long avgTime;
        private long minTime;
        private long maxTime;
        private int successCount;
        private int failCount;
        private double successRate;
        private Map<String, Object> details;
        
        public String getToolName() { return toolName; }
        public void setToolName(String toolName) { this.toolName = toolName; }
        
        public int getIterations() { return iterations; }
        public void setIterations(int iterations) { this.iterations = iterations; }
        
        public long getTotalTime() { return totalTime; }
        public void setTotalTime(long totalTime) { this.totalTime = totalTime; }
        
        public long getAvgTime() { return avgTime; }
        public void setAvgTime(long avgTime) { this.avgTime = avgTime; }
        
        public long getMinTime() { return minTime; }
        public void setMinTime(long minTime) { this.minTime = minTime; }
        
        public long getMaxTime() { return maxTime; }
        public void setMaxTime(long maxTime) { this.maxTime = maxTime; }
        
        public int getSuccessCount() { return successCount; }
        public void setSuccessCount(int successCount) { this.successCount = successCount; }
        
        public int getFailCount() { return failCount; }
        public void setFailCount(int failCount) { this.failCount = failCount; }
        
        public double getSuccessRate() { return successRate; }
        public void setSuccessRate(double successRate) { this.successRate = successRate; }
        
        public Map<String, Object> getDetails() { return details; }
        public void setDetails(Map<String, Object> details) { this.details = details; }
    }
    
    /**
     * 压力测试报告
     */
    public static class StressTestReport {
        private String toolName;
        private int concurrentUsers;
        private int totalRequests;
        private int successRequests;
        private int failedRequests;
        private double requestsPerSecond;
        private long avgResponseTime;
        private long p95ResponseTime;
        private long p99ResponseTime;
        private String status; // PASS/FAIL
        private List<String> errors;
        
        public String getToolName() { return toolName; }
        public void setToolName(String toolName) { this.toolName = toolName; }
        
        public int getConcurrentUsers() { return concurrentUsers; }
        public void setConcurrentUsers(int concurrentUsers) { this.concurrentUsers = concurrentUsers; }
        
        public int getTotalRequests() { return totalRequests; }
        public void setTotalRequests(int totalRequests) { this.totalRequests = totalRequests; }
        
        public int getSuccessRequests() { return successRequests; }
        public void setSuccessRequests(int successRequests) { this.successRequests = successRequests; }
        
        public int getFailedRequests() { return failedRequests; }
        public void setFailedRequests(int failedRequests) { this.failedRequests = failedRequests; }
        
        public double getRequestsPerSecond() { return requestsPerSecond; }
        public void setRequestsPerSecond(double requestsPerSecond) { this.requestsPerSecond = requestsPerSecond; }
        
        public long getAvgResponseTime() { return avgResponseTime; }
        public void setAvgResponseTime(long avgResponseTime) { this.avgResponseTime = avgResponseTime; }
        
        public long getP95ResponseTime() { return p95ResponseTime; }
        public void setP95ResponseTime(long p95ResponseTime) { this.p95ResponseTime = p95ResponseTime; }
        
        public long getP99ResponseTime() { return p99ResponseTime; }
        public void setP99ResponseTime(long p99ResponseTime) { this.p99ResponseTime = p99ResponseTime; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public List<String> getErrors() { return errors; }
        public void setErrors(List<String> errors) { this.errors = errors; }
    }
    
    /**
     * 执行工具测试
     */
    public TestResult executeTest(String toolName, Map<String, String> parameters) {
        log.info("执行工具测试：{}", toolName);
        
        TestResult result = new TestResult();
        result.setToolName(toolName);
        result.setMetrics(new HashMap<>());
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 获取工具
            Tool tool = toolManager.getTool(toolName);
            if (tool == null) {
                result.setSuccess(false);
                result.setMessage("工具不存在：" + toolName);
                return result;
            }
            
            // 验证参数
            List<ToolParameter> toolParams = new ArrayList<>();
            if (parameters != null) {
                for (Map.Entry<String, String> entry : parameters.entrySet()) {
                    ToolParameter param = new ToolParameter();
                    param.setName(entry.getKey());
                    param.setDefaultValue(entry.getValue());
                    toolParams.add(param);
                }
            }
            
            // 执行工具
            ToolExecutionResult executionResult = toolManager.executeTool(toolName, toolParams);
            
            long duration = System.currentTimeMillis() - startTime;
            
            result.setSuccess(executionResult.isSuccess());
            result.setMessage(executionResult.isSuccess() ? "测试通过" : "测试失败");
            result.setOutput(executionResult.getOutput());
            result.setDuration(duration);
            result.getMetrics().put("iterations", 1);
            
            if (!executionResult.isSuccess()) {
                result.getMetrics().put("error", executionResult.getError());
            }
            
            log.info("工具测试完成：{} - {}, 耗时：{}ms", toolName, 
                executionResult.isSuccess() ? "成功" : "失败", duration);
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            result.setSuccess(false);
            result.setMessage("测试异常：" + e.getMessage());
            result.setDuration(duration);
            result.getMetrics().put("error", e.getMessage());
            
            log.error("工具测试异常：{}", toolName, e);
        }
        
        return result;
    }
    
    /**
     * 执行基准测试
     */
    public BenchmarkReport executeBenchmark(String toolName, Map<String, String> parameters, int iterations) {
        log.info("执行基准测试：{}, 迭代次数：{}", toolName, iterations);
        
        BenchmarkReport report = new BenchmarkReport();
        report.setToolName(toolName);
        report.setIterations(iterations);
        report.setDetails(new HashMap<>());
        
        List<Long> durations = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;
        long totalTime = 0;
        long minTime = Long.MAX_VALUE;
        long maxTime = Long.MIN_VALUE;
        
        for (int i = 0; i < iterations; i++) {
            TestResult result = executeTest(toolName, parameters);
            long duration = result.getDuration();
            
            durations.add(duration);
            totalTime += duration;
            
            if (duration < minTime) minTime = duration;
            if (duration > maxTime) maxTime = duration;
            
            if (result.isSuccess()) {
                successCount++;
            } else {
                failCount++;
            }
        }
        
        // 计算统计
        long avgTime = totalTime / iterations;
        double successRate = (double) successCount / iterations * 100;
        
        // 计算 P95 和 P99
        Collections.sort(durations);
        int p95Index = (int) (durations.size() * 0.95);
        int p99Index = (int) (durations.size() * 0.99);
        long p95Time = durations.get(Math.min(p95Index, durations.size() - 1));
        long p99Time = durations.get(Math.min(p99Index, durations.size() - 1));
        
        report.setTotalTime(totalTime);
        report.setAvgTime(avgTime);
        report.setMinTime(minTime);
        report.setMaxTime(maxTime);
        report.setSuccessCount(successCount);
        report.setFailCount(failCount);
        report.setSuccessRate(successRate);
        
        report.getDetails().put("p95Time", p95Time);
        report.getDetails().put("p99Time", p99Time);
        
        log.info("基准测试完成：{} - 成功率：{}%, 平均耗时：{}ms", 
            toolName, successRate, avgTime);
        
        return report;
    }
    
    /**
     * 执行压力测试
     */
    public StressTestReport executeStressTest(String toolName, Map<String, String> parameters, 
                                               int concurrentUsers, int requestsPerUser) {
        log.info("执行压力测试：{}, 并发用户：{}, 每用户请求：{}", toolName, concurrentUsers, requestsPerUser);
        
        StressTestReport report = new StressTestReport();
        report.setToolName(toolName);
        report.setConcurrentUsers(concurrentUsers);
        report.setTotalRequests(concurrentUsers * requestsPerUser);
        report.setErrors(new ArrayList<>());
        
        ExecutorService executor = Executors.newFixedThreadPool(concurrentUsers);
        CountDownLatch latch = new CountDownLatch(concurrentUsers);
        
        List<Long> allDurations = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        
        long startTime = System.currentTimeMillis();
        
        // 创建并发任务
        for (int i = 0; i < concurrentUsers; i++) {
            final int userId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < requestsPerUser; j++) {
                        long reqStart = System.currentTimeMillis();
                        
                        try {
                            TestResult result = executeTest(toolName, parameters);
                            long reqDuration = System.currentTimeMillis() - reqStart;
                            allDurations.add(reqDuration);
                            
                            if (result.isSuccess()) {
                                successCount.incrementAndGet();
                            } else {
                                failCount.incrementAndGet();
                                synchronized (report.getErrors()) {
                                    report.getErrors().add("用户" + userId + "请求" + j + ": " + result.getMessage());
                                }
                            }
                        } catch (Exception e) {
                            failCount.incrementAndGet();
                            synchronized (report.getErrors()) {
                                report.getErrors().add("用户" + userId + "请求" + j + ": " + e.getMessage());
                            }
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // 等待完成
        try {
            latch.await(5, TimeUnit.MINUTES); // 最多 5 分钟超时
        } catch (InterruptedException e) {
            log.error("压力测试被中断", e);
        }
        
        long totalTime = System.currentTimeMillis() - startTime;
        executor.shutdown();
        
        // 计算指标
        report.setSuccessRequests(successCount.get());
        report.setFailedRequests(failCount.get());
        report.setRequestsPerSecond((double) report.getTotalRequests() / totalTime * 1000);
        
        if (!allDurations.isEmpty()) {
            double avgTimeDouble = allDurations.stream().mapToLong(Long::longValue).average().orElse(0.0);
            long avgTime = (long) avgTimeDouble;
            Collections.sort(allDurations);
            
            int p95Index = (int) (allDurations.size() * 0.95);
            int p99Index = (int) (allDurations.size() * 0.99);
            
            report.setAvgResponseTime(avgTime);
            report.setP95ResponseTime(allDurations.get(Math.min(p95Index, allDurations.size() - 1)));
            report.setP99ResponseTime(allDurations.get(Math.min(p99Index, allDurations.size() - 1)));
        }
        
        // 判断是否通过
        double successRate = (double) successCount.get() / report.getTotalRequests() * 100;
        report.setStatus(successRate >= 95 ? "PASS" : "FAIL");
        
        log.info("压力测试完成：{} - 成功率：{}%, QPS: {}", 
            toolName, successRate, report.getRequestsPerSecond());
        
        return report;
    }
    
    /**
     * 批量测试所有工具
     */
    public Map<String, TestResult> testAllTools() {
        log.info("批量测试所有工具");
        
        Map<String, TestResult> results = new LinkedHashMap<>();
        
        for (Tool tool : toolManager.getAllTools()) {
            String toolName = tool.getName();
            
            // 使用默认参数测试
            Map<String, String> params = createDefaultParameters(toolName);
            TestResult result = executeTest(toolName, params);
            
            results.put(toolName, result);
        }
        
        log.info("批量测试完成，成功：{}/{}", 
            results.values().stream().filter(TestResult::isSuccess).count(),
            results.size());
        
        return results;
    }
    
    /**
     * 创建默认测试参数
     */
    private Map<String, String> createDefaultParameters(String toolName) {
        Map<String, String> params = new HashMap<>();
        
        switch (toolName) {
            case "calculator":
                params.put("expression", "1+1");
                break;
            case "datetime":
                // 无参数
                break;
            case "weather":
                params.put("city", "北京");
                break;
            case "search":
                params.put("query", "test");
                params.put("limit", "1");
                break;
            case "translation":
                params.put("text", "hello");
                params.put("source_lang", "en");
                params.put("target_lang", "zh");
                break;
            case "code_executor":
                params.put("language", "python");
                params.put("code", "print('test')");
                break;
            default:
                // 其他工具使用空参数
                break;
        }
        
        return params;
    }
}
