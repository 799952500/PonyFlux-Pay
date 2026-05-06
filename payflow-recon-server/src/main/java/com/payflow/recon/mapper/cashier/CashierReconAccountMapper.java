package com.payflow.recon.mapper.cashier;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 读取收银库渠道账户（JOIN 渠道表）。
 *
 * @author PayFlow Team
 */
@Mapper
public interface CashierReconAccountMapper {

    /**
     * 列出可对账的启用账户（支付宝、微信）。
     */
    @Select("""
            SELECT a.id, a.account_code AS accountCode, a.channel_config AS channelConfig, c.channel_code AS channelCode
            FROM cashier_channel_accounts a
            INNER JOIN cashier_channels c ON a.channel_id = c.id
            WHERE a.status = 'ENABLED' AND c.channel_code IN ('alipay', 'wechat_pay')
            """)
    List<CashierReconAccountRow> listEnabledForRecon();

    @Select("""
            SELECT a.id, a.account_code AS accountCode, a.channel_config AS channelConfig, c.channel_code AS channelCode
            FROM cashier_channel_accounts a
            INNER JOIN cashier_channels c ON a.channel_id = c.id
            WHERE a.account_code = #{accountCode} AND a.status = 'ENABLED'
            LIMIT 1
            """)
    CashierReconAccountRow findByAccountCode(String accountCode);
}
