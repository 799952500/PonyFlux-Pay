export type PfSurface = 'portal'

export function applyPfSurface(_surface: PfSurface = 'portal'): void {
  document.documentElement.dataset.pfSurface = 'portal'
}
