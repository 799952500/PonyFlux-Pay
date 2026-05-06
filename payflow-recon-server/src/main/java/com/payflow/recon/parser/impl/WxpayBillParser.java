package com.payflow.recon.parser.impl;

import com.payflow.recon.entity.ReconBillRecord;
import com.payflow.recon.parser.BillParser;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 微信支付对账 CSV 解析。
 *
 * @author PayFlow Team
 */
@Slf4j
@Component("wxpayBillParser")
public class WxpayBillParser implements BillParser {

    @Override
    public List<ReconBillRecord> parse(Path csvPath, String taskId, String channel) throws IOException {
        String text = Files.readString(csvPath, StandardCharsets.UTF_8);
        if (text.startsWith("\uFEFF")) {
            text = text.substring(1);
        }
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .build();
        List<ReconBillRecord> out = new ArrayList<>();
        try (CSVParser parser = CSVParser.parse(new StringReader(text), format)) {
            for (CSVRecord record : parser) {
                String rowLine = record.toString();
                try {
                    String tradeNo = firstNonBlank(record, "微信订单号", "微信支付订单号");
                    String outNo = firstNonBlank(record, "商户订单号");
                    String amountYuan = firstNonBlank(record, "应结订单金额(元)", "订单金额(元)", "总金额(元)");
                    String status = firstNonBlank(record, "交易状态", "支付状态");
                    if (tradeNo == null && outNo == null) {
                        continue;
                    }
                    Long fen = yuanToFen(amountYuan);
                    out.add(ReconBillRecord.builder()
                            .taskId(taskId)
                            .channel(channel)
                            .channelTradeNo(tradeNo)
                            .outTradeNo(outNo)
                            .amountFen(fen)
                            .refundFen(null)
                            .channelStatus(status)
                            .finishTime(null)
                            .rawLine(rowLine)
                            .parseError(false)
                            .build());
                } catch (Exception ex) {
                    log.warn("微信账单行解析失败: {}, error={}", rowLine, ex.getMessage());
                    out.add(ReconBillRecord.builder()
                            .taskId(taskId)
                            .channel(channel)
                            .rawLine(rowLine)
                            .parseError(true)
                            .build());
                }
            }
        }
        return out;
    }

    private static String firstNonBlank(CSVRecord record, String... names) {
        for (String n : names) {
            try {
                String v = record.get(n);
                if (v != null && !v.isBlank()) {
                    return v.trim();
                }
            } catch (IllegalArgumentException ignored) {
                // 列名不存在
            }
        }
        return null;
    }

    private static Long yuanToFen(String yuan) {
        if (yuan == null || yuan.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(yuan.trim()).multiply(BigDecimal.valueOf(100))
                    .setScale(0, RoundingMode.HALF_UP).longValue();
        } catch (Exception e) {
            return null;
        }
    }
}
