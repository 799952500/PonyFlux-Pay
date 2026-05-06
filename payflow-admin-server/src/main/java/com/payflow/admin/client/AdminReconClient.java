package com.payflow.admin.client;

import com.payflow.admin.config.ReconClientProperties;
import com.payflow.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * 调用对账服务内部 API。
 *
 * @author PayFlow Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminReconClient {

    public static final String HEADER_INTERNAL_TOKEN = "X-Payflow-Internal-Token";

    private final RestTemplate restTemplate;
    private final ReconClientProperties reconClientProperties;

    /**
     * 分页查询任务。
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> pageTasks(long page, long size, LocalDate billDate, String channel, String status) {
        ensureConfigured();
        UriComponentsBuilder b = UriComponentsBuilder.fromUriString(base() + "/api/v1/internal/recon/tasks")
                .queryParam("page", page)
                .queryParam("size", size);
        if (billDate != null) {
            b.queryParam("billDate", billDate.toString());
        }
        if (StringUtils.hasText(channel)) {
            b.queryParam("channel", channel);
        }
        if (StringUtils.hasText(status)) {
            b.queryParam("status", status);
        }
        try {
            ResponseEntity<Map> resp = restTemplate.exchange(
                    b.build(true).toUriString(),
                    HttpMethod.GET,
                    new HttpEntity<>(headers()),
                    Map.class);
            return unwrapData(resp.getBody());
        } catch (RestClientException ex) {
            log.warn("对账服务 pageTasks 调用失败: {}", ex.getMessage());
            throw new BizException(7550, "调用对账服务失败: " + ex.getMessage());
        }
    }

    /**
     * 任务详情。
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getTask(String taskId) {
        ensureConfigured();
        try {
            ResponseEntity<Map> resp = restTemplate.exchange(
                    base() + "/api/v1/internal/recon/tasks/" + taskId,
                    HttpMethod.GET,
                    new HttpEntity<>(headers()),
                    Map.class);
            return unwrapData(resp.getBody());
        } catch (RestClientException ex) {
            log.warn("对账服务 getTask 调用失败: taskId={}, message={}", taskId, ex.getMessage());
            throw new BizException(7550, "调用对账服务失败: " + ex.getMessage());
        }
    }

    /**
     * 分页查询差异。
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> pageDiffs(String taskId, long page, long size, String diffType, String handleStatus) {
        ensureConfigured();
        UriComponentsBuilder b = UriComponentsBuilder
                .fromUriString(base() + "/api/v1/internal/recon/tasks/" + taskId + "/diffs")
                .queryParam("page", page)
                .queryParam("size", size);
        if (StringUtils.hasText(diffType)) {
            b.queryParam("diffType", diffType);
        }
        if (StringUtils.hasText(handleStatus)) {
            b.queryParam("handleStatus", handleStatus);
        }
        try {
            ResponseEntity<Map> resp = restTemplate.exchange(
                    b.build(true).toUriString(),
                    HttpMethod.GET,
                    new HttpEntity<>(headers()),
                    Map.class);
            return unwrapData(resp.getBody());
        } catch (RestClientException ex) {
            log.warn("对账服务 pageDiffs 调用失败: taskId={}, message={}", taskId, ex.getMessage());
            throw new BizException(7550, "调用对账服务失败: " + ex.getMessage());
        }
    }

    /**
     * 获取预签名或内部下载路径元数据。
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getFileUrl(String taskId) {
        ensureConfigured();
        try {
            ResponseEntity<Map> resp = restTemplate.exchange(
                    base() + "/api/v1/internal/recon/tasks/" + taskId + "/file-url",
                    HttpMethod.GET,
                    new HttpEntity<>(headers()),
                    Map.class);
            return unwrapData(resp.getBody());
        } catch (RestClientException ex) {
            log.warn("对账服务 getFileUrl 调用失败: taskId={}, message={}", taskId, ex.getMessage());
            throw new BizException(7550, "调用对账服务失败: " + ex.getMessage());
        }
    }

    /**
     * 下载原始对账文件字节。
     */
    public byte[] downloadTaskFile(String taskId) {
        ensureConfigured();
        try {
            ResponseEntity<byte[]> resp = restTemplate.exchange(
                    base() + "/api/v1/internal/recon/tasks/" + taskId + "/download",
                    HttpMethod.GET,
                    new HttpEntity<>(headers()),
                    byte[].class);
            if (!resp.getStatusCode().is2xxSuccessful()) {
                throw new BizException(7551, "对账文件下载 HTTP " + resp.getStatusCode().value());
            }
            byte[] body = resp.getBody();
            return body != null ? body : new byte[0];
        } catch (RestClientException ex) {
            log.warn("对账服务 downloadTaskFile 调用失败: taskId={}, message={}", taskId, ex.getMessage());
            throw new BizException(7550, "调用对账服务失败: " + ex.getMessage());
        }
    }

    /**
     * 手动触发对账。
     *
     * @return 新建或复用的 taskId
     */
    @SuppressWarnings("unchecked")
    public String manualRun(String reconChannel, String accountCode, LocalDate billDate) {
        ensureConfigured();
        Map<String, Object> body = new HashMap<>();
        body.put("reconChannel", reconChannel);
        body.put("accountCode", accountCode);
        body.put("billDate", billDate.toString());
        HttpHeaders h = headers();
        h.setContentType(MediaType.APPLICATION_JSON);
        try {
            ResponseEntity<Map> resp = restTemplate.exchange(
                    base() + "/api/v1/internal/recon/tasks/manual-run",
                    HttpMethod.POST,
                    new HttpEntity<>(body, h),
                    Map.class);
            Map<String, Object> data = unwrapData(resp.getBody());
            Object tid = data.get("taskId");
            return tid != null ? tid.toString() : "";
        } catch (RestClientException ex) {
            log.warn("对账服务 manualRun 调用失败: message={}", ex.getMessage());
            throw new BizException(7550, "调用对账服务失败: " + ex.getMessage());
        }
    }

    /**
     * 处理单条差异。
     */
    public void handleDiff(long diffId, String action, String remark, String operator) {
        ensureConfigured();
        Map<String, Object> body = new HashMap<>();
        body.put("action", action);
        body.put("remark", remark);
        if (StringUtils.hasText(operator)) {
            body.put("operator", operator);
        }
        HttpHeaders h = headers();
        h.setContentType(MediaType.APPLICATION_JSON);
        try {
            ResponseEntity<Map> resp = restTemplate.exchange(
                    base() + "/api/v1/internal/recon/diffs/" + diffId + "/handle",
                    HttpMethod.POST,
                    new HttpEntity<>(body, h),
                    Map.class);
            unwrapData(resp.getBody());
        } catch (RestClientException ex) {
            log.warn("对账服务 handleDiff 调用失败: diffId={}, message={}", diffId, ex.getMessage());
            throw new BizException(7550, "调用对账服务失败: " + ex.getMessage());
        }
    }

    private String base() {
        return reconClientProperties.getBaseUrl().replaceAll("/+$", "");
    }

    private HttpHeaders headers() {
        HttpHeaders h = new HttpHeaders();
        if (StringUtils.hasText(reconClientProperties.getInternalToken())) {
            h.set(HEADER_INTERNAL_TOKEN, reconClientProperties.getInternalToken());
        }
        return h;
    }

    private void ensureConfigured() {
        if (!StringUtils.hasText(reconClientProperties.getBaseUrl())) {
            throw new BizException(7552, "未配置 payflow.recon.base-url，无法访问对账服务");
        }
        if (!StringUtils.hasText(reconClientProperties.getInternalToken())) {
            throw new BizException(7553, "未配置 payflow.recon.internal-token，无法访问对账服务");
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> unwrapData(Map<String, Object> body) {
        if (body == null) {
            throw new BizException(7554, "对账服务响应体为空");
        }
        Object codeObj = body.get("code");
        int code = codeObj instanceof Number n ? n.intValue() : Integer.parseInt(String.valueOf(codeObj));
        Object msg = body.get("message");
        String message = msg != null ? msg.toString() : "";
        if (code != 0) {
            throw new BizException(code, message.isEmpty() ? "对账服务业务错误 code=" + code : message);
        }
        Object data = body.get("data");
        if (data == null) {
            return Map.of();
        }
        if (data instanceof Map<?, ?> m) {
            return (Map<String, Object>) m;
        }
        throw new BizException(7555, "对账服务 data 字段格式异常");
    }
}
