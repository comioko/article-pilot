<template>
  <div class="vip-page">
    <div class="vip-container">
      <!-- 页面头部 -->
      <div class="page-header">
        <div
          ref="headerReveal.elRef"
          :class="['header-content reveal-fade-up', { 'is-revealed': headerReveal.isRevealed.value }]"
        >
          <span class="eyebrow-handwritten">Join the Crew</span>
          <h1 class="page-title">成为会员<br /><span class="title-italic">开启无界创作</span></h1>
          <p class="page-subtitle">解锁全部高级功能，无限创作配额，一次拥有，长久陪伴</p>
        </div>
        <!-- 顶部波浪分隔 -->
        <WaveDivider position="bottom" color="#FBF6EE" :variant="2" height="80px" />
      </div>

      <!-- 主内容区：左右布局 -->
      <div class="main-section">
        <!-- 左侧：价格卡片 -->
        <div
          ref="pricingReveal.elRef"
          :class="['pricing-card reveal-slide-left', { 'is-revealed': pricingReveal.isRevealed.value }]"
        >
          <div class="pricing-badge">限时礼遇</div>
          <div class="pricing-header">
            <div class="plan-icon">
              <CrownOutlined />
            </div>
            <h2 class="plan-name">永久会员</h2>
            <p class="plan-desc">一次拥有，长久陪伴</p>
            <div class="price-display">
              <span class="currency">¥</span>
              <span ref="priceEl" class="price">{{ priceDisplay }}</span>
              <span class="period">/ 永久</span>
            </div>
            <div class="original-price">
              <span class="original-label">原价</span>
              <span class="original-value">¥299</span>
            </div>
          </div>

          <div class="pricing-divider"></div>

          <div class="pricing-features">
            <div v-for="(item, index) in pricingFeatures" :key="index" class="pricing-feature">
              <CheckCircleOutlined class="feature-check" />
              <span>{{ item }}</span>
            </div>
          </div>

          <button
            class="purchase-btn"
            :disabled="purchasing || isVip"
            @click="handlePurchase"
          >
            <ThunderboltOutlined v-if="!isVip" />
            {{ isVip ? '您已是永久会员' : purchasing ? '正在处理…' : '立即升级' }}
          </button>

          <div class="security-notice">
            <SafetyOutlined />
            <span>安全支付 · 七日无理由退款</span>
          </div>
        </div>

        <!-- 右侧：会员特权 -->
        <div class="features-section">
          <div
            ref="featuresHeaderReveal.elRef"
            :class="['features-header reveal-fade-up', { 'is-revealed': featuresHeaderReveal.isRevealed.value }]"
          >
            <GiftOutlined class="features-icon" />
            <h3 class="features-title">会员礼遇</h3>
          </div>
          <div
            ref="featuresGridReveal.elRef"
            :class="['features-grid stagger-children', { 'is-revealed': featuresGridReveal.isRevealed.value }]"
          >
            <div v-for="(feature, index) in features" :key="index" class="feature-card">
              <div class="feature-icon-wrapper">
                <component :is="feature.icon" class="feature-icon" />
              </div>
              <div class="feature-content">
                <h4 class="feature-title">{{ feature.title }}</h4>
                <p class="feature-desc">{{ feature.desc }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 常见问题 -->
      <div class="faq-section">
        <div
          ref="faqHeaderReveal.elRef"
          :class="['section-header reveal-fade-up', { 'is-revealed': faqHeaderReveal.isRevealed.value }]"
        >
          <QuestionCircleOutlined class="section-icon" />
          <h2 class="section-title">常见问题</h2>
        </div>
        <div
          ref="faqGridReveal.elRef"
          :class="['faq-grid stagger-children', { 'is-revealed': faqGridReveal.isRevealed.value }]"
        >
          <div v-for="(faq, index) in faqs" :key="index" class="faq-card">
            <h4 class="faq-question">{{ faq.question }}</h4>
            <p class="faq-answer">{{ faq.answer }}</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import {
  CheckCircleOutlined,
  CrownOutlined,
  SafetyOutlined,
  ThunderboltOutlined,
  RocketOutlined,
  PictureOutlined,
  AppstoreOutlined,
  EditOutlined,
  StarOutlined,
  GiftOutlined,
  QuestionCircleOutlined
} from '@ant-design/icons-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import { createVipPaymentSession } from '@/api/paymentController'
import { isVip as checkIsVip } from '@/utils/permission'
import { useReveal } from '@/composables/useReveal'
import { useCountUp } from '@/composables/useCountUp'
import WaveDivider from '@/components/WaveDivider.vue'

const router = useRouter()
const route = useRoute()
const loginUserStore = useLoginUserStore()
const purchasing = ref(false)

const isVip = computed(() => checkIsVip(loginUserStore.loginUser))

// 会员特权列表
const features = [
  { icon: RocketOutlined,    title: '无限创作', desc: '不限次数使用文章创作功能，告别配额限制' },
  { icon: PictureOutlined,   title: 'AI 生图',  desc: '使用 Nano Banana AI 生成独特配图' },
  { icon: AppstoreOutlined,  title: 'SVG 图表', desc: '自动生成精美的概念示意图和思维导图' },
  { icon: EditOutlined,      title: 'AI 大纲',  desc: '使用 AI 助手快速优化文章大纲' },
  { icon: StarOutlined,      title: '优先队列', desc: '享受更快的生成速度和优先服务' },
  { icon: GiftOutlined,      title: '终身有效', desc: '一次购买，永久使用，无需续费' }
]

const pricingFeatures = [
  '无限创作配额',
  '全部高级配图功能',
  'AI 大纲智能编辑',
  '优先生成队列',
  '终身有效'
]

const faqs = [
  { question: '支付后多久生效？',     answer: '支付成功后立即生效，您将立即获得永久会员权限，刷新页面即可看到变化。' },
  { question: '如何申请退款？',       answer: '购买后 7 天内，如不满意可申请退款，退款后会员权限将被取消。' },
  { question: '会员是否需要续费？',   answer: '不需要。永久会员一次购买，终身有效，无需任何续费。' },
  { question: '支付安全吗？',         answer: '我们使用 Stripe 国际支付平台，全程加密传输，安全可靠。' }
]

// ===== 动效 =====
const headerReveal = useReveal<HTMLElement>({ threshold: 0.2 })
const pricingReveal = useReveal<HTMLElement>({ threshold: 0.2 })
const featuresHeaderReveal = useReveal<HTMLElement>({ threshold: 0.2 })
const featuresGridReveal = useReveal<HTMLElement>({ threshold: 0.1 })
const faqHeaderReveal = useReveal<HTMLElement>({ threshold: 0.2 })
const faqGridReveal = useReveal<HTMLElement>({ threshold: 0.1 })

// 价格数字滚动计数
const priceTarget = ref(199)
const { display: priceDisplay, startCount: startPriceCount } = useCountUp(priceTarget, {
  start: 0,
  duration: 1400,
  decimals: 0,
})
const priceEl = ref<HTMLElement | null>(null)

onMounted(() => {
  const io = new IntersectionObserver((entries) => {
    for (const entry of entries) {
      if (entry.isIntersecting && priceEl.value) {
        startPriceCount(priceEl.value)
        io.disconnect()
        break
      }
    }
  }, { threshold: 0.5 })
  if (priceEl.value) io.observe(priceEl.value)

  // 支付结果处理
  const success = route.query.success
  const cancelled = route.query.cancelled
  if (success === 'true') {
    (async () => {
      await loginUserStore.fetchLoginUser()
      Modal.success({
        title: '支付成功！',
        content: '恭喜您成为永久会员，已解锁全部高级功能！',
        okText: '开始创作',
        onOk: () => router.push('/create'),
      })
      router.replace('/vip')
    })()
  } else if (cancelled === 'true') {
    message.info('支付已取消')
    router.replace('/vip')
  }
})

const handlePurchase = async () => {
  if (!loginUserStore.loginUser.id) {
    message.warning('请先登录')
    router.push('/user/login')
    return
  }
  if (isVip.value) {
    message.info('您已经是永久会员')
    return
  }
  purchasing.value = true
  try {
    const res = await createVipPaymentSession()
    if (res.data.code === 0 && res.data.data) {
      window.location.href = res.data.data
    } else {
      message.error(res.data.message || '创建支付失败')
    }
  } catch (error) {
    console.error('创建支付失败:', error)
    message.error('创建支付失败，请稍后重试')
  } finally {
    purchasing.value = false
  }
}
</script>

<style scoped lang="scss">
.vip-page {
  min-height: calc(100vh - 76px);
  background: var(--gradient-hero);
  padding: 0 24px 120px;
  position: relative;
  overflow: hidden;
}

.vip-container {
  max-width: var(--container-wide);
  margin: 0 auto;
  position: relative;
  z-index: 1;
}

/* 页面头部 */
.page-header {
  text-align: center;
  padding: 100px 24px 120px;
  position: relative;
  overflow: visible;
}

.header-content {
  max-width: 720px;
  margin: 0 auto;
}

.header-content .eyebrow-handwritten {
  margin-bottom: 20px;
}

.page-title {
  font-family: var(--font-display);
  font-size: 56px;
  font-weight: 600;
  margin: 0 0 18px;
  color: var(--color-text);
  letter-spacing: -0.02em;
  line-height: 1.15;
}

.title-italic {
  font-style: italic;
  color: var(--color-primary-dark);
  font-weight: 500;
}

.page-subtitle {
  font-size: 16px;
  color: var(--color-text-secondary);
  margin: 0;
  letter-spacing: 0.02em;
  line-height: 1.7;
}

/* 主内容区 */
.main-section {
  display: grid;
  grid-template-columns: 380px 1fr;
  gap: 32px;
  margin-bottom: 80px;
  align-items: start;
}

/* 价格卡片 - hover 微倾斜 */
.pricing-card {
  background: var(--color-background-elevated);
  border-radius: var(--radius-xl);
  padding: 44px 36px 36px;
  box-shadow: var(--shadow-md);
  border: 1px solid var(--color-border-soft);
  position: relative;
  transition: all var(--transition-normal);
}

.pricing-card:hover {
  box-shadow: var(--shadow-card-hover);
  transform: translateY(-4px);
}

.pricing-badge {
  position: absolute;
  top: 20px;
  right: 20px;
  background: var(--gradient-accent);
  color: white;
  padding: 5px 14px;
  border-radius: var(--radius-pill);
  font-size: 11px;
  font-weight: 600;
  letter-spacing: var(--letter-spacing-wide);
  text-transform: uppercase;
  box-shadow: var(--shadow-accent);
}

.pricing-header {
  text-align: center;
  padding-bottom: 24px;
}

.plan-icon {
  width: 56px;
  height: 56px;
  margin: 0 auto 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--gradient-accent);
  border-radius: 50%;
  color: white;

  .anticon {
    font-size: 24px;
  }
}

