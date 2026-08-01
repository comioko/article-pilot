<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useLoginUserStore } from '@/stores/loginUser'
import { listArticle } from '@/api/articleController'
import dayjs from 'dayjs'
import {
  RocketOutlined,
  FileTextOutlined,
  OrderedListOutlined,
  EditOutlined,
  PictureOutlined,
  ThunderboltOutlined,
  ClockCircleOutlined,
  RightOutlined,
  ArrowRightOutlined
} from '@ant-design/icons-vue'
import { useReveal } from '@/composables/useReveal'
import { useTypewriter } from '@/composables/useTypewriter'
import { useParallax } from '@/composables/useParallax'
import DecorativeBg from '@/components/DecorativeBg.vue'
import WaveDivider from '@/components/WaveDivider.vue'

const router = useRouter()
const loginUserStore = useLoginUserStore()

// 输入框
const topic = ref('')

// 最近文章
const recentArticles = ref<API.ArticleVO[]>([])
const loadingArticles = ref(false)

const goToCreate = () => {
  if (topic.value.trim()) {
    router.push({ path: '/create', query: { topic: topic.value } })
  } else {
    router.push('/create')
  }
}

const goToList = () => {
  router.push('/article/list')
}

const viewArticle = (article: API.ArticleVO) => {
  router.push(`/article/${article.taskId}`)
}

// 加载最近文章
const loadRecentArticles = async () => {
  if (!loginUserStore.loginUser.id) return

  loadingArticles.value = true
  try {
    const res = await listArticle({ pageNum: 1, pageSize: 6 })
    recentArticles.value = res.data.data?.records || []
  } catch (error) {
    console.error('加载文章失败:', error)
  } finally {
    loadingArticles.value = false
  }
}

// 格式化时间
const formatTime = (time: string | undefined) => {
  if (!time) return '--'
  return dayjs(time).format('MM-DD HH:mm')
}

// 功能卡片数据
const features = [
  {
    icon: FileTextOutlined,
    title: '灵感化标题',
    description: '由 AI 捕捉选题脉络，凝练为打动人心的开场语',
  },
  {
    icon: OrderedListOutlined,
    title: '结构化大纲',
    description: '为每篇文章梳理呼吸般的章节，让逻辑自然延展',
  },
  {
    icon: EditOutlined,
    title: '流式书写',
    description: '文字如涓流般逐段展开，让创作过程安静而专注',
  },
  {
    icon: PictureOutlined,
    title: '自然配图',
    description: '为内容匹配恰到好处的画面，让图文彼此呼应',
  },
  {
    icon: ThunderboltOutlined,
    title: '轻盈高效',
    description: '数分钟即可完成全文初稿，将时间留给真正的思考',
  },
  {
    icon: ClockCircleOutlined,
    title: '历史回溯',
    description: '所有创作安然归档，随时回到任何一次灵光闪现',
  }
]

// 热门主题快捷填充
const quickTopics = [
  '周末阅读札记',
  '一杯咖啡的时光',
  '深夜的小情绪',
  '远程工作的专注感',
  '春天的城市角落',
]

function fillTopic(idea: string) {
  topic.value = idea
}

// ===== 动效 =====

// Hero 大标题逐字
const heroTitle = ref('Got a Spark? Let\'s Brew It.')
const { display: titleDisplay, start: startTypewriter } = useTypewriter(heroTitle, {
  speed: 60,
  startDelay: 200,
  loop: false,
})

// Hero / 各板块 reveal refs（独立 ref，不共享）
const heroSubtitleReveal = useReveal<HTMLElement>({ threshold: 0.2 })
const heroInputReveal = useReveal<HTMLElement>({ threshold: 0.2 })
const heroTipsReveal = useReveal<HTMLElement>({ threshold: 0.2 })
const featuresHeaderReveal = useReveal<HTMLElement>({ threshold: 0.15 })
const featuresGridReveal = useReveal<HTMLElement>({ threshold: 0.15 })
const articlesHeaderReveal = useReveal<HTMLElement>({ threshold: 0.1 })
const articlesGridReveal = useReveal<HTMLElement>({ threshold: 0.1 })

// Hero 视差背景
const { offset: parallaxY } = useParallax({ speed: 0.35 })
const { offset: parallaxYSteam } = useParallax({ speed: 0.15 })

