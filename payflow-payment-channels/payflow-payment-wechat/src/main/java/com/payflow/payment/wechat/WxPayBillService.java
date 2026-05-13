package com.payflow.payment.wechat;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.payflow.payment.core.ChannelConfigHolder;
import com.payflow.common.exception.BizException;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

/**
 * 微信支付对账单：申请下载链接并拉取账单文件（V3）。
 *
 * @author PayFlow Team
 */
@Slf4j
@Component
public class WxPayBillService {

    private static final String API_BASE = "https://api.mch.weixin.qq.com";

    private static final DateTimeFormatter BILL_DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final HttpClient httpClient;

    public WxPayBillService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * 查询交易账单下载信息。
     *
     * @param account  渠道账户
     * @param billDate 账单日期
     * @return 下载地址与摘要
     */
    public WxBillDownloadMeta queryTradeBill(ChannelConfigHolder account, LocalDate billDate) {
        WxPayAccountConfig config = WxPayConfigLoader.load(account);
        String dateStr = billDate.format(BILL_DATE_FMT);
        String path = "/v3/bill/tradebill?bill_date=" + dateStr;
        try {
            String body = executeV3Get(config, path);
            JSONObject resp = JSONUtil.parseObj(body);
            String url = resp.getStr("download_url");
            if (StrUtil.isBlank(url)) {
                throw new BizException(6005, "微信账单响应缺少 download_url: " + body);
            }
            return WxBillDownloadMeta.builder()
                    .downloadUrl(url)
                    .hashValue(resp.getStr("hash_value"))
                    .hashType(resp.getStr("hash_type"))
                    .build();
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("微信查询交易账单失败: billDate={}", dateStr, e);
            throw new BizException(6005, "微信查询交易账单失败", e);
        }
    }

    /**
     * 下载账单文件（通常为 gzip 压缩的 CSV）。
     *
     * @param downloadUrl 微信返回的下载地址
     * @return 原始字节
     */
    public byte[] downloadBillBytes(String downloadUrl) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(downloadUrl))
                    .GET()
                    .timeout(Duration.ofMinutes(2))
                    .build();
            HttpResponse<byte[]> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BizException(6005, "微信账单下载 HTTP " + response.statusCode());
            }
            return response.body();
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("微信账单下载失败: url={}", downloadUrl, e);
            throw new BizException(6005, "微信账单下载失败", e);
        }
    }

    private String executeV3Get(WxPayAccountConfig config, String pathWithQuery) throws Exception {
        long timestamp = Instant.now().getEpochSecond();
        String nonceStr = StrUtil.uuid().replace("-", "");
        String signStr = buildSignature(config, "GET", pathWithQuery, timestamp, nonceStr, "");
        String authorization = buildAuthorization(config, timestamp, nonceStr, signStr);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_BASE + pathWithQuery))
                .header("Accept", "application/json")
                .header("Authorization", authorization)
                .header("User-Agent", "PayFlow-Recon/1.0")
                .timeout(Duration.ofSeconds(60))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        int statusCode = response.statusCode();
        String body = response.body();
        if (statusCode != 200) {
            log.error("微信GET失败: path={}, status={}, body={}", pathWithQuery, statusCode, body);
            JSONObject errResp = JSONUtil.parseObj(body);
            String errMsg = errResp.getStr("message", "请求失败");
            throw new BizException(6005, "微信账单API错误: " + errMsg);
        }
        return body;
    }

    private String buildSignature(WxPayAccountConfig config, String method,
                                  String path, long timestamp, String nonce, String body) {
        try {
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initSign(config.getPrivateKeyObj());
            String signStr = method + "\n" + path + "\n" + timestamp + "\n" + nonce + "\n" + body + "\n";
            sig.update(signStr.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(sig.sign());
        } catch (NoSuchAlgorithmException | InvalidKeyException | java.security.SignatureException e) {
            throw new RuntimeException("签名算法异常", e);
        }
    }

    private String buildAuthorization(WxPayAccountConfig config, long timestamp,
                                      String nonce, String signature) {
        return "WXAUTH appId=\"" + config.appId + "\", "
                + "nonceStr=\"" + nonce + "\", "
                + "timestamp=\"" + timestamp + "\", "
                + "signature=\"" + signature + "\"";
    }

    /**
     * 账单下载元数据。
     */
    @Value
    @Builder
    public static class WxBillDownloadMeta {
        String downloadUrl;
        String hashValue;
        String hashType;
    }
}
