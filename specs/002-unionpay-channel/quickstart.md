# Quickstart: 银联支付渠道

**Feature**: 002-unionpay-channel
**Date**: 2026-05-12

## 前置条件

1. 银联开放平台注册账号，获取沙箱商户号 (merId)
2. 从银联商户平台下载签名证书 (.pfx) 和银联公钥证书 (.cer)
3. 本地开发环境已安装 Java 17 + Maven + MySQL 8.x

## 本地开发快速开始

### 1. 模块重构 (一次性操作)

```bash
# 创建 channels 父 POM 目录
mkdir -p payflow-payment-channels

# 迁移 3 个模块 (保留 git 历史)
git mv payflow-payment-wechat payflow-payment-channels/
git mv payflow-payment-alipay payflow-payment-channels/
git mv payflow-payment-union payflow-payment-channels/

# 编译验证
mvn -B -DskipTests compile
```

### 2. 配置银联沙箱参数

在管理后台 (http://localhost:3003) 的渠道管理中创建银联支付账号，填写:

- 商户号: 沙箱测试商户号
- 签名证书路径: 本地证书文件路径
- 签名证书密码: 证书密码（系统自动加密存储）
- 网关地址: `https://gateway.test.95516.com/gateway/api`

### 3. 启动必要服务

```bash
# 编译全部模块
mvn -B -DskipTests compile

# 启动 cashier-server (端口 3002)
mvn -B -pl payflow-cashier-server spring-boot:run

# 启动 admin-server (端口 3003)
mvn -B -pl payflow-admin-server spring-boot:run

# 启动 admin-client (端口 3001)
cd payflow-admin-client && npm run dev

# 启动 cashier-client (端口 5173)
cd payflow-cashier-client && npm run dev
```

### 4. 验证支付流程

```bash
# 1. 创建测试订单
curl -X POST http://localhost:3002/api/v1/merchant/payment/create \
  -H "Content-Type: application/json" \
  -H "X-Merchant-Id: MERCHANT_001" \
  -H "X-Signature: <HMAC-SHA256>" \
  -H "X-Timestamp: <timestamp>" \
  -H "X-Nonce: <random>" \
  -d '{
    "orderId": "TEST-UNIONPAY-001",
    "amount": 100,
    "subject": "测试商品",
    "payChannel": "UNION_PAY",
    "payMethod": "UNION_H5",
    "returnUrl": "http://localhost:5173/result"
  }'

# 2. 检查返回的 H5 URL（应跳转到银联支付页面）
# 3. 在银联沙箱页面完成模拟支付
# 4. 验证订单状态更新为 PAID
```

### 5. 银联沙箱测试

- 银联沙箱地址: https://open.unionpay.com/ (需注册获取测试账号)
- 沙箱测试卡号: 见银联开放平台测试文档
- 沙箱仅在工作日 9:00-18:00 开放（以银联官方公告为准）

## Mock 模式 (推荐开发阶段使用)

银联沙箱可用时间有限，开发阶段推荐使用 Mock 模式:

1. 在 `UnionPayH5Handler` 和 `UnionPayQrHandler` 中保留 Mock 分支
2. 通过配置开关 `payflow.unionpay.mock=true` 启用 Mock
3. Mock 模式返回模拟的支付结果，不实际调用银联网关
