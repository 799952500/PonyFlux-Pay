# 支付渠道管理后端实现 — 任务完成报告

## 任务
支付渠道管理模块后端实现（com.payflow.cashier 包）

## 编译结果
✅ `mvn compile` — BUILD SUCCESS

## 新建文件清单（共12个）

| 文件 | 类型 |
|------|------|
| `entity/PayChannel.java` | Entity — 支付渠道 |
| `entity/PayChannelAccount.java` | Entity — 渠道账户 |
| `entity/PayChannelMerchantRoute.java` | Entity — 商户路由 |
| `mapper/PayChannelMapper.java` | Mapper — 渠道 |
| `mapper/PayChannelAccountMapper.java` | Mapper — 账户 |
| `mapper/PayChannelMerchantRouteMapper.java` | Mapper — 路由 |
| `dto/ChannelAccountDTO.java` | DTO — 账户（含脱敏） |
| `dto/ChannelRouteDTO.java` | DTO — 路由 |
| `service/PayChannelService.java` | Service 接口 |
| `service/impl/PayChannelServiceImpl.java` | Service 实现（含 routeToAccount） |
| `controller/AdminChannelController.java` | Admin REST API（/api/v1/admin/channels） |

## 修改文件清单（共4个）

| 文件 | 修改内容 |
|------|---------|
| `resources/schema.sql` | 追加 pay_channels / pay_channel_accounts / pay_channel_merchant_routes 建表语句 |
| `resources/data.sql` | 追加渠道、账户、路由测试数据 |
| `config/WebMvcConfig.java` | `/api/v1/admin/**` 加入 JWT 拦截器排除路径 |
| `service/impl/OrderCacheNoOpServiceImpl.java` | 修复方法签名（void 返回类型，与接口一致） |
| `service/impl/OrderCacheServiceImpl.java` | 同上修复 |
| `service/impl/OrderMqNoOpProducerImpl.java` | 补充 sendMerchantNotifyRetry 方法 |
| `service/impl/OrderMqProducerImpl.java` | 补充 sendMerchantNotifyRetry 实现 |

## API 路由一览

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/v1/admin/channels | 渠道列表 |
| POST | /api/v1/admin/channels | 创建渠道 |
| PUT | /api/v1/admin/channels/{id} | 更新渠道 |
| PUT | /api/v1/admin/channels/{id}/toggle | 启用/禁用渠道 |
| GET | /api/v1/admin/channels/{channelId}/accounts | 渠道账户列表 |
| POST | /api/v1/admin/channels/accounts | 创建账户 |
| PUT | /api/v1/admin/channels/accounts/{id} | 更新账户 |
| PUT | /api/v1/admin/channels/accounts/{id}/toggle | 启用/禁用账户 |
| DELETE | /api/v1/admin/channels/accounts/{id} | 删除账户（软删除） |
| GET | /api/v1/admin/channels/routes?merchantId= | 路由列表 |
| POST | /api/v1/admin/channels/routes | 创建路由 |
| DELETE | /api/v1/admin/channels/routes/{routeId} | 删除路由 |
| PUT | /api/v1/admin/channels/routes/{routeId}/toggle | 启用/禁用路由 |

## 关键实现细节

- **routeToAccount()**：根据 merchantId + channelCode 查路由表 → 关联账户 → 返回账户配置JSON；无路由返回 null
- **删除操作**：软删除，改 status 为 DISABLED
- **敏感字段脱敏**：channelConfig JSON 中 appSecret/apiKey 显示为 `****`
- **预编译bug修复**：OrderCacheServiceImpl/OrderCacheNoOpServiceImpl 的 cacheOrder/updateCache 方法签名与接口不一致（返回 Order vs void）；OrderMqNoOpProducerImpl 缺少 sendMerchantNotifyRetry 方法；OrderMqProducerImpl 缺少 sendMerchantNotifyRetry 实现
