<template>
  <a-layout-header :class="['header', { scrolled: isScrolled }]">
    <div class="header-container">
      <div class="header-left">
        <RouterLink to="/" class="logo-link">
          <div class="logo-wrapper">
            <img src="@/assets/logo.png" alt="Logo" class="logo-img" />
            <div class="logo-text">
              <h1 class="site-title">ArticlePilot</h1>
              <span class="site-tagline">Crafted with care</span>
            </div>
          </div>
        </RouterLink>
      </div>

      <!-- 中间：导航菜单 -->
      <nav class="nav-center">
        <RouterLink
          v-for="item in menuItems"
          :key="item.key"
          :to="item.key"
          :class="['nav-item', { active: selectedKeys.includes(item.key) }]"
        >
          <component :is="item.icon" class="nav-icon" />
          <span>{{ item.label }}</span>
        </RouterLink>
      </nav>

      <!-- 右侧：用户操作区域 -->
      <div class="header-right">
        <div v-if="loginUserStore.loginUser.id" class="user-dropdown">
          <RouterLink v-if="!isVip" to="/vip" class="upgrade-vip-btn">
            <CrownOutlined />
            <span>升级会员</span>
          </RouterLink>
          <RouterLink v-else to="/vip" class="vip-badge">
            <CrownOutlined />
            <span>会员</span>
          </RouterLink>

          <a-dropdown>
            <a-space class="user-info">
              <a-avatar :src="loginUserStore.loginUser.userAvatar" :size="32" class="user-avatar" />
              <span class="user-name">
                {{ loginUserStore.loginUser.userName ?? '匿名' }}
              </span>
            </a-space>
            <template #overlay>
              <a-menu class="dropdown-menu">
                <a-menu-item v-if="isVip" key="vip-info" class="vip-info-item" @click="router.push('/vip')">
                  <CrownOutlined />
                  <span>永久会员权益</span>
                </a-menu-item>
                <a-menu-divider v-if="isVip" />
                <a-menu-item @click="doLogout" class="dropdown-item">
                  <LogoutOutlined />
                  <span>退出登录</span>
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>
        <div v-else class="header-actions">
          <RouterLink to="/user/login" class="login-text">登录</RouterLink>
          <RouterLink to="/user/register" class="login-btn">开启创作</RouterLink>
        </div>
      </div>
    </div>
  </a-layout-header>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser.ts'
import { userLogout } from '@/api/userController.ts'
import {
  LogoutOutlined,
  HomeOutlined,
  EditOutlined,
  UnorderedListOutlined,
  SettingOutlined,
  CrownOutlined,
  BarChartOutlined
} from '@ant-design/icons-vue'
import { isVip as checkIsVip } from '@/utils/permission'

const loginUserStore = useLoginUserStore()
const router = useRouter()

const selectedKeys = ref<string[]>(['/'])
router.afterEach((to) => {
  selectedKeys.value = [to.path]
})

const isVip = computed(() => checkIsVip(loginUserStore.loginUser))

const originItems = [
  { key: '/',          icon: HomeOutlined,           label: '首页' },
  { key: '/create',    icon: EditOutlined,           label: '创作' },
  { key: '/article/list', icon: UnorderedListOutlined, label: '历史' },
  { key: '/admin/userManage',   icon: SettingOutlined,    label: '管理', admin: true },
  { key: '/admin/statistics',   icon: BarChartOutlined,   label: '数据', admin: true },
]

const menuItems = computed(() => {
  return originItems.filter((item) => {
    if (item.admin) {
      const loginUser = loginUserStore.loginUser
      return loginUser && loginUser.userRole === 'admin'
    }
    return true
  })
})

// ===== 滚动状态 =====
const isScrolled = ref(false)
function onScroll() {
  isScrolled.value = window.scrollY > 12
}
onMounted(() => {
  window.addEventListener('scroll', onScroll, { passive: true })
  onScroll()
})
onUnmounted(() => {
  window.removeEventListener('scroll', onScroll)
})

const doLogout = async () => {
  const res = await userLogout()
  if (res.data.code === 0) {
    loginUserStore.setLoginUser({ userName: '未登录' })
    message.success('退出登录成功')
    await router.push('/user/login')
  } else {
    message.error('退出登录失败，' + res.data.message)
  }
}
</script>

<style scoped>
.header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: var(--glass-bg);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
  padding: 0;
  height: 76px;
  line-height: 76px;
  border-bottom: 1px solid transparent;
  transition: background var(--transition-normal), border-color var(--transition-normal), box-shadow var(--transition-normal);
  overflow: hidden;
}

.header.scrolled {
  background: var(--glass-bg-scrolled);
  border-bottom-color: var(--color-border-light);
  box-shadow: 0 6px 20px rgba(62, 42, 31, 0.06);
}

.header-container {
  max-width: var(--container-wide);
  margin: 0 auto;
  padding: 0 32px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 100%;
}

.header-left {
  display: flex;
  align-items: center;
}

.logo-link {
  display: block;
  transition: opacity var(--transition-fast);
}

.logo-link:hover {
  opacity: 0.85;
}

