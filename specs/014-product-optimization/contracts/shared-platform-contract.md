# Internal Contract: 共享平台能力（common）

**Created**: 2026-05-29  
**Feature**: [../spec.md](../spec.md)

本契约描述跨 `payflow-admin-server`、`payflow-cashier-server`、`payflow-recon-server` 的统一约定。**不新增对外 REST 端点**，但变更会影响所有 API 响应与认证行为。

## 1) 统一响应体 `R<T>`

**包路径（目标）**: `com.payflow.common.web.R`

```json
{
  "code": 0,
  "message": "success",
  "data": { }
}
```

| code | 含义 |
|------|------|
| `0` | 成功 |
| `1xxx` | 管理端业务错误 |
| `2xxx` | 收银台业务错误 |
| `7500-7599` | 对账错误 |

**规则**:
- 禁止各模块自定义 `ApiResponse` 等平行包装类（迁移期除外，见 plan Complexity Tracking）。
- `GlobalExceptionHandler` 统一返回 `R`，生产环境 `message` 不得包含堆栈/SQL/内部路径。
- 前端 Axios 拦截器继续解包 `data` 字段。

## 2) 分页 `PageRequest` / `PageResult`

**包路径（目标）**: `com.payflow.common.web.PageRequest`

| 参数 | 规则 |
|------|------|
| `page` | ≥1，默认 1 |
| `size` | `min(requested, 100)`，默认 20 |

**响应**（嵌入 `data`）:

```json
{
  "list": [],
  "total": 0,
  "page": 1,
  "size": 20
}
```

**例外常量**: `RECON_MAX_PAGE_SIZE = 500` 仅用于对账工单等大列表接口，须在 Controller 显式引用，禁止魔法数字。

## 3) JWT `JwtService`

**包路径（目标）**: `com.payflow.common.security.JwtService`

| 能力 | 说明 |
|------|------|
| `issue(subject, claims)` | 签发 Token，**必须**含 `jti`（UUID） |
| `parse(token)` | 校验签名与过期 |
| `revoke(jti, ttl)` | 登出黑名单写入 Redis |

**配置项**（各服务 `application.yml`）:

| 键 | 说明 |
|----|------|
| `payflow.jwt.secret` | 环境变量注入，禁止默认值上生产 |
| `payflow.jwt.expiration-hours` | 默认 24 |

## 4) 迁移期双轨（临时）

| 阶段 | admin | cashier | recon |
|------|-------|---------|-------|
| T0（当前） | ApiResponse | R | R |
| T1 | R + 废弃 ApiResponse | common.R | common.R |
| T2 | 删除 ApiResponse | — | — |

**验收**: `grep -r "class ApiResponse" payflow-admin-server` 无结果；三模块均 `import com.payflow.common.web.R`。
