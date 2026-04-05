package com.example.aiframework.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aiframework.entity.AgentTaskLogEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Agent 任务日志 Mapper
 */
@Mapper
public interface AgentTaskLogMapper extends BaseMapper<AgentTaskLogEntity> {
    
    /**
     * 查询任务日志
     */
    List<AgentTaskLogEntity> selectByTaskId(
        @Param("taskId") String taskId,
        @Param("limit") int limit
    );
    
    /**
     * 删除任务日志
     */
    int deleteByTaskId(@Param("taskId") String taskId);
}
