import { onMounted, onUnmounted, ref, type Ref } from 'vue'

interface UseRevealOptions {
  threshold?: number
  rootMargin?: string
  /** 仅触发一次，触发后停止观察 */
  once?: boolean
  /** 触发后移除的 class 名（默认 'pre-reveal'） */
  preRevealClass?: string
  /** 触发后添加的 class 名（默认 'is-revealed'） */
  revealedClass?: string
}

export function useReveal<T extends HTMLElement = HTMLElement>(
  options: UseRevealOptions = {},
): { elRef: Ref<T | null>; isRevealed: Ref<boolean> } {
  const {
    threshold = 0,
    rootMargin = '0px 0px 200px 0px',
    once = true,
    preRevealClass = 'pre-reveal',
    revealedClass = 'is-revealed',
  } = options
  const elRef = ref<T | null>(null) as Ref<T | null>
  const isRevealed = ref(false)
  let observer: IntersectionObserver | null = null
  let fallbackTimer: ReturnType<typeof setTimeout> | null = null

  function reveal(target: HTMLElement) {
    target.classList.remove(preRevealClass)
    target.classList.add(revealedClass)
    isRevealed.value = true
    if (observer) {
      observer.unobserve(target)
    }
  }

  onMounted(() => {
    const target = elRef.value
    if (!target) return

    // 环境不支持 IntersectionObserver：跳过隐藏，直接显示
    if (typeof IntersectionObserver === 'undefined') {
      reveal(target)
      return
    }

    // 先添加隐藏 class，让初始态有动画准备
    target.classList.add(preRevealClass)

    // 兜底超时（1.5s）—— 即便观察器没触发也强制显示
    fallbackTimer = setTimeout(() => {
      if (!isRevealed.value) reveal(target)
    }, 1500)

    observer = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          if (entry.isIntersecting) {
            reveal(entry.target as HTMLElement)
            if (fallbackTimer) {
              clearTimeout(fallbackTimer)
              fallbackTimer = null
            }
          } else if (!once) {
            entry.target.classList.add(preRevealClass)
            entry.target.classList.remove(revealedClass)
            isRevealed.value = false
          }
        }
      },
      { threshold, rootMargin },
    )
    observer.observe(target)
  })

  onUnmounted(() => {
    if (observer) {
      observer.disconnect()
      observer = null
    }
    if (fallbackTimer) {
      clearTimeout(fallbackTimer)
      fallbackTimer = null
    }
  })

  return { elRef, isRevealed }
}