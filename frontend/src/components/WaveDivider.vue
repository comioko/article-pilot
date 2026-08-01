<template>
  <div :class="['wave-divider', `wave-${position}`, `variant-${variant}`]" aria-hidden="true">
    <svg
      :viewBox="`0 0 ${vbW} ${vbH}`"
      preserveAspectRatio="none"
      :width="'100%'"
      :height="'100%'"
    >
      <path :d="path" :fill="color" />
    </svg>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  /** 位置：top（凸起）/ bottom（凹陷/扇形顶部） */
  position?: 'top' | 'bottom'
  /** 颜色：填充色 */
  color?: string
  /** 波浪变体 */
  variant?: number
  /** 高度（CSS 长度） */
  height?: string
}

const props = withDefaults(defineProps<Props>(), {
  position: 'bottom',
  color: '#FBF6EE',
  variant: 1,
  height: '80px',
})

const vbW = 1440
const vbH = 100

const path = computed(() => {
  const variants: Record<number, string> = {
    1: 'M0,50 C240,90 480,10 720,50 C960,90 1200,10 1440,50 L1440,100 L0,100 Z',
    2: 'M0,40 C180,80 360,0 540,40 C720,80 900,0 1080,40 C1260,80 1440,30 1440,40 L1440,100 L0,100 Z',
    3: 'M0,60 C200,80 400,40 720,55 C1040,70 1240,30 1440,50 L1440,100 L0,100 Z',
    4: 'M0,30 C300,80 600,20 900,50 C1200,80 1440,40 1440,40 L1440,100 L0,100 Z',
  }
  const key = Math.max(1, Math.min(4, Math.floor(props.variant || 1)))
  return variants[key]
})
</script>

<style scoped>
.wave-divider {
  position: absolute;
  left: 0;
  right: 0;
  width: 100%;
  pointer-events: none;
  z-index: 1;
  line-height: 0;
}

.wave-divider svg {
  display: block;
}

/* 顶部波浪 - 向上凸出（如泡腾泡沫盖住上方板块） */
.wave-top {
  top: -1px;
  height: v-bind(height);
  transform: rotate(180deg);
}

/* 底部波浪 - 向下扇出（让下方板块从波浪中浮起） */
.wave-bottom {
  bottom: -1px;
  height: v-bind(height);
}
</style>