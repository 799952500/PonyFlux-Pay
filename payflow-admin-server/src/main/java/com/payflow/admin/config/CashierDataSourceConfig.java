package com.payflow.admin.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.sql.DataSource;

/**
 * 第二数据源配置 - payflow_cashier
 * 包含：订单、支付记录等
 *
 * 精确扫描 cashier 子包，确保 OrderMapper/PaymentMapper 使用 cashier 数据源。
 */
@Configuration
@MapperScan(
        basePackages = "com.payflow.admin.mapper.cashier",
        sqlSessionFactoryRef = "cashierSqlSessionFactory",
        sqlSessionTemplateRef = "cashierSqlSessionTemplate"
)
public class CashierDataSourceConfig {

    @Bean(name = "cashierDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.cashier")
    public DataSource cashierDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "cashierSqlSessionFactory")
    public SqlSessionFactory cashierSqlSessionFactory(
            @Qualifier("cashierDataSource") DataSource dataSource) throws Exception {
        MybatisSqlSessionFactoryBean bean = new MybatisSqlSessionFactoryBean();
        bean.setDataSource(dataSource);

        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        bean.setPlugins(interceptor);

        return bean.getObject();
    }

    @Bean(name = "cashierSqlSessionTemplate")
    public SqlSessionTemplate cashierSqlSessionTemplate(
            @Qualifier("cashierSqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }

    @Bean(name = "cashierTransactionManager")
    public DataSourceTransactionManager cashierTransactionManager(
            @Qualifier("cashierDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
