# 支付后台菜单改造

**时间**: 2026-04-22 21:15 GMT+8
**状态**: ✅ 完成

---

## 一、改造需求

1. **渠道管理** → 提升为一级菜单
2. **商户管理** → 列表增加"支付方式配置"按钮，关联商户和支付方式

---

## 二、改造内容

### 2.1 前端改造

#### 菜单结构（layout.vue）

**改造前：**
```
- 数据概览
- 订单管理（子菜单）
- 支付管理（子菜单）
  - 渠道管理
  - 支付方式
  - 商户支付方式
- 商户管理
- 风控管理
- 系统设置
```

**改造后：**
```
- 数据概览
- 订单管理（子菜单）
- 渠道管理（一级菜单）✓
- 支付方式（一级菜单）✓
- 商户管理（一级菜单，操作列增加"支付方式"按钮）✓
- 风控管理
- 系统设置
```

#### 商户管理页面（merchants.vue）

- 操作列增加"支付方式"按钮
- 新增支付方式配置对话框
- 支持勾选/取消勾选商户可用的支付方式
- 保存配置调用后端API

#### API接口（api/admin.ts）

新增接口：
```typescript
// 获取支付方式列表
getPaymentMethods(params: { page, pageSize })

// 获取商户已配置的支付方式
getMerchantPaymentMethods(merchantId: string)

// 保存商户支付方式配置
saveMerchantPaymentMethods(merchantId: string, paymentMethodIds: number[])
```

#### 类型定义（types/index.ts）

新增 `PaymentMethod` 接口类型。

### 2.2 后端改造

#### PaymentMethodController

- 路由改为 `/api/v1/admin/payment-methods`
- 返回格式统一为 `{ code, message, data: { list, total, page, pageSize } }`

#### MerchantPaymentMethodController

- 路由改为 `/api/v1/admin/merchant-payment-methods`
- 新增批量保存接口 `POST /api/v1/admin/merchant-payment-methods`
  - 请求体：`{ merchantId: string, paymentMethodIds: number[] }`
  - 逻辑：先删除商户所有配置，再批量新增

#### MerchantPaymentMethodService

新增 `deleteByMerchant(Long merchantId)` 方法

---

## 三、测试结果

| API | 状态 |
|-----|------|
| 登录 | ✅ code=0 |
| MERCHANTS | ✅ code=0 |
| CHANNELS | ✅ code=0 |
| PAYMENT_METHODS | ✅ code=0 |
| ORDERS | ✅ code=0 |
| MERCHANT_PAYMENTS | ✅ code=0 |

**前端访问**: http://localhost:3001
**后端访问**: http://localhost:3003
**登录账号**: admin / admin123

---

## 四、修改文件列表

### 前端
- `src/pages/admin/layout.vue` - 菜单结构改造
- `src/pages/admin/merchants.vue` - 商户管理增加支付方式配置
- `src/api/admin.ts` - 新增API接口
- `src/types/index.ts` - 新增PaymentMethod类型

### 后端
- `src/main/java/.../controller/PaymentMethodController.java` - 路由和返回格式修改
- `src/main/java/.../controller/MerchantPaymentMethodController.java` - 路由和批量保存接口
- `src/main/java/.../service/MerchantPaymentMethodService.java` - 新增deleteByMerchant方法
- `src/main/java/.../service/impl/MerchantPaymentMethodServiceImpl.java` - 实现deleteByMerchant

---

## 五、后续优化建议

1. 商户支付方式配置支持优先级设置
2. 支持每个商户支付方式的独立配置（如不同的手续费率）
3. 前端表格支持分页和搜索
4. 增加操作日志记录
