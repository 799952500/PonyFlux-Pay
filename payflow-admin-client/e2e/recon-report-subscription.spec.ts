import { test, expect } from '@playwright/test'
import { loginAs } from './helpers'

test('recon report subscription tab', async ({ page }) => {
  await loginAs(page, 'admin', 'admin123')
  await page.goto('/admin/profile?tab=recon-reports')
  await expect(page.getByRole('main').getByText('对账报告订阅')).toBeVisible()
})
