<template>
  <div class="deco-bg" aria-hidden="true">
    <!-- 蒸汽装饰（顶部弧形曲线） -->
    <svg class="deco-steam" viewBox="0 0 400 200" preserveAspectRatio="none">
      <path
        d="M0,160 C80,120 160,180 240,140 C320,100 360,140 400,120 L400,200 L0,200 Z"
        fill="currentColor"
        opacity="0.06"
      />
    </svg>

    <!-- 飘浮咖啡豆 -->
    <div class="bean bean-1">
      <svg viewBox="0 0 60 80" fill="none">
        <ellipse cx="30" cy="40" rx="22" ry="34" fill="currentColor" opacity="0.55" />
        <path d="M30 8 Q24 40 30 72" stroke="rgba(255,255,255,0.4)" stroke-width="2" fill="none" />
      </svg>
    </div>
    <div class="bean bean-2">
      <svg viewBox="0 0 60 80" fill="none">
        <ellipse cx="30" cy="40" rx="22" ry="34" fill="currentColor" opacity="0.5" />
        <path d="M30 8 Q36 40 30 72" stroke="rgba(255,255,255,0.4)" stroke-width="2" fill="none" />
      </svg>
    </div>
    <div class="bean bean-3">
      <svg viewBox="0 0 60 80" fill="none">
        <ellipse cx="30" cy="40" rx="22" ry="34" fill="currentColor" opacity="0.45" />
        <path d="M30 8 Q24 40 30 72" stroke="rgba(255,255,255,0.4)" stroke-width="2" fill="none" />
      </svg>
    </div>
    <div class="bean bean-4">
      <svg viewBox="0 0 60 80" fill="none">
        <ellipse cx="30" cy="40" rx="22" ry="34" fill="currentColor" opacity="0.5" />
        <path d="M30 8 Q36 40 30 72" stroke="rgba(255,255,255,0.4)" stroke-width="2" fill="none" />
      </svg>
    </div>
    <div class="bean bean-5">
      <svg viewBox="0 0 60 80" fill="none">
        <ellipse cx="30" cy="40" rx="22" ry="34" fill="currentColor" opacity="0.4" />
        <path d="M30 8 Q24 40 30 72" stroke="rgba(255,255,255,0.4)" stroke-width="2" fill="none" />
      </svg>
    </div>
    <div class="bean bean-6">
      <svg viewBox="0 0 60 80" fill="none">
        <ellipse cx="30" cy="40" rx="22" ry="34" fill="currentColor" opacity="0.5" />
        <path d="M30 8 Q36 40 30 72" stroke="rgba(255,255,255,0.4)" stroke-width="2" fill="none" />
      </svg>
    </div>
    <div class="bean bean-7">
      <svg viewBox="0 0 60 80" fill="none">
        <ellipse cx="30" cy="40" rx="22" ry="34" fill="currentColor" opacity="0.4" />
        <path d="M30 8 Q24 40 30 72" stroke="rgba(255,255,255,0.4)" stroke-width="2" fill="none" />
      </svg>
    </div>
    <div class="bean bean-8">
      <svg viewBox="0 0 60 80" fill="none">
        <ellipse cx="30" cy="40" rx="22" ry="34" fill="currentColor" opacity="0.45" />
        <path d="M30 8 Q36 40 30 72" stroke="rgba(255,255,255,0.4)" stroke-width="2" fill="none" />
      </svg>
    </div>
    <div class="bean bean-9">
      <svg viewBox="0 0 60 80" fill="none">
        <ellipse cx="30" cy="40" rx="22" ry="34" fill="currentColor" opacity="0.5" />
        <path d="M30 8 Q24 40 30 72" stroke="rgba(255,255,255,0.4)" stroke-width="2" fill="none" />
      </svg>
    </div>
    <div class="bean bean-10">
      <svg viewBox="0 0 60 80" fill="none">
        <ellipse cx="30" cy="40" rx="22" ry="34" fill="currentColor" opacity="0.4" />
        <path d="M30 8 Q36 40 30 72" stroke="rgba(255,255,255,0.4)" stroke-width="2" fill="none" />
      </svg>
    </div>

    <!-- 蒸汽气泡（细线条小圆） -->
    <span class="bubble bubble-1"></span>
    <span class="bubble bubble-2"></span>
    <span class="bubble bubble-3"></span>
    <span class="bubble bubble-4"></span>
    <span class="bubble bubble-5"></span>
    <span class="bubble bubble-6"></span>
  </div>
