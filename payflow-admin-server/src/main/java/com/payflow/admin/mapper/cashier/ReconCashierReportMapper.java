package com.payflow.admin.mapper.cashier;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 收银库对账报表查询（成功支付按账单日）。
 *
 * @author PayFlow Team
 */
@Mapper
public interface ReconCashierReportMapper {

    @Select("""
            SELECT COUNT(1)
            FROM cashier_payments p
            INNER JOIN cashier_orders o ON o.order_id = p.order_id
            WHERE p.status = 'SUCCESS'
              AND DATE(COALESCE(p.updated_at, p.created_at)) = #{billDate}
              AND (#{payChannel} IS NULL OR #{payChannel} = '' OR p.pay_channel = #{payChannel})
              AND (#{merchantId} IS NULL OR #{merchantId} = '' OR o.merchant_id = #{merchantId})
              AND (#{orderKeyword} IS NULL OR #{orderKeyword} = '' OR p.order_id LIKE CONCAT('%', #{orderKeyword}, '%'))
            """)
    long countSuccessPaymentsOnBillDate(
            @Param("billDate") LocalDate billDate,
            @Param("payChannel") String payChannel,
            @Param("merchantId") String merchantId,
            @Param("orderKeyword") String orderKeyword);

    @Select("""
            SELECT p.payment_id AS paymentId, p.order_id AS orderId, o.merchant_id AS merchantId,
                   p.pay_channel AS payChannel, p.account_code AS accountCode,
                   p.channel_transaction_id AS channelTransactionId, p.amount AS amount
            FROM cashier_payments p
            INNER JOIN cashier_orders o ON o.order_id = p.order_id
            WHERE p.status = 'SUCCESS'
              AND DATE(COALESCE(p.updated_at, p.created_at)) = #{billDate}
              AND (#{payChannel} IS NULL OR #{payChannel} = '' OR p.pay_channel = #{payChannel})
              AND (#{merchantId} IS NULL OR #{merchantId} = '' OR o.merchant_id = #{merchantId})
              AND (#{orderKeyword} IS NULL OR #{orderKeyword} = '' OR p.order_id LIKE CONCAT('%', #{orderKeyword}, '%'))
            ORDER BY p.payment_id
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<ReconCashierPaymentRow> listSuccessPaymentsOnBillDate(
            @Param("billDate") LocalDate billDate,
            @Param("payChannel") String payChannel,
            @Param("merchantId") String merchantId,
            @Param("orderKeyword") String orderKeyword,
            @Param("offset") long offset,
            @Param("limit") long limit);

    /**
     * 本地成功收款按支付账号汇总（含历史数据 account_code 为空时归入单列）。
     */
    @Select("""
            <script>
            SELECT COALESCE(NULLIF(TRIM(p.account_code), ''), '__NO_ACCOUNT__') AS accountCode,
                   MAX(p.pay_channel) AS payChannel,
                   COUNT(1) AS cnt,
                   COALESCE(SUM(p.amount), 0) AS sumAmount
            FROM cashier_payments p
            WHERE p.status = 'SUCCESS'
              AND DATE(COALESCE(p.updated_at, p.created_at)) = #{billDate}
              AND p.pay_channel IN ('ALIPAY', 'WECHAT_PAY')
            <if test="accountCode != null and accountCode != ''">
              AND COALESCE(NULLIF(TRIM(p.account_code), ''), '__NO_ACCOUNT__') = #{accountCode}
            </if>
            GROUP BY COALESCE(NULLIF(TRIM(p.account_code), ''), '__NO_ACCOUNT__')
            </script>
            """)
    List<ReconLocalAccountAggRow> aggregateLocalSuccessByAccount(
            @Param("billDate") LocalDate billDate,
            @Param("accountCode") String accountCode);

    @Select("""
            <script>
            SELECT o.order_id AS orderId, o.merchant_id AS merchantId
            FROM cashier_orders o
            WHERE o.order_id IN
            <foreach collection="orderIds" item="id" open="(" separator="," close=")">#{id}</foreach>
            </script>
            """)
    List<OrderMerchantRow> lookupMerchantsByOrderIds(@Param("orderIds") List<String> orderIds);
}
