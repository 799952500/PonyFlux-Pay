package com.payflow.recon.openservice.bill.impl;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.alipay.easysdk.factory.Factory;
import com.alipay.easysdk.util.generic.models.AlipayOpenApiGenericResponse;
import com.payflow.common.exception.BizException;
import com.payflow.payment.alipay.AliPayClientCache;
import com.payflow.recon.entity.ReconBillRecord;
import com.payflow.recon.model.BillDownloadResult;
import com.payflow.recon.model.PayChannelAccountView;
import com.payflow.recon.openservice.bill.ReconChannelOpenService;
import com.payflow.recon.parser.BillParserStrategyLocator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 支付宝对账账单下载与解析。
 *
 * @author PayFlow Team
 */
@Slf4j
@Service("alipayReconChannelOpenService")
@RequiredArgsConstructor
public class AlipayReconChannelOpenService implements ReconChannelOpenService {

    private static final DateTimeFormatter BILL_DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final BillParserStrategyLocator billParserStrategyLocator;

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .callTimeout(Duration.ofMinutes(2))
            .build();

    @Override
    public String channelCode() {
        return "alipay";
    }

    @Override
    public BillDownloadResult downloadBill(LocalDate billDate, String billType, PayChannelAccountView account) {
        String type = (billType == null || billType.isBlank()) ? "trade" : billType;
        String dateStr = billDate.format(BILL_DATE_FMT);
        try {
            AliPayClientCache.configure(account);
            Map<String, String> textParams = Collections.emptyMap();
            Map<String, String> bizParams = new HashMap<>();
            bizParams.put("bill_type", type);
            bizParams.put("bill_date", dateStr);
            AlipayOpenApiGenericResponse apiResp = Factory.Util.Generic()
                    .execute("alipay.data.dataservice.bill.downloadurl.query", textParams, bizParams);
            String raw = apiResp.getHttpBody();
            log.debug("支付宝账单下载地址响应长度: {}", raw != null ? raw.length() : 0);
            JSONObject root = JSONUtil.parseObj(raw);
            JSONObject resp = root.getJSONObject("alipay_data_dataservice_bill_downloadurl_query_response");
            if (resp == null) {
                throw new BizException(7510, "支付宝账单响应格式异常: " + raw);
            }
            if (!"10000".equals(resp.getStr("code"))) {
                throw new BizException(7510, "支付宝账单接口失败: " + resp.getStr("sub_msg", resp.getStr("msg", raw)));
            }
            String url = resp.getStr("bill_download_url");
            if (url == null || url.isBlank()) {
                throw new BizException(7510, "支付宝账单缺少 bill_download_url");
            }
            Path zipPath = Files.createTempFile("alipay-bill-", ".zip");
            try (Response response = httpClient.newCall(new Request.Builder().url(url).get().build()).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    throw new BizException(7511, "支付宝账单文件下载失败 HTTP " + response.code());
                }
                Files.write(zipPath, response.body().bytes());
            }
            long zipSize = Files.size(zipPath);
            Path csvPath = unzipFirstCsv(zipPath);
            Files.deleteIfExists(zipPath);
            return BillDownloadResult.builder()
                    .csvPath(csvPath)
                    .sizeBytes(Files.size(csvPath))
                    .originalFileName("alipay_" + dateStr + ".csv")
                    .build();
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("支付宝下载账单异常: billDate={}", dateStr, e);
            throw new BizException(7512, "支付宝下载账单异常: " + e.getMessage());
        }
    }

    @Override
    public List<ReconBillRecord> parseBill(BillDownloadResult downloaded, String taskId) {
        try {
            return billParserStrategyLocator.requireByChannelCode("alipay")
                    .parse(downloaded.getCsvPath(), taskId, "alipay");
        } catch (Exception e) {
            log.error("支付宝解析账单失败: taskId={}", taskId, e);
            throw new BizException(7513, "支付宝解析账单失败: " + e.getMessage());
        }
    }

    private static Path unzipFirstCsv(Path zipPath) throws Exception {
        try (InputStream fis = Files.newInputStream(zipPath);
             ZipInputStream zis = new ZipInputStream(fis)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                String name = entry.getName();
                if (name.toLowerCase().endsWith(".csv") || name.toLowerCase().endsWith(".txt")) {
                    Path out = Files.createTempFile("alipay-bill-", ".csv");
                    Files.copy(zis, out, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    return out;
                }
            }
        }
        throw new BizException(7511, "支付宝账单压缩包中未找到 CSV");
    }
}
