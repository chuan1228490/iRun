import { onMounted, type Ref } from 'vue'
import gsap from 'gsap'

/**
 * 数字递增动画 — 从 0 递增到目标值
 */
export function useCountUp(
  elRef: Ref<HTMLElement | null | undefined>,
  target: number | Ref<number>,
  options?: {
    duration?: number
    decimals?: number
    prefix?: string
    suffix?: string
  }
) {
  const { duration = 1.2, decimals = 0, prefix = '', suffix = '' } = options ?? {}

  onMounted(() => {
    const el = elRef.value
    if (!el) return
    const to = typeof target === 'number' ? target : target.value

    const obj = { val: 0 }
    gsap.to(obj, {
      val: to,
      duration,
      ease: 'power2.out',
      onUpdate() {
        el.innerText = prefix + obj.val.toFixed(decimals) + suffix
      },
    })
  })
}
