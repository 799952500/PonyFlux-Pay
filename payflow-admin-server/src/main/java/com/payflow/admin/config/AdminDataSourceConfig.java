package com.payflow.admin.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.payflow.admin.mapper.AdminUserMapper;
import com.payflow.admin.mapper.ChannelMapper;
import com.payflow.admin.mapper.ChannelRouteMapper;
import com.payflow.admin.mapper.MerchantMapper;
import com.payflow.admin.mapper.MerchantPaymentMethodMapper;
import com.payflow.admin.mapper.MerchantPaymentRouteMapper;
import com.payflow.admin.mapper.PaymentAccountMapper;
import com.payflow.admin.mapper.PaymentMethodMapper;
import com.payflow.admin.mapper.RiskRuleMapper;
import com.payflow.admin.mapper.SysRoleMapper;
import com.payflow.admin.mapper.SysMenuMapper;
import com.payflow.admin.mapper.SysRoleMenuMapper;
import com.payflow.admin.mapper.SysUserMapper;
import com.payflow.admin.mapper.SysUserRoleMapper;
import com.payflow.admin.mapper.SystemConfigMapper;
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
            @Qualifier("adminSqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        MapperFactoryBean<AdminUserMapper> factory = new MapperFactoryBean<>(AdminUserMapper.class);
        factory.setSqlSessionFactory(sqlSessionFactory);
        return factory;
    }

    @Bean
    @Primary
    public MapperFactoryBean<ChannelMapper> channelMapper(
            @Qualifier("adminSqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        MapperFactoryBean<ChannelMapper> factory = new MapperFactoryBean<>(ChannelMapper.class);
        factory.setSqlSessionFactory(sqlSessionFactory);
        return factory;
    }

    @Bean
    @Primary
    public MapperFactoryBean<MerchantMapper> merchantMapper(
            @Qualifier("adminSqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        MapperFactoryBean<MerchantMapper> factory = new MapperFactoryBean<>(MerchantMapper.class);
        factory.setSqlSessionFactory(sqlSessionFactory);
        return factory;
    }

    @Bean
    @Primary
    public MapperFactoryBean<MerchantPaymentMethodMapper> merchantPaymentMethodMapper(
            @Qualifier("adminSqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        MapperFactoryBean<MerchantPaymentMethodMapper> factory = new MapperFactoryBean<>(MerchantPaymentMethodMapper.class);
        factory.setSqlSessionFactory(sqlSessionFactory);
        return factory;
    }

    @Bean
    @Primary
    public MapperFactoryBean<PaymentMethodMapper> paymentMethodMapper(
            @Qualifier("adminSqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        MapperFactoryBean<PaymentMethodMapper> factory = new MapperFactoryBean<>(PaymentMethodMapper.class);
        factory.setSqlSessionFactory(sqlSessionFactory);
        return factory;
    }

    @Bean
    @Primary
    public MapperFactoryBean<PaymentAccountMapper> paymentAccountMapper(
            @Qualifier("adminSqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        MapperFactoryBean<PaymentAccountMapper> factory = new MapperFactoryBean<>(PaymentAccountMapper.class);
        factory.setSqlSessionFactory(sqlSessionFactory);
        return factory;
    }

    @Bean
    @Primary
    public MapperFactoryBean<RiskRuleMapper> riskRuleMapper(
            @Qualifier("adminSqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        MapperFactoryBean<RiskRuleMapper> factory = new MapperFactoryBean<>(RiskRuleMapper.class);
        factory.setSqlSessionFactory(sqlSessionFactory);
        return factory;
    }

    @Bean
    @Primary
    public MapperFactoryBean<MerchantPaymentRouteMapper> merchantPaymentRouteMapper(
            @Qualifier("adminSqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        MapperFactoryBean<MerchantPaymentRouteMapper> factory = new MapperFactoryBean<>(MerchantPaymentRouteMapper.class);
        factory.setSqlSessionFactory(sqlSessionFactory);
        return factory;
    }

    @Bean
    @Primary
    public MapperFactoryBean<ChannelRouteMapper> channelRouteMapper(
            @Qualifier("adminSqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        MapperFactoryBean<ChannelRouteMapper> factory = new MapperFactoryBean<>(ChannelRouteMapper.class);
        factory.setSqlSessionFactory(sqlSessionFactory);
        return factory;
    }

    @Bean
    @Primary
    public MapperFactoryBean<SysRoleMapper> sysRoleMapper(
            @Qualifier("adminSqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        MapperFactoryBean<SysRoleMapper> factory = new MapperFactoryBean<>(SysRoleMapper.class);
        factory.setSqlSessionFactory(sqlSessionFactory);
        return factory;
    }

    @Bean
    @Primary
    public MapperFactoryBean<SysMenuMapper> sysMenuMapper(
            @Qualifier("adminSqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        MapperFactoryBean<SysMenuMapper> factory = new MapperFactoryBean<>(SysMenuMapper.class);
        factory.setSqlSessionFactory(sqlSessionFactory);
        return factory;
    }

    @Bean
    @Primary
    public MapperFactoryBean<SysRoleMenuMapper> sysRoleMenuMapper(
            @Qualifier("adminSqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        MapperFactoryBean<SysRoleMenuMapper> factory = new MapperFactoryBean<>(SysRoleMenuMapper.class);
        factory.setSqlSessionFactory(sqlSessionFactory);
        return factory;
    }

    @Bean
    @Primary
    public MapperFactoryBean<SysUserRoleMapper> sysUserRoleMapper(
            @Qualifier("adminSqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        MapperFactoryBean<SysUserRoleMapper> factory = new MapperFactoryBean<>(SysUserRoleMapper.class);
        factory.setSqlSessionFactory(sqlSessionFactory);
        return factory;
    }

    @Bean
    @Primary
    public MapperFactoryBean<SystemConfigMapper> systemConfigMapper(
            @Qualifier("adminSqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        MapperFactoryBean<SystemConfigMapper> factory = new MapperFactoryBean<>(SystemConfigMapper.class);
        factory.setSqlSessionFactory(sqlSessionFactory);
        return factory;
    }

    @Bean
    @Primary
    public MapperFactoryBean<SysUserMapper> sysUserMapper(
            @Qualifier("adminSqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        MapperFactoryBean<SysUserMapper> factory = new MapperFactoryBean<>(SysUserMapper.class);
        factory.setSqlSessionFactory(sqlSessionFactory);
        return factory;
    }
}