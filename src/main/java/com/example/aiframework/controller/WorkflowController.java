package com.example.aiframework.controller;

import com.example.aiframework.workflow.entity.WorkflowDefinitionEntity;
import com.example.aiframework.workflow.entity.WorkflowInstanceEntity;
import com.example.aiframework.workflow.entity.WorkflowNodeInstanceEntity;
import com.example.aiframework.workflow.mapper.WorkflowInstanceMapper;
import com.example.aiframework.workflow.mapper.WorkflowNodeInstanceMapper;
import com.example.aiframework.workflow.service.WorkflowDefinitionService;
import com.example.aiframework.workflow.service.WorkflowEngineService;
import com.example.aiframework.common.util.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工作流控制器
 */
@RestController
@RequestMapping("/api/workflow")
@Tag(name = "工作流引擎", description = "工作流定义和执行管理")
public class WorkflowController {

    @Autowired
    private WorkflowDefinitionService workflowDefinitionService;

    @Autowired
    private WorkflowEngineService workflowEngineService;

    @Autowired
    private WorkflowInstanceMapper workflowInstanceMapper;

    @Autowired
    private WorkflowNodeInstanceMapper nodeInstanceMapper;

    @Operation(summary = "创建工作流定义")
    @PostMapping("/definition")
    public Result<WorkflowDefinitionEntity> createWorkflow(
            @RequestParam String workflowCode,
            @RequestParam String workflowName,
            @RequestParam(required = false) String description,
            @RequestBody Map<String, Object> workflowConfig,
            @RequestParam(required = false) String createdBy
    ) {
        WorkflowDefinitionEntity workflow = workflowDefinitionService.createWorkflow(
            workflowCode, workflowName, description, toJson(workflowConfig),
            createdBy != null ? createdBy : "system"
        );
        return Result.success(workflow);
    }

    @Operation(summary = "更新工作流定义")
    @PutMapping("/definition/{id}")
    public Result<WorkflowDefinitionEntity> updateWorkflow(
            @PathVariable String id,
            @RequestParam(required = false) String workflowName,
            @RequestParam(required = false) String description,
            @RequestBody(required = false) Map<String, Object> workflowConfig
    ) {
        WorkflowDefinitionEntity workflow = workflowDefinitionService.updateWorkflow(
            id, workflowName, description, workflowConfig != null ? toJson(workflowConfig) : null
        );
        if (workflow == null) {
            return Result.error("工作流不存在");
        }
        return Result.success(workflow);
    }

    @Operation(summary = "发布工作流")
    @PostMapping("/definition/{id}/publish")
    public Result<WorkflowDefinitionEntity> publishWorkflow(
            @PathVariable String id,
            @RequestParam(required = false) String publishedBy
    ) {
        WorkflowDefinitionEntity workflow = workflowDefinitionService.publishWorkflow(
            id, publishedBy != null ? publishedBy : "system"
        );
        if (workflow == null) {
            return Result.error("工作流不存在");
        }
        return Result.success(workflow);
    }

    @Operation(summary = "获取工作流定义")
    @GetMapping("/definition/{id}")
    public Result<WorkflowDefinitionEntity> getWorkflow(@PathVariable String id) {
        WorkflowDefinitionEntity workflow = workflowDefinitionService.getWorkflow(id);
        if (workflow == null) {
            return Result.error("工作流不存在");
        }
        return Result.success(workflow);
    }

    @Operation(summary = "查询已发布的工作流列表")
    @GetMapping("/definition/list")
    public Result<List<WorkflowDefinitionEntity>> listWorkflows() {
        List<WorkflowDefinitionEntity> list = workflowDefinitionService.listPublishedWorkflows();
        return Result.success(list);
    }

    @Operation(summary = "启动工作流实例")
    @PostMapping("/definition/{id}/start")
    public Result<WorkflowInstanceEntity> startWorkflow(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, Object> businessData
    ) {
        if (businessData == null) {
            businessData = new HashMap<>();
        }
        
        WorkflowInstanceEntity instance = workflowEngineService.startWorkflow(id, businessData);
        return Result.success(instance);
    }

    @Operation(summary = "获取工作流实例详情")
    @GetMapping("/instance/{id}")
    public Result<WorkflowInstanceEntity> getInstance(@PathVariable String id) {
        WorkflowInstanceEntity instance = workflowInstanceMapper.selectById(id);
        if (instance == null) {
            return Result.error("实例不存在");
        }
        return Result.success(instance);
    }

    @Operation(summary = "查询实例的节点列表")
    @GetMapping("/instance/{id}/nodes")
    public Result<List<WorkflowNodeInstanceEntity>> getInstanceNodes(@PathVariable String id) {
        List<WorkflowNodeInstanceEntity> nodes = nodeInstanceMapper.findByInstance(id);
        return Result.success(nodes);
    }

    @Operation(summary = "查询工作流实例列表")
    @GetMapping("/definition/{id}/instances")
    public Result<List<WorkflowInstanceEntity>> listInstances(
            @PathVariable String id,
            @RequestParam(defaultValue = "20") int limit
    ) {
        List<WorkflowInstanceEntity> list = workflowInstanceMapper.findRecent(id, limit);
        return Result.success(list);
    }

    @Operation(summary = "获取工作流统计")
    @GetMapping("/definition/{id}/stats")
    public Result<Map<String, Object>> getWorkflowStats(@PathVariable String id) {
        Map<String, Object> stats = new HashMap<>();
        
        WorkflowDefinitionEntity definition = workflowDefinitionService.getWorkflow(id);
        if (definition == null) {
            return Result.error("工作流不存在");
        }
        
        stats.put("definition", definition);
        
        List<WorkflowInstanceMapper.InstanceStatusCount> statusCounts = 
            workflowInstanceMapper.countByStatus(id);
        for (WorkflowInstanceMapper.InstanceStatusCount count : statusCounts) {
            stats.put(count.getStatus().toLowerCase(), count.getCount());
        }
        
        stats.put("runningCount", workflowInstanceMapper.findRunning(id).size());

        return Result.success(stats);
    }

    @Operation(summary = "取消工作流实例")
    @PostMapping("/instance/{id}/cancel")
    public Result<String> cancelInstance(@PathVariable String id) {
        WorkflowInstanceEntity instance = workflowInstanceMapper.selectById(id);
        if (instance == null) {
            return Result.error("实例不存在");
        }

        if (!"RUNNING".equals(instance.getStatus())) {
            return Result.error("只有运行中的实例才能取消");
        }

        instance.setStatus("CANCELLED");
        workflowInstanceMapper.updateById(instance);

        return Result.success("实例已取消");
    }

    @Operation(summary = "重试失败的节点")
    @PostMapping("/instance/{instanceId}/node/{nodeId}/retry")
    public Result<String> retryNode(
            @PathVariable String instanceId,
            @PathVariable String nodeId
    ) {
        // TODO: 实现节点重试逻辑
        return Result.success("节点重试已触发");
    }

    private String toJson(Object obj) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }
}
