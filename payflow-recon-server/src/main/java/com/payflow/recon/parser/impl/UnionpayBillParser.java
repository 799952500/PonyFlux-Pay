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
 * 银联对账 CSV 解析。
 * <p>
 * 银联账单 CSV 典型列名：
 * <ul>
 *     <li>交易流水号 / queryId — 银联交易流水号</li>
 *     <li>商户订单号 / orderId — 平台订单号</li>
 *     <li>交易金额 / txnAmt — 交易金额（分）</li>
 *     <li>手续费 / fee — 手续费（分）</li>
 *     <li>交易类型 / txnType — 交易类型</li>
 *     <li>交易时间 / txnTime — 交易时间</li>
 *     <li>清算日期 / settleDate — 清算日期</li>
 * </ul>
 * </p>
 *
 * @author PayFlow Team
 */
@Slf4j
@Component("unionpayBillParser")
public class UnionpayBillParser implements BillParser {

    @Override
    public List<ReconBillRecord> parse(Path csvPath, String taskId, String channel) throws IOException {
        List<String> normalizedLines = new ArrayList<>();
        List<String> raw = Files.readAllLines(csvPath, StandardCharsets.UTF_8);
        for (String line : raw) {
            String t = line.trim();
            if (t.isEmpty()) {
                continue;
            }
            if (t.startsWith("\uFEFF")) {
                t = t.substring(1);
            }
            if (t.startsWith("#")) {
                continue; // 跳过注释行
            }
            normalizedLines.add(t);
        }
        if (normalizedLines.size() < 2) {
            return List.of();
        }
        String joined = String.join("\n", normalizedLines);
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .build();
        List<ReconBillRecord> out = new ArrayList<>();
        try (CSVParser parser = CSVParser.parse(new StringReader(joined), format)) {
            for (CSVRecord record : parser) {
                String rowLine = record.toString();
                try {
                    String tradeNo = firstNonBlank(record, "交易流水号", "queryId", "流水号");
                    String outNo = firstNonBlank(record, "商户订单号", "orderId", "订单号");
                    String amountStr = firstNonBlank(record, "交易金额", "txnAmt", "金额");
                    String feeStr = firstNonBlank(record, "手续费", "fee");
                    String status = firstNonBlank(record, "交易类型", "txnType");
                    if (tradeNo == null && outNo == null) {
                        continue;
                    }
                    // 银联账单金额可能为分或元，尝试智能判断
                    Long fen = parseAmount(amountStr);
                    Long feeFen = parseAmount(feeStr);
                    out.add(ReconBillRecord.builder()
                            .taskId(taskId)
                            .channel(channel)
                            .channelTradeNo(tradeNo)
                            .outTradeNo(outNo)
                            .amountFen(fen)
                            .refundFen(feeFen)
                            .channelStatus(status)
                            .finishTime(null)
                            .rawLine(rowLine)
                            .parseError(false)
                            .build());
                } catch (Exception ex) {
                    log.warn("银联账单行解析失败: {}, error={}", rowLine, ex.getMessage());
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
            }
        }
        return null;
    }

    /**
     * 解析金额字符串为分。自动判断：如果数值小于 10^6 量级，按元→分；否则按分直接返回。
     */
    private static Long parseAmount(String val) {
        if (val == null || val.isBlank()) {
            return null;
        }
        try {
            BigDecimal bd = new BigDecimal(val.trim());
            // 银联账单部分字段可能是元，部分是分；如果大于 10^6 很可能已经是分
            if (bd.abs().compareTo(new BigDecimal("1000000")) < 0) {
                return bd.multiply(BigDecimal.valueOf(100))
                        .setScale(0, RoundingMode.HALF_UP).longValue();
            }
            return bd.longValue();
        } catch (Exception e) {
            return null;
        }
    }
}
