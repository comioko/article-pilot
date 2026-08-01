<template>
  <div id="userRegisterPage">
    <div class="auth-container">
      <!-- 左侧品牌区域 -->
      <div class="brand-section">
        <div class="brand-bg"></div>
        <div class="brand-content">
          <div class="brand-logo">
            <img src="@/assets/logo.png" alt="Logo" class="logo-img" />
          </div>
          <h1 class="brand-title">ArticlePilot</h1>
          <p class="brand-subtitle">AI 文章创作助手</p>
          <div class="brand-features">
            <div class="feature-item">
              <CheckCircleOutlined class="feature-check" />
              <span>智能生成标题与大纲</span>
            </div>
            <div class="feature-item">
              <CheckCircleOutlined class="feature-check" />
              <span>流式生成高质量正文</span>
            </div>
            <div class="feature-item">
              <CheckCircleOutlined class="feature-check" />
              <span>自动配图一键导出</span>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 右侧表单区域 -->
      <div class="form-section">
        <div class="form-card">
          <h2 class="form-title">创建账号</h2>
          <p class="form-subtitle">注册开启您的 AI 创作之旅</p>
          
          <a-form :model="formState" name="basic" autocomplete="off" @finish="handleSubmit" class="register-form">
            <a-form-item name="userAccount" :rules="[{ required: true, message: '请输入账号' }]">
              <a-input 
                v-model:value="formState.userAccount" 
                placeholder="请输入账号" 
                size="large"
                class="form-input"
              >
                <template #prefix>
                  <UserOutlined class="input-icon" />
                </template>
              </a-input>
            </a-form-item>
            <a-form-item
              name="userPassword"
              :rules="[
                { required: true, message: '请输入密码' },
                { min: 8, message: '密码不能小于 8 位' },
              ]"
            >
              <a-input-password 
                v-model:value="formState.userPassword" 
                placeholder="请输入密码" 
                size="large"
                class="form-input"
              >
                <template #prefix>
                  <LockOutlined class="input-icon" />
                </template>
              </a-input-password>
            </a-form-item>
            <a-form-item
              name="checkPassword"
              :rules="[
                { required: true, message: '请确认密码' },
                { min: 8, message: '密码不能小于 8 位' },
                { validator: validateCheckPassword },
              ]"
            >
              <a-input-password 
                v-model:value="formState.checkPassword" 
                placeholder="请确认密码" 
                size="large"
                class="form-input"
              >
                <template #prefix>
                  <SafetyOutlined class="input-icon" />
                </template>
              </a-input-password>
            </a-form-item>
            
            <a-form-item>
              <a-button type="primary" html-type="submit" size="large" block class="submit-btn">
                注册
              </a-button>
            </a-form-item>
          </a-form>
          
          <div class="form-footer">
            <span class="footer-text">已有账号？</span>
            <RouterLink to="/user/login" class="login-link">立即登录</RouterLink>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { userRegister } from '@/api/userController.ts'
import { message } from 'ant-design-vue'
import { reactive } from 'vue'
import { UserOutlined, LockOutlined, SafetyOutlined, CheckCircleOutlined } from '@ant-design/icons-vue'

const router = useRouter()

const formState = reactive<API.UserRegisterRequest>({
  userAccount: '',
  userPassword: '',
  checkPassword: '',
})

/**
 * 验证确认密码
 * @param rule
 * @param value
 * @param callback
 */
const validateCheckPassword = (rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (value && value !== formState.userPassword) {
    callback(new Error('两次输入密码不一致'))
  } else {
    callback()
  }
}

/**
 * 提交表单
 * @param values
 */
const handleSubmit = async (values: API.UserRegisterRequest) => {
  const res = await userRegister(values)
  // 注册成功，跳转到登录页面
  if (res.data.code === 0) {
    message.success('注册成功')
    router.push({
      path: '/user/login',
      replace: true,
    })
  } else {
    message.error('注册失败，' + res.data.message)
  }
}
</script>

<style scoped>
#userRegisterPage {
  min-height: calc(100vh - 64px);
  background: var(--color-background-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
}

.auth-container {
  display: flex;
  width: 100%;
  max-width: 900px;
  min-height: 580px;
  background: white;
  border-radius: var(--radius-2xl);
  overflow: hidden;
  box-shadow: var(--shadow-xl);
}

/* 左侧品牌区域 - 暖咖啡色调 */
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
  background: radial-gradient(circle at 80% 20%, rgba(212, 165, 116, 0.45) 0%, transparent 60%),
              radial-gradient(circle at 20% 80%, rgba(184, 153, 104, 0.40) 0%, transparent 60%),
              radial-gradient(circle at 50% 50%, rgba(244, 233, 213, 0.55) 0%, transparent 70%);
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

/* 右侧表单区域 */
.form-section {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 40px;
  background: white;
}

.form-card {
  width: 100%;
  max-width: 320px;
}

.form-title {
  font-size: 26px;
  font-weight: 700;
  color: var(--color-text);
  margin: 0 0 6px;
  letter-spacing: -0.5px;
}

.form-subtitle {
  font-size: 14px;
  color: var(--color-text-secondary);
  margin: 0 0 28px;
}

.register-form {
  margin-bottom: 24px;
}

.form-input {
  border-radius: var(--radius-lg);
  border-color: var(--color-border);
  transition: all var(--transition-fast);
}

.form-input:hover {
  border-color: var(--color-primary-light);
}

.form-input:focus,
.form-input:focus-within {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(34, 197, 94, 0.1);
}

.form-input :deep(.ant-input) {
  padding: 12px 14px;
}

.input-icon {
  color: var(--color-text-muted);
  font-size: 16px;
}

.submit-btn {
  height: 48px;
  font-size: 16px;
  font-weight: 600;
  border-radius: var(--radius-lg);
  background: var(--gradient-primary) !important;
  border: none !important;
  color: white !important;
  box-shadow: var(--shadow-green) !important;
  transition: opacity var(--transition-normal) !important;
}

.submit-btn:hover,
.submit-btn:focus,
.submit-btn:active {
  background: var(--gradient-primary) !important;
  border: none !important;
  color: white !important;
  box-shadow: var(--shadow-green) !important;
  opacity: 0.92;
}

.submit-btn :deep(.ant-wave) {
  display: none;
}

.form-footer {
  text-align: center;
}

.footer-text {
  color: var(--color-text-secondary);
  font-size: 14px;
}

.login-link {
  color: var(--color-primary);
  font-weight: 600;
  margin-left: 4px;
  transition: color var(--transition-fast);
}

.login-link:hover {
  color: var(--color-primary-dark);
}

/* 响应式 */
@media (max-width: 768px) {
  .auth-container {
    flex-direction: column;
    min-height: auto;
    border-radius: var(--radius-xl);
  }
  
  .brand-section {
    padding: 32px 24px;
  }
  
  .brand-title {
    font-size: 22px;
  }
  
  .brand-features {
    display: none;
  }
  
  .form-section {
    padding: 32px 24px;
  }

  .form-title {
    font-size: 22px;
  }
}
</style>