onMounted(() => {
  startTypewriter()
  loadRecentArticles()
})
</script>

<template>
  <div id="homePage">
    <!-- ============== Hero Section ============== -->
    <section class="hero-section">
      <!-- 视差层 1: 远景 blob（快速） -->
      <div class="hero-bg-far" :style="{ transform: `translate3d(0, ${parallaxY}px, 0)` }" aria-hidden="true">
        <div class="blob blob-1"></div>
        <div class="blob blob-2"></div>
        <div class="blob blob-3"></div>
      </div>
      <!-- 视差层 2: 近景装饰（慢速） -->
      <div class="hero-bg-near" :style="{ transform: `translate3d(0, ${parallaxYSteam}px, 0)` }" aria-hidden="true">
        <DecorativeBg />
      </div>

      <div class="hero-container">
        <!-- 顶部装饰 ribbon -->
        <div class="hero-ribbon" aria-hidden="true">
          <span class="ribbon-line"></span>
          <span class="ribbon-text">
            <span class="ribbon-dot"></span>
            AI · 写作工坊
            <span class="ribbon-dot"></span>
          </span>
          <span class="ribbon-line"></span>
        </div>

        <span class="eyebrow-handwritten">Crafted with care</span>

        <h1 class="hero-title">
          <span class="title-text">{{ titleDisplay }}<span class="cursor">|</span></span>
        </h1>

        <p
          ref="heroSubtitleReveal.elRef"
          :class="['hero-subtitle', { 'is-revealed': heroSubtitleReveal.isRevealed.value }]"
        >
          一处安静而专注的 AI 写作空间<br />
          以更慢的节奏，创作更深的文字
        </p>

        <!-- 核心输入框（关键交互元素，不参与 reveal 隐藏） -->
        <div class="input-wrapper">
          <a-input
            v-model:value="topic"
            placeholder="描述你想写的主题，例如：秋日午后的沉思"
            size="large"
            class="topic-input"
            @pressEnter="goToCreate"
          >
            <template #prefix>
              <EditOutlined class="input-icon" />
            </template>
          </a-input>
          <button class="cta-btn" @click="goToCreate">
            <RocketOutlined />
            <span>开始冲泡</span>
            <ArrowRightOutlined class="cta-arrow" />
          </button>
        </div>

        <p class="hero-tips">
          随笔 · 工作札记 · 演讲底稿 · 生活观察 · 深度报告
        </p>

        <!-- 热门主题 chips：点击可填入输入框 -->
        <div class="hero-chips">
          <span class="chips-label">热门灵感：</span>
          <button
            v-for="idea in quickTopics"
            :key="idea"
            type="button"
            class="topic-chip"
            @click="fillTopic(idea)"
          >
            {{ idea }}
          </button>
        </div>

        </div>

      <!-- 装饰性波浪分隔，让 Hero 自然过渡到下方 -->
      <WaveDivider position="bottom" color="#FFFCF7" :variant="1" height="64px" />
    </section>

    <!-- ============== Features Section ============== -->
    <section id="features" class="features-section">
      <div class="container">
        <div
          ref="featuresHeaderReveal.elRef"
          :class="['section-header reveal-fade-up', { 'is-revealed': featuresHeaderReveal.isRevealed.value }]"
        >
          <span class="eyebrow">What's Brewing</span>
          <h2 class="section-title">为深度写作者而生的 AI 工具</h2>
          <p class="section-subtitle">安静地辅助，让创作回到它本来的样子</p>
        </div>
        <div
          ref="featuresGridReveal.elRef"
          :class="['features-grid stagger-children', { 'is-revealed': featuresGridReveal.isRevealed.value }]"
        >
          <div
            v-for="(feature, index) in features"
            :key="index"
            class="feature-card"
          >
            <div class="feature-icon-wrapper">
              <component :is="feature.icon" class="feature-icon" />
            </div>
            <div class="feature-content">
              <h3 class="feature-title">{{ feature.title }}</h3>
              <p class="feature-description">{{ feature.description }}</p>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- ============== Quote / CTA Section ============== -->
    <section class="quote-section">
      <div class="container quote-container">
        <div class="quote-inner">
          <span class="quote-mark quote-mark-left">"</span>
          <p class="quote-text">
            写作不是机器的对抗，<br />
            是与一杯文字的<span class="quote-italic">对坐长谈</span>。
          </p>
          <p class="quote-by">— by ArticlePilot</p>
          <button class="cta-btn cta-btn-large" @click="goToCreate">
            <RocketOutlined />
            <span>开始你的第一杯</span>
            <ArrowRightOutlined class="cta-arrow" />
          </button>
          <p class="quote-hint">无需注册即可体验 · 一杯文字的时间</p>
        </div>
      </div>
    </section>

    <!-- ============== Recent Articles Section ============== -->
    <section
      v-if="loginUserStore.loginUser.id && recentArticles.length > 0"
      class="articles-section"
    >
      <div class="container">
        <div
          ref="articlesHeaderReveal.elRef"
          :class="['section-header-row reveal-fade-up', { 'is-revealed': articlesHeaderReveal.isRevealed.value }]"
        >
          <div>
            <span class="eyebrow">Recent Brews</span>
            <h2 class="section-title-sm">你的写作痕迹</h2>
            <p class="section-subtitle-sm">安静收藏，随时回访</p>
          </div>
          <button class="link-btn" @click="goToList">
            查看全部
            <RightOutlined />
          </button>
        </div>

        <a-spin :spinning="loadingArticles">
          <div
            ref="articlesGridReveal.elRef"
            :class="['articles-grid stagger-children', { 'is-revealed': articlesGridReveal.isRevealed.value }]"
          >
            <article
              v-for="article in recentArticles"
              :key="article.id"
              class="article-card"
              @click="viewArticle(article)"
            >
              <div class="article-cover">
                <img
                  v-if="article.coverImage"
                  :src="article.coverImage"
                  :alt="article.mainTitle"
                />
                <div v-else class="cover-placeholder">
                  <FileTextOutlined />
                </div>
              </div>
              <div class="article-info">
                <h4 class="article-title">{{ article.mainTitle || article.topic }}</h4>
                <div class="article-meta">
                  <span class="article-time">
                    <ClockCircleOutlined />
                    {{ formatTime(article.createTime) }}
                  </span>
                  <span :class="['article-status', `status-${article.status?.toLowerCase()}`]">
                    {{ article.status === 'COMPLETED' ? '已完成' : article.status === 'PROCESSING' ? '生成中' : '等待中' }}
                  </span>
                </div>
              </div>
            </article>
          </div>
        </a-spin>
      </div>
    </section>
  </div>
