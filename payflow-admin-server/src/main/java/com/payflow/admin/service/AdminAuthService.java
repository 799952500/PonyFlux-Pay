package com.payflow.admin.service;

import com.payflow.admin.dto.LoginRequest;
import com.payflow.admin.dto.LoginResponse;
/**
 * @author Lucas
 */

public interface AdminAuthService {

    LoginResponse login(LoginRequest request);
}
