import { ref, watch, onUnmounted, type Ref } from 'vue'

interface UseTypewriterOptions {
  /** 每字间隔（ms） */
  speed?: number
  /** 全部文本前的延迟（ms） */
  startDelay?: number
  /** 是否循环 */
  loop?: boolean
  /** 循环间隔（ms） */
  loopDelay?: number
}

export function useTypewriter(
  fullText: Ref<string>,
  options: UseTypewriterOptions = {},
): { display: Ref<string>; start: () => void; stop: () => void } {
  const { speed = 45, startDelay = 0, loop = false, loopDelay = 2400 } = options
  const display = ref('')
  let timer: number | null = null
  let loopTimer: number | null = null
  let stopped = false

  function clearTimers() {
    if (timer) {
      clearTimeout(timer)
      timer = null
    }
    if (loopTimer) {
      clearTimeout(loopTimer)
      loopTimer = null
    }
  }

  function start() {
    if (typeof window === 'undefined') return
    clearTimers()
    stopped = false

    const prefersReduced = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches
    if (prefersReduced) {
      display.value = fullText.value
      return
    }

    const chars = Array.from(fullText.value)
    let i = 0

    const step = () => {
      if (stopped) return
      if (i <= chars.length) {
        display.value = chars.slice(0, i).join('')
        i += 1
        if (i <= chars.length) {
          timer = window.setTimeout(step, speed)
        } else if (loop) {
          loopTimer = window.setTimeout(() => {
            if (stopped) return
            i = 0
            display.value = ''
            step()
          }, loopDelay)
        }
      }
    }

    if (startDelay > 0) {
      timer = window.setTimeout(step, startDelay)
    } else {
      step()
    }
  }

  function stop() {
    stopped = true
    clearTimers()
  }

  watch(fullText, () => {
    stop()
    start()
  })

  onUnmounted(() => stop())

  return { display, start, stop }
}