package com.payflow.admin.service;

import com.payflow.admin.entity.SystemConfig;
import com.payflow.admin.kit.GlobalResourceKit;
import com.payflow.admin.mapper.SystemConfigMapper;
import com.payflow.admin.service.impl.SystemConfigServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("全局配置访问与脱敏测试")
class GlobalConfigAccessTest {

    @Mock
    private SystemConfigMapper systemConfigMapper;

    @Test
    @DisplayName("非平台管理员查看敏感配置时脱敏")
    void masksSensitiveConfigForMerchantAdmin() {
        SystemConfig cfg = new SystemConfig();
        cfg.setConfigKey("wechat_api_secret");
        cfg.setConfigValue("plain-secret");
        cfg.setCategory("payment");
        cfg.setValueType("STRING");
        when(systemConfigMapper.selectList(any())).thenReturn(List.of(cfg));

        SystemConfigServiceImpl service = new SystemConfigServiceImpl(systemConfigMapper);
        List<Map<String, Object>> rows = service.listViewByCategory("payment", false);

        assertEquals(1, rows.size());
        assertEquals("******", rows.get(0).get("configValue"));
        assertTrue((Boolean) rows.get(0).get("sensitive"));
        assertEquals(GlobalResourceKit.CLASSIFICATION_GLOBAL, rows.get(0).get("classification"));
    }

    @Test
    @DisplayName("平台管理员可查看敏感配置原文")
    void platformAdminSeesSensitivePlainValue() {
        SystemConfig cfg = new SystemConfig();
        cfg.setConfigKey("wechat_api_secret");
        cfg.setConfigValue("plain-secret");
        cfg.setCategory("payment");
        cfg.setValueType("STRING");
        when(systemConfigMapper.selectList(any())).thenReturn(List.of(cfg));

        SystemConfigServiceImpl service = new SystemConfigServiceImpl(systemConfigMapper);
        List<Map<String, Object>> rows = service.listViewByCategory("payment", true);

        assertEquals("plain-secret", rows.get(0).get("configValue"));
    }
}
