package com.payflow.recon.openservice.bill.impl;

import com.payflow.common.exception.BizException;
import com.payflow.payment.wechat.WxPayBillService;
import com.payflow.recon.entity.ReconBillRecord;
import com.payflow.recon.model.BillDownloadResult;
import com.payflow.recon.model.PayChannelAccountView;
import com.payflow.recon.openservice.bill.ReconChannelOpenService;
import com.payflow.recon.parser.BillParserStrategyLocator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * 微信支付对账账单下载与解析。
 *
 * @author PayFlow Team
 */
@Slf4j
@Service("wxpayReconChannelOpenService")
@RequiredArgsConstructor
public class WxpayReconChannelOpenService implements ReconChannelOpenService {

    private final WxPayBillService wxPayBillService;
    private final BillParserStrategyLocator billParserStrategyLocator;

    @Override
    public String channelCode() {
        return "wxpay";
    }

    @Override
    public BillDownloadResult downloadBill(LocalDate billDate, String billType, PayChannelAccountView account) {
        try {
            WxPayBillService.WxBillDownloadMeta meta = wxPayBillService.queryTradeBill(account, billDate);
            byte[] body = wxPayBillService.downloadBillBytes(meta.getDownloadUrl());
            Path csvPath = Files.createTempFile("wx-bill-", ".csv");
            byte[] data = maybeGunzip(body);
            Files.write(csvPath, data);
            return BillDownloadResult.builder()
                    .csvPath(csvPath)
                    .sizeBytes(data.length)
                    .originalFileName("wxpay_" + billDate + ".csv")
                    .build();
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("微信下载账单异常: billDate={}", billDate, e);
            throw new BizException(7520, "微信下载账单异常", e);
        }
    }

    @Override
    public List<ReconBillRecord> parseBill(BillDownloadResult downloaded, String taskId) {
        try {
            return billParserStrategyLocator.requireByChannelCode("wxpay")
                    .parse(downloaded.getCsvPath(), taskId, "wxpay");
        } catch (Exception e) {
            log.error("微信解析账单失败: taskId={}", taskId, e);
            throw new BizException(7521, "微信解析账单失败", e);
        }
    }

    private static byte[] maybeGunzip(byte[] body) throws Exception {
        if (body.length >= 2 && (body[0] & 0xFF) == 0x1f && (body[1] & 0xFF) == 0x8b) {
            try (GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(body))) {
                return gis.readAllBytes();
            }
        }
        return body;
    }
}
