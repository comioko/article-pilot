import { ref, watch, onUnmounted, type Ref } from 'vue'

interface UseCountUpOptions {
  /** 起始值 */
  start?: number
  /** 缓动时长（ms） */
  duration?: number
  /** 保留小数位 */
  decimals?: number
  /** 缓动函数 */
  easing?: (t: number) => number
  /** 仅触发一次（默认 true） */
  once?: boolean
}

const easeOutCubic = (t: number) => 1 - Math.pow(1 - t, 3)

export function useCountUp(
  target: Ref<number>,
  options: UseCountUpOptions = {},
): { display: Ref<string>; startCount: (el?: HTMLElement | null) => void } {
  const { start = 0, duration = 1400, decimals = 0, easing = easeOutCubic, once = true } = options
  const display = ref(formatNumber(start, decimals))
  let rafId: number | null = null
  let triggered = false

  function formatNumber(n: number, d: number): string {
    if (d === 0) return Math.round(n).toLocaleString()
    return n.toFixed(d)
  }

  function startCount(el?: HTMLElement | null) {
    if (typeof window === 'undefined') return
    if (triggered && once) return

    const prefersReduced = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches
    if (prefersReduced) {
      display.value = formatNumber(target.value, decimals)
      triggered = true
      return
    }

    triggered = true
    const from = start
    const to = target.value
    const begin = performance.now()

    const tick = (now: number) => {
      const elapsed = now - begin
      const t = Math.min(1, elapsed / duration)
      const eased = easing(t)
      const current = from + (to - from) * eased
      display.value = formatNumber(current, decimals)
      if (t < 1) {
        rafId = requestAnimationFrame(tick)
      } else {
        display.value = formatNumber(to, decimals)
        rafId = null
      }
    }

    if (rafId) cancelAnimationFrame(rafId)
    rafId = requestAnimationFrame(tick)
  }

  onUnmounted(() => {
    if (rafId) cancelAnimationFrame(rafId)
  })

  return { display, startCount }
}