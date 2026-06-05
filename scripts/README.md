# 脚本说明

## 唯一入口

| 平台 | 命令 |
|------|------|
| Windows | `.\setup.ps1` |
| Linux / macOS | `./setup.sh` |
| 通用 | `pip install -r scripts/requirements.txt && python scripts/setup.py` |

`setup.py` 自动完成：

1. 检测 MySQL（及可选 Redis）
2. **默认重置**并安装 `sql/schema/` + `sql/seed/`
3. 校验表前缀、`admin/admin123` 账号
4. Maven 安装后端模块 + `npm install` 双前端

### 常用参数

```bash
python scripts/setup.py --db-only          # 仅数据库
python scripts/setup.py --no-reset         # 不删库（不推荐，易半套库）
python scripts/setup.py --skip-build       # 跳过 Maven/npm
python scripts/setup.py --host HOST --user USER --password PASS
```

安装完成后按终端提示分别启动 admin/cashier 后端与前端 dev 服务。
