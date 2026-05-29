package com.payflow.cashier.sdk.wxpay;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflow.cashier.entity.PayChannel;
import com.payflow.cashier.entity.PayChannelAccount;
import com.payflow.cashier.mapper.PayChannelAccountMapper;
import com.payflow.cashier.mapper.PayChannelMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 微信支付平台证书公钥缓存（按证书序列号索引，用于回调验签）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WxPayPlatformCertCache {

    private final PayChannelAccountMapper payChannelAccountMapper;
    private final PayChannelMapper payChannelMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final ConcurrentHashMap<String, PublicKey> cacheBySerial = new ConcurrentHashMap<>();

    /**
     * 按 Wechatpay-Serial 查找平台公钥。
     */
    public PublicKey findBySerial(String serial) {
        if (serial == null || serial.isBlank()) {
            return null;
        }
        if (cacheBySerial.isEmpty()) {
            refreshCache();
        }
        return cacheBySerial.get(serial.trim());
    }

    public synchronized void refreshCache() {
        cacheBySerial.clear();
        try {
            List<PayChannel> wxChannels = payChannelMapper.selectList(
                    new LambdaQueryWrapper<PayChannel>()
                            .eq(PayChannel::getStatus, "ENABLED")
                            .like(PayChannel::getChannelCode, "wxpay"));
            if (wxChannels.isEmpty()) {
                log.warn("未找到启用的微信渠道，平台证书缓存为空");
                return;
            }
            List<Long> channelIds = wxChannels.stream().map(PayChannel::getId).collect(Collectors.toList());
            List<PayChannelAccount> accounts = payChannelAccountMapper.selectList(
                    new LambdaQueryWrapper<PayChannelAccount>()
                            .in(PayChannelAccount::getChannelId, channelIds)
                            .eq(PayChannelAccount::getStatus, "ENABLED"));

            for (PayChannelAccount account : accounts) {
                loadAccountCerts(account);
            }
            log.info("微信平台证书缓存刷新完成: 共 {} 个序列号", cacheBySerial.size());
        } catch (Exception e) {
            log.error("刷新微信平台证书缓存失败", e);
        }
    }

    private void loadAccountCerts(PayChannelAccount account) {
        try {
            String configJson = account.getChannelConfig();
            if (configJson == null || configJson.isBlank()) {
                return;
            }
            Map<String, String> config = objectMapper.readValue(configJson,
                    new TypeReference<Map<String, String>>() {});
            String serial = firstNonBlank(config.get("platformSerialNo"),
                    config.get("wechatPayPlatformSerial"), config.get("serialNo"));
            String pem = firstNonBlank(config.get("wechatPayPlatformCert"),
                    config.get("platformCert"), config.get("platformPublicKey"));
            if (serial == null || pem == null) {
                return;
            }
            PublicKey publicKey = parsePublicKey(pem);
            cacheBySerial.put(serial.trim(), publicKey);
            log.info("缓存微信平台证书: serial={}, accountCode={}", serial, account.getAccountCode());
        } catch (Exception e) {
            log.warn("解析账户 {} 平台证书失败: {}", account.getAccountCode(), e.getMessage());
        }
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return null;
    }

    private static PublicKey parsePublicKey(String pem) throws Exception {
        String trimmed = pem.trim();
        byte[] der;
        if (trimmed.contains("BEGIN CERTIFICATE")) {
            String normalized = trimmed
                    .replace("-----BEGIN CERTIFICATE-----", "")
                    .replace("-----END CERTIFICATE-----", "")
                    .replaceAll("\\s", "");
            der = Base64.getDecoder().decode(normalized);
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            Certificate cert = cf.generateCertificate(new ByteArrayInputStream(der));
            return cert.getPublicKey();
        }
        String normalized = trimmed
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        der = Base64.getDecoder().decode(normalized);
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
    }

    public void invalidate() {
        cacheBySerial.clear();
    }
}
