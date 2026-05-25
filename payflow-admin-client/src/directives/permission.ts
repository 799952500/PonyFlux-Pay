import type { App, DirectiveBinding } from 'vue'
import { useAdminStore } from '@/stores/admin'

type PermissionValue = string | string[]

function resolveLogical(binding: DirectiveBinding<PermissionValue>): 'AND' | 'OR' {
  return binding.modifiers.any ? 'OR' : 'AND'
}

function checkPermission(binding: DirectiveBinding<PermissionValue>): boolean {
  const adminStore = useAdminStore()
  const value = binding.value
  if (!value) {
    return true
  }
  return adminStore.hasPermission(value, resolveLogical(binding))
}

function applyPermission(el: HTMLElement, binding: DirectiveBinding<PermissionValue>) {
  if (!checkPermission(binding)) {
    el.remove()
  }
}

export function registerPermissionDirective(app: App) {
  app.directive('permission', {
    mounted(el, binding) {
      applyPermission(el as HTMLElement, binding)
    },
    updated(el, binding) {
      applyPermission(el as HTMLElement, binding)
    },
  })
}
