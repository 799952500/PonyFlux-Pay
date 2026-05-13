package com.payflow.recon.openservice.bill.impl;

import com.payflow.common.exception.BizException;
import com.payflow.payment.union.UnionPayAccountConfig;
import com.payflow.payment.union.UnionPayBillService;
import com.payflow.payment.union.UnionPayConfigLoader;
import com.payflow.recon.entity.ReconBillRecord;
import com.payflow.recon.model.BillDownloadResult;
import com.payflow.recon.model.PayChannelAccountView;
import com.payflow.recon.openservice.bill.ReconChannelOpenService;
import com.payflow.recon.parser.BillParserStrategyLocator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 银联对账账单下载与解析。
 *
 * @author PayFlow Team
 */
@Slf4j
@Service("unionpayReconChannelOpenService")
@RequiredArgsConstructor
public class UnionpayReconChannelOpenService implements ReconChannelOpenService {

    private static final DateTimeFormatter BILL_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final BillParserStrategyLocator billParserStrategyLocator;

    @Override
    public String channelCode() {
        return "unionpay";
    }

    @Override
    public BillDownloadResult downloadBill(LocalDate billDate, String billType, PayChannelAccountView account) {
        String dateStr = billDate.format(BILL_DATE_FMT);
        try {
            UnionPayAccountConfig config = UnionPayConfigLoader.load(account);
            UnionPayBillService billService = new UnionPayBillService();
            byte[] csvBytes = billService.downloadBill(billDate, config);

            if (csvBytes == null || csvBytes.length == 0) {
                throw new BizException(7541, "银联账单下载为空: billDate=" + dateStr);
            }

            Path csvPath = Files.createTempFile("unionpay-bill-", ".csv");
            Files.write(csvPath, csvBytes);

            return BillDownloadResult.builder()
                    .csvPath(csvPath)
                    .sizeBytes(csvBytes.length)
                    .originalFileName("unionpay_" + dateStr + ".csv")
                    .build();
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("银联下载账单异常: billDate={}", dateStr, e);
            throw new BizException(7542, "银联下载账单异常", e);
        }
    }

    @Override
    public List<ReconBillRecord> parseBill(BillDownloadResult downloaded, String taskId) {
        try {
            return billParserStrategyLocator.requireByChannelCode("unionpay")
                    .parse(downloaded.getCsvPath(), taskId, "unionpay");
        } catch (Exception e) {
            log.error("银联解析账单失败: taskId={}", taskId, e);
            throw new BizException(7543, "银联解析账单失败", e);
        }
    }
}
