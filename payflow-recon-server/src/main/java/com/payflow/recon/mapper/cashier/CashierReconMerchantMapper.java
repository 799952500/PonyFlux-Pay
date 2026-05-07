package com.payflow.recon.mapper.cashier;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 收银库商户维度查询（对账主调度、商户对账单）。
 *
 * @author PayFlow Team
 */
@Mapper
public interface CashierReconMerchantMapper {

    /**
     * 账单日存在成功支付的商户编号列表。
     */
    @Select("""
            SELECT DISTINCT o.merchant_id
            FROM cashier_payments p
            INNER JOIN cashier_orders o ON o.order_id = p.order_id
            WHERE p.status = 'SUCCESS'
              AND DATE(COALESCE(p.updated_at, p.created_at)) = #{billDate}
            ORDER BY o.merchant_id
            """)
    List<String> listMerchantIdsWithSuccessPaymentsOnDate(@Param("billDate") LocalDate billDate);

    /**
     * 某商户在账单日的成功支付明细（生成对账单）。
     */
    @Select("""
            SELECT p.payment_id AS paymentId, p.order_id AS orderId, p.pay_channel AS payChannel,
                   p.channel_transaction_id AS channelTransactionId, p.amount, p.status,
                   COALESCE(p.updated_at, p.created_at) AS paidAt
            FROM cashier_payments p
            INNER JOIN cashier_orders o ON o.order_id = p.order_id
            WHERE o.merchant_id = #{merchantId}
              AND p.status = 'SUCCESS'
              AND DATE(COALESCE(p.updated_at, p.created_at)) = #{billDate}
            ORDER BY p.payment_id
            """)
    List<CashierMerchantStatementPaymentRow> listSuccessPaymentsForMerchantOnDate(
            @Param("merchantId") String merchantId,
            @Param("billDate") LocalDate billDate);
}
