package com.payflow.payment.union;

import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 银联账单下载服务：调用文件传输接口下载 T-1 账单 ZIP 文件，解压后返回 CSV 内容。
 *
 * @author PayFlow Team
 */
@Slf4j
public class UnionPayBillService {

    private static final DateTimeFormatter SETTLE_DATE_FMT = DateTimeFormatter.ofPattern("MMdd");

    /**
     * 下载指定日期的银联商户账单，返回 CSV 内容字节数组。
     *
     * @param settleDate 账单日期（T-1）
     * @param config     银联账号配置
     * @return 解压后的 CSV 内容（UTF-8 字节）
     */
    public byte[] downloadBill(LocalDate settleDate, UnionPayAccountConfig config) {
        UnionPayHttpClient client = new UnionPayHttpClient(config);

        String settleStr = settleDate.format(SETTLE_DATE_FMT);

        Map<String, String> bizParams = new HashMap<>();
        bizParams.put("txnType", UnionPayApiConstants.TXN_TYPE_FILE);
        bizParams.put("fileType", UnionPayApiConstants.FILE_TYPE_MERCHANT_BILL);
        bizParams.put("settleDate", settleStr);

        byte[] raw = client.fileDownload(bizParams);

        // 银联返回的可能是 ZIP 文件，尝试解压
        return maybeUnzip(raw);
    }

    /**
     * 如果内容是 ZIP，解压并拼接所有 entry 的文本内容。
     */
    private byte[] maybeUnzip(byte[] data) {
        if (data == null || data.length == 0) {
            return new byte[0];
        }
        // ZIP 文件魔数：0x50 0x4B
        if (data.length < 4 || data[0] != 0x50 || data[1] != 0x4B) {
            return data; // 不是 ZIP，直接返回
        }
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(data))) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                log.info("银联账单 ZIP 解压: entry={}, size={}", entry.getName(), entry.getSize());
                int len;
                while ((len = zis.read(buf)) > 0) {
                    bos.write(buf, 0, len);
                }
                bos.write('\n'); // entry 间分隔
                zis.closeEntry();
            }
            return bos.toByteArray();
        } catch (Exception e) {
            log.warn("银联账单 ZIP 解压失败，返回原始数据: {}", e.getMessage());
            return data;
        }
    }
}