.plan-name {
  font-family: var(--font-display);
  font-size: 26px;
  font-weight: 600;
  margin: 0 0 6px;
  color: var(--color-text);
  letter-spacing: -0.005em;
}

.plan-desc {
  font-size: 13px;
  color: var(--color-text-secondary);
  margin: 0 0 26px;
  letter-spacing: 0.04em;
}

.price-display {
  display: flex;
  align-items: baseline;
  justify-content: center;
  margin-bottom: 6px;
  color: var(--color-accent-warm);
}

.currency {
  font-size: 20px;
  color: var(--color-accent-warm);
  margin-right: 4px;
  font-weight: 500;
  font-family: var(--font-display);
}

.price {
  font-family: var(--font-display);
  font-size: 64px;
  font-weight: 700;
  color: var(--color-accent-warm);
  line-height: 1;
  letter-spacing: -0.02em;
}

.period {
  font-size: 13px;
  color: var(--color-text-secondary);
  margin-left: 6px;
  letter-spacing: 0.04em;
}

.original-price {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 12px;
  letter-spacing: 0.04em;
}

.original-label {
  color: var(--color-text-muted);
}

.original-value {
  color: var(--color-text-muted);
  text-decoration: line-through;
}

.pricing-divider {
  height: 1px;
  background: var(--color-border-light);
  margin: 24px 0;
}

