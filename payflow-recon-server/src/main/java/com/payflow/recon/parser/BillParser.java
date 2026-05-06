package com.payflow.recon.parser;

import com.payflow.recon.entity.ReconBillRecord;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * 渠道账单 CSV 解析策略。
 * <p>
 * Bean 命名：{channelCode}BillParser。
 * </p>
 *
 * @author PayFlow Team
 */
public interface BillParser {

    /**
     * 解析为明细实体列表（不含 id、createdAt）。
     */
    List<ReconBillRecord> parse(Path csvPath, String taskId, String channel) throws IOException;
}
