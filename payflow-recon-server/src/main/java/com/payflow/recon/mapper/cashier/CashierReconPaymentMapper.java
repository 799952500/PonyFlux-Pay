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
            SELECT payment_id AS paymentId, order_id AS orderId, pay_channel AS payChannel,
                   channel_transaction_id AS channelTransactionId, amount, status,
                   created_at AS createdAt, updated_at AS updatedAt
            FROM cashier_payments
            WHERE pay_channel = #{payChannel}
              AND status = 'SUCCESS'
              AND DATE(COALESCE(updated_at, created_at)) = #{billDate}
            """)
    List<CashierReconPaymentRow> listSuccessByBillDate(@Param("payChannel") String payChannel,
                                                        @Param("billDate") LocalDate billDate);
}
