import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getResourceDependencies,
  type ResourceDependencyType,
  type ResourceRefItem,
} from '@/api/admin'

function formatRefsHtml(refs: ResourceRefItem[]): string {
  if (!refs?.length) {
    return ''
  }
  const items = refs
    .map((r) => `<li>${r.label}${r.merchantId ? `（商户 ${r.merchantId}）` : ''}</li>`)
    .join('')
  return `<ul style="margin:8px 0;padding-left:20px">${items}</ul>`
}

async function showBlockedDialog(summary: string, refs: ResourceRefItem[]) {
  await ElMessageBox.alert(
    `<p>${summary}</p>${formatRefsHtml(refs)}<p style="margin-top:8px">请先解除上述关联后再删除。</p>`,
    '无法删除',
    { dangerouslyUseHTMLString: true, type: 'warning', confirmButtonText: '知道了' }
  )
}

function extractRefsFromError(err: unknown): ResourceRefItem[] {
  const e = err as { data?: { refs?: ResourceRefItem[] }; refs?: ResourceRefItem[] }
  return e?.data?.refs ?? e?.refs ?? []
}

function extractMessageFromError(err: unknown, fallback: string): string {
  const e = err as { message?: string; data?: { summary?: string } }
  return e?.data?.summary ?? e?.message ?? fallback
}

export interface ConfirmDeleteWithGuardOptions {
  resourceType: ResourceDependencyType
  resourceId: string | number
  displayName: string
  deleteFn: () => Promise<unknown>
  onSuccess?: () => void | Promise<void>
}

/**
 * 删除前预检依赖；存在关联时提示并中止，否则确认后执行删除。
 */
export async function confirmDeleteWithGuard(options: ConfirmDeleteWithGuardOptions): Promise<void> {
  const resourceId = String(options.resourceId)
  try {
    const check = await getResourceDependencies(options.resourceType, resourceId)
    if (check.blocked) {
      await showBlockedDialog(check.summary, check.refs ?? [])
      return
    }
    await ElMessageBox.confirm(
      `确定要删除「${options.displayName}」吗？删除后不可恢复。`,
      '删除确认',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
    await options.deleteFn()
    ElMessage.success('删除成功')
    await options.onSuccess?.()
  } catch (err: unknown) {
    if (err === 'cancel' || (err as { message?: string })?.message === 'cancel') {
      return
    }
    const refs = extractRefsFromError(err)
    if (refs.length > 0) {
      await showBlockedDialog(extractMessageFromError(err, '存在未解除的关联，无法删除'), refs)
      return
    }
    ElMessage.error(extractMessageFromError(err, '删除失败'))
  }
}
