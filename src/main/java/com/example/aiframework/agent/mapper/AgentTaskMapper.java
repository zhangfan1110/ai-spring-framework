package com.example.aiframework.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aiframework.agent.entity.AgentTaskEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Agent 任务 Mapper
 */
@Mapper
public interface AgentTaskMapper extends BaseMapper<AgentTaskEntity> {
    
    /**
     * 查询任务列表
     */
    List<AgentTaskEntity> selectTaskList(
        @Param("status") String status,
        @Param("limit") int limit
    );
    
    /**
     * 清理旧任务
     */
    int deleteOldTasks(@Param("beforeTime") String beforeTime);
}
