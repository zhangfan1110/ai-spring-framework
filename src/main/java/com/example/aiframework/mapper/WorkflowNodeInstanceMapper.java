package com.example.aiframework.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aiframework.entity.WorkflowNodeInstanceEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 工作流节点实例 Mapper
 */
@Mapper
public interface WorkflowNodeInstanceMapper extends BaseMapper<WorkflowNodeInstanceEntity> {

    /**
     * 查询实例的所有节点
     */
    @Select("SELECT * FROM workflow_node_instance WHERE workflow_instance_id = #{instanceId} ORDER BY create_time ASC")
    List<WorkflowNodeInstanceEntity> findByInstance(String instanceId);

    /**
     * 查询正在执行的节点
     */
    @Select("SELECT * FROM workflow_node_instance WHERE workflow_instance_id = #{instanceId} AND status = 'RUNNING'")
    List<WorkflowNodeInstanceEntity> findRunningNodes(String instanceId);
}
