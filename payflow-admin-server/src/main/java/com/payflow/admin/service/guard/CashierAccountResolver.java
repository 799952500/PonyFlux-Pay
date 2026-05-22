package com.payflow.admin.service.guard;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payflow.admin.entity.Channel;
import com.payflow.admin.entity.PaymentAccount;
import com.payflow.admin.entity.cashier.CashierChannel;
import com.payflow.admin.entity.cashier.CashierChannelAccount;
import com.payflow.admin.mapper.ChannelMapper;
import com.payflow.admin.mapper.cashier.CashierChannelAccountMapper;
import com.payflow.admin.mapper.cashier.CashierChannelMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 管理端支付账号与收银台渠道账户的解析（与路由同步逻辑一致）。
 */
@Component
@RequiredArgsConstructor
public class CashierAccountResolver {

    private static final String CASHIER_ACCOUNT_ENABLED = "ENABLED";

    private final ChannelMapper channelMapper;
    private final CashierChannelMapper cashierChannelMapper;
    private final CashierChannelAccountMapper cashierChannelAccountMapper;

    /**
     * 解析可能关联到该管理端账号的收银台账户 ID（含同编码与渠道回退账户）。
     */
    public List<Long> resolveCashierAccountIds(PaymentAccount adminAccount) {
        if (adminAccount == null || adminAccount.getChannelId() == null) {
            return List.of();
        }
        Channel adminChannel = channelMapper.selectById(adminAccount.getChannelId());
        if (adminChannel == null || adminChannel.getChannelCode() == null) {
            return List.of();
        }

        CashierChannel cashierChannel = cashierChannelMapper.selectOne(
                new LambdaQueryWrapper<CashierChannel>()
                        .apply("UPPER(channel_code) = UPPER({0})", adminChannel.getChannelCode())
                        .last("LIMIT 1"));
        if (cashierChannel == null) {
            return List.of();
        }

        Set<Long> ids = new LinkedHashSet<>();
        if (adminAccount.getAccountCode() != null && !adminAccount.getAccountCode().isBlank()) {
            CashierChannelAccount exact = cashierChannelAccountMapper.selectOne(
                    new LambdaQueryWrapper<CashierChannelAccount>()
                            .eq(CashierChannelAccount::getChannelId, cashierChannel.getId())
                            .eq(CashierChannelAccount::getAccountCode, adminAccount.getAccountCode())
                            .eq(CashierChannelAccount::getStatus, CASHIER_ACCOUNT_ENABLED)
                            .last("LIMIT 1"));
            if (exact != null && exact.getId() != null) {
                ids.add(exact.getId());
            }
        }

        List<CashierChannelAccount> enabledOnChannel = cashierChannelAccountMapper.selectList(
                new LambdaQueryWrapper<CashierChannelAccount>()
                        .eq(CashierChannelAccount::getChannelId, cashierChannel.getId())
                        .eq(CashierChannelAccount::getStatus, CASHIER_ACCOUNT_ENABLED));
        for (CashierChannelAccount account : enabledOnChannel) {
            if (account.getId() != null) {
                ids.add(account.getId());
            }
        }
        return new ArrayList<>(ids);
    }
}
