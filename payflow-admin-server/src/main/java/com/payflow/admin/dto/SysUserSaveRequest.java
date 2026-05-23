package com.payflow.admin.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

/**
 * 系统用户创建/更新请求
 */
@Data
public class SysUserSaveRequest {

    private String username;
    private String password;
    private String nickname;
    @JsonAlias("mobile")
    private String phone;
    private String email;
    private String status;
    private Long roleId;
}
