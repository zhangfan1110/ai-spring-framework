package com.example.aiframework.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aiframework.workflow.entity.WorkflowInstanceEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 工作流实例 Mapper
 */
@Mapper
public interface WorkflowInstanceMapper extends BaseMapper<WorkflowInstanceEntity> {

    /**
     * 查询运行中的实例
     */
    @Select("SELECT * FROM workflow_instance WHERE workflow_definition_id = #{definitionId} AND status = 'RUNNING' ORDER BY create_time DESC")
    List<WorkflowInstanceEntity> findRunning(String definitionId);

    /**
     * 查询最近的实例
     */
    @Select("SELECT * FROM workflow_instance WHERE workflow_definition_id = #{definitionId} ORDER BY create_time DESC LIMIT #{limit}")
    List<WorkflowInstanceEntity> findRecent(String definitionId, int limit);

    /**
     * 统计实例数量
     */
    @Select("SELECT status, COUNT(*) as count FROM workflow_instance WHERE workflow_definition_id = #{definitionId} GROUP BY status")
    List<InstanceStatusCount> countByStatus(String definitionId);

    class InstanceStatusCount {
        private String status;
        private Long count;

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Long getCount() { return count; }
        public void setCount(Long count) { this.count = count; }
    }
}