.logo-wrapper {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo-img {
  width: 36px;
  height: 36px;
  object-fit: contain;
  border-radius: 50%;
}

.logo-text {
  display: flex;
  flex-direction: column;
  line-height: 1.1;
  gap: 2px;
}

.site-title {
  margin: 0;
  font-family: var(--font-display);
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text);
  white-space: nowrap;
  letter-spacing: -0.005em;
  line-height: 1;
}

.site-tagline {
  font-family: var(--font-handwritten);
  font-size: 16px;
  font-weight: 500;
  color: var(--color-accent);
  line-height: 1;
  transform: rotate(-1deg);
}

/* 导航菜单 */
.nav-center {
  display: flex;
  align-items: center;
  gap: 4px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 18px;
  border-radius: var(--radius-pill);
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-secondary);
  transition: all var(--transition-fast);
  text-decoration: none;
  letter-spacing: 0.02em;
  line-height: 1;
  background-image: none !important;
}

.nav-item:hover {
  color: var(--color-primary-dark);
  background: var(--color-background-secondary);
}

.nav-item.active {
  color: var(--color-accent-warm);
  background: rgba(224, 123, 58, 0.10);
}

.nav-icon {
  font-size: 15px;
}

/* 用户区域 */
.header-right {
  display: flex;
  align-items: center;
  gap: 14px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-dropdown {
  cursor: pointer;
  height: 76px;
  display: flex;
  align-items: center;
  gap: 14px;
}

.upgrade-vip-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 16px;
  border-radius: var(--radius-pill);
  font-size: 13px;
  font-weight: 500;
  background: transparent;
  color: var(--color-accent-warm);
  text-decoration: none;
  border: 1px solid var(--color-border-soft);
  transition: all var(--transition-fast);
  line-height: 1;

  &:hover {
    background: rgba(224, 123, 58, 0.08);
    border-color: var(--color-caramel);
    color: var(--color-accent-warm);
  }

  .anticon { font-size: 13px; }
}

.vip-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 14px;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: var(--letter-spacing-wide);
  text-transform: uppercase;
  color: white;
  background: var(--gradient-accent);
  border-radius: var(--radius-pill);
  text-decoration: none;
  transition: all var(--transition-fast);
  line-height: 1;

  &:hover {
    color: white;
    box-shadow: var(--shadow-accent);
  }

  .anticon { font-size: 12px; }
}

.user-info {
  padding: 4px 12px 4px 4px;
  border-radius: var(--radius-pill);
  transition: all var(--transition-fast);
  display: flex;
  align-items: center;
  line-height: 1;
}

.user-info:hover {
  background: var(--color-background-secondary);
}

.user-avatar {
  border: none;
}

.user-name {
  font-weight: 500;
  color: var(--color-text);
  font-size: 14px;
  letter-spacing: 0.01em;
}

.login-text {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text);
  padding: 8px 4px;
  transition: color var(--transition-fast);
  background-image: none !important;
}

.login-text:hover {
  color: var(--color-accent-warm);
}

.login-btn {
  position: relative;
  overflow: hidden;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 42px;
  padding: 0 24px;
  border-radius: var(--radius-pill);
  font-size: 13px;
  font-weight: 500;
  letter-spacing: 0.06em;
  color: white;
  background: var(--gradient-accent);
  border: none;
  box-shadow: var(--shadow-accent);
  transition: all var(--transition-normal);
  text-decoration: none;
  line-height: 1;
}

.login-btn::after {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 60%;
  height: 100%;
  background: linear-gradient(120deg, transparent, rgba(255,255,255,0.45), transparent);
  transition: left 700ms cubic-bezier(0.25, 0.1, 0.25, 1);
}

.login-btn:hover {
  color: white;
  box-shadow: 0 12px 28px rgba(224, 123, 58, 0.32);
  transform: translateY(-1px);
}

.login-btn:hover::after {
  left: 130%;
}

.dropdown-menu {
  border-radius: var(--radius-md);
  overflow: hidden;
  box-shadow: var(--shadow-lg);
  border: 1px solid var(--color-border);
  background: var(--color-background-elevated);
}

.dropdown-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  transition: all var(--transition-fast);
  font-size: 14px;
}

.dropdown-item:hover {
  background: var(--color-background-secondary);
}

.vip-info-item {
  color: var(--color-accent-warm);
  background: rgba(224, 123, 58, 0.10);
  font-weight: 600;
  cursor: default;

  &:hover {
    background: rgba(224, 123, 58, 0.18);
  }
}

/* 响应式 */
@media (max-width: 900px) {
  .header { height: 68px; line-height: 68px; }
  .user-dropdown { height: 68px; }
  .header-container { padding: 0 20px; }
  .site-tagline { display: none; }
  .nav-item span { display: none; }
  .nav-item { padding: 8px 12px; }
  .user-name { display: none; }
  .header-actions { gap: 8px; }
  .login-text { display: none; }
}

@media (max-width: 640px) {
  .site-title { font-size: 16px; }
  .logo-img { width: 32px; height: 32px; }
}
</style>