</template>

<style scoped>
#homePage {
  width: 100%;
  margin: 0;
  padding: 0;
  background: var(--color-background);
  overflow: hidden;
}

/* ============ Hero ============ */
.hero-section {
  position: relative;
  padding: 88px 24px 0;
  text-align: center;
  overflow: hidden;
  background: var(--gradient-hero);
  isolation: isolate;
}

.hero-bg-far,
.hero-bg-near {
  position: absolute;
  inset: -40px;
  pointer-events: none;
  z-index: 0;
  will-change: transform;
}

.hero-bg-near {
  z-index: 1;
}

.blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.55;
}

.blob-1 {
  width: 520px;
  height: 520px;
  top: -160px;
  right: -140px;
  background: radial-gradient(circle, rgba(212, 165, 116, 0.55) 0%, rgba(212, 165, 116, 0) 70%);
}

.blob-2 {
  width: 420px;
  height: 420px;
  bottom: -120px;
  left: -100px;
  background: radial-gradient(circle, rgba(244, 233, 213, 0.7) 0%, rgba(244, 233, 213, 0) 70%);
}

.blob-3 {
  width: 300px;
  height: 300px;
  top: 35%;
  left: 20%;
  background: radial-gradient(circle, rgba(184, 153, 104, 0.32) 0%, rgba(184, 153, 104, 0) 70%);
}

.hero-container {
  position: relative;
  z-index: 2;
  max-width: 880px;
  margin: 0 auto;
}

/* 顶部装饰 ribbon */
.hero-ribbon {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14px;
  margin-bottom: 28px;
  color: var(--color-caramel);
}

.ribbon-line {
  width: 60px;
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--color-caramel), transparent);
}

.ribbon-text {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  font-family: var(--font-sans);
  font-size: 12px;
  font-weight: 600;
  letter-spacing: var(--letter-spacing-wide);
  text-transform: uppercase;
  color: var(--color-accent-warm);
}

