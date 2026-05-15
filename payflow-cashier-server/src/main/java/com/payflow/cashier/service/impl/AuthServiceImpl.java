package com.payflow.cashier.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payflow.cashier.config.PayflowProperties;
import com.payflow.cashier.dto.LoginRequest;
import com.payflow.cashier.dto.LoginResponse;
import com.payflow.cashier.entity.Merchant;
import com.payflow.common.exception.BizException;
import com.payflow.cashier.mapper.MerchantMapper;
import com.payflow.cashier.service.AuthService;
import com.payflow.cashier.util.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 认证服务实现
 *
 * @author PayFlow Team
 */
@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    private static final int MAX_LOGIN_FAILURES = 5;
    private static final long LOCK_DURATION_MINUTES = 15;

    private final MerchantMapper merchantMapper;
    private final PayflowProperties properties;
    private final StringRedisTemplate stringRedisTemplate;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthServiceImpl(MerchantMapper merchantMapper, PayflowProperties properties,
                           StringRedisTemplate stringRedisTemplate) {
        this.merchantMapper = merchantMapper;
        this.properties = properties;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        String merchantId = request.getMerchantId();
        String password = request.getPassword();

        log.info("商户登录请求: merchantId={}", merchantId);

        // 0. 登录频率限制检查
        String lockKey = "login:locked:" + merchantId;
        try {
            Boolean locked = stringRedisTemplate.hasKey(lockKey);
            if (Boolean.TRUE.equals(locked)) {
                log.warn("商户登录锁定中: merchantId={}", merchantId);
                throw new BizException(4011,
                        "登录失败次数过多，请" + LOCK_DURATION_MINUTES + "分钟后再试");
            }
        } catch (Exception e) {
            if (e instanceof BizException) {
                throw (BizException) e;
            }
            // Redis 不可用时不阻止登录
            log.warn("Redis登录限制检查失败（已放行）: {}", e.getMessage());
        }

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

        // 4. 校验密码（bcrypt + MD5 兼容升级）
        String storedPassword = merchant.getPassword();
        boolean passwordMatch = false;
        boolean needsUpgrade = false;

        // 先尝试 bcrypt 验证
        try {
            if (storedPassword != null && storedPassword.startsWith("$2a$")) {
                passwordMatch = passwordEncoder.matches(password, storedPassword);
            }
        } catch (Exception ignored) {
        }

        // bcrypt 不匹配或非 bcrypt 格式 → 尝试 MD5 兼容旧密码
        if (!passwordMatch && storedPassword != null && storedPassword.length() == 32) {
            String passwordMd5 = md5(password);
            if (passwordMd5.equalsIgnoreCase(storedPassword)) {
                passwordMatch = true;
                needsUpgrade = true;
                log.info("MD5密码验证通过，将自动升级为bcrypt: merchantId={}", merchantId);
            }
        }

        if (!passwordMatch) {
            log.warn("密码错误: merchantId={}", merchantId);
            recordLoginFailure(merchantId);
            throw new BizException(4010, "密码错误");
        }

        // 自动升级为 bcrypt
        if (needsUpgrade) {
            merchant.setPassword(passwordEncoder.encode(password));
            merchantMapper.updateById(merchant);
            log.info("商户密码已升级为bcrypt: merchantId={}", merchantId);
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
                .merchantStatus(merchant.getStatus())
                .build();

        log.info("商户登录成功: merchantId={}, merchantName={}", merchantId, merchant.getMerchantName());

        // 登录成功，清除失败计数
        clearLoginAttempts(merchantId);

        return response;
    }

    /**
     * 记录登录失败，达到阈值后锁定。
     */
    private void recordLoginFailure(String merchantId) {
        try {
            String attemptsKey = "login:attempts:" + merchantId;
            Long attempts = stringRedisTemplate.opsForValue().increment(attemptsKey);
            if (attempts != null && attempts == 1L) {
                stringRedisTemplate.expire(attemptsKey, Duration.ofMinutes(LOCK_DURATION_MINUTES));
            }
            if (attempts != null && attempts >= MAX_LOGIN_FAILURES) {
                String lockKey = "login:locked:" + merchantId;
                stringRedisTemplate.opsForValue().set(lockKey, "1", Duration.ofMinutes(LOCK_DURATION_MINUTES));
                stringRedisTemplate.delete(attemptsKey);
                log.warn("商户登录已锁定: merchantId={}, failures={}", merchantId, attempts);
            }
        } catch (Exception e) {
            log.warn("记录登录失败异常（不影响主流程）: {}", e.getMessage());
        }
    }

    /**
     * 清除登录失败计数（登录成功后调用）。
     */
    private void clearLoginAttempts(String merchantId) {
        try {
            stringRedisTemplate.delete("login:attempts:" + merchantId);
            stringRedisTemplate.delete("login:locked:" + merchantId);
        } catch (Exception e) {
            log.warn("清除登录计数异常（不影响主流程）: {}", e.getMessage());
        }
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
