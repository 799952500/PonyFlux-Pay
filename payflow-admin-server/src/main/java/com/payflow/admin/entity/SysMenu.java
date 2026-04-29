package com.payflow.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@TableName("sys_menus")
public class SysMenu {

    @TableId(type = IdType.AUTO)
    @EqualsAndHashCode.Include
    @ToString.Include    private Long id;

    private Long parentId;
    private String menuCode;
    private String menuName;
    private String menuType;
    private String path;
    private String icon;
    private Integer sortOrder;
    private Boolean visible;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private List<SysMenu> children;
}
