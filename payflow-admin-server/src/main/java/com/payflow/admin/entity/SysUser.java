package com.payflow.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import java.time.LocalDateTime;

@Data
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@TableName("sys_users")
/**
 * @author Lucas
 */
public class SysUser {
    @TableId(type = IdType.AUTO)
    @EqualsAndHashCode.Include
    @ToString.Include    private Long id;
    private String username;
    private String password;        // BCrypt 加密存储
    private String nickname;
    private String phone;
    private String email;
    private String status;          // ACTIVE / DISABLED
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
