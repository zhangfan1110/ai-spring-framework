package com.example.aiframework.service;

import com.example.aiframework.entity.WorkflowDefinitionEntity;
import com.example.aiframework.entity.WorkflowInstanceEntity;
import com.example.aiframework.entity.WorkflowNodeInstanceEntity;
import com.example.aiframework.mapper.WorkflowDefinitionMapper;
import com.example.aiframework.mapper.WorkflowInstanceMapper;
import com.example.aiframework.mapper.WorkflowNodeInstanceMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

/**
 * 工作流引擎服务
 */
@Service
public class WorkflowEngineService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowEngineService.class);

    @Autowired
    private WorkflowDefinitionMapper workflowDefinitionMapper;

    @Autowired
    private WorkflowInstanceMapper workflowInstanceMapper;

    @Autowired
    private WorkflowNodeInstanceMapper nodeInstanceMapper;

    @Autowired
    private ObjectMapper objectMapper;

    // 线程池
    private final ExecutorService executorService = Executors.newFixedThreadPool(20);

    /**
     * 启动工作流实例
     */
    @Transactional
    public WorkflowInstanceEntity startWorkflow(String workflowDefinitionId, Map<String, Object> businessData) {
        WorkflowDefinitionEntity definition = workflowDefinitionMapper.selectById(workflowDefinitionId);
        if (definition == null || !"PUBLISHED".equals(definition.getStatus())) {
            throw new RuntimeException("工作流未发布或不存在");
        }

        // 创建工作流实例
        WorkflowInstanceEntity instance = new WorkflowInstanceEntity();
        instance.setId(UUID.randomUUID().toString());
        instance.setWorkflowDefinitionId(workflowDefinitionId);
        instance.setInstanceNo(generateInstanceNo());
        instance.setBusinessData(toJson(businessData));
        instance.setStatus("RUNNING");
        instance.setCreateTime(LocalDateTime.now());

        workflowInstanceMapper.insert(instance);

        // 解析工作流配置
        WorkflowDefinitionService.WorkflowConfig config = parseConfig(definition.getWorkflowConfig());
        if (config == null) {
            throw new RuntimeException("工作流配置解析失败");
        }

        // 启动执行
        executorService.submit(() -> executeWorkflow(instance, config, businessData));

        return instance;
    }

    /**
     * 执行工作流
     */
    private void executeWorkflow(WorkflowInstanceEntity instance, 
                                 WorkflowDefinitionService.WorkflowConfig config,
                                 Map<String, Object> businessData) {
        try {
            String currentNodeId = config.getStartNodeId();
            Map<String, Object> context = new HashMap<>(businessData);

            while (currentNodeId != null) {
                WorkflowDefinitionService.NodeConfig nodeConfig = findNode(config, currentNodeId);
                if (nodeConfig == null) {
                    break;
                }

                // 执行节点
                currentNodeId = executeNode(instance, nodeConfig, context);
            }

            // 工作流完成
            completeWorkflow(instance, "SUCCESS", context);

        } catch (Exception e) {
            log.error("Workflow execution failed: {}", instance.getId(), e);
            failWorkflow(instance, e.getMessage());
        }
    }

    /**
     * 执行节点
     */
    private String executeNode(WorkflowInstanceEntity instance, 
                               WorkflowDefinitionService.NodeConfig nodeConfig,
                               Map<String, Object> context) {
        log.info("Executing node: {} type: {}", nodeConfig.getName(), nodeConfig.getType());

        // 创建节点实例
        WorkflowNodeInstanceEntity nodeInstance = createNodeInstance(instance, nodeConfig);

        try {
            nodeInstance.setStatus("RUNNING");
            nodeInstance.setStartTime(LocalDateTime.now());
            nodeInstance.setInputData(toJson(context));
            nodeInstanceMapper.updateById(nodeInstance);

            String output = null;

            // 根据节点类型执行
            switch (nodeConfig.getType()) {
                case "START":
                    output = executeStartNode(nodeConfig, context);
                    break;
                case "TASK":
                    output = executeTaskNode(nodeConfig, context);
                    break;
                case "CONDITION":
                    return executeConditionNode(instance, nodeConfig, context);
                case "PARALLEL":
                    output = executeParallelNode(instance, nodeConfig, context);
                    break;
                case "END":
                    output = executeEndNode(nodeConfig, context);
                    return null;
            }

            // 更新节点状态
            nodeInstance.setStatus("COMPLETED");
            nodeInstance.setOutputData(output);
            nodeInstance.setEndTime(LocalDateTime.now());
            nodeInstance.setDuration(System.currentTimeMillis() - nodeInstance.getStartTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
            nodeInstanceMapper.updateById(nodeInstance);

            // 查找下一个节点
            return findNextNode(instance, nodeConfig.getId());

        } catch (Exception e) {
            log.error("Node execution failed: {}", nodeConfig.getName(), e);
            nodeInstance.setStatus("FAILED");
            nodeInstance.setErrorMessage(e.getMessage());
            nodeInstanceMapper.updateById(nodeInstance);
            throw e;
        }
    }

    /**
     * 执行开始节点
     */
    private String executeStartNode(WorkflowDefinitionService.NodeConfig nodeConfig, Map<String, Object> context) {
        log.info("Start node: {}", nodeConfig.getName());
        return null;
    }

    /**
     * 执行任务节点
     */
    private String executeTaskNode(WorkflowDefinitionService.NodeConfig nodeConfig, Map<String, Object> context) {
        log.info("Task node: {}", nodeConfig.getName());
        
        // 从配置中获取任务类型
        Map<String, Object> config = nodeConfig.getConfig();
        if (config != null) {
            String taskType = (String) config.get("taskType");
            log.info("Executing task type: {}", taskType);
            
            // TODO: 根据 taskType 调用相应的处理器
            // 这里可以集成现有的 Agent 任务、聊天任务等
        }

        return toJson(Map.of("status", "completed"));
    }

    /**
     * 执行条件节点
     */
    private String executeConditionNode(WorkflowInstanceEntity instance, 
                                        WorkflowDefinitionService.NodeConfig nodeConfig,
                                        Map<String, Object> context) {
        log.info("Condition node: {}", nodeConfig.getName());
        
        // 查找满足条件的分支
        WorkflowDefinitionService.WorkflowConfig config = parseConfig(
            workflowDefinitionMapper.selectById(instance.getWorkflowDefinitionId()).getWorkflowConfig()
        );

        for (WorkflowDefinitionService.EdgeConfig edge : config.getEdges()) {
            if (edge.getSource().equals(nodeConfig.getId())) {
                String condition = edge.getCondition();
                if (condition == null || "true".equals(condition) || evaluateCondition(condition, context)) {
                    return edge.getTarget();
                }
            }
        }

        return null;
    }

    /**
     * 执行并行节点
     */
    private String executeParallelNode(WorkflowInstanceEntity instance,
                                       WorkflowDefinitionService.NodeConfig nodeConfig,
                                       Map<String, Object> context) {
        log.info("Parallel node: {}", nodeConfig.getName());
        
        // TODO: 实现并行分支执行
        // 需要等待所有分支完成后继续
        
        return findNextNode(instance, nodeConfig.getId());
    }

    /**
     * 执行结束节点
     */
    private String executeEndNode(WorkflowDefinitionService.NodeConfig nodeConfig, Map<String, Object> context) {
        log.info("End node: {}", nodeConfig.getName());
        return null;
    }

    /**
     * 查找下一个节点
     */
    private String findNextNode(WorkflowInstanceEntity instance, String currentNodeId) {
        WorkflowDefinitionService.WorkflowConfig config = parseConfig(
            workflowDefinitionMapper.selectById(instance.getWorkflowDefinitionId()).getWorkflowConfig()
        );

        for (WorkflowDefinitionService.EdgeConfig edge : config.getEdges()) {
            if (edge.getSource().equals(currentNodeId)) {
                return edge.getTarget();
            }
        }

        return null;
    }

    /**
     * 完成工作流
     */
    private void completeWorkflow(WorkflowInstanceEntity instance, String status, Map<String, Object> result) {
        instance.setStatus(status);
        instance.setResult(toJson(result));
        instance.setEndTime(LocalDateTime.now());
        workflowInstanceMapper.updateById(instance);
        log.info("Workflow completed: {} status: {}", instance.getId(), status);
    }

    /**
     * 失败工作流
     */
    private void failWorkflow(WorkflowInstanceEntity instance, String errorMessage) {
        instance.setStatus("FAILED");
        instance.setErrorMessage(errorMessage);
        instance.setEndTime(LocalDateTime.now());
        workflowInstanceMapper.updateById(instance);
    }

    /**
     * 创建节点实例
     */
    private WorkflowNodeInstanceEntity createNodeInstance(WorkflowInstanceEntity instance,
                                                          WorkflowDefinitionService.NodeConfig nodeConfig) {
        WorkflowNodeInstanceEntity nodeInstance = new WorkflowNodeInstanceEntity();
        nodeInstance.setId(UUID.randomUUID().toString());
        nodeInstance.setWorkflowInstanceId(instance.getId());
        nodeInstance.setNodeId(nodeConfig.getId());
        nodeInstance.setNodeName(nodeConfig.getName());
        nodeInstance.setNodeType(nodeConfig.getType());
        nodeInstance.setStatus("PENDING");
        nodeInstance.setRetryCount(0);
        nodeInstance.setCreateTime(LocalDateTime.now());

        nodeInstanceMapper.insert(nodeInstance);
        return nodeInstance;
    }

    /**
     * 查找节点配置
     */
    private WorkflowDefinitionService.NodeConfig findNode(WorkflowDefinitionService.WorkflowConfig config, String nodeId) {
        if (config.getNodes() == null) return null;
        
        for (WorkflowDefinitionService.NodeConfig node : config.getNodes()) {
            if (node.getId().equals(nodeId)) {
                return node;
            }
        }
        return null;
    }

    /**
     * 解析配置
     */
    private WorkflowDefinitionService.WorkflowConfig parseConfig(String configJson) {
        try {
            return objectMapper.readValue(configJson, WorkflowDefinitionService.WorkflowConfig.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 评估条件
     */
    private boolean evaluateCondition(String condition, Map<String, Object> context) {
        // TODO: 实现条件表达式评估
        // 可以使用 SpEL 或其他表达式引擎
        return true;
    }

    /**
     * 生成实例编号
     */
    private String generateInstanceNo() {
        return "WF" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }
}
