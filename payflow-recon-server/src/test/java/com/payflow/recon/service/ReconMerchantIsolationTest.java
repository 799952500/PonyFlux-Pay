package com.payflow.recon.service;

import com.payflow.recon.mapper.cashier.CashierReconPaymentMapper;
import com.payflow.recon.mapper.cashier.CashierReconPaymentRow;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("对账商户归属一致性测试")
class ReconMerchantIsolationTest {

    @Test
    @DisplayName("支付比对行模型包含 merchantId 字段")
    void paymentRowSupportsMerchantId() throws NoSuchFieldException {
        assertNotNull(CashierReconPaymentRow.class.getDeclaredField("merchantId"));
    }

    @Test
    @DisplayName("支付查询 SQL 通过订单关联商户号")
    void paymentMapperJoinsOrderForMerchantId() throws NoSuchMethodException {
        String sql = CashierReconPaymentMapper.class
                .getDeclaredMethod("listSuccessByBillDate", String.class, java.time.LocalDate.class)
                .getAnnotation(org.apache.ibatis.annotations.Select.class)
                .value();
        assertTrue(sql.contains("merchant_id"));
        assertTrue(sql.contains("cashier_orders"));
    }
}
