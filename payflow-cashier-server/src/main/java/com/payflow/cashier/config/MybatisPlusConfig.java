package com.payflow.cashier.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.payflow.cashier.constant.MerchantSecurityErrorCodes;
import com.payflow.cashier.context.MerchantContext;
import com.payflow.cashier.context.MerchantScopeHolder;
import com.payflow.common.exception.BizException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.StringValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

/**
 * MyBatis-Plus 拦截器：商户数据隔离（merchant_id 行级过滤）。
 *
 * @author PayFlow Team
 */
@Configuration
public class MybatisPlusConfig {

    private static final Set<String> MERCHANT_SCOPED_TABLES = Set.of(
            "cashier_orders",
            "cashier_payment_link"
    );

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new MerchantTenantLineHandler()));
        return interceptor;
    }

    /**
     * 基于 TenantLine 的 merchant_id 注入（仅作用于含 merchant_id 列的表）。
     */
    private static final class MerchantTenantLineHandler implements com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler {

        @Override
        public Expression getTenantId() {
            String merchantId = MerchantContext.getMerchantId();
            if (merchantId == null || merchantId.isBlank()) {
                throw new BizException(MerchantSecurityErrorCodes.MERCHANT_ID_MISMATCH,
                        "缺少商户上下文，拒绝数据访问");
            }
            return new StringValue(merchantId);
        }

        @Override
        public String getTenantIdColumn() {
            return "merchant_id";
        }

        @Override
        public boolean ignoreTable(String tableName) {
            if (MerchantScopeHolder.isSystemMode()) {
                return true;
            }
            if (tableName == null) {
                return true;
            }
            String normalized = tableName.toLowerCase();
            return !MERCHANT_SCOPED_TABLES.contains(normalized);
        }
    }
}
