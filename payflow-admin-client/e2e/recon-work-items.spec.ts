import { test, expect } from '@playwright/test'
import { loginAs } from './helpers'

test('recon work items basic flow', async ({ page }) => {
  await loginAs(page, 'admin', 'admin123')
  await page.goto('/admin/reconcile/work-items')

  await expect(page.getByRole('main').getByText('差异工单')).toBeVisible()
  await page.waitForTimeout(500)

  // 打开第一条详情（如果有数据）
  const detailBtn = page.getByRole('button', { name: '详情' }).first()
  if (await detailBtn.count()) {
    await detailBtn.click()
    await expect(page.getByText('工单详情')).toBeVisible()
  }
})

