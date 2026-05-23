package com.payflow.admin.service;

import com.payflow.admin.dto.AdminUiPreferencesDto;
import com.payflow.admin.dto.UpdateAdminUiPreferencesRequest;
import com.payflow.admin.entity.AdminUser;

/**
 * 管理员 UI 偏好读写。
 */
public interface AdminUserPreferenceService {

    AdminUiPreferencesDto fromUser(AdminUser user);

    AdminUiPreferencesDto updateCurrentUser(String username, UpdateAdminUiPreferencesRequest request);
}
