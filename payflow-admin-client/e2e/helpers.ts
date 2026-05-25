import { expect, type Page } from '@playwright/test'

export async function loginAs(
  page: Page,
  username: string,
  password: string,
): Promise<void> {
  await page.goto('/login')
  await page.locator('input[autocomplete="username"]').fill(username)
  await page.locator('input[autocomplete="current-password"]').fill(password)

  const captcha = page.getByPlaceholder('请输入计算结果')
  if (await captcha.isVisible().catch(() => false)) {
    const expr = await page.locator('.login-captcha__expr').textContent()
    const answer = solveCaptcha(expr ?? '')
    await captcha.fill(answer)
  }

  const loginResponse = page.waitForResponse(
    (res) => res.url().includes('/admin/auth/login') && res.request().method() === 'POST',
  )
  await page.locator('button.login-submit').click()
  const res = await loginResponse
  expect(res.ok(), `登录 HTTP 失败: ${res.status()} ${await res.text()}`).toBeTruthy()

  await expect(page).toHaveURL(/\/admin\/dashboard/, { timeout: 30_000 })
  await expect(page.locator('.login-alert')).toHaveCount(0)
}

function solveCaptcha(question: string): string {
  const m = question.match(/(\d+)\s*\+\s*(\d+)/)
  if (!m) throw new Error(`无法解析验证码: ${question}`)
  return String(Number(m[1]) + Number(m[2]))
}

export async function expectTableHasRows(page: Page): Promise<void> {
  const rows = page.locator('.el-table__body-wrapper tbody tr')
  await expect(rows.first()).toBeVisible({ timeout: 20_000 })
  expect(await rows.count()).toBeGreaterThan(0)
}
