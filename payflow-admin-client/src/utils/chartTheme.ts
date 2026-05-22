/** ECharts 随 data-theme 切换的配色 */
import type { ThemeKey } from '@/stores/theme'

export interface ChartThemeColors {
  text: string
  textMuted: string
  axis: string
  tooltipBg: string
  tooltipBorder: string
  tooltipText: string
  linePrimary: string
  lineSecondary: string
  areaGradientStart: string
  pieColors: string[]
  barGradientStart: string
  barGradientEnd: string
}

function readThemeKey(): ThemeKey {
  const raw = document.documentElement.getAttribute('data-theme')
  if (raw === 'ocean' || raw === 'violet' || raw === 'dark' || raw === 'mint') {
    return raw
  }
  return 'mint'
}

const CHART_BY_THEME: Record<ThemeKey, ChartThemeColors> = {
  mint: {
    text: '#3d4a59',
    textMuted: '#64748b',
    axis: '#94a3b8',
    tooltipBg: '#fcfdfd',
    tooltipBorder: 'rgba(51, 65, 85, 0.12)',
    tooltipText: '#3d4a59',
    linePrimary: '#0d9488',
    lineSecondary: '#14b8a6',
    areaGradientStart: 'rgba(20, 184, 166, 0.22)',
    pieColors: ['#14b8a6', '#0d9488', '#f59e0b', '#6366f1', '#94a3b8'],
    barGradientStart: '#0d9488',
    barGradientEnd: '#2aa89a',
  },
  ocean: {
    text: '#3d4a59',
    textMuted: '#64748b',
    axis: '#94a3b8',
    tooltipBg: '#fcfdfe',
    tooltipBorder: 'rgba(51, 65, 85, 0.12)',
    tooltipText: '#3d4a59',
    linePrimary: '#0369a1',
    lineSecondary: '#0284c7',
    areaGradientStart: 'rgba(2, 132, 199, 0.2)',
    pieColors: ['#0284c7', '#38bdf8', '#f59e0b', '#818cf8', '#94a3b8'],
    barGradientStart: '#0369a1',
    barGradientEnd: '#0284c7',
  },
  violet: {
    text: '#3d4a59',
    textMuted: '#64748b',
    axis: '#94a3b8',
    tooltipBg: '#fdfcfe',
    tooltipBorder: 'rgba(51, 65, 85, 0.12)',
    tooltipText: '#3d4a59',
    linePrimary: '#6d28d9',
    lineSecondary: '#7c3aed',
    areaGradientStart: 'rgba(124, 58, 237, 0.18)',
    pieColors: ['#7c3aed', '#a78bfa', '#f59e0b', '#14b8a6', '#94a3b8'],
    barGradientStart: '#6d28d9',
    barGradientEnd: '#7c3aed',
  },
  dark: {
    text: '#e2e8f0',
    textMuted: '#94a3b8',
    axis: '#94a3b8',
    tooltipBg: '#1e293b',
    tooltipBorder: 'rgba(255,255,255,0.12)',
    tooltipText: '#f8fafc',
    linePrimary: '#34d399',
    lineSecondary: '#2dd4bf',
    areaGradientStart: 'rgba(52,211,153,0.25)',
    pieColors: ['#2dd4bf', '#14b8a6', '#fbbf24', '#818cf8', '#94a3b8'],
    barGradientStart: '#14b8a6',
    barGradientEnd: '#2dd4bf',
  },
}

export function isDarkTheme(): boolean {
  return readThemeKey() === 'dark'
}

export function getChartTheme(): ChartThemeColors {
  return CHART_BY_THEME[readThemeKey()]
}
