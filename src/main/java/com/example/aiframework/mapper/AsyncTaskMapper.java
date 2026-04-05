package com.example.aiframework.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aiframework.entity.AsyncTaskEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 异步任务 Mapper
 */
@Mapper
public interface AsyncTaskMapper extends BaseMapper<AsyncTaskEntity> {

    /**
     * 查询待处理的任务
     */
    @Select("SELECT * FROM async_task WHERE status = 'PENDING' ORDER BY priority ASC, create_time ASC LIMIT #{limit}")
    List<AsyncTaskEntity> findPendingTasks(int limit);

    /**
     * 查询运行中的任务
     */
    @Select("SELECT * FROM async_task WHERE status = 'RUNNING' AND start_time < #{beforeTime}")
    List<AsyncTaskEntity> findTimeoutTasks(LocalDateTime beforeTime);

    /**
     * 统计各状态任务数量
     */
    @Select("SELECT status, COUNT(*) as count FROM async_task GROUP BY status")
    List<TaskCount> countByStatus();
    
    /**
     * 任务计数 DTO
     */
    class TaskCount {
        private String status;
        private Long count;
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Long getCount() { return count; }
        public void setCount(Long count) { this.count = count; }
    }
}
