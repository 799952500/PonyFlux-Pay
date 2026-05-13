package com.payflow.payment.union;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import lombok.extern.slf4j.Slf4j;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

/**
 * 银联 HTTP 客户端：构建请求、签名、POST 提交、响应解析。
 *
 * @author PayFlow Team
 */
@Slf4j
public final class UnionPayHttpClient {

    private static final DateTimeFormatter TXN_TIME_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final UnionPayAccountConfig config;

    public UnionPayHttpClient(UnionPayAccountConfig config) {
        this.config = config;
    }

    // ---- 前台交易（H5，返回重定向 HTML） ----

    /**
     * 发起前台交易请求，返回银联支付页面的完整 URL。
     */
    public String frontTransUrl(Map<String, String> bizParams, String frontUrl, String backUrl) {
        String path = config.getGatewayUrl() + UnionPayApiConstants.PATH_FRONT_TRANS;
        // 构建完整请求参数，不含 signature
        Map<String, String> req = buildCommonParams();
        req.put("txnType", UnionPayApiConstants.TXN_TYPE_PAY);
        req.put("txnSubType", bizParams.getOrDefault("txnSubType", UnionPayApiConstants.TXN_SUB_TYPE_H5));
        req.put("bizType", bizParams.getOrDefault("bizType", UnionPayApiConstants.BIZ_TYPE_H5));
        req.put("channelType", bizParams.getOrDefault("channelType", UnionPayApiConstants.CHANNEL_TYPE_MOBILE));
        req.put("accessType", UnionPayApiConstants.ACCESS_TYPE_MERCHANT);
        req.put("merId", config.getMerId());
        req.put("orderId", bizParams.get("orderId"));
        req.put("txnAmt", bizParams.get("txnAmt"));
        // txnTime 需要方已经设好或在此生成
        if (!req.containsKey("txnTime")) {
            req.put("txnTime", LocalDateTime.now().format(TXN_TIME_FMT));
        }
        req.put("frontUrl", frontUrl);
        req.put("backUrl", backUrl);

        req.put("signature", UnionPaySignature.sign(req, config.getSignCertPath(), config.getSignCertPassword()));

        // 构建 GET URL（前台交易为 GET 重定向）
        return path + "?" + buildQueryString(req);
    }

    // ---- 后台交易（QR、退款、查询） ----

    /**
     * 发起后台交易请求，返回 key=value&... 格式的响应字符串。
     */
    public Map<String, String> backTrans(Map<String, String> bizParams) {
        String url = config.getGatewayUrl() + UnionPayApiConstants.PATH_BACK_TRANS;

        Map<String, String> req = buildCommonParams();
        req.putAll(bizParams);
        req.put("merId", config.getMerId());

        req.put("signature", UnionPaySignature.sign(req, config.getSignCertPath(), config.getSignCertPassword()));

        String body = buildFormBody(req);
        log.info("银联后台交易请求: url={}, body={}", url, body);

        try (HttpResponse resp = HttpRequest.post(url)
                .header("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8")
                .body(body)
                .timeout(30000)
                .execute()) {
            String respBody = resp.body();
            log.info("银联后台交易响应: status={}, body={}", resp.getStatus(), respBody);
            return parseFormBody(respBody);
        }
    }

    // ---- 文件下载（账单） ----

    /**
     * 下载文件（账单），返回原始字节数组。
     */
    public byte[] fileDownload(Map<String, String> bizParams) {
        String url = config.getGatewayUrl() + UnionPayApiConstants.PATH_FILE_TRANS;

        Map<String, String> req = buildCommonParams();
        req.putAll(bizParams);
        req.put("merId", config.getMerId());

        req.put("signature", UnionPaySignature.sign(req, config.getSignCertPath(), config.getSignCertPassword()));

        String body = buildFormBody(req);
        log.info("银联文件下载请求: url={}", url);

        try (HttpResponse resp = HttpRequest.post(url)
                .header("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8")
                .body(body)
                .timeout(60000)
                .execute()) {
            log.info("银联文件下载响应: status={}, contentLength={}", resp.getStatus(), resp.bodyBytes().length);
            return resp.bodyBytes();
        }
    }

    // ---- 公共参数 ----

    private Map<String, String> buildCommonParams() {
        Map<String, String> m = new HashMap<>();
        m.put("version", UnionPayApiConstants.VERSION);
        m.put("encoding", UnionPayApiConstants.ENCODING);
        m.put("signMethod", UnionPayApiConstants.SIGN_METHOD_RSA2);
        m.put("txnTime", LocalDateTime.now().format(TXN_TIME_FMT));
        m.put("accessType", UnionPayApiConstants.ACCESS_TYPE_MERCHANT);
        return m;
    }

    // ---- 工具 ----

    static String buildQueryString(Map<String, String> params) {
        StringJoiner sj = new StringJoiner("&");
        params.forEach((k, v) -> {
            if (v != null) {
                sj.add(k + "=" + URLEncoder.encode(v, StandardCharsets.UTF_8));
            }
        });
        return sj.toString();
    }

    static String buildFormBody(Map<String, String> params) {
        StringJoiner sj = new StringJoiner("&");
        params.forEach((k, v) -> {
            if (v != null) {
                sj.add(k + "=" + URLEncoder.encode(v, StandardCharsets.UTF_8));
            }
        });
        return sj.toString();
    }

    static Map<String, String> parseFormBody(String body) {
        Map<String, String> result = new HashMap<>();
        if (StrUtil.isBlank(body)) {
            return result;
        }
        for (String pair : body.split("&")) {
            int idx = pair.indexOf('=');
            if (idx > 0) {
                result.put(pair.substring(0, idx), pair.substring(idx + 1));
            }
        }
        return result;
    }
}
