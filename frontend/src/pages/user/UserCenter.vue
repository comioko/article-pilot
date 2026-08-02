<template>
  <div id="userCenterPage">
    <div class="container">
      <!-- 左侧：用户信息卡 -->
      <aside class="info-card">
        <div class="avatar-wrapper">
          <a-avatar :src="loginUserStore.loginUser.userAvatar" :size="96" class="avatar">
            {{ (loginUserStore.loginUser.userName ?? '匿').charAt(0) }}
          </a-avatar>
          <a-upload
            :show-upload-list="false"
            :before-upload="handleBeforeUpload"
            :custom-request="handleUploadAvatar"
            accept="image/*"
          >
            <a-button shape="circle" size="small" class="upload-btn">
              <CameraOutlined />
            </a-button>
          </a-upload>
        </div>
        <h2 class="user-name">{{ loginUserStore.loginUser.userName ?? '匿名用户' }}</h2>
        <p class="user-profile">
          {{ loginUserStore.loginUser.userProfile || '这个人很懒，什么都没写' }}
        </p>

        <a-divider class="divider" />

        <div class="meta-row">
          <span class="meta-label">账号</span>
          <span class="meta-value">{{ loginUserStore.loginUser.userAccount }}</span>
        </div>
        <div class="meta-row">
          <span class="meta-label">角色</span>
          <a-tag :color="loginUserStore.loginUser.userRole === 'admin' ? 'gold' : 'blue'">
            {{ loginUserStore.loginUser.userRole === 'admin' ? '管理员' : '普通用户' }}
          </a-tag>
        </div>

        <a-divider class="divider" />

        <div class="meta-row quota-row">
          <span class="meta-label">剩余配额</span>
          <a-progress
            :percent="quotaPercent"
            :stroke-color="quotaColor"
            :show-info="false"
            class="quota-progress"
          />
          <span class="quota-value">{{ loginUserStore.loginUser.quota ?? 0 }} / {{ MAX_QUOTA }}</span>
        </div>

        <div class="vip-row">
          <div class="vip-status">
            <CrownOutlined v-if="isVipUser" class="vip-icon" />
            <span :class="['vip-text', isVipUser ? 'vip-active' : 'vip-inactive']">
              {{ isVipUser ? `会员到期：${formatVipTime}` : '非会员' }}
            </span>
          </div>
          <a-button type="primary" size="small" @click="goVip" class="vip-btn">
            {{ isVipUser ? '续费' : '开通会员' }}
          </a-button>
        </div>
      </aside>

      <!-- 右侧：设置 tabs -->
      <main class="settings-panel">
        <a-tabs v-model:active-key="activeTab" class="settings-tabs" tab-position="top">
          <!-- 基本资料 -->
          <a-tab-pane key="profile" tab="基本资料">
            <a-form
              :model="profileForm"
              :label-col="{ span: 4 }"
              :wrapper-col="{ span: 16 }"
              class="settings-form"
              @finish="handleSaveProfile"
            >
              <a-form-item label="昵称" name="userName">
                <a-input
                  v-model:value="profileForm.userName"
                  placeholder="请输入昵称"
                  :maxlength="20"
                  allow-clear
                />
              </a-form-item>
              <a-form-item label="头像 URL" name="userAvatar">
                <a-input
                  v-model:value="profileForm.userAvatar"
                  placeholder="可粘贴 CDN URL，或点击左侧头像上传"
                  allow-clear
                />
              </a-form-item>
              <a-form-item label="个人简介" name="userProfile">
                <a-textarea
                  v-model:value="profileForm.userProfile"
                  placeholder="一句话介绍自己（不超过 200 字）"
                  :rows="4"
                  :maxlength="200"
                  show-count
                />
              </a-form-item>
              <a-form-item :wrapper-col="{ offset: 4, span: 16 }">
                <a-button type="primary" html-type="submit" :loading="profileSaving">
                  保存
                </a-button>
                <a-button style="margin-left: 12px" @click="resetProfileForm">重置</a-button>
              </a-form-item>
            </a-form>
          </a-tab-pane>

          <!-- 修改密码 -->
          <a-tab-pane key="password" tab="修改密码">
            <a-form
              :model="passwordForm"
              :rules="passwordRules"
              :label-col="{ span: 4 }"
              :wrapper-col="{ span: 16 }"
              class="settings-form"
              @finish="handleChangePassword"
            >
              <a-form-item label="原密码" name="oldPassword">
                <a-input-password
                  v-model:value="passwordForm.oldPassword"
                  placeholder="请输入当前密码"
                />
              </a-form-item>
              <a-form-item label="新密码" name="newPassword">
                <a-input-password
                  v-model:value="passwordForm.newPassword"
                  placeholder="至少 8 位"
                />
              </a-form-item>
              <a-form-item label="确认新密码" name="confirmPassword">
                <a-input-password
                  v-model:value="passwordForm.confirmPassword"
                  placeholder="再输一次新密码"
                />
              </a-form-item>
              <a-form-item :wrapper-col="{ offset: 4, span: 16 }">
                <a-button type="primary" html-type="submit" :loading="passwordSaving">
                  提交
                </a-button>
                <a-button style="margin-left: 12px" @click="resetPasswordForm">重置</a-button>
              </a-form-item>
            </a-form>
          </a-tab-pane>

          <!-- 账号安全 -->
          <a-tab-pane key="security" tab="账号安全">
            <div class="security-list">
              <div class="security-item">
                <div class="security-info">
                  <div class="security-title">登录会话</div>
                  <div class="security-desc">当前浏览器已登录，关闭页面后会话仍保留 30 天</div>
                </div>
                <a-button danger @click="handleLogout">退出登录</a-button>
              </div>
              <a-divider />
              <div class="security-item">
                <div class="security-info">
                  <div class="security-title">账号 ID</div>
                  <div class="security-desc">
                    {{ loginUserStore.loginUser.id ?? '--' }}
                  </div>
                </div>
              </div>
              <a-divider />
              <div class="security-item">
                <div class="security-info">
                  <div class="security-title">注册时间</div>
                  <div class="security-desc">
                    {{ formatCreateTime }}
                  </div>
                </div>
              </div>
              <a-divider />
              <div class="security-item danger-zone">
                <div class="security-info">
                  <div class="security-title danger-text">注销账号</div>
                  <div class="security-desc">
                    注销后账号将无法恢复，请谨慎操作（本功能暂未上线）
                  </div>
                </div>
                <a-button disabled danger>申请注销</a-button>
              </div>
            </div>
          </a-tab-pane>
        </a-tabs>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import {
  CameraOutlined,
  CrownOutlined,
} from '@ant-design/icons-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import {
  updateMyProfile,
  uploadAvatar,
  changePassword,
  userLogout,
} from '@/api/userController'
import { isVip } from '@/utils/permission'

