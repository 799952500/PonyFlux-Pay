import { applyPfSurface } from './usePfSurfaceCore'

/**
 * 收银台全站统一 mint 门户主题（与运管后台同源）。
 */
export function installPfSurface(): void {
  applyPfSurface('portal')
}
