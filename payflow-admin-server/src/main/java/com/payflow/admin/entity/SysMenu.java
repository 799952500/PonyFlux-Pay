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
@TableName("admin_sys_menus")
/**
 * @author Lucas
 */
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
    /** 按钮权限码，仅 menuType=BUTTON 时使用 */
    private String permCode;
    /** 关联 API 路径模式，文档化用途 */
    private String apiPattern;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private List<SysMenu> children;
}
