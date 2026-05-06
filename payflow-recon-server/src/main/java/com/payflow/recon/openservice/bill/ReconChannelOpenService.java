package com.payflow.recon.openservice.bill;

import com.payflow.recon.entity.ReconBillRecord;
import com.payflow.recon.model.BillDownloadResult;
import com.payflow.recon.model.PayChannelAccountView;

import java.time.LocalDate;
import java.util.List;

/**
 * 对账渠道开放服务：下载并解析三方账单。
 * <p>
 * Bean 命名约定：{channelCode}ReconChannelOpenService，例如 alipayReconChannelOpenService。
 * </p>
 *
 * @author PayFlow Team
 */
public interface ReconChannelOpenService {

    /**
     * @return 渠道编码（小写 alipay / wxpay）
     */
    String channelCode();

    /**
     * 下载账单到本地临时 CSV。
     *
     * @param billDate 账单日
     * @param billType 账单子类型（如 trade）
     * @param account  渠道账户
     * @return 本地 CSV 路径
     */
    BillDownloadResult downloadBill(LocalDate billDate, String billType, PayChannelAccountView account);

    /**
     * 解析 CSV 为标准明细（不落库）。
     */
    List<ReconBillRecord> parseBill(BillDownloadResult downloaded, String taskId);
}
