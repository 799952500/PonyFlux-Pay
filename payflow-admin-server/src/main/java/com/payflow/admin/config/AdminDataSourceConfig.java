package com.payflow.admin.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.payflow.admin.mapper.AdminAuditLogMapper;
import com.payflow.admin.mapper.AdminUserMapper;
import com.payflow.admin.mapper.ChannelMapper;
import com.payflow.admin.mapper.ChannelRouteMapper;
import com.payflow.admin.mapper.ChurnAlertMapper;
import com.payflow.admin.mapper.DashboardMetricsMapper;
import com.payflow.admin.mapper.FeeRateAuditLogMapper;
import com.payflow.admin.mapper.FeeRateConfigMapper;
import com.payflow.admin.mapper.MerchantFeeSnapshotMapper;
import com.payflow.admin.mapper.RoutingDecisionLogMapper;
import com.payflow.admin.mapper.MerchantApplicationEntityMapper;
import com.payflow.admin.mapper.MerchantMapper;
import com.payflow.admin.mapper.MerchantPaymentMethodMapper;
import com.payflow.admin.mapper.MerchantPaymentRouteMapper;
import com.payflow.admin.mapper.PaymentAccountMapper;
import com.payflow.admin.mapper.PaymentMethodMapper;
import com.payflow.admin.mapper.RiskHitRecordMapper;
import com.payflow.admin.mapper.RiskRuleAuditLogMapper;
import com.payflow.admin.mapper.RiskRuleMapper;
import com.payflow.admin.mapper.RiskRuleMerchantScopeMapper;
import com.payflow.admin.mapper.SysRoleMapper;
import com.payflow.admin.mapper.SysMenuMapper;
import com.payflow.admin.mapper.SysRoleMenuMapper;
import com.payflow.admin.mapper.SysUserMapper;
import com.payflow.admin.mapper.SysUserRoleMapper;
import com.payflow.admin.mapper.SystemConfigMapper;
import com.payflow.admin.mapper.recon.ReconDiffEntityMapper;
import com.payflow.admin.mapper.recon.ReconHandlerAuditEntityMapper;
import com.payflow.admin.mapper.recon.ReconMerchantTaskEntityMapper;
import com.payflow.admin.mapper.recon.ReconTaskEntityMapper;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.mapper.MapperFactoryBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.sql.DataSource;

/**
 * 主数据源配置 - payflow_admin
 * 包含：管理员、商户、渠道、支付方式等
 *
 * 关键设计：手动定义每个 MapperFactoryBean，阻止 MyBatis-Plus 自动扫描。
 * MyBatis-Plus 的 AutoConfiguration 会在初始化时扫描所有 BaseMapper 实现并自动注册。
 * 若不加干预，它会找到并注册 cashier 包下的 Mapper（绑定到 admin SqlSessionFactory），
 * 导致 CashierDataSourceConfig 的 @MapperScan 跳过这些 Mapper（Bean already defined），
 * 最终 cashier mapper 被绑定到错误的数据源（payflow_admin 而非 payflow_cashier）。
 *
 * 解决方案：移除 @MapperScan，在配置类中手动用 MapperFactoryBean 定义每个 admin mapper。
 * 被手动定义的 mapper 不会被 MyBatis-Plus 的自动扫描重复注册，
 * cashier 包下的 mapper 则由 CashierDataSourceConfig 的 @MapperScan 正确处理。
  * @author Lucas
 */
@Configuration
public class AdminDataSourceConfig {

    // ==================== 数据源 ====================
    @Primary
    @Bean(name = "adminDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.admin")
    public DataSource adminDataSource() {
        return DataSourceBuilder.create().build();
    }

    // ==================== SqlSessionFactory ====================
    @Primary
    @Bean(name = "adminSqlSessionFactory")
    public SqlSessionFactory adminSqlSessionFactory(
            @Qualifier("adminDataSource") DataSource dataSource) throws Exception {
        MybatisSqlSessionFactoryBean bean = new MybatisSqlSessionFactoryBean();
        bean.setDataSource(dataSource);

        // MyBatis-Plus 分页插件
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        bean.setPlugins(interceptor);

        return bean.getObject();
    }

