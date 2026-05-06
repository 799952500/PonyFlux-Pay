package com.payflow.cashier.util;

import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfWriter;
import com.payflow.cashier.dto.ReceiptResponse;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * 电子收据 PDF 生成器（iText5 + 内置简宋字体映射，支持中文）
 *
 * @author PayFlow Team
 */
@Component
public class ReceiptPdfGenerator {

    /**
     * 根据收据数据生成 PDF 字节流
     *
     * @param receipt 收据数据，不可为 null
     * @return PDF 二进制
     */
    public byte[] generate(ReceiptResponse receipt) {
        Objects.requireNonNull(receipt, "receipt");
        try {
            return buildPdf(receipt);
        } catch (DocumentException | IOException e) {
            throw new IllegalStateException(
                    String.format("生成收据PDF失败: orderId=%s", receipt.getOrderId()), e);
        }
    }

    private static byte[] buildPdf(ReceiptResponse r) throws DocumentException, IOException {
        BaseFont bf = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
        Font titleFont = new Font(bf, 18, Font.BOLD);
        Font labelFont = new Font(bf, 11, Font.NORMAL);
        Font smallFont = new Font(bf, 9, Font.NORMAL);

        Document document = new Document(PageSize.A4, 48, 48, 48, 48);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        document.open();

        document.add(new Paragraph("电子收据", titleFont));
        document.add(new Paragraph(" ", smallFont));

        String amountYuan = formatYuan(r.getAmount());
        addKv(document, labelFont, "收据编号", nullToDash(r.getReceiptNo()));
        addKv(document, labelFont, "生成时间", nullToDash(r.getGeneratedAt()));
        document.add(new Paragraph(" ", smallFont));
        addKv(document, labelFont, "商户名称", nullToDash(r.getMerchantName()));
        addKv(document, labelFont, "订单标题", nullToDash(r.getSubject()));
        addKv(document, labelFont, "平台订单号", nullToDash(r.getOrderId()));
        addKv(document, labelFont, "订单状态", nullToDash(r.getStatus()));
        document.add(new Paragraph(" ", smallFont));
        addKv(document, labelFont, "订单金额", amountYuan + " " + nullToDash(r.getCurrency()));
        addKv(document, labelFont, "大写金额", nullToDash(r.getAmountCn()));
        addKv(document, labelFont, "支付渠道", nullToDash(r.getPayChannel()));
        addKv(document, labelFont, "支付时间", nullToDash(r.getPayTime()));
        addKv(document, labelFont, "交易流水号", nullToDash(r.getTransactionNo()));

        document.add(new Paragraph(" ", smallFont));
        Paragraph foot = new Paragraph(
                "本收据由 PonyFlux Pay 系统自动开具，仅供参考，不作为报销凭证之法定依据。",
                smallFont);
        document.add(foot);

        document.close();
        return out.toByteArray();
    }

    private static void addKv(Document document, Font font, String label, String value)
            throws DocumentException {
        Paragraph p = new Paragraph();
        p.add(new Chunk(label + "：", font));
        p.add(new Chunk(value, font));
        document.add(p);
    }

    private static String nullToDash(String s) {
        return (s == null || s.isBlank()) ? "—" : s;
    }

    /**
     * 分 → 元字符串（两位小数）
     */
    private static String formatYuan(Long amountFen) {
        if (amountFen == null) {
            return "—";
        }
        return BigDecimal.valueOf(amountFen)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                .toPlainString();
    }
}
