import type { Router } from 'vue-router'

interface FlipSource {
  rect: DOMRect
  borderRadius: string
}

const sourceRects = new Map<string, FlipSource>()

/** 取消未完成的 FLIP 动画，避免路由切换后节点卡在模糊/错位状态 */
export function cancelFlipAnimations(): void {
  document.querySelectorAll<HTMLElement>('[data-flip].flip-animating, .flip-animating').forEach((el) => {
    el.getAnimations?.().forEach((anim) => anim.cancel())
    el.classList.remove('flip-animating')
    el.style.removeProperty('transform-origin')
    el.style.removeProperty('transform')
    el.style.removeProperty('opacity')
  })
  sourceRects.clear()
}

/** 捕获当前页面上所有 data-flip 节点的位置 */
export function captureFlipSources(): void {
  sourceRects.clear()
  document.querySelectorAll<HTMLElement>('[data-flip]').forEach((el) => {
    const id = el.dataset.flip
    if (!id) return
    sourceRects.set(id, {
      rect: el.getBoundingClientRect(),
      borderRadius: window.getComputedStyle(el).borderRadius,
    })
  })
}

/** 手动注册 FLIP 源（如图表点击等无 DOM 节点的场景） */
export function registerFlipSource(id: string, rect: DOMRect, borderRadius = '8px'): void {
  sourceRects.set(id, { rect, borderRadius })
}

/** 路由进入后，将目标 data-flip 节点从源位置动画到目标位置 */
export function runFlipAnimations(): void {
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
    sourceRects.clear()
    return
  }

  requestAnimationFrame(() => {
    requestAnimationFrame(() => {
      document.querySelectorAll<HTMLElement>('[data-flip]').forEach((el) => {
        const id = el.dataset.flip
        if (!id) return

        const src = sourceRects.get(id)
        if (!src) return

        const dst = el.getBoundingClientRect()
        if (dst.width <= 0 || dst.height <= 0) return

        const dx = src.rect.left - dst.left
        const dy = src.rect.top - dst.top
        const sx = src.rect.width / dst.width
        const sy = src.rect.height / dst.height
        const dstRadius = window.getComputedStyle(el).borderRadius

        el.classList.add('flip-animating')
        el.style.transformOrigin = 'top left'

        const animation = el.animate(
          [
            {
              transform: `translate(${dx}px, ${dy}px) scale(${sx}, ${sy})`,
              borderRadius: src.borderRadius,
              opacity: 0.88,
            },
            {
              transform: 'translate(0, 0) scale(1, 1)',
              borderRadius: dstRadius,
              opacity: 1,
            },
          ],
          {
            duration: 420,
            easing: 'cubic-bezier(.32, .72, 0, 1)',
            fill: 'both',
          },
        )

        animation.onfinish = () => {
          el.classList.remove('flip-animating')
          el.style.removeProperty('transform-origin')
        }
      })
      sourceRects.clear()
    })
  })
}

/** 在 Vue Router 上安装 FLIP 钩子 */
export function installFlip(router: Router): void {
  router.beforeEach((_to, from, next) => {
    cancelFlipAnimations()
    if (from.matched.length > 0) {
      captureFlipSources()
    }
    next()
  })

  router.afterEach(() => {
    if (sourceRects.size === 0) return
    runFlipAnimations()
  })
}