    @Primary
    @Bean(name = "adminSqlSessionTemplate")
    public SqlSessionTemplate adminSqlSessionTemplate(
            @Qualifier("adminSqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }

    @Primary
    @Bean(name = "adminTransactionManager")
    public DataSourceTransactionManager adminTransactionManager(
            @Qualifier("adminDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    // ==================== 手动注册 admin mapper（关键！）====================
    // 手动定义可阻止 MyBatis-Plus 自动扫描这些接口，避免重复注册到错误数据源。
    // 必须加 @Bean 注解才能让 Spring 注册为 bean！

    @Bean
    @Primary
    public MapperFactoryBean<AdminUserMapper> adminUserMapper(
            @Qualifier("adminSqlSessionTemplate") SqlSessionTemplate sqlSessionTemplate) {
        MapperFactoryBean<AdminUserMapper> factory = new MapperFactoryBean<>(AdminUserMapper.class);
        factory.setSqlSessionTemplate(sqlSessionTemplate);
        return factory;
    }

    @Bean
    @Primary
    public MapperFactoryBean<AdminAuditLogMapper> adminAuditLogMapper(
            @Qualifier("adminSqlSessionTemplate") SqlSessionTemplate sqlSessionTemplate) {
        MapperFactoryBean<AdminAuditLogMapper> factory = new MapperFactoryBean<>(AdminAuditLogMapper.class);
        factory.setSqlSessionTemplate(sqlSessionTemplate);
        return factory;
    }

    @Bean
    @Primary
    public MapperFactoryBean<ChannelMapper> channelMapper(
            @Qualifier("adminSqlSessionTemplate") SqlSessionTemplate sqlSessionTemplate) {
        MapperFactoryBean<ChannelMapper> factory = new MapperFactoryBean<>(ChannelMapper.class);
        factory.setSqlSessionTemplate(sqlSessionTemplate);
        return factory;
    }

    @Bean
    @Primary
    public MapperFactoryBean<MerchantMapper> merchantMapper(
            @Qualifier("adminSqlSessionTemplate") SqlSessionTemplate sqlSessionTemplate) {
        MapperFactoryBean<MerchantMapper> factory = new MapperFactoryBean<>(MerchantMapper.class);
        factory.setSqlSessionTemplate(sqlSessionTemplate);
        return factory;
    }

    @Bean
    @Primary
    public MapperFactoryBean<MerchantPaymentMethodMapper> merchantPaymentMethodMapper(
            @Qualifier("adminSqlSessionTemplate") SqlSessionTemplate sqlSessionTemplate) {
        MapperFactoryBean<MerchantPaymentMethodMapper> factory = new MapperFactoryBean<>(MerchantPaymentMethodMapper.class);
        factory.setSqlSessionTemplate(sqlSessionTemplate);
        return factory;
    }

    @Bean
    @Primary
    public MapperFactoryBean<PaymentMethodMapper> paymentMethodMapper(
            @Qualifier("adminSqlSessionTemplate") SqlSessionTemplate sqlSessionTemplate) {
        MapperFactoryBean<PaymentMethodMapper> factory = new MapperFactoryBean<>(PaymentMethodMapper.class);
        factory.setSqlSessionTemplate(sqlSessionTemplate);
        return factory;
    }

    @Bean
    @Primary
    public MapperFactoryBean<PaymentAccountMapper> paymentAccountMapper(
            @Qualifier("adminSqlSessionTemplate") SqlSessionTemplate sqlSessionTemplate) {
        MapperFactoryBean<PaymentAccountMapper> factory = new MapperFactoryBean<>(PaymentAccountMapper.class);
        factory.setSqlSessionTemplate(sqlSessionTemplate);
        return factory;
    }

    @Bean
    @Primary
    public MapperFactoryBean<RiskRuleMapper> riskRuleMapper(
            @Qualifier("adminSqlSessionTemplate") SqlSessionTemplate sqlSessionTemplate) {
        MapperFactoryBean<RiskRuleMapper> factory = new MapperFactoryBean<>(RiskRuleMapper.class);
        factory.setSqlSessionTemplate(sqlSessionTemplate);
        return factory;
    }

    @Bean
    @Primary
    public MapperFactoryBean<RiskRuleMerchantScopeMapper> riskRuleMerchantScopeMapper(
            @Qualifier("adminSqlSessionTemplate") SqlSessionTemplate sqlSessionTemplate) {
        MapperFactoryBean<RiskRuleMerchantScopeMapper> factory =
                new MapperFactoryBean<>(RiskRuleMerchantScopeMapper.class);
        factory.setSqlSessionTemplate(sqlSessionTemplate);
        return factory;
    }

    @Bean
    @Primary
    public MapperFactoryBean<RiskRuleAuditLogMapper> riskRuleAuditLogMapper(
            @Qualifier("adminSqlSessionTemplate") SqlSessionTemplate sqlSessionTemplate) {
        MapperFactoryBean<RiskRuleAuditLogMapper> factory = new MapperFactoryBean<>(RiskRuleAuditLogMapper.class);
        factory.setSqlSessionTemplate(sqlSessionTemplate);
        return factory;
    }

    @Bean
    @Primary
    public MapperFactoryBean<RiskHitRecordMapper> riskHitRecordMapper(
            @Qualifier("adminSqlSessionTemplate") SqlSessionTemplate sqlSessionTemplate) {
        MapperFactoryBean<RiskHitRecordMapper> factory = new MapperFactoryBean<>(RiskHitRecordMapper.class);
        factory.setSqlSessionTemplate(sqlSessionTemplate);
        return factory;
    }

    @Bean
    @Primary
    public MapperFactoryBean<MerchantPaymentRouteMapper> merchantPaymentRouteMapper(
            @Qualifier("adminSqlSessionTemplate") SqlSessionTemplate sqlSessionTemplate) {
        MapperFactoryBean<MerchantPaymentRouteMapper> factory = new MapperFactoryBean<>(MerchantPaymentRouteMapper.class);
        factory.setSqlSessionTemplate(sqlSessionTemplate);
        return factory;
    }

    @Bean
    @Primary
    public MapperFactoryBean<ChannelRouteMapper> channelRouteMapper(
            @Qualifier("adminSqlSessionTemplate") SqlSessionTemplate sqlSessionTemplate) {
        MapperFactoryBean<ChannelRouteMapper> factory = new MapperFactoryBean<>(ChannelRouteMapper.class);
        factory.setSqlSessionTemplate(sqlSessionTemplate);
        return factory;
    }

    @Bean
    @Primary
    public MapperFactoryBean<SysRoleMapper> sysRoleMapper(
            @Qualifier("adminSqlSessionTemplate") SqlSessionTemplate sqlSessionTemplate) {
        MapperFactoryBean<SysRoleMapper> factory = new MapperFactoryBean<>(SysRoleMapper.class);
        factory.setSqlSessionTemplate(sqlSessionTemplate);
        return factory;
    }

    @Bean
    @Primary
    public MapperFactoryBean<SysMenuMapper> sysMenuMapper(
            @Qualifier("adminSqlSessionTemplate") SqlSessionTemplate sqlSessionTemplate) {
        MapperFactoryBean<SysMenuMapper> factory = new MapperFactoryBean<>(SysMenuMapper.class);
        factory.setSqlSessionTemplate(sqlSessionTemplate);
        return factory;
    }

    @Bean
    @Primary
    public MapperFactoryBean<SysRoleMenuMapper> sysRoleMenuMapper(
            @Qualifier("adminSqlSessionTemplate") SqlSessionTemplate sqlSessionTemplate) {
        MapperFactoryBean<SysRoleMenuMapper> factory = new MapperFactoryBean<>(SysRoleMenuMapper.class);
        factory.setSqlSessionTemplate(sqlSessionTemplate);
        return factory;
    }

    @Bean
    @Primary
    public MapperFactoryBean<SysUserRoleMapper> sysUserRoleMapper(
            @Qualifier("adminSqlSessionTemplate") SqlSessionTemplate sqlSessionTemplate) {
        MapperFactoryBean<SysUserRoleMapper> factory = new MapperFactoryBean<>(SysUserRoleMapper.class);
        factory.setSqlSessionTemplate(sqlSessionTemplate);
        return factory;
    }

    @Bean
    @Primary
    public MapperFactoryBean<SystemConfigMapper> systemConfigMapper(
            @Qualifier("adminSqlSessionTemplate") SqlSessionTemplate sqlSessionTemplate) {
        MapperFactoryBean<SystemConfigMapper> factory = new MapperFactoryBean<>(SystemConfigMapper.class);
        factory.setSqlSessionTemplate(sqlSessionTemplate);
        return factory;
    }

    @Bean
    @Primary
    public MapperFactoryBean<SysUserMapper> sysUserMapper(
            @Qualifier("adminSqlSessionTemplate") SqlSessionTemplate sqlSessionTemplate) {
        MapperFactoryBean<SysUserMapper> factory = new MapperFactoryBean<>(SysUserMapper.class);
        factory.setSqlSessionTemplate(sqlSessionTemplate);
        return factory;
    }

    // ==================== 对账 recon_* mapper（同库直连）====================

    @Bean
    @Primary
    public MapperFactoryBean<ReconTaskEntityMapper> reconTaskEntityMapper(
            @Qualifier("adminSqlSessionTemplate") SqlSessionTemplate sqlSessionTemplate) {
        MapperFactoryBean<ReconTaskEntityMapper> factory = new MapperFactoryBean<>(ReconTaskEntityMapper.class);
        factory.setSqlSessionTemplate(sqlSessionTemplate);
        return factory;
    }

    @Bean
    @Primary
    public MapperFactoryBean<ReconDiffEntityMapper> reconDiffEntityMapper(
            @Qualifier("adminSqlSessionTemplate") SqlSessionTemplate sqlSessionTemplate) {
        MapperFactoryBean<ReconDiffEntityMapper> factory = new MapperFactoryBean<>(ReconDiffEntityMapper.class);
        factory.setSqlSessionTemplate(sqlSessionTemplate);
        return factory;
    }

    @Bean
    @Primary
    public MapperFactoryBean<ReconHandlerAuditEntityMapper> reconHandlerAuditEntityMapper(
            @Qualifier("adminSqlSessionTemplate") SqlSessionTemplate sqlSessionTemplate) {
        MapperFactoryBean<ReconHandlerAuditEntityMapper> factory = new MapperFactoryBean<>(ReconHandlerAuditEntityMapper.class);
        factory.setSqlSessionTemplate(sqlSessionTemplate);
        return factory;
    }

    @Bean
    @Primary
    public MapperFactoryBean<ReconMerchantTaskEntityMapper> reconMerchantTaskEntityMapper(
            @Qualifier("adminSqlSessionTemplate") SqlSessionTemplate sqlSessionTemplate) {
        MapperFactoryBean<ReconMerchantTaskEntityMapper> factory = new MapperFactoryBean<>(ReconMerchantTaskEntityMapper.class);
        factory.setSqlSessionTemplate(sqlSessionTemplate);
        return factory;
    }

    @Bean
    @Primary
    public MapperFactoryBean<MerchantApplicationEntityMapper> merchantApplicationEntityMapper(
            @Qualifier("adminSqlSessionTemplate") SqlSessionTemplate sqlSessionTemplate) {
        MapperFactoryBean<MerchantApplicationEntityMapper> factory = new MapperFactoryBean<>(MerchantApplicationEntityMapper.class);
        factory.setSqlSessionTemplate(sqlSessionTemplate);
        return factory;
    }

    @Bean
    @Primary
    public MapperFactoryBean<DashboardMetricsMapper> dashboardMetricsMapper(
            @Qualifier("adminSqlSessionTemplate") SqlSessionTemplate sqlSessionTemplate) {
        MapperFactoryBean<DashboardMetricsMapper> factory = new MapperFactoryBean<>(DashboardMetricsMapper.class);
        factory.setSqlSessionTemplate(sqlSessionTemplate);
        return factory;
    }

    @Bean
    @Primary
    public MapperFactoryBean<FeeRateConfigMapper> feeRateConfigMapper(
            @Qualifier("adminSqlSessionTemplate") SqlSessionTemplate sqlSessionTemplate) {
        MapperFactoryBean<FeeRateConfigMapper> factory = new MapperFactoryBean<>(FeeRateConfigMapper.class);
        factory.setSqlSessionTemplate(sqlSessionTemplate);
        return factory;
    }

    @Bean
    @Primary
    public MapperFactoryBean<MerchantFeeSnapshotMapper> merchantFeeSnapshotMapper(
            @Qualifier("adminSqlSessionTemplate") SqlSessionTemplate sqlSessionTemplate) {
        MapperFactoryBean<MerchantFeeSnapshotMapper> factory = new MapperFactoryBean<>(MerchantFeeSnapshotMapper.class);
        factory.setSqlSessionTemplate(sqlSessionTemplate);
        return factory;
    }

    @Bean
    @Primary
    public MapperFactoryBean<FeeRateAuditLogMapper> feeRateAuditLogMapper(
            @Qualifier("adminSqlSessionTemplate") SqlSessionTemplate sqlSessionTemplate) {
        MapperFactoryBean<FeeRateAuditLogMapper> factory = new MapperFactoryBean<>(FeeRateAuditLogMapper.class);
        factory.setSqlSessionTemplate(sqlSessionTemplate);
        return factory;
    }

    @Bean
    @Primary
    public MapperFactoryBean<RoutingDecisionLogMapper> routingDecisionLogMapper(
            @Qualifier("adminSqlSessionTemplate") SqlSessionTemplate sqlSessionTemplate) {
        MapperFactoryBean<RoutingDecisionLogMapper> factory = new MapperFactoryBean<>(RoutingDecisionLogMapper.class);
        factory.setSqlSessionTemplate(sqlSessionTemplate);
        return factory;
    }

    @Bean
    @Primary
    public MapperFactoryBean<ChurnAlertMapper> churnAlertMapper(
            @Qualifier("adminSqlSessionTemplate") SqlSessionTemplate sqlSessionTemplate) {
        MapperFactoryBean<ChurnAlertMapper> factory = new MapperFactoryBean<>(ChurnAlertMapper.class);
        factory.setSqlSessionTemplate(sqlSessionTemplate);
        return factory;
    }
}