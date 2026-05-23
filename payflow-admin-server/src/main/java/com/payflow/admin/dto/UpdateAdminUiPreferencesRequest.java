package com.payflow.admin.dto;

import lombok.Data;

/**
 * 更新当前用户 UI 偏好请求体。
 */
@Data
public class UpdateAdminUiPreferencesRequest {

    private String themeKey;

    private String tableDensity;

    private Boolean sidebarCollapsed;
}
