import {
  ORDER_STATUS_LABEL,
  ORDER_STATUS_TAG,
  PAY_CHANNEL_LABEL,
  PAY_CHANNEL_TAG,
  labelOf,
  tagTypeOf,
} from '@/utils/format'

export type DictKind = 'enum' | 'mapping'

export interface DictCategoryMeta {
  key: string
  title: string
  description: string
  kind: DictKind
  usedIn: string[]
}

export interface EnumDictRow {
  code: string
  label: string
  tagType: string
}

export interface MappingDictRow {
  db: string
  ui: string
  label: string
  uiTagType: string
}

/** 各字典分类说明（与后端 AdminDictController 键名一致） */
export const DICT_CATEGORY_META: Record<string, DictCategoryMeta> = {
  refundStatus: {
    key: 'refundStatus',
    title: '退款状态（数据库）',
    description: '收银库 cashier_refund.status 的存储枚举，与渠道回调状态一致。',
    kind: 'enum',
    usedIn: ['退款审批', '对账差异', '后台查询条件'],
  },
  orderStatus: {
    key: 'orderStatus',
    title: '订单状态',
    description: '订单主状态，用于订单列表筛选、统计看板与详情展示。',
    kind: 'enum',
    usedIn: ['订单管理', '全局搜索', '数据概览'],
  },
  payChannels: {
    key: 'payChannels',
    title: '支付渠道',
    description: '下单与支付路由使用的渠道编码，与渠道配置 channelCode 对齐。',
    kind: 'enum',
    usedIn: ['订单管理', '渠道管理', '支付账号'],
  },
  refundUiMapping: {
    key: 'refundUiMapping',
    title: '退款状态映射（库表 → 管理端）',
    description: '数据库退款状态与管理端列表/筛选项之间的对照，避免直接改库值导致界面不一致。',
    kind: 'mapping',
    usedIn: ['退款管理列表', '退款筛选下拉'],
  },
}

const REFUND_DB_TAG: Record<string, string> = {
  REFUNDING: 'warning',
  REFUNDED: 'success',
  FAILED: 'danger',
  CLOSED: 'info',
}

const REFUND_UI_TAG: Record<string, string> = {
  PENDING: 'warning',
  APPROVED: 'primary',
  COMPLETED: 'success',
  REJECTED: 'danger',
}

export function dictMetaFor(key: string): DictCategoryMeta {
  return (
    DICT_CATEGORY_META[key] ?? {
      key,
      title: key,
      description: '系统枚举字典项',
      kind: 'enum',
      usedIn: [],
    }
  )
}

export function isPlainEnumObject(val: unknown): val is Record<string, string> {
  if (val == null || typeof val !== 'object' || Array.isArray(val)) return false
  return Object.values(val).every((v) => typeof v === 'string' || typeof v === 'number')
}

export function parseEnumRows(key: string, val: unknown): EnumDictRow[] {
  if (!isPlainEnumObject(val)) return []
  const labelMap =
    key === 'orderStatus'
      ? ORDER_STATUS_LABEL
      : key === 'payChannels'
        ? PAY_CHANNEL_LABEL
        : {}
  const tagMap =
    key === 'orderStatus'
      ? ORDER_STATUS_TAG
      : key === 'payChannels'
        ? PAY_CHANNEL_TAG
        : key === 'refundStatus'
          ? REFUND_DB_TAG
          : {}

  return Object.entries(val).map(([code, label]) => {
    const text = String(label)
    const displayLabel = labelMap[code] ?? text
    const tagType =
      key === 'refundStatus'
        ? tagTypeOf(REFUND_DB_TAG, code)
        : tagTypeOf(tagMap, code)
    return {
      code,
      label: displayLabel,
      tagType,
    }
  })
}

export function parseMappingRows(val: unknown): MappingDictRow[] {
  if (!Array.isArray(val)) return []
  return val
    .filter((item): item is Record<string, unknown> => item != null && typeof item === 'object')
    .map((item) => {
      const db = String(item.db ?? '')
      const ui = String(item.ui ?? '')
      const label = String(item.label ?? '')
      return {
        db,
        ui,
        label,
        uiTagType: tagTypeOf(REFUND_UI_TAG, ui),
      }
    })
}

export function filterEnumRows(rows: EnumDictRow[], keyword: string): EnumDictRow[] {
  const q = keyword.trim().toLowerCase()
  if (!q) return rows
  return rows.filter(
    (r) => r.code.toLowerCase().includes(q) || r.label.toLowerCase().includes(q),
  )
}

export function filterMappingRows(rows: MappingDictRow[], keyword: string): MappingDictRow[] {
  const q = keyword.trim().toLowerCase()
  if (!q) return rows
  return rows.filter(
    (r) =>
      r.db.toLowerCase().includes(q)
      || r.ui.toLowerCase().includes(q)
      || r.label.toLowerCase().includes(q),
  )
}

export { labelOf, tagTypeOf }
