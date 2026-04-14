package com.payflow.cashier.middleware;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflow.cashier.config.PayflowProperties;
import com.payflow.cashier.entity.Merchant;
import com.payflow.cashier.exception.R;
import com.payflow.cashier.mapper.MerchantMapper;
import com.payflow.cashier.util.MerchantSignatureUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 商户签名验证拦截器
 * <p>
 * 拦截所有 /api/v1/merchant/** 请求，通过 HMAC-SHA256 签名验证请求合法性。
 * <p>
 * 签名生成规则：
 * 签名原文 = HTTP方法 + "\n" + 请求路径 + "\n" + queryString(按字典序排序) + "\n" + 时间戳
 * 签名 = HMAC-SHA256(签名原文, appSecret) → hex小写
 * <p>
 * 请求头规范：
 * - X-Merchant-Id: 商户ID
 * - X-Timestamp: 时间戳（Unix秒）
 * - X-Sign: 签名结果
 *
 * @author PayFlow Team
 */
@Slf4j
@Component
public class MerchantSignatureInterceptor implements HandlerInterceptor {

    /** 请求属性 Key：存放已验证的商户号 */
    public static final String ATTR_MERCHANT_ID = "merchantId";

    private final PayflowProperties properties;
    private final ObjectMapper objectMapper;
    private final MerchantMapper merchantMapper;

    public MerchantSignatureInterceptor(PayflowProperties properties,
                                          ObjectMapper objectMapper,
                                          MerchantMapper merchantMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.merchantMapper = merchantMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String merchantId = request.getHeader("X-Merchant-Id");
        String timestampStr = request.getHeader("X-Timestamp");
        String sign = request.getHeader("X-Sign");

        // 1. 参数校验
        if (merchantId == null || merchantId.isBlank()) {
            log.warn("商户签名验证失败: 缺少 X-Merchant-Id 头, path={}", request.getRequestURI());
            return fail(response, "缺少 X-Merchant-Id");
        }
        if (timestampStr == null || timestampStr.isBlank()) {
            log.warn("商户签名验证失败: 缺少 X-Timestamp 头, path={}", request.getRequestURI());
            return fail(response, "缺少 X-Timestamp");
        }
        if (sign == null || sign.isBlank()) {
            log.warn("商户签名验证失败: 缺少 X-Sign 头, path={}", request.getRequestURI());
            return fail(response, "缺少 X-Sign");
        }

        // 2. 时间戳校验（5分钟窗口）
        long timestamp;
        try {
            timestamp = Long.parseLong(timestampStr);
        } catch (NumberFormatException e) {
            log.warn("商户签名验证失败: X-Timestamp 格式错误, value={}, path={}", timestampStr, request.getRequestURI());
            return fail(response, "X-Timestamp 格式错误");
        }
        long now = System.currentTimeMillis() / 1000;
        int tolerance = properties.getSignature().getTimestampTolerance();
        if (Math.abs(now - timestamp) > tolerance) {
            log.warn("商户签名验证失败: 请求已过期, timestamp={}, now={}, diff={}s, path={}",
                    timestamp, now, Math.abs(now - timestamp), request.getRequestURI());
            return fail(response, "请求已过期，时间戳偏差过大");
        }

        // 3. 查询商户密钥（优先从配置文件读取；若配置为空再查数据库）
        String appSecret = getMerchantAppSecret(merchantId);
        if (appSecret == null || appSecret.isBlank()) {
            log.warn("商户签名验证失败: 商户不存在或未配置密钥, merchantId={}, path={}", merchantId, request.getRequestURI());
            return fail(response, "商户不存在或未配置签名密钥");
        }

        // 4. 签名验证
        String method = request.getMethod();
        String path = request.getRequestURI();
        String queryString = MerchantSignatureUtil.buildQueryStringSorted(request);

        boolean valid = MerchantSignatureUtil.verify(method, path, queryString, timestamp, appSecret, sign);
        if (!valid) {
            log.warn("商户签名验证失败: 签名不匹配, merchantId={}, method={}, path={}, queryString={}, timestamp={}",
                    merchantId, method, path, queryString, timestamp);
            return fail(response, "签名验证失败");
        }

        // 5. 注入 merchantId 到请求属性
        request.setAttribute(ATTR_MERCHANT_ID, merchantId);
        log.debug("商户签名验证成功: merchantId={}, path={}", merchantId, request.getRequestURI());
        return true;
    }

    /**
     * 获取商户签名密钥
     * 优先级：配置文件 merchants 配置 > 数据库 merchants.app_secret 字段
     *
     * @param merchantId 商户号
     * @return 商户密钥，未找到返回 null
     */
    private String getMerchantAppSecret(String merchantId) {
        // 1. 优先从配置文件读取
        String appSecret = properties.getMerchantAppSecret(merchantId);
        if (appSecret != null && !appSecret.isBlank()) {
            log.debug("从配置文件获取商户密钥: merchantId={}", merchantId);
            return appSecret;
        }
        // 2. 配置为空时从数据库查询
        log.debug("配置中未找到密钥，尝试从数据库查询: merchantId={}", merchantId);
        Merchant merchant = merchantMapper.selectOne(
                new LambdaQueryWrapper<Merchant>()
                        .eq(Merchant::getMerchantId, merchantId)
                        .isNotNull(Merchant::getAppSecret)
                        .select(Merchant::getAppSecret)
        );
        return merchant != null ? merchant.getAppSecret() : null;
    }

    private boolean fail(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        R<?> r = R.unauthorized(message);
        objectMapper.writeValue(response.getWriter(), r);
        return false;
    }
}
