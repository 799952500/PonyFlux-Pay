package com.payflow.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理员 UI 外观偏好（跨设备同步）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUiPreferencesDto {

    private String themeKey;

    private String tableDensity;

    private Boolean sidebarCollapsed;
}
