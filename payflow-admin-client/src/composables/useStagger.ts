const STAGGER_SELECTORS =
  '.content-card, .filter-bar, .stats-bar, .stats-panel, .stat-card, .el-row > .el-col > .content-card, .el-row > .el-col > .stat-card'

const MAX_STAGGER = 8
const STEP_MS = 60

/** 页面进入后，对主区域内卡片依次错峰入场 */
export function triggerStagger(root: HTMLElement): void {
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return

  const nodes = Array.from(root.querySelectorAll<HTMLElement>(STAGGER_SELECTORS))
    .filter((el) => {
      const rect = el.getBoundingClientRect()
      return rect.width > 0 && rect.height > 0
    })
    .slice(0, MAX_STAGGER)

  nodes.forEach((el, index) => {
    el.classList.remove('stagger-enter')
    void el.offsetWidth
    el.style.setProperty('--stagger-i', String(index))
    el.style.setProperty('--stagger-step', `${STEP_MS}ms`)
    el.classList.add('stagger-enter')

    const onEnd = () => {
      el.classList.remove('stagger-enter')
      el.style.removeProperty('--stagger-i')
      el.style.removeProperty('--stagger-step')
      el.removeEventListener('animationend', onEnd)
    }
    el.addEventListener('animationend', onEnd)
  })
}
