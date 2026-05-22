package com.payflow.recon.mapper.cashier;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 收银库支付记录查询（对账比对）。
 *
 * @author PayFlow Team
 */
@Mapper
public interface CashierReconPaymentMapper {

    @Select("""
            SELECT p.payment_id AS paymentId, p.order_id AS orderId, p.pay_channel AS payChannel,
                   p.channel_transaction_id AS channelTransactionId, p.amount, p.status,
                   p.created_at AS createdAt, p.updated_at AS updatedAt,
                   o.merchant_id AS merchantId
            FROM cashier_payments p
            INNER JOIN cashier_orders o ON o.order_id = p.order_id
            WHERE p.pay_channel = #{payChannel}
              AND p.status = 'SUCCESS'
              AND DATE(COALESCE(p.updated_at, p.created_at)) = #{billDate}
            """)
    List<CashierReconPaymentRow> listSuccessByBillDate(@Param("payChannel") String payChannel,
                                                        @Param("billDate") LocalDate billDate);
}
