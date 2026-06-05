import { test } from '@playwright/test'
import fs from 'fs'
import path from 'path'

const outDir = path.resolve(__dirname, '../../docs/images/readme')
fs.mkdirSync(outDir, { recursive: true })

function solveCaptchaQuestion(question: string): string {
  const m = question.match(/(\d+)\s*([+\-×x*])\s*(\d+)/)
  if (!m) return '0'
  const a = Number(m[1])
  const op = m[2]
  const b = Number(m[3])
  if (op === '+' || op === '＋') return String(a + b)
  if (op === '-' || op === '－') return String(a - b)
  return String(a * b)
}

test.describe.configure({ mode: 'serial' })

test('capture README screenshots', async ({ page }) => {
  test.setTimeout(120_000)
  await page.setViewportSize({ width: 1440, height: 900 })

  await page.goto('http://127.0.0.1:5173/cashier/pc/demo')
  await page.getByText('选择支付方式').waitFor({ timeout: 15000 })
  await page.screenshot({ path: path.join(outDir, 'cashier-pc.png') })

  await page.goto('http://127.0.0.1:5173/login')
  await page.locator('.locale-switcher__select').waitFor({ timeout: 15000 })
  await page.screenshot({ path: path.join(outDir, 'cashier-login.png') })

  await page.goto('http://127.0.0.1:5173/cashier/pc/demo?lang=zh-TW')
  await page.getByText('選擇支付方式').waitFor({ timeout: 15000 })
  await page.screenshot({ path: path.join(outDir, 'cashier-zh-tw.png') })

  await page.goto('http://127.0.0.1:3001/login')
  await page.locator('.login-card__title').waitFor({ timeout: 15000 })
  await page.screenshot({ path: path.join(outDir, 'admin-login.png') })

  const captchaRes = await page.request.get('http://127.0.0.1:3003/api/v1/admin/auth/captcha')
  const captchaJson = await captchaRes.json()
  const captchaId = captchaJson.data.captchaId as string
  const answer = solveCaptchaQuestion((captchaJson.data.question as string) ?? '')

  const loginRes = await page.request.post('http://127.0.0.1:3003/api/v1/admin/auth/login', {
    data: {
      username: 'admin',
      password: 'admin123',
      captchaId,
      captchaAnswer: answer,
    },
  })
  const loginJson = await loginRes.json()
  const token = loginJson.data.token as string
  const adminUser = JSON.stringify({ ...loginJson.data, token })

  await page.evaluate(
    ({ tok, user }) => {
      localStorage.setItem('adminToken', tok)
      localStorage.setItem('adminUser', user)
    },
    { tok: token, user: adminUser }
  )

  await page.goto('http://127.0.0.1:3001/admin/dashboard')
  await page.waitForTimeout(2500)
  await page.screenshot({ path: path.join(outDir, 'admin-dashboard.png') })

  await page.goto('http://127.0.0.1:3001/admin/payment-methods')
  await page.waitForTimeout(2000)
  await page.screenshot({ path: path.join(outDir, 'admin-payment-methods.png') })

  await page.goto('http://127.0.0.1:3001/admin/orders')
  await page.waitForTimeout(2000)
  await page.screenshot({ path: path.join(outDir, 'admin-orders.png') })
})
