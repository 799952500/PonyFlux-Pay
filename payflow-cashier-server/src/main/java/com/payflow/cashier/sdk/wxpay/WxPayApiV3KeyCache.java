package com.payflow.cashier.sdk.wxpay;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflow.cashier.entity.PayChannelAccount;
import com.payflow.cashier.mapper.PayChannelAccountMapper;
import com.payflow.cashier.mapper.PayChannelMapper;
import com.payflow.cashier.entity.PayChannel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 微信支付 APIv3 密钥缓存。
 * 从数据库 channelConfig JSON 中加载 apiV3Key，支持 Redis Pub/Sub 刷新。
 *
 * @author PayFlow Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WxPayApiV3KeyCache {

    private final PayChannelAccountMapper payChannelAccountMapper;
    private final PayChannelMapper payChannelMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final ConcurrentHashMap<String, byte[]> cache = new ConcurrentHashMap<>();

    /**
     * 获取所有已缓存的 APIv3 密钥（用于回调解密时逐个尝试）。
     */
    public List<byte[]> getCachedKeys() {
        if (cache.isEmpty()) {
            refreshCache();
        }
        return List.copyOf(cache.values());
    }

    /**
     * 刷新缓存：从数据库查询所有启用的微信渠道账户，提取 apiV3Key。
     */
    public synchronized void refreshCache() {
        cache.clear();
        try {
            List<PayChannel> wxChannels = payChannelMapper.selectList(
                    new LambdaQueryWrapper<PayChannel>()
                            .eq(PayChannel::getStatus, "ENABLED")
                            .like(PayChannel::getChannelCode, "wxpay")
            );
            if (wxChannels.isEmpty()) {
                log.warn("未找到启用的微信渠道，APIv3 密钥缓存为空");
                return;
            }
            List<Long> channelIds = wxChannels.stream()
                    .map(PayChannel::getId)
                    .collect(Collectors.toList());

            List<PayChannelAccount> accounts = payChannelAccountMapper.selectList(
                    new LambdaQueryWrapper<PayChannelAccount>()
                            .in(PayChannelAccount::getChannelId, channelIds)
                            .eq(PayChannelAccount::getStatus, "ENABLED")
            );

            for (PayChannelAccount account : accounts) {
                try {
                    String configJson = account.getChannelConfig();
                    if (configJson == null || configJson.isBlank()) {
                        continue;
                    }
                    Map<String, String> config = objectMapper.readValue(configJson,
                            new TypeReference<Map<String, String>>() {});
                    String apiV3Key = config.get("apiV3Key");
                    if (apiV3Key != null && !apiV3Key.isBlank()) {
                        cache.put(account.getAccountCode(), apiV3Key.getBytes(StandardCharsets.UTF_8));
                        log.info("缓存微信 APIv3 密钥: accountCode={}", account.getAccountCode());
                    }
                } catch (Exception e) {
                    log.warn("解析渠道账户 {} 的 channelConfig 失败: {}", account.getAccountCode(), e.getMessage());
                }
            }
            log.info("微信 APIv3 密钥缓存刷新完成: 共 {} 个密钥", cache.size());
        } catch (Exception e) {
            log.error("刷新微信 APIv3 密钥缓存失败", e);
        }
    }

    /**
     * 清空缓存（响应 Redis Pub/Sub 配置刷新事件）。
     */
    public void invalidate() {
        cache.clear();
        log.info("微信 APIv3 密钥缓存已失效");
    }
}
