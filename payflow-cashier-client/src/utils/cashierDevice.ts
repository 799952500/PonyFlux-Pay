/** 收银台终端：与后端 getCashierInfo 的 client 参数一致 */
export type CashierTerminal = 'PC' | 'H5'

export type CashierClientParam = CashierTerminal | 'APP'

/**
 * 解析收银台终端。
 * 优先级：URL ?client= > UA 检测。
 */
export function resolveCashierTerminal(search?: string | URLSearchParams): CashierTerminal {
  const params =
    typeof search === 'string'
      ? new URLSearchParams(search.startsWith('?') ? search.slice(1) : search)
      : search ?? new URLSearchParams(typeof window !== 'undefined' ? window.location.search : '')

  const forced = params.get('client')?.toUpperCase()
  if (forced === 'PC') return 'PC'
  if (forced === 'H5' || forced === 'APP') return 'H5'

  const ua = typeof navigator !== 'undefined' ? navigator.userAgent : ''
  if (/iphone|ipod|android|ipad|mobile|webos|blackberry|iemobile|opera mini/i.test(ua)) {
    return 'H5'
  }
  return 'PC'
}

export function terminalPathSegment(terminal: CashierTerminal): 'pc' | 'h5' {
  return terminal === 'H5' ? 'h5' : 'pc'
}

/** 构建收银台页面路径（含 query，保留 sig 等参数） */
export function buildCashierPath(
  orderId: string,
  terminal: CashierTerminal,
  query?: Record<string, string | string[] | undefined>
): string {
  const seg = terminalPathSegment(terminal)
  const base = `/cashier/${seg}/${encodeURIComponent(orderId)}`
  if (!query || Object.keys(query).length === 0) return base

  const qs = new URLSearchParams()
  for (const [key, val] of Object.entries(query)) {
    if (val === undefined) continue
    if (Array.isArray(val)) {
      val.forEach((v) => qs.append(key, v))
    } else {
      qs.set(key, val)
    }
  }
  const s = qs.toString()
  return s ? `${base}?${s}` : base
}

/** 统一入口：按当前环境解析终端并生成路径 */
export function buildCashierEntryPath(
  orderId: string,
  query?: Record<string, string | string[] | undefined>
): string {
  const search = query
    ? new URLSearchParams(
        Object.entries(query).flatMap(([k, v]) =>
          v === undefined ? [] : Array.isArray(v) ? v.map((x) => [k, x]) : [[k, v]]
        ) as [string, string][]
      ).toString()
    : typeof window !== 'undefined'
      ? window.location.search
      : ''
  const terminal = resolveCashierTerminal(search ? `?${search}` : '')
  return buildCashierPath(orderId, terminal, query)
}