.ribbon-dot {
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: var(--color-accent);
}

.hero-title {
  font-family: var(--font-display);
  font-size: clamp(40px, 7vw, 84px);
  font-weight: 600;
  margin: 24px 0 28px;
  letter-spacing: -0.02em;
  line-height: 1.05;
  color: var(--color-text);
}

.title-text {
  display: inline-block;
}

.cursor {
  display: inline-block;
  margin-left: 4px;
  animation: blink 1s step-end infinite;
  color: var(--color-accent);
  font-weight: 300;
}

.hero-subtitle {
  font-size: 18px;
  margin: 0 0 56px;
  color: var(--color-text-secondary);
  font-weight: 400;
  line-height: 1.7;
  letter-spacing: 0.01em;
}

/* 核心输入框 - 胶囊 + 白光扫过 */
.input-wrapper {
  display: flex;
  gap: 12px;
  max-width: 720px;
  margin: 0 auto 24px;
  padding: 10px;
  background: var(--color-background-elevated);
  border-radius: var(--radius-pill);
  box-shadow: var(--shadow-md);
  border: 1px solid var(--color-border);
  transition: box-shadow var(--transition-normal), transform var(--transition-normal);
}

.input-wrapper:focus-within {
  box-shadow: var(--shadow-lg);
  border-color: var(--color-caramel);
  transform: translateY(-2px);
}

.topic-input {
  flex: 1;
  border: none !important;
  box-shadow: none !important;
  font-size: 16px;
  padding: 8px 18px;
  background: transparent !important;
  border-radius: var(--radius-pill) !important;
}

.topic-input :deep(.ant-input) {
  background: transparent !important;
}

.input-icon {
  color: var(--color-text-muted);
  font-size: 16px;
}

.cta-btn {
  position: relative;
  overflow: hidden;
  height: 54px;
  padding: 0 30px;
  font-size: 14px;
  font-weight: 500;
  letter-spacing: 0.06em;
  border-radius: var(--radius-pill);
  background: var(--gradient-accent);
  border: none;
  color: white;
  box-shadow: var(--shadow-accent);
  display: inline-flex;
  align-items: center;
  gap: 10px;
  white-space: nowrap;
  cursor: pointer;
  transition: all var(--transition-normal);
}

.cta-btn::after {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 60%;
  height: 100%;
  background: linear-gradient(120deg, transparent, rgba(255,255,255,0.45), transparent);
  transition: left 700ms cubic-bezier(0.25, 0.1, 0.25, 1);
}

.cta-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 16px 32px rgba(224, 123, 58, 0.36);
}

.cta-btn:hover::after {
  left: 130%;
}

.cta-arrow {
  font-size: 12px;
  transition: transform var(--transition-fast);
}

.cta-btn:hover .cta-arrow {
  transform: translateX(4px);
}

.hero-tips {
  font-size: 13px;
  color: var(--color-text-muted);
  margin: 0;
  letter-spacing: 0.06em;
}

/* 热门主题 chips */
.hero-chips {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  align-items: center;
  gap: 10px;
  margin-top: 32px;
  max-width: 760px;
  margin-left: auto;
  margin-right: auto;
}

.chips-label {
  font-family: var(--font-handwritten);
  font-size: 22px;
  color: var(--color-accent-warm);
  transform: rotate(-2deg);
  margin-right: 4px;
}

.topic-chip {
  font-family: var(--font-body);
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-secondary);
  background: rgba(255, 252, 247, 0.7);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-pill);
  padding: 8px 16px;
  cursor: pointer;
  letter-spacing: 0.01em;
  transition: all var(--transition-fast);
  backdrop-filter: var(--glass-blur-light);
}

.topic-chip:hover {
  background: var(--color-accent);
  border-color: var(--color-accent);
  color: white;
  transform: translateY(-1px);
  box-shadow: var(--shadow-accent);
}

/* Scroll-down 指示器（绝对定位到 Hero 底部） */
.scroll-indicator {
  position: absolute;
  bottom: 32px;
  left: 50%;
  transform: translateX(-50%);
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  text-decoration: none;
  color: var(--color-text-secondary);
  cursor: pointer;
  z-index: 3;
  transition: opacity var(--transition-fast);
}

.scroll-indicator:hover .scroll-text {
  color: var(--color-accent-warm);
}

