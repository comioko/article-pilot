import { ref, onMounted, onUnmounted, type Ref } from 'vue'

interface UseParallaxOptions {
  /** 视差速度（0=不移动，1=正常跟随，正数向下，负数反向） */
  speed?: number
  /** 是否使用 transform translateY（false 则用 backgroundPositionY） */
  useTransform?: boolean
}

export function useParallax(
  options: UseParallaxOptions = {},
): { offset: Ref<number>; onScroll: () => void } {
  const { speed = 0.3, useTransform = true } = options
  const offset = ref(0)
  let ticking = false

  function onScroll() {
    if (ticking) return
    ticking = true
    requestAnimationFrame(() => {
      offset.value = window.scrollY * speed
      ticking = false
    })
  }

  onMounted(() => {
    window.addEventListener('scroll', onScroll, { passive: true })
    onScroll()
  })

  onUnmounted(() => {
    window.removeEventListener('scroll', onScroll)
  })

  return { offset, onScroll }
}