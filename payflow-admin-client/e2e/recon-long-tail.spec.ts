import { test, expect } from '@playwright/test'
import { loginAs } from './helpers'

test('recon long tail page', async ({ page }) => {
  await loginAs(page, 'admin', 'admin123')
  await page.goto('/admin/reconcile/long-tail')
  await expect(page.getByRole('main').getByText('统计截止')).toBeVisible()
})
