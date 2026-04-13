package com.payflow.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("admin_users")
public class AdminUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;
    private String password;
    private String role;       // SUPER_ADMIN / ADMIN / FINANCE / RISK
    private String nickname;
    private String status;     // ACTIVE / DISABLED
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
