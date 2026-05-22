package com.payflow.admin.service;

import com.payflow.admin.dto.MerchantScopeDTO;
import com.payflow.admin.entity.AdminUser;
import com.payflow.admin.mapper.AdminUserMapper;
import com.payflow.admin.service.impl.AdminMerchantScopeServiceImpl;
import com.payflow.common.exception.BizException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminMerchantScopeService 测试")
class AdminMerchantScopeServiceTest {

    @Mock
    private AdminUserMapper adminUserMapper;

    @Test
    @DisplayName("超级管理员无商户字段时解析为平台范围")
    void resolvesPlatformAdminWhenSuperAdminWithoutMerchantIds() {
        AdminMerchantScopeServiceImpl service = new AdminMerchantScopeServiceImpl(adminUserMapper);
        AdminUser user = new AdminUser();
        user.setId(1L);
        user.setUsername("admin");
        user.setRole("SUPER_ADMIN");

        MerchantScopeDTO scope = service.resolve(user);

        assertTrue(scope.isPlatformAdmin());
        assertEquals("PLATFORM", scope.getScopeMode());
        assertEquals(List.of(), scope.getAuthorizedMerchantIds());
    }

    @Test
    @DisplayName("商户管理员解析逗号分隔授权商户并去重")
    void resolvesMerchantAdminScopeFromDataMerchantIds() {
        AdminMerchantScopeServiceImpl service = new AdminMerchantScopeServiceImpl(adminUserMapper);
        AdminUser user = new AdminUser();
        user.setId(4L);
        user.setUsername("merchant_admin");
        user.setRole("ADMIN");
        user.setDataMerchantIds(" M100001, M100002, M100001 ");

        MerchantScopeDTO scope = service.resolve(user);

        assertFalse(scope.isPlatformAdmin());
        assertEquals("MERCHANT", scope.getScopeMode());
        assertEquals(List.of("M100001", "M100002"), scope.getAuthorizedMerchantIds());
    }

    @Test
    @DisplayName("授权商户求交只保留当前用户可访问商户")
    void intersectsAuthorizedMerchants() {
        AdminMerchantScopeServiceImpl service = new AdminMerchantScopeServiceImpl(adminUserMapper);
        MerchantScopeDTO scope = MerchantScopeDTO.builder()
                .platformAdmin(false)
                .authorizedMerchantIds(List.of("M100001", "M100002"))
                .build();

        List<String> result = service.intersectAuthorizedMerchants(scope, List.of("M100002", "M100003"));

        assertEquals(List.of("M100002"), result);
    }

    @Test
    @DisplayName("平台管理员按请求商户范围返回")
    void platformAdminUsesRequestedMerchantScope() {
        AdminMerchantScopeServiceImpl service = new AdminMerchantScopeServiceImpl(adminUserMapper);
        MerchantScopeDTO scope = MerchantScopeDTO.builder()
                .platformAdmin(true)
                .authorizedMerchantIds(List.of())
                .build();

        List<String> result = service.intersectAuthorizedMerchants(scope, List.of("M100002", "M100003"));

        assertEquals(List.of("M100002", "M100003"), result);
    }

    @Test
    @DisplayName("授权外商户访问抛出跨商户拒绝业务异常")
    void rejectsMerchantOutsideScope() {
        AdminMerchantScopeServiceImpl service = new AdminMerchantScopeServiceImpl(adminUserMapper);
        MerchantScopeDTO scope = MerchantScopeDTO.builder()
                .platformAdmin(false)
                .authorizedMerchantIds(List.of("M100001"))
                .build();

        BizException ex = assertThrows(BizException.class,
                () -> service.assertCanAccessMerchant(scope, "M100002"));

        assertEquals(6101, ex.getCode());
        assertEquals("无权访问该资源", ex.getMessage());
    }
}
