import { test, expect } from '@playwright/test'
import { loginAs } from './helpers'

test('recon sla rules page', async ({ page }) => {
  await loginAs(page, 'admin', 'admin123')
  await page.goto('/admin/reconcile/sla-rules')
  await expect(page.getByRole('main').getByText('SLA 规则')).toBeVisible()
})