.scroll-text {
  font-family: var(--font-handwritten);
  font-size: 22px;
  color: var(--color-accent-warm);
  transform: rotate(-2deg);
  transition: color var(--transition-fast);
}

.scroll-arrow {
  width: 22px;
  height: 22px;
  border-right: 1.5px solid var(--color-caramel);
  border-bottom: 1.5px solid var(--color-caramel);
  transform: rotate(45deg);
  animation: scroll-bounce 2s ease-in-out infinite;
}

@keyframes scroll-bounce {
  0%, 100% { transform: rotate(45deg) translate(0, 0); opacity: 0.6; }
  50%      { transform: rotate(45deg) translate(4px, 4px); opacity: 1; }
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

/* ============ Features ============ */
.features-section {
  position: relative;
  padding: 100px 24px 120px;
  background: var(--color-background);
  scroll-margin-top: 76px;
}

.features-section .container {
  max-width: var(--container-base);
  margin: 0 auto;
}

.section-header {
  text-align: center;
  margin-bottom: 80px;
}

.section-header .eyebrow {
  margin-bottom: 20px;
}

.section-title {
  font-family: var(--font-display);
  font-size: 46px;
  font-weight: 600;
  margin: 0 0 16px;
  color: var(--color-text);
  letter-spacing: -0.02em;
  line-height: 1.15;
}

.section-subtitle {
  font-size: 16px;
  color: var(--color-text-secondary);
  margin: 0;
  letter-spacing: 0.02em;
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 28px;
}

.feature-card {
  background: var(--color-background-elevated);
  border-radius: var(--radius-xl);
  border: 1px solid var(--color-border-light);
  padding: 36px 30px;
  display: flex;
  flex-direction: column;
  gap: 24px;
  transition: all var(--transition-normal);
  cursor: default;
}

.feature-card:hover {
  border-color: var(--color-caramel);
  box-shadow: var(--shadow-card-hover);
  transform: translateY(-4px);
}

.feature-icon-wrapper {
  width: 56px;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  flex-shrink: 0;
  background: var(--gradient-accent);
  color: white;
  transition: transform var(--transition-bounce);
}

.feature-card:hover .feature-icon-wrapper {
  transform: rotate(-8deg) scale(1.05);
}

.feature-icon {
  font-size: 22px;
}

.feature-content {
  flex: 1;
  min-width: 0;
}

.feature-title {
  font-family: var(--font-display);
  font-size: 22px;
  font-weight: 600;
  margin: 0 0 10px;
  color: var(--color-text);
  letter-spacing: -0.005em;
}

.feature-description {
  font-size: 14px;
  color: var(--color-text-secondary);
  margin: 0;
  line-height: 1.75;
  letter-spacing: 0.01em;
}

/* ============ Process ============ */
.process-section {
  position: relative;
  padding: 100px 24px 120px;
  background: var(--color-background-secondary);
  overflow: hidden;
}

/* ============ Quote / CTA ============ */
.quote-section {
  position: relative;
  padding: 120px 24px 140px;
  background: var(--color-background);
  overflow: hidden;
}

.quote-section::before {
  content: '';
  position: absolute;
  top: 40%;
  left: 50%;
  width: 720px;
  height: 720px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(212, 165, 116, 0.18) 0%, transparent 65%);
  transform: translate(-50%, -50%);
  pointer-events: none;
}

.quote-container {
  position: relative;
  z-index: 1;
  max-width: 760px;
  margin: 0 auto;
}

.quote-inner {
  text-align: center;
}

.quote-mark {
  font-family: var(--font-serif);
  font-size: 140px;
  font-weight: 500;
  line-height: 1;
  color: var(--color-caramel);
  opacity: 0.4;
  display: inline-block;
  position: absolute;
}

.quote-mark-left {
  top: -40px;
  left: -10px;
}

.quote-text {
  font-family: var(--font-serif);
  font-size: 38px;
  font-weight: 400;
  line-height: 1.5;
  color: var(--color-text);
  margin: 0 0 24px;
  letter-spacing: -0.005em;
}

.quote-italic {
  font-style: italic;
  color: var(--color-primary-dark);
  font-weight: 500;
}

.quote-by {
  font-family: var(--font-handwritten);
  font-size: 26px;
  color: var(--color-accent-warm);
  margin: 0 0 36px;
  transform: rotate(-2deg);
  display: inline-block;
}