.pricing-features {
  margin-bottom: 28px;
}

.pricing-feature {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0;
  font-size: 14px;
  color: var(--color-text);
  letter-spacing: 0.01em;

  .feature-check {
    color: var(--color-accent);
    font-size: 16px;
    flex-shrink: 0;
  }
}

.purchase-btn {
  position: relative;
  overflow: hidden;
  width: 100%;
  height: 54px;
  font-size: 14px;
  font-weight: 500;
  letter-spacing: 0.08em;
  background: var(--gradient-accent) !important;
  border: none !important;
  box-shadow: var(--shadow-accent) !important;
  border-radius: var(--radius-pill) !important;
  color: white;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  transition: all var(--transition-normal);

  &::after {
    content: '';
    position: absolute;
    top: 0;
    left: -100%;
    width: 60%;
    height: 100%;
    background: linear-gradient(120deg, transparent, rgba(255,255,255,0.45), transparent);
    transition: left 700ms cubic-bezier(0.25, 0.1, 0.25, 1);
  }

  &:hover:not(:disabled) {
    transform: translateY(-2px);
    box-shadow: 0 14px 30px rgba(224, 123, 58, 0.36) !important;
  }

  &:hover:not(:disabled)::after {
    left: 130%;
  }

  &:disabled {
    background: var(--color-background-tertiary) !important;
    color: var(--color-text-muted) !important;
    box-shadow: none !important;
    cursor: not-allowed;
  }
}

