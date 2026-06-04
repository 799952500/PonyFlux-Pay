import { test, expect } from '@playwright/test'

test.describe('收银台冒烟 (011)', () => {
  test('PC 收银台加载演示订单', async ({ page }) => {
    await page.goto('/cashier/pc/demo')
    await expect(page.getByText('测试商品', { exact: false })).toBeVisible()
    await expect(page.getByText('选择支付方式', { exact: false })).toBeVisible()
    await expect(page.locator('body')).not.toContainText('服务器错误')
  })

  test('入驻结果页可打开', async ({ page }) => {
    await page.goto('/onboarding/result')
    await expect(page.locator('body')).toBeVisible()
    await expect(page.locator('body')).not.toContainText('服务器错误')
  })

  test('门户页展示语言切换器', async ({ page }) => {
    await page.goto('/login')
    await expect(page.locator('.locale-switcher__select')).toBeVisible()
    await expect(page.locator('#locale-select')).toHaveCount(1)
  })

  test('演示收银台可按 query lang 显示繁体', async ({ page }) => {
    await page.goto('/cashier/pc/demo?lang=zh-TW')
    await expect(page.getByText('選擇支付方式', { exact: false })).toBeVisible()
    await expect(page.locator('#locale-select')).toHaveCount(0)
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