.cta-btn-large {
  height: 56px;
  padding: 0 36px;
  font-size: 15px;
}

.quote-hint {
  font-size: 13px;
  color: var(--color-text-muted);
  margin: 16px 0 0;
  letter-spacing: 0.06em;
}

@media (max-width: 768px) {
  .quote-section { padding: 80px 20px 100px; }
  .quote-text { font-size: 26px; }
  .quote-mark { font-size: 90px; }
  .quote-mark-left { top: -20px; left: 0; }
}

/* ============ Articles ============ */
.articles-section {
  position: relative;
  padding: 80px 24px 160px;
  background: var(--color-background-secondary);
}

.articles-section .container {
  max-width: var(--container-base);
  margin: 0 auto;
}

.section-header-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 56px;
  gap: 20px;
}

.section-header-row .eyebrow {
  margin-bottom: 12px;
}

.section-title-sm {
  font-family: var(--font-display);
  font-size: 32px;
  font-weight: 600;
  margin: 12px 0 6px;
  color: var(--color-text);
  letter-spacing: -0.01em;
}

.section-subtitle-sm {
  font-size: 14px;
  color: var(--color-text-secondary);
  margin: 0;
}

.link-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: transparent;
  border: none;
  color: var(--color-accent-warm);
  font-weight: 500;
  font-size: 14px;
  letter-spacing: 0.04em;
  cursor: pointer;
  padding: 8px 0;
  transition: all var(--transition-fast);
}

.link-btn:hover {
  color: var(--color-accent-warm);
  gap: 10px;
}

.articles-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}

.article-card {
  background: var(--color-background-elevated);
  border-radius: var(--radius-xl);
  border: 1px solid var(--color-border-light);
  overflow: hidden;
  transition: all var(--transition-normal);
  cursor: pointer;
}

.article-card:hover {
  border-color: var(--color-caramel);
  box-shadow: var(--shadow-card-hover);
  transform: translateY(-4px);
}

.article-cover {
  height: 160px;
  background: var(--color-background-secondary);
  overflow: hidden;
  position: relative;
}

.article-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform var(--transition-slow);
}

.article-card:hover .article-cover img {
  transform: scale(1.06);
}

.cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  color: var(--color-caramel);
}

.article-info {
  padding: 22px 24px;
}

.article-title {
  font-family: var(--font-display);
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 16px;
  color: var(--color-text);
  line-height: 1.45;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.article-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.article-time {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--color-text-muted);
}

.article-status {
  font-size: 12px;
  padding: 3px 10px;
  border-radius: var(--radius-pill);
  font-weight: 500;
  letter-spacing: 0.02em;
}

.article-status.status-completed {
  background: rgba(107, 127, 92, 0.16);
  color: #4F5E42;
}

.article-status.status-processing {
  background: rgba(212, 165, 116, 0.22);
  color: var(--color-primary-dark);
}

.article-status.status-pending {
  background: var(--color-background-secondary);
  color: var(--color-text-secondary);
}

/* ============ Responsive ============ */
@media (max-width: 992px) {
  .hero-title { font-size: 48px; }
  .section-title { font-size: 36px; }
  .features-grid { grid-template-columns: repeat(2, 1fr); }
  .articles-grid { grid-template-columns: repeat(2, 1fr); }
}

@media (max-width: 768px) {
  .hero-section { padding: 100px 20px 120px; }
  .hero-title { font-size: 36px; }
  .hero-subtitle { font-size: 16px; }
  .hero-subtitle br { display: none; }
  .input-wrapper {
    flex-direction: column;
    padding: 12px;
    border-radius: var(--radius-xl);
  }
  .topic-input { padding: 8px 12px; }
  .cta-btn { width: 100%; justify-content: center; }
  .features-section { padding: 80px 20px; }
  .section-title { font-size: 28px; }
  .section-header { margin-bottom: 48px; }
  .features-grid { grid-template-columns: 1fr; }
  .articles-grid { grid-template-columns: 1fr; }
  .articles-section { padding: 60px 20px 100px; }
  .section-header-row {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
    margin-bottom: 40px;
  }
  .section-title-sm { font-size: 26px; }
  .eyebrow-handwritten { font-size: 26px; }
}
</style>