.security-notice {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-top: 16px;
  font-size: 12px;
  color: var(--color-text-secondary);
  letter-spacing: 0.04em;

  .anticon {
    color: var(--color-accent);
    font-size: 13px;
  }
}

/* 会员特权 */
.features-section {
  background: var(--color-background-elevated);
  border-radius: var(--radius-xl);
  padding: 40px;
  border: 1px solid var(--color-border-light);
  box-shadow: var(--shadow-card);
}

.features-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 32px;
}

.features-icon {
  font-size: 20px;
  color: var(--color-accent-warm);
}

.features-title {
  font-family: var(--font-display);
  font-size: 24px;
  font-weight: 600;
  margin: 0;
  color: var(--color-text);
  letter-spacing: -0.005em;
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

.feature-card {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 22px;
  background: var(--color-background-secondary);
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border-light);
  transition: all var(--transition-normal);

  &:hover {
    background: rgba(212, 165, 116, 0.14);
    border-color: var(--color-caramel);
    transform: translateY(-3px);
    box-shadow: var(--shadow-card);
  }
}

.feature-icon-wrapper {
  flex-shrink: 0;
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--gradient-accent);
  border-radius: 50%;
  color: white;
  transition: transform var(--transition-bounce);
}

.feature-card:hover .feature-icon-wrapper {
  transform: rotate(-10deg) scale(1.05);
}

.feature-icon {
  font-size: 18px;
}

.feature-content {
  flex: 1;
  min-width: 0;
}

.feature-title {
  font-family: var(--font-display);
  font-size: 17px;
  font-weight: 600;
  margin: 0 0 6px;
  color: var(--color-text);
  letter-spacing: -0.005em;
}

.feature-desc {
  font-size: 13px;
  color: var(--color-text-secondary);
  margin: 0;
  line-height: 1.65;
  letter-spacing: 0.01em;
}

/* FAQ */
.faq-section {
  background: var(--color-background-elevated);
  border-radius: var(--radius-xl);
  padding: 40px;
  border: 1px solid var(--color-border-light);
  box-shadow: var(--shadow-card);
}

.section-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 28px;
}

.section-icon {
  font-size: 20px;
  color: var(--color-accent-warm);
}

.section-title {
  font-family: var(--font-display);
  font-size: 24px;
  font-weight: 600;
  margin: 0;
  color: var(--color-text);
  letter-spacing: -0.005em;
}

.faq-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

.faq-card {
  padding: 22px;
  background: var(--color-background-secondary);
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border-light);
  transition: all var(--transition-normal);

  &:hover {
    border-color: var(--color-caramel);
    transform: translateY(-2px);
  }
}

.faq-question {
  font-family: var(--font-display);
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 10px;
  color: var(--color-text);
}

.faq-answer {
  font-size: 13px;
  color: var(--color-text-secondary);
  margin: 0;
  line-height: 1.75;
}

/* 响应式 */
@media (max-width: 992px) {
  .main-section { grid-template-columns: 1fr; }
  .pricing-card { max-width: 460px; margin: 0 auto; }
  .features-grid { grid-template-columns: 1fr; }
  .faq-grid { grid-template-columns: 1fr; }
}

@media (max-width: 768px) {
  .vip-page { padding: 0 16px 100px; }
  .page-header { padding: 70px 20px 90px; }
  .page-title { font-size: 40px; }
  .page-subtitle { font-size: 14px; }
  .pricing-card { padding: 32px 24px 28px; }
  .price { font-size: 52px; }
  .features-section, .faq-section { padding: 28px 22px; }
  .eyebrow-handwritten { font-size: 26px; }
}
</style>