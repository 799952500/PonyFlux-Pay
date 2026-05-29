import type { Component } from 'vue'
import {
  Monitor,
  Odometer,
  TrendCharts,
  Bell,
  Search,
  Message,
  List,
  RefreshLeft,
  Memo,
  Calendar,
  Finished,
  DataAnalysis,
  CreditCard,
  Connection,
  Wallet,
  Money,
  Share,
  CircleCheck,
  Notebook,
  Coin,
  Tickets,
  Timer,
  Clock,
  Shop,
  OfficeBuilding,
  UserFilled,
  Warning,
  Setting,
  Tools,
  Avatar,
  Key,
  Menu as MenuIcon,
  Collection,
  DocumentCopy,
  Lock,
  Grid,
  Document,
  Goods,
  Histogram,
  DataLine,
  Operation,
} from '@element-plus/icons-vue'
import type { SysMenu } from '@/types'

/** 叶子菜单：按路由精确匹配 */
const PATH_ICON_MAP: Record<string, Component> = {
  '/admin/dashboard': Odometer,
  '/admin/insights/funnel': TrendCharts,
  '/admin/dashboard/churn-alerts': Bell,
  '/admin/notifications': Message,
  '/admin/search': Search,
  '/admin/orders': List,
  '/admin/refunds': RefreshLeft,
  '/admin/reconcile/work-items': Tickets,
  '/admin/reconcile/tasks': Calendar,
  '/admin/reconcile/results': Finished,
  '/admin/reconcile/insights-dashboard': TrendCharts,
  '/admin/reconcile/sla-rules': Timer,
  '/admin/reconcile/long-tail': Clock,
  '/admin/reconcile/summary': DataAnalysis,
  '/admin/channels': Connection,
  '/admin/payment-methods': Wallet,
  '/admin/payment-accounts': Money,
  '/admin/channel-routing/health': CircleCheck,
  '/admin/routing/logs': Notebook,
  '/admin/fee-rate/config': Coin,
  '/admin/fee-rate/audit-log': Tickets,
  '/admin/merchants': OfficeBuilding,
  '/admin/onboarding': UserFilled,
  '/admin/risk': Warning,
  '/admin/settings': Tools,
  '/admin/users': Avatar,
  '/admin/roles': Key,
  '/admin/menus': MenuIcon,
  '/admin/dicts': Collection,
  '/admin/audit-logs': DocumentCopy,
  '/admin/security-audit': Lock,
  '/admin/data-isolation': Grid,
}

/** 一级分组：按 menuCode */
const CODE_ICON_MAP: Record<string, Component> = {
  grp_workspace: Monitor,
  grp_trade: Goods,
  grp_reconcile: Memo,
  grp_channel_account: CreditCard,
  grp_routing: Share,
  grp_merchant: Shop,
  grp_system: Setting,
  dashboard: Odometer,
  insights_funnel: TrendCharts,
  churn_alerts: Bell,
  notifications: Message,
  search: Search,
  orders: List,
  refunds: RefreshLeft,
  reconcile_work_items: Tickets,
  reconcile_tasks: Calendar,
  reconcile_results: Finished,
  reconcile_insights_dashboard: TrendCharts,
  reconcile_sla_rules: Timer,
  reconcile_long_tail: Clock,
  reconcile_summary: DataAnalysis,
  channels: Connection,
  payment_methods: Wallet,
  payment_accounts: Money,
  channel_routing_health: CircleCheck,
  routing_logs: Notebook,
  fee_rate_config: Coin,
  fee_rate_audit_log: Tickets,
  merchants: OfficeBuilding,
  onboarding: UserFilled,
  risk: Warning,
  settings: Tools,
  users: Avatar,
  roles: Key,
  menus: MenuIcon,
  dicts: Collection,
  audit_logs: DocumentCopy,
  security_audit: Lock,
  data_isolation: Grid,
}

/** 数据库 icon 字段：仅接受 Element Plus 组件名（PascalCase） */
const DB_ICON_MAP: Record<string, Component> = {
  Monitor,
  Odometer,
  TrendCharts,
  Bell,
  Search,
  Message,
  List,
  RefreshLeft,
  Memo,
  Calendar,
  Finished,
  DataAnalysis,
  CreditCard,
  Connection,
  Wallet,
  Money,
  Share,
  CircleCheck,
  Notebook,
  Coin,
  Tickets,
  Timer,
  Clock,
  Shop,
  OfficeBuilding,
  UserFilled,
  Warning,
  Setting,
  Tools,
  Avatar,
  Key,
  Menu: MenuIcon,
  Collection,
  DocumentCopy,
  Lock,
  Grid,
  Document,
  Goods,
  Histogram,
  DataLine,
  Operation,
}

const EMOJI_PATTERN = /[\u{1F300}-\u{1FAFF}\u{2600}-\u{27BF}]/u

function isDbIconUsable(icon?: string): boolean {
  if (!icon?.trim()) return false
  const trimmed = icon.trim()
  if (EMOJI_PATTERN.test(trimmed)) return false
  if (trimmed.length <= 2) return false
  return trimmed in DB_ICON_MAP
}

function resolveByPath(path?: string): Component | null {
  if (!path) return null
  if (PATH_ICON_MAP[path]) return PATH_ICON_MAP[path]
  if (path.includes('/orders')) return List
  if (path.includes('/refunds')) return RefreshLeft
  if (path.includes('/reconcile')) return Memo
  if (path.includes('/merchants')) return OfficeBuilding
  if (path.includes('/risk')) return Warning
  if (path.includes('/settings')) return Tools
  if (path.includes('/audit')) return DocumentCopy
  if (path.includes('/routing')) return Share
  if (path.includes('/fee-rate')) return Coin
  if (path.includes('/channel')) return Connection
  if (path.includes('/payment')) return Wallet
  if (path.includes('/dashboard')) return Odometer
  if (path.includes('/insights')) return TrendCharts
  return null
}

function resolveByCode(menuCode?: string): Component | null {
  if (!menuCode) return null
  return CODE_ICON_MAP[menuCode] ?? null
}

/**
 * 为侧栏菜单解析 Element Plus 图标，忽略数据库中的 emoji。
 */
export function resolveMenuIcon(menu: SysMenu): Component {
  const fromPath = resolveByPath(menu.path)
  if (fromPath) return fromPath

  const fromCode = resolveByCode(menu.menuCode)
  if (fromCode) return fromCode

  if (isDbIconUsable(menu.icon)) {
    return DB_ICON_MAP[menu.icon!.trim()]
  }

  const hasChildren = Boolean(menu.children?.length)
  if (hasChildren) return Histogram
  return DataLine
}
