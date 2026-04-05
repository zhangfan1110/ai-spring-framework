package com.example.aiframework.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

/**
 * 用户角色关联实体
 */
@TableName("sys_user_role")
public class UserRoleEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 用户 ID
     */
    private String userId;

    /**
     * 角色 ID
     */
    private String roleId;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getRoleId() { return roleId; }
    public void setRoleId(String roleId) { this.roleId = roleId; }
    
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
