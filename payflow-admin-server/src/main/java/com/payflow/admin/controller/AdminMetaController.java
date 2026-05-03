package com.payflow.admin.controller;

import com.payflow.admin.config.AdminSecurityProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 公开元数据：功能开关、版本信息等（无需 JWT）。
 *
 * @author Lucas
 */
@RestController
@RequestMapping("/api/v1/admin/meta")
@RequiredArgsConstructor
public class AdminMetaController {

    private final AdminSecurityProperties adminSecurityProperties;
    private final Environment environment;

    @GetMapping("/features")
    public ResponseEntity<Map<String, Object>> features() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("loginCaptchaEnabled", adminSecurityProperties.isLoginCaptchaEnabled());
        data.put("loginMaxFailures", adminSecurityProperties.getLoginMaxFailures());
        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", data));
    }

    @GetMapping("/version")
    public ResponseEntity<Map<String, Object>> version() {
        String profiles = String.join(",", environment.getActiveProfiles());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("application", environment.getProperty("spring.application.name", "payflow-admin-server"));
        data.put("profiles", profiles.isEmpty() ? "default" : profiles);
        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", data));
    }
}
