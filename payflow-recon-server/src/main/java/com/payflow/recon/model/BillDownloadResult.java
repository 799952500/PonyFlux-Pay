package com.payflow.recon.model;

import lombok.Builder;
import lombok.Value;

import java.nio.file.Path;

/**
 * 账单下载落盘结果。
 *
 * @author PayFlow Team
 */
@Value
@Builder
public class BillDownloadResult {
    /** 解压/解压后的 CSV 路径 */
    Path csvPath;
    long sizeBytes;
    String originalFileName;
}
