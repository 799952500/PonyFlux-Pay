package com.payflow.admin.service;

import com.payflow.admin.dto.AdminUiPreferencesDto;
import com.payflow.admin.dto.LoginRequest;
import com.payflow.admin.dto.LoginResponse;
import com.payflow.admin.dto.UpdateAdminUiPreferencesRequest;
import jakarta.servlet.http.HttpServletRequest;

/**
 * @author Lucas
 */
public interface AdminAuthService {

    LoginResponse login(LoginRequest request, HttpServletRequest httpRequest);

    /**
     * 当前登录用户资料（不含密码；需已通过 JWT 拦截器写入 username）。
     */
    LoginResponse profile(HttpServletRequest httpRequest);

    AdminUiPreferencesDto updateUiPreferences(HttpServletRequest httpRequest, UpdateAdminUiPreferencesRequest request);
}
