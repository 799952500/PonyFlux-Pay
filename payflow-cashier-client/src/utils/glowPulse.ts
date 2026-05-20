const GLOW_TARGETS =
  '.el-button:not(.is-link):not(.is-text), .pay-btn, .btn-primary, .btn-outline, .channel-card, .logout-btn'

/** 触发一次性辉光脉冲动画 */
export function triggerGlowPulse(el: HTMLElement): void {
  el.classList.remove('is-glow-pulse')
  void el.offsetWidth
  el.classList.add('is-glow-pulse')
  const onEnd = () => {
    el.classList.remove('is-glow-pulse')
    el.removeEventListener('animationend', onEnd)
  }
  el.addEventListener('animationend', onEnd)
}

/** 全局委托：点击交互元素时附加辉光脉冲 */
export function installGlowPulseDelegation(): void {
  document.addEventListener(
    'pointerdown',
    (event) => {
      const target = (event.target as HTMLElement | null)?.closest<HTMLElement>(GLOW_TARGETS)
      if (!target || target.closest('.is-disabled, [disabled]')) return
      triggerGlowPulse(target)
    },
    { passive: true },
  )
}
