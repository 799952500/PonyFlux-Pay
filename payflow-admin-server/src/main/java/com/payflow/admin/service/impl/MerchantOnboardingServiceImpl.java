package com.payflow.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflow.admin.client.CashierMerchantProvisionClient;
import com.payflow.admin.dto.onboarding.MerchantApplicationDetailVO;
import com.payflow.admin.dto.onboarding.MerchantApplicationRejectRequest;
import com.payflow.admin.dto.onboarding.MerchantApplicationResultRequest;
import com.payflow.admin.dto.onboarding.MerchantApplicationResultVO;
import com.payflow.admin.dto.onboarding.MerchantApplicationSubmitRequest;
import com.payflow.admin.dto.onboarding.MerchantApplicationSubmitResponse;
import com.payflow.admin.dto.onboarding.MerchantProvisionRequest;
import com.payflow.admin.entity.AdminUser;
import com.payflow.admin.entity.Merchant;
import com.payflow.admin.entity.MerchantApplicationEntity;
import com.payflow.admin.entity.SysRole;
import com.payflow.admin.entity.SysUserRole;
import com.payflow.admin.kit.OnboardingSecretCipherKit;
import com.payflow.admin.mapper.AdminUserMapper;
import com.payflow.admin.mapper.MerchantApplicationEntityMapper;
import com.payflow.admin.mapper.MerchantMapper;
import com.payflow.admin.mapper.SysRoleMapper;
import com.payflow.admin.mapper.SysUserRoleMapper;
import com.payflow.admin.service.AuditLogService;
import com.payflow.admin.service.MerchantOnboardingService;
import com.payflow.admin.service.MerchantService;
import com.payflow.admin.service.OnboardingRateLimitService;
import com.payflow.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 商户入驻申请：提交、审批、密钥自助查询。
 */
@Service
@RequiredArgsConstructor
public class MerchantOnboardingServiceImpl implements MerchantOnboardingService {

    private static final int NOT_FOUND = 6201;
    private static final int CONTACT_MISMATCH = 6202;
    private static final int NOT_APPROVED = 6203;
    private static final int PENDING = 6204;
    private static final int QUERY_LIMIT = 6205;
    private static final int INVALID_STATE = 6207;

    private static final int MAX_RESULT_QUERIES = 5;
    private static final String ROLE_MERCHANT_ADMIN = "MERCHANT_ADMIN";

    private final MerchantApplicationEntityMapper applicationMapper;
    private final MerchantMapper merchantMapper;
    private final MerchantService merchantService;
    private final AdminUserMapper adminUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final CashierMerchantProvisionClient cashierMerchantProvisionClient;
    private final OnboardingSecretCipherKit secretCipherKit;
    private final OnboardingRateLimitService rateLimitService;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Value("${payflow.onboarding.admin-login-url:http://localhost:3001/login}")
    private String adminLoginUrl;

    @Value("${payflow.onboarding.result-query-url:http://localhost:5173/onboarding/result}")
    private String resultQueryUrl;

    @Override
    @Transactional
    public MerchantApplicationSubmitResponse submit(MerchantApplicationSubmitRequest request, String clientIp) {
        rateLimitService.assertSubmitAllowed(clientIp);

        String phone = normalizePhone(request.getContactPhone());
        String email = normalizeEmail(request.getContactEmail());

        long pending = applicationMapper.selectCount(new LambdaQueryWrapper<MerchantApplicationEntity>()
                .eq(MerchantApplicationEntity::getContactPhone, phone)
                .in(MerchantApplicationEntity::getStatus,
                        MerchantApplicationEntity.STATUS_SUBMITTED,
                        MerchantApplicationEntity.STATUS_REVIEWING));
        if (pending > 0) {
            throw new BizException(6206, "该手机号已有待审核申请，请勿重复提交");
        }

        String applicationNo = generateApplicationNo();
        Map<String, Object> payload = new LinkedHashMap<>();
        if (StringUtils.hasText(request.getWebsiteUrl())) {
            payload.put("websiteUrl", request.getWebsiteUrl().trim());
        }
        if (StringUtils.hasText(request.getBusinessScope())) {
            payload.put("businessScope", request.getBusinessScope().trim());
        }
        if (StringUtils.hasText(request.getRemark())) {
            payload.put("remark", request.getRemark().trim());
        }

        MerchantApplicationEntity row = new MerchantApplicationEntity();
        row.setApplicationNo(applicationNo);
        row.setMerchantName(request.getMerchantName().trim());
        row.setStatus(MerchantApplicationEntity.STATUS_SUBMITTED);
        row.setApplicationSource(MerchantApplicationEntity.SOURCE_CASHIER_PUBLIC);
        row.setBizLicenseNo(trimToNull(request.getBizLicenseNo()));
        row.setContactName(request.getContactName().trim());
        row.setContactPhone(phone);
        row.setContactEmail(email);
        row.setResultQueryCount(0);
        try {
            row.setPayloadJson(objectMapper.writeValueAsString(payload));
        } catch (Exception ex) {
            throw new BizException(6210, "申请扩展信息序列化失败", ex);
        }
        LocalDateTime now = LocalDateTime.now();
        row.setCreatedAt(now);
        row.setUpdatedAt(now);
        applicationMapper.insert(row);

        auditLogService.record("PUBLIC", "MERCHANT_PUBLIC", null, "ONBOARDING_SUBMIT",
                "/api/v1/internal/onboarding/applications", "MERCHANT_APPLICATION", applicationNo,
                "提交入驻申请: " + applicationNo, "SUCCESS", null, clientIp);

        return MerchantApplicationSubmitResponse.builder()
                .applicationNo(applicationNo)
                .queryUrl(resultQueryUrl)
                .build();
    }

