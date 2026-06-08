import { onMounted, type Ref } from 'vue'
import gsap from 'gsap'

/**
 * 页面入场动画 — 淡入 + 上移，子元素依次出现
 */
export function usePageEnter(
  rootRef: Ref<HTMLElement | null | undefined>,
  options?: {
    y?: number
    duration?: number
    stagger?: number
    delay?: number
  }
) {
  const { y = 12, duration = 0.4, stagger = 0.06, delay = 0 } = options ?? {}

  onMounted(() => {
    const el = rootRef.value
    if (!el) return

    gsap.from(el, {
      y, opacity: 0, duration, ease: 'power3.out', delay,
    })

    // 子卡片逐次入场
    const cards = el.querySelectorAll('.el-card, .page-card')
    if (cards.length > 1) {
      gsap.from(cards, {
        y, opacity: 0, duration, stagger, ease: 'power3.out', delay: delay + 0.05,
      })
    }
  })
}
