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
@TableName("sys_role_menus")
public class SysRoleMenu {

    @TableId(type = IdType.AUTO)
    @EqualsAndHashCode.Include
    @ToString.Include    private Long id;

    private Long roleId;
    private Long menuId;
    private LocalDateTime createdAt;
}