</template>

<script setup lang="ts">
interface Props {
  /** 装饰色调（颜色作用于 bean 的 currentColor） */
  tone?: 'primary' | 'accent' | 'caramel'
}

withDefaults(defineProps<Props>(), { tone: 'primary' })
</script>

<style scoped>
.deco-bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
  z-index: 0;
  color: var(--color-mocha);
}

.deco-steam {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 200px;
  color: var(--color-mocha);
}

/* 咖啡豆 */
.bean {
  position: absolute;
  width: 28px;
  height: 36px;
  opacity: 0.85;
  color: var(--color-mocha);
}

.bean svg {
  width: 100%;
  height: 100%;
}

.bean-1 { top: 18%;  left: 8%;   animation: drift 16s ease-in-out infinite; }
.bean-2 { top: 38%;  right: 10%; width: 22px; height: 30px; animation: drift 20s ease-in-out infinite reverse; animation-delay: -3s; }
.bean-3 { top: 65%;  left: 14%;  width: 18px; height: 24px; opacity: 0.7; animation: drift 22s ease-in-out infinite; animation-delay: -7s; }
.bean-4 { top: 12%;  right: 22%; width: 24px; height: 32px; animation: drift 18s ease-in-out infinite reverse; animation-delay: -10s; }
.bean-5 { top: 78%;  right: 18%; width: 16px; height: 22px; opacity: 0.6; animation: drift 24s ease-in-out infinite; animation-delay: -5s; }
.bean-6 { top: 28%;  left: 28%;  width: 20px; height: 28px; opacity: 0.65; animation: drift 19s ease-in-out infinite reverse; animation-delay: -12s; }
.bean-7 { top: 52%;  left: 6%;   width: 14px; height: 20px; opacity: 0.55; animation: drift 26s ease-in-out infinite; animation-delay: -8s; }
.bean-8 { top: 8%;   left: 48%;  width: 18px; height: 24px; opacity: 0.6; animation: drift 21s ease-in-out infinite reverse; animation-delay: -4s; }
.bean-9 { top: 88%;  left: 36%;  width: 22px; height: 30px; opacity: 0.55; animation: drift 23s ease-in-out infinite; animation-delay: -15s; }
.bean-10 { top: 44%; right: 32%; width: 16px; height: 22px; opacity: 0.6; animation: drift 25s ease-in-out infinite reverse; animation-delay: -6s; }

/* 气泡 */
.bubble {
  position: absolute;
  border-radius: 50%;
  border: 1px solid var(--color-caramel);
  opacity: 0.35;
}

.bubble-1 {
  width: 18px; height: 18px;
  top: 22%; left: 28%;
  animation: float 10s ease-in-out infinite;
}
.bubble-2 {
  width: 12px; height: 12px;
  top: 55%; right: 28%;
  animation: float 12s ease-in-out infinite reverse;
  animation-delay: -2s;
}
.bubble-3 {
  width: 8px; height: 8px;
  top: 40%; left: 42%;
  animation: float 14s ease-in-out infinite;
  animation-delay: -5s;
}
.bubble-4 {
  width: 14px; height: 14px;
  top: 72%; right: 40%;
  animation: float 16s ease-in-out infinite reverse;
  animation-delay: -8s;
}
.bubble-5 {
  width: 10px; height: 10px;
  top: 32%; right: 8%;
  animation: float 11s ease-in-out infinite;
  animation-delay: -3s;
}
.bubble-6 {
  width: 6px; height: 6px;
  top: 62%; left: 22%;
  animation: float 13s ease-in-out infinite reverse;
  animation-delay: -7s;
}

@keyframes drift {
  0%, 100% { transform: translate(0, 0) rotate(0deg); }
  25%      { transform: translate(20px, -30px) rotate(8deg); }
  50%      { transform: translate(-15px, 25px) rotate(-6deg); }
  75%      { transform: translate(10px, 15px) rotate(4deg); }
}

@keyframes float {
  0%, 100% { transform: translateY(0) translateX(0); opacity: 0.35; }
  50%      { transform: translateY(-20px) translateX(6px); opacity: 0.6; }
}

@media (prefers-reduced-motion: reduce) {
  .bean, .bubble {
    animation: none !important;
  }
}
</style>