const router = useRouter()
const loginUserStore = useLoginUserStore()
const MAX_QUOTA = 100

// ===== 左侧信息卡 =====
const quotaPercent = computed(() => {
  const q = loginUserStore.loginUser.quota ?? 0
  return Math.min(100, Math.round((q / MAX_QUOTA) * 100))
})

const quotaColor = computed(() => {
  const p = quotaPercent.value
  if (p > 50) return '#22C55E'
  if (p > 20) return '#EAB308'
  return '#EF4444'
})

const isVipUser = computed(() => isVip(loginUserStore.loginUser))

const formatVipTime = computed(() => {
  const t = loginUserStore.loginUser.vipTime
  return t ? dayjs(t).format('YYYY-MM-DD') : '--'
})

const formatCreateTime = computed(() => {
  const t = (loginUserStore.loginUser as any).createTime
  return t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '--'
})

const goVip = () => {
  router.push('/vip')
}

// ===== 头像上传 =====
const handleBeforeUpload = (file: File): boolean => {
  if (file.size > 2 * 1024 * 1024) {
    message.error('头像文件不能超过 2MB')
    return false
  }
  const isImage = file.type.startsWith('image/')
  if (!isImage) {
    message.error('只能上传图片')
    return false
  }
  return true
}

const handleUploadAvatar = async (options: any) => {
  const { file, onSuccess, onError } = options
  try {
    const res = await uploadAvatar(file as File)
    if (res.data.code === 0 && res.data.data?.url) {
      const url = res.data.data.url
      profileForm.userAvatar = url
      loginUserStore.setLoginUser({ ...loginUserStore.loginUser, userAvatar: url })
      message.success('头像上传成功')
      onSuccess?.(res.data)
    } else {
      message.error('头像上传失败：' + res.data.message)
      onError?.(new Error(res.data.message))
    }
  } catch (e: any) {
    message.error('头像上传失败：' + (e?.message ?? '未知错误'))
    onError?.(e)
  }
}

// ===== 基本资料 =====
const activeTab = ref<string>('profile')
const profileSaving = ref(false)
const profileForm = reactive({
  userName: '',
  userProfile: '',
  userAvatar: '',
})

const fillProfileForm = () => {
  profileForm.userName = loginUserStore.loginUser.userName ?? ''
  profileForm.userProfile = loginUserStore.loginUser.userProfile ?? ''
  profileForm.userAvatar = loginUserStore.loginUser.userAvatar ?? ''
}

