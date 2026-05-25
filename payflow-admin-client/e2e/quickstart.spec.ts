import { test, expect } from '@playwright/test'
import { loginAs, expectTableHasRows } from './helpers'

test.describe('Quickstart 核心冒烟 (011)', () => {
  test.beforeEach(async ({ page }) => {
    await loginAs(page, 'admin', 'admin123')
  })

  test('1. 后台登录后进入仪表盘', async ({ page }) => {
    await expect(page.getByText('工作台', { exact: false }).first()).toBeVisible()
  })

  test('2. 订单列表有演示数据', async ({ page }) => {
    await page.goto('/admin/orders')
    await expectTableHasRows(page)
    await expect(page.locator('body')).not.toContainText('服务器错误')
  })

  test('3. 退款列表有演示数据', async ({ page }) => {
    await page.goto('/admin/refunds')
    await expectTableHasRows(page)
  })

  test('4. 对账任务页可打开', async ({ page }) => {
    await page.goto('/admin/reconcile/tasks')
    await expect(page.locator('.el-table, .el-empty').first()).toBeVisible()
    await expect(page.locator('body')).not.toContainText('Table')
  })

  test('5. RBAC 角色与菜单页可打开', async ({ page }) => {
    await page.goto('/admin/roles')
    await expectTableHasRows(page)
    await page.goto('/admin/menus')
    await expectTableHasRows(page)
  })

  test('6. 商户隔离 finance_demo 仅见授权商户', async ({ page, context }) => {
    await context.clearCookies()
    await page.evaluate(() => localStorage.clear())
    await loginAs(page, 'finance_demo', 'admin123')
    await page.goto('/admin/orders')
    await expectTableHasRows(page)
    const body = await page.locator('body').innerText()
    // 订单表不展示 merchant_id，用 M100001 种子商品名断言可见范围
    expect(body).toMatch(/iPhone 保护壳|笔记本电脑|数据线/)
    // M100002 专属订单商品名不应出现
    expect(body).not.toMatch(/双人套餐|下午茶券|团建宴席/)
  })

})

test.describe('Quickstart API (011)', () => {
  test('7. 支付回调链 API 健康', async ({ request }) => {
    const adminMeta = await request.get('http://127.0.0.1:3003/api/v1/admin/meta/features')
    expect(adminMeta.status()).toBe(200)
    const cashierDocs = await request.get('http://127.0.0.1:3002/api-docs')
    expect(cashierDocs.status()).toBe(200)
    const link = await request.get('http://127.0.0.1:3002/api/v1/public/payment-links/PLK-DEMO-001')
    expect(link.status()).toBe(200)
    const json = await link.json()
    expect(json.code).toBe(0)
  })
})