    @Override
    public IPage<MerchantApplicationEntity> pageApplications(int page, int pageSize, String status, String keyword) {
        LambdaQueryWrapper<MerchantApplicationEntity> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) {
            wrapper.eq(MerchantApplicationEntity::getStatus, status.trim().toUpperCase(Locale.ROOT));
        }
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(MerchantApplicationEntity::getMerchantName, kw)
                    .or().like(MerchantApplicationEntity::getApplicationNo, kw)
                    .or().like(MerchantApplicationEntity::getContactPhone, kw)
                    .or().like(MerchantApplicationEntity::getContactEmail, kw));
        }
        wrapper.orderByDesc(MerchantApplicationEntity::getCreatedAt);
        return applicationMapper.selectPage(new Page<>(page, pageSize), wrapper);
    }

    @Override
    public MerchantApplicationDetailVO getDetail(Long id) {
        MerchantApplicationEntity row = requireApplication(id);
        return toDetailVo(row);
    }

    @Override
    @Transactional
    public void approve(Long id, String approverUsername, Long approverId, String clientIp) {
        MerchantApplicationEntity app = requireApplication(id);
        if (!MerchantApplicationEntity.STATUS_SUBMITTED.equals(app.getStatus())
                && !MerchantApplicationEntity.STATUS_REVIEWING.equals(app.getStatus())) {
            throw new BizException(INVALID_STATE, "当前状态不允许审批通过");
        }

        String merchantId = allocateMerchantId();
        String appSecret = randomAlphanumeric(32);
        String tempPassword = randomAlphanumeric(12);
        String adminUsername = resolveAdminUsername(app.getContactEmail(), merchantId);

        Merchant merchant = new Merchant();
        merchant.setMerchantId(merchantId);
        merchant.setMerchantName(app.getMerchantName());
        merchant.setMerchantKey(appSecret);
        merchant.setStatus("ACTIVE");
        merchant.setCommissionRate(new BigDecimal("0.0060"));
        merchant.setRateCalcMode("flat");
        merchant.setCreatedAt(LocalDateTime.now());
        merchant.setUpdatedAt(LocalDateTime.now());
        merchantService.create(merchant);

        AdminUser adminUser = new AdminUser();
        ensureAdminUsernameAvailable(adminUsername);
        adminUser.setUsername(adminUsername);
        adminUser.setPassword(passwordEncoder.encode(tempPassword));
        adminUser.setRole(ROLE_MERCHANT_ADMIN);
        adminUser.setNickname(app.getMerchantName());
        adminUser.setStatus("ACTIVE");
        adminUser.setDataMerchantIds(merchantId);
        adminUser.setUiTheme("mint");
        adminUser.setUiTableDensity("standard");
        adminUser.setUiSidebarCollapsed(false);
        LocalDateTime now = LocalDateTime.now();
        adminUser.setCreatedAt(now);
        adminUser.setUpdatedAt(now);
        adminUserMapper.insert(adminUser);

        bindMerchantAdminRole(adminUser.getId());

        MerchantProvisionRequest provision = new MerchantProvisionRequest();
        provision.setMerchantId(merchantId);
        provision.setMerchantName(app.getMerchantName());
        provision.setAppSecret(appSecret);
        provision.setPasswordHash(passwordEncoder.encode(tempPassword));
        provision.setContact(app.getContactName());
        provision.setPhone(app.getContactPhone());
        provision.setEmail(app.getContactEmail());
        provision.setDescription("入驻审批开通");
        cashierMerchantProvisionClient.provision(provision);

        OnboardingSecretCipherKit.SecretPayload payload = new OnboardingSecretCipherKit.SecretPayload(
                merchantId, appSecret, tempPassword, adminUsername, adminLoginUrl);
        String cipher = secretCipherKit.encrypt(payload);

        app.setStatus(MerchantApplicationEntity.STATUS_APPROVED);
        app.setAllocatedMerchantId(merchantId);
        app.setSecretCipher(cipher);
        app.setApproverId(approverId);
        app.setApprovedAt(now);
        app.setUpdatedAt(now);
        applicationMapper.updateById(app);

        auditLogService.record(approverUsername, "SYSTEM_ADMIN", merchantId, "ONBOARDING_APPROVE",
                "/api/v1/admin/onboarding/applications/" + id + "/approve", "MERCHANT_APPLICATION",
                app.getApplicationNo(), "审批通过并开通商户 " + merchantId, "SUCCESS", null, clientIp);
    }

    @Override
    @Transactional
    public void reject(Long id, MerchantApplicationRejectRequest request, String approverUsername,
                       Long approverId, String clientIp) {
        MerchantApplicationEntity app = requireApplication(id);
        if (!MerchantApplicationEntity.STATUS_SUBMITTED.equals(app.getStatus())
                && !MerchantApplicationEntity.STATUS_REVIEWING.equals(app.getStatus())) {
            throw new BizException(INVALID_STATE, "当前状态不允许拒绝");
        }
        LocalDateTime now = LocalDateTime.now();
        app.setStatus(MerchantApplicationEntity.STATUS_REJECTED);
        app.setRejectReason(request.getRejectReason().trim());
        app.setApproverId(approverId);
        app.setRejectedAt(now);
        app.setUpdatedAt(now);
        applicationMapper.updateById(app);

        auditLogService.record(approverUsername, "SYSTEM_ADMIN", null, "ONBOARDING_REJECT",
                "/api/v1/admin/onboarding/applications/" + id + "/reject", "MERCHANT_APPLICATION",
                app.getApplicationNo(), request.getRejectReason(), "SUCCESS", null, clientIp);
    }

    @Override
    @Transactional
    public MerchantApplicationResultVO queryResult(MerchantApplicationResultRequest request, String clientIp) {
        String applicationNo = request.getApplicationNo().trim();
        rateLimitService.assertResultNotLocked(applicationNo);

        MerchantApplicationEntity app = applicationMapper.selectOne(
                new LambdaQueryWrapper<MerchantApplicationEntity>()
                        .eq(MerchantApplicationEntity::getApplicationNo, applicationNo));
        if (app == null) {
            throw new BizException(NOT_FOUND, "申请单不存在");
        }

        if (!contactMatches(app, request.getContact())) {
            rateLimitService.recordResultContactMismatch(applicationNo);
            throw new BizException(CONTACT_MISMATCH, "联系方式与申请时不一致");
        }

        if (MerchantApplicationEntity.STATUS_REJECTED.equals(app.getStatus())) {
            String reason = app.getRejectReason() != null ? app.getRejectReason() : "审核未通过";
            throw new BizException(NOT_APPROVED, "审核未通过：" + reason);
        }
        if (!MerchantApplicationEntity.STATUS_APPROVED.equals(app.getStatus())) {
            throw new BizException(PENDING, "申请审核中，请耐心等待");
        }

        int count = app.getResultQueryCount() != null ? app.getResultQueryCount() : 0;
        if (count >= MAX_RESULT_QUERIES) {
            throw new BizException(QUERY_LIMIT, "已超过允许的查询次数（" + MAX_RESULT_QUERIES + " 次）");
        }

        OnboardingSecretCipherKit.SecretPayload payload = secretCipherKit.decrypt(app.getSecretCipher());
        LocalDateTime now = LocalDateTime.now();
        app.setResultQueryCount(count + 1);
        if (app.getSecretViewedAt() == null) {
            app.setSecretViewedAt(now);
        }
        app.setUpdatedAt(now);
        applicationMapper.updateById(app);

        rateLimitService.clearResultFailures(applicationNo);

        auditLogService.record("PUBLIC", "MERCHANT_PUBLIC", payload.merchantId(), "ONBOARDING_RESULT_QUERY",
                "/api/v1/internal/onboarding/result", "MERCHANT_APPLICATION", applicationNo,
                "自助查询密钥", "SUCCESS", null, clientIp);

        return MerchantApplicationResultVO.builder()
                .applicationNo(applicationNo)
                .merchantId(payload.merchantId())
                .appSecret(payload.appSecret())
                .tempPassword(payload.tempPassword())
                .adminUsername(payload.adminUsername())
                .loginUrl(payload.loginUrl())
                .remainingQueries(Math.max(0, MAX_RESULT_QUERIES - (count + 1)))
                .build();
    }

    private void ensureAdminUsernameAvailable(String username) {
        AdminUser existing = adminUserMapper.selectOne(new LambdaQueryWrapper<AdminUser>()
                .eq(AdminUser::getUsername, username));
        if (existing != null) {
            throw new BizException(6206, "登录账号已存在，请使用其他邮箱或联系运营处理");
        }
    }

    private void bindMerchantAdminRole(Long userId) {
        SysRole role = sysRoleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, ROLE_MERCHANT_ADMIN)
                .eq(SysRole::getStatus, "ACTIVE"));
        if (role == null) {
            throw new IllegalStateException("未配置 MERCHANT_ADMIN 角色，请先执行入驻迁移脚本");
        }
        long exists = sysUserRoleMapper.selectCount(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, userId)
                .eq(SysUserRole::getRoleId, role.getId()));
        if (exists == 0) {
            SysUserRole link = new SysUserRole();
            link.setUserId(userId);
            link.setRoleId(role.getId());
            link.setCreatedAt(LocalDateTime.now());
            sysUserRoleMapper.insert(link);
        }
    }

    private String allocateMerchantId() {
        Merchant latest = merchantMapper.selectOne(new LambdaQueryWrapper<Merchant>()
                .likeRight(Merchant::getMerchantId, "M")
                .orderByDesc(Merchant::getMerchantId)
                .last("LIMIT 1"));
        int next = 100001;
        if (latest != null && latest.getMerchantId() != null) {
            String mid = latest.getMerchantId();
            if (mid.startsWith("M") && mid.length() > 1) {
                try {
                    next = Integer.parseInt(mid.substring(1)) + 1;
                } catch (NumberFormatException ignored) {
                    next = 100001;
                }
            }
        }
        while (merchantService.getByMerchantId("M" + next) != null) {
            next++;
        }
        return "M" + next;
    }

    private static String generateApplicationNo() {
        String date = DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDateTime.now());
        int suffix = ThreadLocalRandom.current().nextInt(100000, 999999);
        return "AP" + date + suffix;
    }

    private static String randomAlphanumeric(int len) {
        final String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private static String resolveAdminUsername(String email, String merchantId) {
        if (email != null && email.contains("@")) {
            String local = email.trim().toLowerCase(Locale.ROOT);
            if (local.length() <= 64) {
                return local;
            }
        }
        return "m_" + merchantId.toLowerCase(Locale.ROOT);
    }

    private MerchantApplicationEntity requireApplication(Long id) {
        MerchantApplicationEntity row = applicationMapper.selectById(id);
        if (row == null) {
            throw new BizException(NOT_FOUND, "申请不存在");
        }
        return row;
    }

    private static boolean contactMatches(MerchantApplicationEntity app, String contact) {
        if (contact == null) {
            return false;
        }
        String normalized = contact.trim();
        if (normalized.contains("@")) {
            return normalizeEmail(app.getContactEmail()).equalsIgnoreCase(normalized.toLowerCase(Locale.ROOT));
        }
        return normalizePhone(app.getContactPhone()).equals(normalizePhone(normalized));
    }

    private static String normalizePhone(String phone) {
        return phone == null ? "" : phone.trim();
    }

    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private static String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private static MerchantApplicationDetailVO toDetailVo(MerchantApplicationEntity row) {
        return MerchantApplicationDetailVO.builder()
                .id(row.getId())
                .applicationNo(row.getApplicationNo())
                .merchantName(row.getMerchantName())
                .status(row.getStatus())
                .applicationSource(row.getApplicationSource())
                .bizLicenseNo(row.getBizLicenseNo())
                .contactName(row.getContactName())
                .contactPhone(row.getContactPhone())
                .contactEmail(row.getContactEmail())
                .allocatedMerchantId(row.getAllocatedMerchantId())
                .payloadJson(row.getPayloadJson())
                .rejectReason(row.getRejectReason())
                .resultQueryCount(row.getResultQueryCount())
                .secretViewedAt(row.getSecretViewedAt())
                .approverId(row.getApproverId())
                .approvedAt(row.getApprovedAt())
                .rejectedAt(row.getRejectedAt())
                .createdAt(row.getCreatedAt())
                .updatedAt(row.getUpdatedAt())
                .build();
    }
}
