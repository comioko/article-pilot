<template>
  <div class="brand-section">
    <div class="brand-bg">
      <div class="brand-leaf brand-leaf-1"></div>
      <div class="brand-leaf brand-leaf-2"></div>
      <div class="brand-leaf brand-leaf-3"></div>
    </div>
    <div class="brand-content">
      <div class="brand-logo">
        <img src="@/assets/logo.png" alt="Logo" class="logo-img" />
      </div>
      <h1 class="brand-title">ArticlePilot</h1>
      <p class="brand-subtitle">AI 文章创作助手</p>
      <div class="brand-features">
        <div class="feature-item">
          <CheckCircleOutlined class="feature-check" />
          <span>灵感化的标题与大纲</span>
        </div>
        <div class="feature-item">
          <CheckCircleOutlined class="feature-check" />
          <span>流式书写高质量正文</span>
        </div>
        <div class="feature-item">
          <CheckCircleOutlined class="feature-check" />
          <span>自然配图 一键导出</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { CheckCircleOutlined } from '@ant-design/icons-vue'
</script>

<style scoped>
.brand-section {
  flex: 1;
  padding: 56px 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  background: linear-gradient(160deg, #E8D5B7 0%, #F4E9D5 50%, #FBF6EE 100%);
}

.brand-bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}

.brand-leaf {
  position: absolute;
  border-radius: 50%;
  filter: blur(40px);
  opacity: 0.7;
}

.brand-leaf-1 {
  width: 360px;
  height: 360px;
  top: -100px;
  right: -100px;
  background: radial-gradient(circle, rgba(212, 165, 116, 0.55) 0%, transparent 70%);
}

.brand-leaf-2 {
  width: 280px;
  height: 280px;
  bottom: -80px;
  left: -60px;
  background: radial-gradient(circle, rgba(184, 153, 104, 0.40) 0%, transparent 70%);
}

.brand-leaf-3 {
  width: 200px;
  height: 200px;
  top: 40%;
  left: 30%;
  background: radial-gradient(circle, rgba(244, 233, 213, 0.65) 0%, transparent 70%);
}

.brand-content {
  position: relative;
  z-index: 1;
  text-align: center;
  color: var(--color-text);
  max-width: 360px;
}

.brand-logo {
  margin-bottom: 32px;
}

.logo-img {
  width: 76px;
  height: 76px;
  object-fit: cover;
  background: rgba(255, 252, 247, 0.95);
  border-radius: 50%;
  padding: 5px;
  box-shadow: var(--shadow-md);
}

.brand-title {
  font-family: var(--font-display);
  font-size: 36px;
  font-weight: 600;
  margin: 0 0 14px;
  letter-spacing: -0.005em;
  color: var(--color-text);
}

.brand-subtitle {
  font-size: 15px;
  color: var(--color-text-secondary);
  margin: 0 0 40px;
  line-height: 1.7;
  letter-spacing: 0.02em;
}

.brand-features {
  text-align: left;
  background: rgba(255, 252, 247, 0.55);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-lg);
  padding: 24px 26px;
  backdrop-filter: var(--glass-blur-light);
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
  font-size: 14px;
  color: var(--color-text-secondary);
  letter-spacing: 0.02em;
}

.feature-item:last-child {
  margin-bottom: 0;
}

.feature-check {
  font-size: 16px;
  color: var(--color-accent);
}

@media (max-width: 768px) {
  .brand-section {
    padding: 40px 28px;
  }
  .brand-title {
    font-size: 28px;
  }
  .brand-features {
    display: none;
  }
}
</style>