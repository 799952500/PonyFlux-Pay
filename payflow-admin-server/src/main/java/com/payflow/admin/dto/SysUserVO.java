package com.payflow.admin.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统用户展示对象（含主角色 ID）
 */
@Data
public class SysUserVO {

    private Long id;
    private String username;
    private String nickname;
    private String phone;
    private String email;
    private String status;
    private Long roleId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
