package com.payflow.cashier.service;

import com.payflow.cashier.dto.LoginRequest;
import com.payflow.cashier.dto.LoginResponse;

/**
 * 认证服务接口
 *
 * @author PayFlow Team
 */
public interface AuthService {

    /**
     * 商户登录
     *
     * @param request 登录请求
     * @return 登录响应（含 JWT Token）
     */
    LoginResponse login(LoginRequest request);
}
