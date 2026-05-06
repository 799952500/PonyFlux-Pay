package com.payflow.cashier.controller;

import com.payflow.cashier.dto.ReceiptResponse;
import com.payflow.cashier.exception.R;
import com.payflow.cashier.service.ReceiptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 电子收据控制器
 *
 * <p>提供收据数据查询和 PDF 下载，属于公开接口（与收银台一致，无需认证）。</p>
 *
 * @author PayFlow Team
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/cashier")
@RequiredArgsConstructor
@Tag(name = "电子收据", description = "查询收据数据与下载PDF收据")
public class ReceiptController {

    private final ReceiptService receiptService;

    /**
     * GET /api/v1/cashier/{orderId}/receipt — 获取电子收据数据
     *
     * @param orderId 平台订单号
     * @return 收据数据
     */
    @GetMapping("/{orderId}/receipt")
    @Operation(summary = "获取电子收据", description = "查询已支付订单的电子收据数据")
    public R<ReceiptResponse> getReceipt(
            @Parameter(description = "平台订单号") @PathVariable String orderId) {
        log.info("获取电子收据: orderId={}", orderId);
        ReceiptResponse receipt = receiptService.getReceipt(orderId);
        return R.ok(receipt);
    }

    /**
     * GET /api/v1/cashier/{orderId}/receipt/pdf — 下载PDF收据
     *
     * @param orderId 平台订单号
     * @return PDF文件字节流
     */
    @GetMapping("/{orderId}/receipt/pdf")
    @Operation(summary = "下载PDF收据", description = "下载已支付订单的PDF格式电子收据")
    public ResponseEntity<byte[]> downloadReceiptPdf(
            @Parameter(description = "平台订单号") @PathVariable String orderId) {
        log.info("下载PDF收据: orderId={}", orderId);
        byte[] pdfBytes = receiptService.generateReceiptPdf(orderId);

        String fileName = URLEncoder.encode("收据_" + orderId + ".pdf", StandardCharsets.UTF_8)
                .replace("+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + fileName)
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdfBytes.length)
                .body(pdfBytes);
    }
}
