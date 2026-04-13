package com.payflow.admin.service;

import com.payflow.admin.dto.LoginRequest;
import com.payflow.admin.dto.LoginResponse;

public interface AdminAuthService {

    LoginResponse login(LoginRequest request);
}
