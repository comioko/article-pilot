<template>
  <a-layout-footer class="footer">
    <!-- 顶部波浪分隔：让 footer 像从波浪中浮起 -->
    <div class="footer-wave">
      <WaveDivider position="top" color="#F4E9D5" :variant="1" height="60px" />
    </div>
    <div class="footer-content">
      <div class="footer-grid">
        <div class="footer-brand">
          <p class="footer-mark">ArticlePilot</p>
          <p class="footer-tagline">一处安静而专注的 AI 写作空间<br />让文字自然生长</p>
        </div>
        <div class="footer-meta">
          <p class="footer-line">
            <a
              href="https://github.com/comioko/"
              target="_blank"
              rel="noopener noreferrer"
              class="author-link"
            >
              comioko
            </a>
          </p>
          <p class="footer-meta-text">AI 写作工坊 · Crafted with care</p>
        </div>
      </div>
    </div>
  </a-layout-footer>
</template>

<script setup lang="ts">
import WaveDivider from './WaveDivider.vue'
</script>

<style scoped>
.footer {
  position: relative;
  background: var(--color-background-secondary);
  text-align: center;
  padding: 0;
  margin-top: 0;
  border-top: none;
}

.footer-wave {
  position: relative;
  width: 100%;
  height: 60px;
  line-height: 0;
  margin-bottom: 0;
}

.footer-content {
  padding: 56px 24px 48px;
}

.footer-grid {
  max-width: var(--container-base);
  margin: 0 auto;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 40px;
  text-align: left;
}

.footer-brand {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.footer-mark {
  font-family: var(--font-display);
  font-size: 22px;
  font-weight: 600;
  color: var(--color-text);
  margin: 0;
  letter-spacing: -0.005em;
}

.footer-tagline {
  margin: 0;
  font-size: 13px;
  color: var(--color-text-secondary);
  line-height: 1.7;
  letter-spacing: 0.02em;
}

.footer-meta {
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  gap: 8px;
  text-align: right;
}

.footer-line {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: 13px;
  letter-spacing: 0.02em;
}

.author-link {
  color: var(--color-text-secondary);
  transition: color var(--transition-fast);
  border-bottom: 1px solid transparent;
  background-image: linear-gradient(currentColor, currentColor);
  background-size: 0 1px;
  background-position: 0 100%;
  background-repeat: no-repeat;
  transition: background-size var(--transition-normal), color var(--transition-fast), border-color var(--transition-fast);
}

.author-link:hover {
  color: var(--color-primary-dark);
  background-size: 100% 1px;
}

.footer-meta-text {
  margin: 0;
  font-family: var(--font-handwritten);
  font-size: 18px;
  color: var(--color-accent);
  transform: rotate(-1deg);
  display: inline-block;
}

@media (max-width: 640px) {
  .footer-grid {
    grid-template-columns: 1fr;
    gap: 28px;
    text-align: center;
  }
  .footer-meta {
    text-align: center;
    align-items: center;
  }
  .footer-meta-text { font-size: 16px; }
}
</style>
