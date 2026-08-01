<script setup lang="ts">
import { provide } from 'vue'
import BasicLayout from '@/layouts/BasicLayout.vue'
import zhCN from 'ant-design-vue/es/locale/zh_CN'

// 提供全局中文语言配置
provide('locale', zhCN)
</script>

<template>
  <a-config-provider :locale="zhCN">
    <BasicLayout />
  </a-config-provider>
</template>

<style>
/* 引入字体 - Outfit + Work Sans + Caveat 手写 */
@import url('https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700&family=Work+Sans:wght@300;400;500;600;700&family=Caveat:wght@400;500;600&display=swap');

/* 全局CSS变量 - 暖咖啡色调（Brewitty 风格） */
:root {
  /* 品牌色 · 咖啡 */
  --color-primary: #8B6F47;
  --color-primary-light: #B89968;
  --color-primary-dark: #5C4830;
  --color-accent: #E07B3A;
  --color-accent-warm: #C8632A;
  --color-mocha: #6B4F33;
  --color-caramel: #D4A574;

  --color-secondary: #3E2A1F;
  --color-secondary-light: #5C4830;

  /* 功能色 */
  --color-cta: #E07B3A;
  --color-success: #6B7F5C;
  --color-warning: #D4A574;
  --color-error: #B07263;
  --color-info: #7A8E99;

  /* 中性背景 · 奶白米白 */
  --color-background: #FBF6EE;
  --color-background-secondary: #F4E9D5;
  --color-background-tertiary: #E8D5B7;
  --color-background-elevated: #FFFCF7;
  --color-background-dark: #3E2A1F;

  /* 文字 · 暖灰棕 */
  --color-text: #3E2A1F;
  --color-text-secondary: #6B5B4D;
  --color-text-muted: #9A8B7B;
  --color-text-on-dark: #F4E9D5;

  --color-border: #E8D9C2;
  --color-border-light: #F0E5CE;
  --color-border-soft: #DCC9A8;

  /* 渐变 */
  --gradient-primary: linear-gradient(135deg, #8B6F47 0%, #5C4830 100%);
  --gradient-accent: linear-gradient(135deg, #E07B3A 0%, #C8632A 100%);
  --gradient-hero: linear-gradient(180deg, #F4E9D5 0%, #FBF6EE 100%);
  --gradient-hero-dark: linear-gradient(180deg, #5C4830 0%, #3E2A1F 100%);
  --gradient-cream: linear-gradient(180deg, #FBF6EE 0%, #F4E9D5 100%);
  --gradient-coffee: linear-gradient(135deg, #8B6F47 0%, #B89968 100%);

  /* 纯色背景 */
  --bg-primary: #8B6F47;
  --bg-primary-hover: #5C4830;
  --bg-accent: #E07B3A;
  --bg-accent-hover: #C8632A;
  --bg-dark: #3E2A1F;
  --bg-dark-hover: #5C4830;
  --bg-success: #6B7F5C;

  /* 阴影 · 暖色调 */
  --shadow-xs: 0 1px 2px rgba(62, 42, 31, 0.04);
  --shadow-sm: 0 2px 8px rgba(62, 42, 31, 0.05), 0 1px 2px rgba(62, 42, 31, 0.03);
  --shadow-md: 0 8px 24px rgba(62, 42, 31, 0.08), 0 2px 6px rgba(62, 42, 31, 0.04);
  --shadow-lg: 0 20px 50px rgba(62, 42, 31, 0.10), 0 6px 16px rgba(62, 42, 31, 0.05);
  --shadow-xl: 0 30px 70px rgba(62, 42, 31, 0.12), 0 10px 30px rgba(62, 42, 31, 0.06);
  --shadow-card: 0 2px 16px rgba(62, 42, 31, 0.05);
  --shadow-card-hover: 0 16px 40px rgba(62, 42, 31, 0.10);
  --shadow-green: 0 6px 20px rgba(139, 111, 71, 0.25);
  --shadow-accent: 0 8px 24px rgba(224, 123, 58, 0.30);
  --shadow-coffee: 0 6px 20px rgba(139, 111, 71, 0.22);

  /* 圆角 */
  --radius-xs: 4px;
  --radius-sm: 8px;
  --radius-md: 12px;
  --radius-lg: 18px;
  --radius-xl: 28px;
  --radius-2xl: 40px;
  --radius-3xl: 56px;
  --radius-pill: 999px;
  --radius-full: 9999px;
  --radius-arch: 56px 56px 0 0;

  /* 过渡 */
  --transition-fast: 220ms cubic-bezier(0.4, 0, 0.2, 1);
  --transition-normal: 400ms cubic-bezier(0.4, 0, 0.2, 1);
  --transition-slow: 600ms cubic-bezier(0.4, 0, 0.2, 1);
  --transition-soft: 500ms cubic-bezier(0.25, 0.1, 0.25, 1);
  --transition-bounce: 500ms cubic-bezier(0.34, 1.56, 0.64, 1);

  /* 毛玻璃 */
  --glass-bg: rgba(251, 246, 238, 0.78);
  --glass-bg-warm: rgba(244, 233, 213, 0.70);
  --glass-bg-dark: rgba(62, 42, 31, 0.55);
  --glass-bg-scrolled: rgba(251, 246, 238, 0.92);
  --glass-blur: blur(20px) saturate(140%);
  --glass-blur-light: blur(14px);

  /* 字体 */
  --font-display: 'Outfit', 'PingFang SC', 'Hiragino Sans GB', sans-serif;
  --font-body: 'Work Sans', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', -apple-system, BlinkMacSystemFont, sans-serif;
  --font-handwritten: 'Caveat', 'Brush Script MT', cursive;

  --letter-spacing-wide: 0.18em;
  --letter-spacing-base: 0.01em;
  --letter-spacing-tight: -0.01em;
}

/* 全局样式优化 */
* {
  box-sizing: border-box;
}

html {
  overflow-x: hidden;
  scroll-behavior: smooth;
}

body {
  margin: 0;
  padding: 0;
  font-family: var(--font-body);
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  overflow-x: hidden;
  background-color: var(--color-background);
  color: var(--color-text);
  line-height: 1.7;
  letter-spacing: var(--letter-spacing-base);
  font-weight: 400;
}

h1, h2, h3, h4, h5, h6 {
  font-family: var(--font-display);
  font-weight: 600;
  letter-spacing: var(--letter-spacing-tight);
  color: var(--color-text);
}

p {
  margin: 0 0 1em;
}

/* 全局链接样式 - 下划线从左到右展开 */
a {
  color: var(--color-text);
  text-decoration: none;
  transition: color var(--transition-fast);
  background-image: linear-gradient(currentColor, currentColor);
  background-size: 0 1px;
  background-position: 0 100%;
  background-repeat: no-repeat;
  transition: background-size var(--transition-normal), color var(--transition-fast);
}

a:hover {
  color: var(--color-primary-dark);
  background-size: 100% 1px;
}

/* 全局按钮增强 - 圆角胶囊 */
.ant-btn {
  transition: all var(--transition-normal);
  font-weight: 500;
  border-radius: var(--radius-pill);
  letter-spacing: 0.04em;
  border-color: var(--color-border);
}

.ant-btn-default {
  background: var(--color-background-elevated);
  color: var(--color-text);
  border-color: var(--color-border);
}

.ant-btn-default:hover {
  background: var(--color-background-secondary);
  border-color: var(--color-border-soft);
  color: var(--color-text);
}

.ant-btn-primary {
  background: var(--gradient-accent);
  border-color: var(--bg-accent);
  box-shadow: var(--shadow-accent);
}

.ant-btn-primary:not(:disabled):hover {
  background: var(--gradient-accent);
  border-color: var(--bg-accent-hover);
  box-shadow: 0 12px 28px rgba(224, 123, 58, 0.32);
  transform: translateY(-1px);
}

/* 全局卡片增强 */
.ant-card {
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border);
  background: var(--color-background-elevated);
  box-shadow: var(--shadow-card);
  transition: all var(--transition-normal);
}

.ant-card-hoverable:hover {
  box-shadow: var(--shadow-card-hover);
  transform: translateY(-3px);
  border-color: var(--color-border-soft);
}

/* 可点击元素统一cursor */
.clickable,
[role="button"],
button,
.ant-btn,
.ant-card.hoverable {
  cursor: pointer;
}

/* 选中状态 */
::selection {
  background: rgba(224, 123, 58, 0.22);
  color: var(--color-text);
}

/* Ant Design 菜单覆盖 */
.ant-menu-horizontal > .ant-menu-item:hover::after,
.ant-menu-horizontal > .ant-menu-item-active::after,
.ant-menu-horizontal > .ant-menu-item-selected::after {
  border-bottom-color: var(--color-accent) !important;
}

.ant-menu-horizontal > .ant-menu-item:hover,
.ant-menu-horizontal > .ant-menu-item-active {
  color: var(--color-text) !important;
}

/* Ant Design 按钮 disabled 状态 */
.ant-btn:disabled {
  background: var(--color-background-tertiary) !important;
  color: var(--color-text-muted) !important;
  border-color: var(--color-border) !important;
  box-shadow: none !important;
}

.ant-btn-primary:disabled {
  background: var(--color-border) !important;
  color: var(--color-text-muted) !important;
}

/* Steps 组件样式覆盖 */
.ant-steps .ant-steps-item-process .ant-steps-item-icon {
  background: var(--color-accent);
  border-color: var(--color-accent);
}

.ant-steps .ant-steps-item-finish .ant-steps-item-icon {
  border-color: var(--color-accent);
}

.ant-steps .ant-steps-item-finish .ant-steps-item-icon > .ant-steps-icon {
  color: var(--color-accent);
}

.ant-steps .ant-steps-item-finish > .ant-steps-item-container > .ant-steps-item-tail::after {
  background-color: var(--color-accent);
}

/* 输入框聚焦状态 - 暖橙外发光 */
.ant-input:focus,
.ant-input-focused,
.ant-input-affix-wrapper:focus,
.ant-input-affix-wrapper-focused {
  border-color: var(--color-caramel) !important;
  box-shadow: 0 0 0 3px rgba(224, 123, 58, 0.12) !important;
}

.ant-input:hover {
  border-color: var(--color-caramel);
}

.ant-input,
.ant-input-affix-wrapper {
  background-color: var(--color-background-elevated);
  border-color: var(--color-border);
  border-radius: var(--radius-md);
}

textarea.ant-input {
  background: var(--color-background-elevated);
}

.ant-input-textarea-show-count::after {
  color: var(--color-text-muted);
}

/* Progress 组件 */
.ant-progress-bg {
  background: var(--gradient-accent);
}

/* Spin 组件 */
.ant-spin-dot-item {
  background-color: var(--color-accent);
}

/* Tag 组件 - 柔和 */
.ant-tag {
  border-radius: var(--radius-pill);
  padding: 2px 12px;
  font-size: 12px;
  font-weight: 500;
  border-color: var(--color-border);
  background: var(--color-background-secondary);
  color: var(--color-text-secondary);
}

/* Divider 柔和 */
.ant-divider {
  border-color: var(--color-border-light);
}

/* Tooltip / Popover 圆角 */
.ant-tooltip-inner,
.ant-popover-inner {
  border-radius: var(--radius-md);
  background: var(--color-background-dark);
  color: var(--color-text-on-dark);
}

/* Modal 圆角 */
.ant-modal-content {
  border-radius: var(--radius-xl);
  background: var(--color-background-elevated);
}

/* Drawer 圆角 */
.ant-drawer-content {
  background: var(--color-background);
}

/* Message 提示 */
.ant-message-notice-content {
  border-radius: var(--radius-md);
  background: var(--color-background-elevated);
  border: 1px solid var(--color-border);
  box-shadow: var(--shadow-md);
}

/* 滚动条 - 暖色，简洁无 border */
::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}
::-webkit-scrollbar-track {
  background: transparent;
}
::-webkit-scrollbar-thumb {
  background: var(--color-border-soft);
  border-radius: 4px;
}
::-webkit-scrollbar-thumb:hover {
  background: var(--color-caramel);
}

/* ========== 动效工具类 ========== */

/* 滚动揭示 - 起始态（默认可见，避免 IntersectionObserver 不触发导致内容消失）
   通过 `.pre-reveal` 显式开启隐藏态，进场动画完成后由 JS 移除该 class */
.reveal,
.reveal-fade-up,
.reveal-fade,
.reveal-slide-left,
.reveal-slide-right,
.reveal-scale {
  transition: opacity 800ms cubic-bezier(0.25, 0.1, 0.25, 1),
              transform 800ms cubic-bezier(0.25, 0.1, 0.25, 1);
}

.reveal.pre-reveal,
.reveal-fade-up.pre-reveal,
.reveal-fade.pre-reveal,
.reveal-slide-left.pre-reveal,
.reveal-slide-right.pre-reveal,
.reveal-scale.pre-reveal {
  opacity: 0;
  will-change: opacity, transform;
}

.reveal-fade-up.pre-reveal { transform: translateY(40px); }
.reveal-slide-left.pre-reveal { transform: translateX(-40px); }
.reveal-slide-right.pre-reveal { transform: translateX(40px); }
.reveal-scale.pre-reveal { transform: scale(0.94); }

/* 揭示后状态 */
.reveal.is-revealed,
.reveal-fade-up.is-revealed,
.reveal-fade.is-revealed,
.reveal-slide-left.is-revealed,
.reveal-slide-right.is-revealed,
.reveal-scale.is-revealed {
  opacity: 1;
  transform: translate(0, 0) scale(1);
}

/* 交错延迟 - 用于列表/网格
   默认可见；`.pre-reveal` 开启隐藏态后才会有入场动画 */
.stagger-children > * {
  transition: opacity 700ms cubic-bezier(0.25, 0.1, 0.25, 1),
              transform 700ms cubic-bezier(0.25, 0.1, 0.25, 1);
}
.stagger-children.pre-reveal > * {
  opacity: 0;
  transform: translateY(30px);
  will-change: opacity, transform;
}
.stagger-children.is-revealed > *:nth-child(1) { transition-delay: 0ms; }
.stagger-children.is-revealed > *:nth-child(2) { transition-delay: 100ms; }
.stagger-children.is-revealed > *:nth-child(3) { transition-delay: 200ms; }
.stagger-children.is-revealed > *:nth-child(4) { transition-delay: 300ms; }
.stagger-children.is-revealed > *:nth-child(5) { transition-delay: 400ms; }
.stagger-children.is-revealed > *:nth-child(6) { transition-delay: 500ms; }
.stagger-children.is-revealed > *:nth-child(7) { transition-delay: 600ms; }
.stagger-children.is-revealed > *:nth-child(8) { transition-delay: 700ms; }
.stagger-children.is-revealed > * {
  opacity: 1;
  transform: translateY(0);
}

/* Eyebrow 标签 - 手写体 */
.eyebrow-handwritten {
  font-family: var(--font-handwritten);
  font-size: 28px;
  font-weight: 500;
  color: var(--color-accent);
  line-height: 1;
  display: inline-block;
}

/* Eyebrow 标准 */
.eyebrow {
  display: inline-block;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: var(--letter-spacing-wide);
  text-transform: uppercase;
  color: var(--color-accent-warm);
  padding: 6px 16px;
  border: 1px solid var(--color-border-soft);
  border-radius: var(--radius-pill);
  background: rgba(224, 123, 58, 0.08);
}

/* 容器工具类 */
.container-narrow {
  max-width: var(--container-narrow);
  margin: 0 auto;
  padding: 0 24px;
}
.container-base {
  max-width: var(--container-base);
  margin: 0 auto;
  padding: 0 24px;
}
.container-wide {
  max-width: var(--container-wide);
  margin: 0 auto;
  padding: 0 24px;
}

/* prefers-reduced-motion 禁用动效 */
@media (prefers-reduced-motion: reduce) {
  *, *::before, *::after {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
    scroll-behavior: auto !important;
  }
  .reveal, .reveal-fade-up, .reveal-fade,
  .reveal-slide-left, .reveal-slide-right, .reveal-scale {
    opacity: 1 !important;
    transform: none !important;
  }
}
</style>
