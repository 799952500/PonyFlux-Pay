package com.payflow.cashier.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payflow.cashier.config.PayflowProperties;
import com.payflow.cashier.dto.LoginRequest;
import com.payflow.cashier.dto.LoginResponse;
import com.payflow.cashier.entity.Merchant;
import com.payflow.cashier.exception.BizException;
import com.payflow.cashier.mapper.MerchantMapper;
import com.payflow.cashier.service.AuthService;
import com.payflow.cashier.util.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 认证服务实现
 *
 * @author PayFlow Team
 */
@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    private final MerchantMapper merchantMapper;
    private final PayflowProperties properties;

    public AuthServiceImpl(MerchantMapper merchantMapper, PayflowProperties properties) {
        this.merchantMapper = merchantMapper;
        this.properties = properties;
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        String merchantId = request.getMerchantId();
        String password = request.getPassword();

        log.info("商户登录请求: merchantId={}", merchantId);

        // 1. 查询商户
        Merchant merchant = merchantMapper.selectOne(
                new LambdaQueryWrapper<Merchant>()
                        .eq(Merchant::getMerchantId, merchantId)
        );

        // 2. 校验商户存在
        if (merchant == null) {
            log.warn("商户不存在: merchantId={}", merchantId);
            throw new BizException(4010, "商户不存在");
        }

        // 3. 校验商户状态
        if (!Merchant.STATUS_ACTIVE.equals(merchant.getStatus())) {
            log.warn("商户状态异常: merchantId={}, status={}", merchantId, merchant.getStatus());
            throw new BizException(4010, "商户状态异常，请联系管理员");
        }

        // 4. 校验密码（MD5 比对）
        String passwordMd5 = md5(password);
        if (!passwordMd5.equalsIgnoreCase(merchant.getPassword())) {
            log.warn("密码错误: merchantId={}", merchantId);
            throw new BizException(4010, "密码错误");
        }

        // 5. 生成 JWT Token
        long expireSeconds = properties.getJwt().getExpireSeconds();
        String token = JwtUtils.generateToken(
                properties.getJwt().getSecret(),
                merchant.getMerchantId(),
                merchant.getMerchantName(),
                expireSeconds
        );

        // 6. 构建响应
        LoginResponse response = LoginResponse.builder()
                .token(token)
                .merchantId(merchant.getMerchantId())
                .merchantName(merchant.getMerchantName())
                .expireTime(JwtUtils.calcExpireTimeStr(expireSeconds))
                .build();

        log.info("商户登录成功: merchantId={}, merchantName={}", merchantId, merchant.getMerchantName());
        return response;
    }

    /**
     * 简单 MD5 摘要（用于密码比对）
     */
    private String md5(String input) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5算法不可用", e);
        }
    }
}