const handleSaveProfile = async () => {
  profileSaving.value = true
  try {
    const res = await updateMyProfile({
      userName: profileForm.userName,
      userProfile: profileForm.userProfile,
      userAvatar: profileForm.userAvatar,
    })
    if (res.data.code === 0 && res.data.data) {
      loginUserStore.setLoginUser(res.data.data)
      message.success('保存成功')
    } else {
      message.error('保存失败：' + res.data.message)
    }
  } catch (e: any) {
    message.error('保存失败：' + (e?.message ?? '未知错误'))
  } finally {
    profileSaving.value = false
  }
}

const resetProfileForm = () => {
  fillProfileForm()
}

// ===== 修改密码 =====
const passwordSaving = ref(false)
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入原密码' }],
  newPassword: [
    { required: true, message: '请输入新密码' },
    { min: 8, message: '新密码至少 8 位' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码' },
    {
      validator: (_: any, value: string) => {
        if (value !== passwordForm.newPassword) {
          return Promise.reject(new Error('两次输入的新密码不一致'))
        }
        return Promise.resolve()
      },
    },
  ],
}

const handleChangePassword = async () => {
  passwordSaving.value = true
  try {
    const res = await changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
    })
    if (res.data.code === 0) {
      message.success('密码修改成功，请重新登录')
      resetPasswordForm()
      setTimeout(() => router.push('/user/login'), 800)
    } else {
      message.error('密码修改失败：' + res.data.message)
    }
  } catch (e: any) {
    message.error('密码修改失败：' + (e?.message ?? '未知错误'))
  } finally {
    passwordSaving.value = false
  }
}

const resetPasswordForm = () => {
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
}

// ===== 退出登录 =====
const handleLogout = async () => {
  const res = await userLogout()
  if (res.data.code === 0) {
    loginUserStore.setLoginUser({ userName: '未登录' } as any)
    message.success('退出登录成功')
    router.push('/user/login')
  } else {
    message.error('退出登录失败：' + res.data.message)
  }
}

onMounted(() => {
  fillProfileForm()
})
</script>

<style scoped>
#userCenterPage {
  width: 100%;
  min-height: calc(100vh - 76px);
  padding: 40px 24px;
  background: var(--color-background-secondary);
}

.container {
  max-width: 1100px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 24px;
}

/* ===== 信息卡 ===== */
.info-card {
  background: white;
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border);
  padding: 32px 24px;
  text-align: center;
  height: fit-content;
  position: sticky;
  top: 100px;
}

.avatar-wrapper {
  position: relative;
  display: inline-block;
  margin-bottom: 16px;
}

.avatar {
  border: 4px solid var(--color-background);
  box-shadow: var(--shadow-md);
}

.upload-btn {
  position: absolute;
  bottom: 0;
  right: 0;
  border: 2px solid white;
  background: var(--color-primary);
  color: white;
}

.user-name {
  font-size: 20px;
  font-weight: 600;
  margin: 8px 0 4px;
  color: var(--color-text);
}

.user-profile {
  font-size: 13px;
  color: var(--color-text-secondary);
  margin: 0 0 8px;
  min-height: 36px;
  line-height: 1.5;
}

.divider {
  margin: 16px 0;
  border-color: var(--color-border-light);
}

.meta-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  margin: 8px 0;
}

.meta-label {
  color: var(--color-text-muted);
}

.meta-value {
  color: var(--color-text);
  font-weight: 500;
}

.quota-row {
  flex-direction: column;
  align-items: stretch;
  gap: 6px;
}

.quota-progress {
  margin: 4px 0;
}

.quota-value {
  font-size: 12px;
  color: var(--color-text-muted);
  text-align: right;
}

.vip-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-top: 8px;
}

.vip-status {
  display: flex;
  align-items: center;
  gap: 6px;
}

.vip-icon {
  color: #EAB308;
  font-size: 16px;
}

.vip-text {
  font-size: 13px;
  font-weight: 500;
}

.vip-active {
  color: #CA8A04;
}

.vip-inactive {
  color: var(--color-text-muted);
}

.vip-btn {
  border-radius: var(--radius-pill);
}

/* ===== 设置面板 ===== */
.settings-panel {
  background: white;
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border);
  padding: 24px 32px;
}

.settings-tabs :deep(.ant-tabs-nav) {
  margin-bottom: 24px;
}

.settings-form {
  max-width: 640px;
  padding-top: 8px;
}

/* ===== 安全列表 ===== */
.security-list {
  max-width: 640px;
  padding-top: 8px;
}

.security-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
}

.security-info {
  flex: 1;
}

.security-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text);
  margin-bottom: 4px;
}

.security-desc {
  font-size: 13px;
  color: var(--color-text-muted);
}

.danger-zone {
  margin-top: 8px;
}

.danger-text {
  color: var(--color-error);
}

/* ===== 响应式 ===== */
@media (max-width: 900px) {
  .container {
    grid-template-columns: 1fr;
  }
  .info-card {
    position: static;
  }
}
</style>