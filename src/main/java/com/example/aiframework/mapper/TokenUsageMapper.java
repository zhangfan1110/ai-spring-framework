package com.example.aiframework.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aiframework.entity.TokenUsageEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Token 使用统计 Mapper
 */
@Mapper
public interface TokenUsageMapper extends BaseMapper<TokenUsageEntity> {

    /**
     * 按日期统计 Token 使用量
     */
    @Select("SELECT DATE(create_time) as date, SUM(total_tokens) as total_tokens " +
            "FROM ai_token_usage " +
            "WHERE create_time >= #{startDate} AND create_time <= #{endDate} " +
            "GROUP BY DATE(create_time) " +
            "ORDER BY date")
    List<Map<String, Object>> countByDate(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 按模型统计 Token 使用量
     */
    @Select("SELECT model, SUM(total_tokens) as total_tokens, COUNT(*) as call_count " +
            "FROM ai_token_usage " +
            "WHERE create_time >= #{startDate} AND create_time <= #{endDate} " +
            "GROUP BY model")
    List<Map<String, Object>> countByModel(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 按用户统计 Token 使用量
     */
    @Select("SELECT user_id, SUM(total_tokens) as total_tokens, COUNT(*) as call_count " +
            "FROM ai_token_usage " +
            "WHERE create_time >= #{startDate} AND create_time <= #{endDate} " +
            "GROUP BY user_id")
    List<Map<String, Object>> countByUser(LocalDateTime startDate, LocalDateTime endDate);
}
