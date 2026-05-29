import { test, expect } from '@playwright/test'
import { loginAs } from './helpers'

test('recon insights dashboard', async ({ page }) => {
  await loginAs(page, 'admin', 'admin123')
  await page.goto('/admin/reconcile/insights-dashboard')
  await expect(page.getByRole('main').getByText('差异归因看板')).toBeVisible()
})
