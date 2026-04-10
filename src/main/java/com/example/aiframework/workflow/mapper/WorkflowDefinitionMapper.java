package com.example.aiframework.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aiframework.workflow.entity.WorkflowDefinitionEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 工作流定义 Mapper
 */
@Mapper
public interface WorkflowDefinitionMapper extends BaseMapper<WorkflowDefinitionEntity> {

    /**
     * 查询已发布的工作流列表
     */
    @Select("SELECT * FROM workflow_definition WHERE status = 'PUBLISHED' ORDER BY published_time DESC")
    List<WorkflowDefinitionEntity> findPublished();

    /**
     * 根据编码查询最新版本
     */
    @Select("SELECT * FROM workflow_definition WHERE workflow_code = #{code} AND status = 'PUBLISHED' ORDER BY version DESC LIMIT 1")
    WorkflowDefinitionEntity findLatestByCode(String code);

    /**
     * 统计工作流数量
     */
    @Select("SELECT COUNT(*) FROM workflow_definition WHERE created_by = #{userId}")
    int countByUser(String userId);
}
