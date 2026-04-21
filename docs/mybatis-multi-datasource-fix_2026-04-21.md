# MyBatis-Plus 多数据源 Bug 修复

**时间**: 2026-04-21 09:20 GMT+8
**状态**: ✅ 已修复

---

## 一、问题描述

运营后台（payflow-admin-server）需要连接两个MySQL数据库：
- `payflow_admin`：管理员、商户、渠道
- `payflow_cashier`：订单、支付记录

**症状**: 订单查询报错 `Table 'payflow_admin.orders' doesn't exist`  
订单表 `orders` 存在于 `payflow_cashier`，但 MyBatis 错误地查询了 `payflow_admin` 数据库。

---

## 二、根本原因

`AdminDataSourceConfig.java` 中手动注册 mapper 的方法**缺少 `@Bean` 注解**：

```java
// 错误代码 - @Bean 缺失！
@Primary
public MapperFactoryBean<AdminUserMapper> adminUserMapper(...) {
    ...
}
```

没有 `@Bean`，Spring 根本不会执行这些方法，导致：
1. Admin mapper bean 未注册
2. `AdminAuthServiceImpl` 找不到 `AdminUserMapper`
3. 服务器启动失败

之前的会话尝试手动注册，但忘记加 `@Bean`，导致整个方案失败。

---

## 三、解决方案

添加 `@Bean` 注解：

```java
@Bean
@Primary
public MapperFactoryBean<AdminUserMapper> adminUserMapper(
        @Qualifier("adminSqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
    MapperFactoryBean<AdminUserMapper> factory = new MapperFactoryBean<>(AdminUserMapper.class);
    factory.setSqlSessionFactory(sqlSessionFactory);
    return factory;
}
```

---

## 四、为什么需要手动注册？

### 4.1 MyBatis-Plus 自动扫描机制

`MybatisPlusAutoConfiguration` 在 `SqlSessionFactory` 初始化时会：
1. 扫描 classpath 下所有 `BaseMapper` 接口
2. 自动注册到主 SqlSessionFactory
3. 包括 `com.payflow.admin.mapper.cashier` 包下的 OrderMapper

### 4.2 两个 @MapperScan 冲突

```java
// AdminDataSourceConfig
@MapperScan(basePackages = "com.payflow.admin.mapper", ...)  // 扫描整个 mapper 包

// CashierDataSourceConfig  
@MapperScan(basePackages = "com.payflow.admin.mapper.cashier", ...)
```

`admin.mapper` 是 `admin.mapper.cashier` 的父包，导致：
1. Admin 扫描先执行 → 注册 OrderMapper 到 adminSqlSessionFactory
2. Cashier 扫描后执行 → "Bean already defined" → 跳过 OrderMapper
3. 结果：OrderMapper 绑定到错误的数据源

### 4.3 手动注册的优势

- 明确指定每个 admin mapper 使用 `adminSqlSessionFactory`
- 不依赖包扫描的顺序
- 避免 MyBatis-Plus 自动扫描的干扰
- CashierDataSourceConfig 的 `@MapperScan` 只处理未被手动注册的 cashier mapper

---

## 五、最终架构

```
AdminApplication
├── exclude = MybatisPlusAutoConfiguration.class  ← 禁用自动配置
│
├── AdminDataSourceConfig
│   ├── @Bean adminDataSource (payflow_admin)
│   ├── @Bean adminSqlSessionFactory
│   ├── @Bean adminSqlSessionTemplate
│   └── 手动注册 5 个 admin mapper
│       ├── AdminUserMapper
│       ├── ChannelMapper
│       ├── MerchantMapper
│       ├── MerchantPaymentMethodMapper
│       └── PaymentMethodMapper
│
└── CashierDataSourceConfig
    ├── @Bean cashierDataSource (payflow_cashier)
    ├── @Bean cashierSqlSessionFactory
    └── @MapperScan("com.payflow.admin.mapper.cashier")
        ├── OrderMapper   ← 正确绑定 cashier 数据源
        └── PaymentMapper
```

---

## 六、API 测试结果

| 接口 | 数据库 | 状态 |
|------|--------|------|
| 登录 | admin | ✅ JWT 正常 |
| MERCHANTS | admin | ✅ total=156 |
| CHANNELS | admin | ✅ |
| **ORDERS** | **cashier** | ✅ **total=5** |
| **ORDERS_STATS** | **cashier** | ✅ |
| DASHBOARD | admin | ✅ |

---

## 七、关键教训

1. **`@Bean` 注解不可或缺** - 没有 `@Bean`，方法不会被 Spring 调用
2. **配置类方法需要 `@Bean`** - 即使其返回 `FactoryBean`
3. **MyBatis-Plus 会自动扫描所有 BaseMapper** - 需要显式干预
4. **手动注册比包扫描更可靠** - 在多数据源场景下避免顺序问题

---

## 八、相关文件

- `AdminApplication.java` - 排除 `MybatisPlusAutoConfiguration`
- `AdminDataSourceConfig.java` - 手动注册 5 个 admin mapper
- `CashierDataSourceConfig.java` - @MapperScan 扫描 cashier 包
- `application.yml` - 双数据源配置
