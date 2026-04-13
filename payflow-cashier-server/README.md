# PayFlow 收银台后端服务

收银台后端 API 服务，基于 Java 17 + Spring Boot 3.x + MyBatis-Plus + H2。

## 技术栈

- Java 17
- Spring Boot 3.2.5
- MyBatis-Plus 3.5.6
- H2 Database
- JWT (jjwt 0.12.5)
- Swagger/OpenAPI 2.5.0

## 快速开始

### 编译

```bash
mvn compile
```

### 运行

```bash
mvn spring-boot:run
```

服务启动于 `http://localhost:3002`

### H2 Console

访问 `http://localhost:3002/h2-console`

- JDBC URL: `jdbc:h2:./data/cashier`
- 用户名: `sa`
- 密码: (空)

## API 列表

### 认证

| 方法 | 路径 | 描述 | 需要认证 |
|------|------|------|---------|
| POST | `/api/v1/auth/login` | 商户登录 | 否 |

### 订单

| 方法 | 路径 | 描述 | 需要认证 |
|------|------|------|---------|
| POST | `/api/v1/orders` | 创建订单 | 是 |
| GET | `/api/v1/orders/{orderId}` | 查询订单 | 是 |

### 收银台

| 方法 | 路径 | 描述 | 需要认证 |
|------|------|------|---------|
| GET | `/api/v1/cashier/{orderId}` | 收银台信息 | 否 |

### 支付

| 方法 | 路径 | 描述 | 需要认证 |
|------|------|------|---------|
| POST | `/api/v1/payments` | 发起支付 | 是 |
| GET | `/api/v1/payments/status/{paymentId}` | 查询支付状态 | 是 |

## 测试账号

| 商户号 | 密码 | 商户名称 |
|--------|------|---------|
| M2024040001 | 123456 | XX科技旗舰店 |

## 登录示例

```bash
curl -X POST http://localhost:3002/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"merchantId":"M2024040001","password":"123456"}'
```

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "merchantId": "M2024040001",
    "merchantName": "XX科技旗舰店",
    "expireTime": "2026-04-13T15:00:00+08:00"
  }
}
```

## 使用 Token 访问受保护接口

```bash
curl http://localhost:3002/api/v1/orders/PO123456 \
  -H "Authorization: Bearer <token>"
```

## Swagger UI

访问 `http://localhost:3002/swagger-ui.html`
