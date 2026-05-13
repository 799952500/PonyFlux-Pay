# Research: 银联支付渠道完整接入

**Feature**: 002-unionpay-channel
**Date**: 2026-05-12

## 1. 银联 SDK 选型

**Decision**: 自实现 HTTP 签名客户端，不引入官方 SDK 依赖

**Rationale**:
- 项目已有自实现 HTTP 签名客户端的先例：`WxPayV3HttpClient` (wechat) 和 `AliPayClientCache` (alipay)
- 银联官方 ACP SDK (`com.unionpay.acp:sdk`) 基于旧版 Java EE，与 Spring Boot 3.2.5 (Jakarta EE) 存在兼容性问题
- 银联云闪付新版 OpenAPI 仍处于演进阶段，API 变化频繁，自实现更灵活
- 参照现有 `Hutool` + `HttpURLConnection` 或 Spring `RestTemplate` 模式即可实现

**Alternatives considered**:
- `com.unionpay.acp:sdk` (全渠道 SDK): 依赖过重，不兼容 Jakarta EE
- 纯 RESTful SDK (网上非官方): 维护风险高，安全审计困难

## 2. 银联 API 网关版本

**Decision**: 采用银联全渠道 v5.x 接口规范

**Rationale**:
- 银联全渠道 (UnionPay Gateway) 是最广泛使用的银联线上支付产品
- H5 支付接口（`frontUrl` + `backUrl`）和 QR 扫码接口成熟稳定
- 现有 `payflow-payment-union` 占位模块已引用 `open.unionpay.com`，方向一致

**Key API endpoints** (沙箱环境):
| 接口 | 用途 | 路径 |
|------|------|------|
| H5 支付下单 | 获取重定向 URL | `/gateway/api/frontTransReq.do` |
| QR 扫码下单 | 获取二维码 | `/gateway/api/backTransReq.do` (QRCode 产品) |
| 退款 | 原路退回 | `/gateway/api/backTransReq.do` (refund) |
| 退款查询 | 查询退款结果 | `/gateway/api/queryTrans.do` |
| 订单查询 | 查询支付结果 | `/gateway/api/queryTrans.do` |
| 账单下载 | T-1 日账单 | `/gateway/api/fileTransReq.do` |

## 3. 签名算法方案

**Decision**: RSA-SHA256 签名 + SHA256 验签

**Rationale**:
- 银联全渠道使用商户私钥对请求签名，使用银联公钥验证通知签名
- 私钥/证书使用 .pfx 格式，通过 Java `KeyStore` 加载（`PKCS12`）
- 签名流程：按字段名 ASCII 排序 → 拼接 key=value&... → SHA256 → RSA 签名 → Base64
- 此签名算法属于行业标准，与已有 AesEncryptor 模式互补

**Alternatives considered**:
- 仅 SHA256 摘要（无 RSA）：银联要求 RSA-SHA256，不可选
- SM2 国密：仅境内强要求场景，非银联标准模式

## 4. Maven 模块迁移方案

**Decision**: `git mv` 保留历史 + worktree 隔离验证

**Rationale**:
- `git mv payflow-payment-wechat payflow-payment-channels/payflow-payment-wechat` 保留完整 git 历史
- 在 worktree 中预先验证整个迁移过程，确保编译+测试通过后再合并
- 根 POM `<modules>` 声明顺序: channels 父模块替换 3 个叶子模块
- `dependencyManagement` 中的 artifactId 无需变更（模块 artifactId 不变）

**Migration steps**:
1. 创建 `payflow-payment-channels/pom.xml`（packaging=pom，聚合 3 个子模块）
2. `git mv` 3 个支付模块到 channels 下
3. 更新根 POM `<modules>`: 移除 3 行 → 添加 1 行 `<module>payflow-payment-channels</module>`
4. 更新每个子模块 POM 的 `<parent><relativePath>`: `../pom.xml` → `../../pom.xml`
5. 更新 `payflow-cashier-server/pom.xml` 依赖路径（如有 `systemPath` 引用）
6. Maven 编译验证: `mvn -B -DskipTests compile`

## 5. 对账账单格式

**Decision**: 银联标准 CSV 格式，按现有 `BillParser` 接口适配

**Rationale**:
- 银联账单为 ZIP 压缩的 CSV 文件，与支付宝账单格式类似
- 账单包含字段: 交易时间、交易流水号、订单号、交易金额、手续费、交易类型、清算日期
- 可复用 `AlipayBillParser` 的 CSV 解析模式
- `UnionpayBillParser` 实现 `BillParser` 接口，注册为 Spring Bean
- Bean 命名约定: `unionpayBillParser`

## 6. 证书管理方案

**Decision**: .pfx 证书文件系统存储，密码通过 AES-256-GCM 加密存入数据库

**Rationale**:
- 与现有 WeChat/Alipay 密钥管理一致
- 银联商户证书为 .pfx (PKCS12) 格式，通过 `KeyStore.getInstance("PKCS12")` 加载
- 证书文件路径存储在 `channel_config` JSON 中 (`signCertPath`, `encryptCertPath`)
- 证书密码通过 `AesEncryptor` 加密存储在 `channel_config` JSON 中 (`signCertPassword`, `encryptCertPassword`)

## 7. 异步通知方案

**Decision**: HTTP POST 接收银联通知，body 中解析 `respCode` + 验签 `signature`

**Rationale**:
- 银联异步通知与微信/支付宝回调模式一致：HTTP POST + 签名验证
- 复用现有 `POST /notify/{channelCode}` 路由，channelCode = "unionpay"
- 通知 body 为 `application/x-www-form-urlencoded`，包含 `signature` 字段
- 验签流程：解析 body → 提取并移除 signature → 重新排序拼接 → RSA 验签（银联公钥）
- 支付成功判断: `respCode == "00"` 且验签通过
