import { test, expect } from '@playwright/test'

test.describe('收银台冒烟 (011)', () => {
  test('PC 收银台加载演示订单', async ({ page }) => {
    await page.goto('/cashier/pc/ORD-20260518-0001')
    await expect(page.getByText('iPhone 保护壳套装', { exact: false })).toBeVisible()
    await expect(page.getByText('选择支付方式', { exact: false })).toBeVisible()
    await expect(page.locator('body')).not.toContainText('服务器错误')
  })

  test('入驻结果页可打开', async ({ page }) => {
    await page.goto('/onboarding/result')
    await expect(page.locator('body')).toBeVisible()
    await expect(page.locator('body')).not.toContainText('服务器错误')
  })
})

test.describe('收银台 API (011)', () => {
  test('公开支付链接 API', async ({ request }) => {
    const res = await request.get('http://127.0.0.1:3002/api/v1/public/payment-links/PLK-DEMO-001')
    expect(res.status()).toBe(200)
    const json = await res.json()
    expect(json.code).toBe(0)
  })
})
