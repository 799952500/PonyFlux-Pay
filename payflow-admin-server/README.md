# PayFlow Admin Server

支付网关管理后台后端服务，基于 Java 17 + Spring Boot 3.x + MyBatis-Plus + H2。

## 技术栈

| 技术 | 版本 |
|------|------|
| Java | 17 |
| Spring Boot | 3.2.5 |
| MyBatis-Plus | 3.5.7 |
| H2 Database | - |
| JWT (jjwt) | 0.12.5 |
| Lombok | 1.18.30 |

## 快速启动

```bash
# 编译
mvn compile

# 运行
mvn spring-boot:run

# 测试编译
mvn test
```

服务启动后访问 http://localhost:8080

## 账号

| 用户名 | 密码 | 角色 |
|--------|------|------|
| admin | admin123 | SUPER_ADMIN |
| finance | finance123 | FINANCE |
| risk | risk123 | RISK |

## API 认证

登录接口（无需认证）：
```
POST /api/v1/admin/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123"
}
```

响应：
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "token": "eyJhbGci...",
    "username": "admin",
    "role": "SUPER_ADMIN",
    "expireTime": "2026-04-13T15:00:00+08:00"
  }
}
```

后续请求在 Header 中携带 Token：
```
Authorization: Bearer <token>
```

## 接口列表

所有接口均需认证（登录接口除外）。

### Dashboard
- `GET /api/v1/admin/dashboard/stats` - 今日统计
- `GET /api/v1/admin/dashboard/trend` - 趋势数据
- `GET /api/v1/admin/dashboard/channel-dist` - 渠道分布

### 订单
- `GET /api/v1/admin/orders` - 订单列表
- `GET /api/v1/admin/orders/{orderId}` - 订单详情
- `POST /api/v1/admin/orders/{orderId}/close` - 关闭订单

### 退款
- `GET /api/v1/admin/refunds` - 退款列表

### 渠道
- `GET /api/v1/admin/channels` - 渠道列表
- `PUT /api/v1/admin/channels/{channel}/toggle` - 切换渠道状态

### 风控
- `GET /api/v1/admin/risk/rules` - 风控规则列表
- `PUT /api/v1/admin/risk/rules/{ruleId}` - 更新风控规则

### 商户
- `GET /api/v1/admin/merchants` - 商户列表
- `GET /api/v1/admin/merchants/{merchantId}` - 商户详情

## H2 控制台

开发环境可访问 H2 控制台：
- URL: http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:mem:payflow_admin`
- 用户名: `sa`
- 密码: (空)
