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
@TableName("admin_users")
/**
 * @author Lucas
 */
public class AdminUser {

    @TableId(type = IdType.AUTO)
    @EqualsAndHashCode.Include
    @ToString.Include    private Long id;

    private String username;
    private String password;
    private String role;       // SUPER_ADMIN / ADMIN / FINANCE / RISK
    private String nickname;
    private String status;     // ACTIVE / DISABLED

    /** 数据权限：可见收银台商户号列表，逗号分隔；NULL 表示不按字段限制（仍建议配合 SUPER_ADMIN） */
    private String dataMerchantIds;

    /** UI 主题：mint / ocean / violet / dark */
    private String uiTheme;

    /** 表格密度：standard / compact */
    private String uiTableDensity;

    /** 侧栏是否折叠 */
    private Boolean uiSidebarCollapsed;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
