<template>
  <div class="login-page">
    <!-- 左侧品牌区 -->
    <div class="brand-panel" ref="brandPanelRef">
      <canvas ref="particleCanvasRef" class="particle-canvas"></canvas>
      <div class="brand-content">
        <div class="brand-logo" ref="logoRef">
          <div class="logo-icon">
            <svg viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
              <rect width="48" height="48" rx="12" fill="currentColor" fill-opacity="0.15"/>
              <path d="M14 20 L24 12 L34 20" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>
              <path d="M18 20 L18 32 L30 32 L30 20" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>
              <path d="M22 32 L22 26 L26 26 L26 32" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>
              <circle cx="24" cy="16" r="2" fill="currentColor"/>
            </svg>
          </div>
          <h1 class="brand-title">小i跑腿 · 管理端</h1>
        </div>
        <p class="brand-slogan" ref="sloganRef">让校园生活更高效</p>
        <div class="brand-footer" ref="footerRef">
          <span>Campus Errand Service Platform</span>
        </div>
      </div>
    </div>

    <!-- 右侧登录面板 -->
    <div class="login-panel" ref="loginPanelRef">
      <div class="login-card">
        <div class="login-header" ref="formHeaderRef">
          <h2>欢迎回来</h2>
          <p>请登录您的管理账号</p>
        </div>
        <el-form
          :model="form"
          :rules="rules"
          ref="formRef"
          class="login-form"
          @keyup.enter="handleLogin"
        >
          <div ref="formFieldsRef">
            <el-form-item prop="username">
              <el-input
                v-model="form.username"
                placeholder="用户名"
                :prefix-icon="User"
                size="large"
              />
            </el-form-item>
            <el-form-item prop="password">
              <el-input
                v-model="form.password"
                type="password"
                placeholder="密码"
                :prefix-icon="Lock"
                size="large"
                show-password
              />
            </el-form-item>
          </div>
          <div ref="formActionsRef">
            <el-form-item>
              <el-button
                type="primary"
                size="large"
                class="login-btn"
                :loading="loading"
                @click="handleLogin"
              >
                登 录
              </el-button>
            </el-form-item>
          </div>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import gsap from 'gsap'
import { useAuthStore } from '@/stores/auth'
import { login as loginApi } from '@/api/auth'

const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)
const formRef = ref()
const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

// ---- GSAP template refs ----
const brandPanelRef = ref<HTMLElement>()
const particleCanvasRef = ref<HTMLCanvasElement>()
const logoRef = ref<HTMLElement>()
const sloganRef = ref<HTMLElement>()
const footerRef = ref<HTMLElement>()
const loginPanelRef = ref<HTMLElement>()
const formHeaderRef = ref<HTMLElement>()
const formFieldsRef = ref<HTMLElement>()
const formActionsRef = ref<HTMLElement>()

// ---- Canvas 粒子系统 ----
let animId = 0
let particles: Particle[] = []

interface Particle {
  x: number; y: number; vx: number; vy: number
  r: number; alpha: number; alphaDir: number
}

function initParticles(w: number, h: number) {
  particles = []
  const count = 50
  for (let i = 0; i < count; i++) {
    particles.push({
      x: Math.random() * w,
      y: Math.random() * h,
      vx: (Math.random() - 0.5) * 0.3,
      vy: (Math.random() - 0.5) * 0.3,
      r: Math.random() * 3 + 1,
      alpha: Math.random() * 0.4 + 0.1,
      alphaDir: Math.random() > 0.5 ? 0.002 : -0.002,
    })
  }
}

function drawParticles(ctx: CanvasRenderingContext2D, w: number, h: number) {
  ctx.clearRect(0, 0, w, h)
  for (const p of particles) {
    p.x += p.vx
    p.y += p.vy
    p.alpha += p.alphaDir
    if (p.alpha <= 0.05 || p.alpha >= 0.45) p.alphaDir *= -1
    if (p.x < 0) p.x = w
    if (p.x > w) p.x = 0
    if (p.y < 0) p.y = h
    if (p.y > h) p.y = 0
    ctx.beginPath()
    ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2)
    ctx.fillStyle = `rgba(255,255,255,${p.alpha})`
    ctx.fill()
  }
}

function startCanvas() {
  const canvas = particleCanvasRef.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  if (!ctx) return
  const resize = () => {
    const rect = canvas.parentElement!.getBoundingClientRect()
    canvas.width = rect.width * devicePixelRatio
    canvas.height = rect.height * devicePixelRatio
    canvas.style.width = rect.width + 'px'
    canvas.style.height = rect.height + 'px'
    ctx.scale(devicePixelRatio, devicePixelRatio)
    initParticles(rect.width, rect.height)
  }
  resize()
  window.addEventListener('resize', resize)
  const loop = () => {
    const w = canvas.width / devicePixelRatio
    const h = canvas.height / devicePixelRatio
    drawParticles(ctx, w, h)
    animId = requestAnimationFrame(loop)
  }
  loop()

  onUnmounted(() => {
    cancelAnimationFrame(animId)
    window.removeEventListener('resize', resize)
  })
}

// ---- GSAP 入场动画 ----
function splitText(el: HTMLElement) {
  const text = el.textContent || ''
  el.textContent = ''
  const chars: HTMLSpanElement[] = []
  for (const ch of text) {
    const span = document.createElement('span')
    span.textContent = ch
    span.style.display = 'inline-block'
    el.appendChild(span)
    chars.push(span)
  }
  return chars
}

async function runEntranceAnimation() {
  await nextTick()

  // 先启动 canvas 粒子
  startCanvas()

  const tl = gsap.timeline({ defaults: { ease: 'power4.out' } })

  // Logo 从下方淡入 + 弹性回弹
  if (logoRef.value) {
    tl.from(logoRef.value, {
      y: 40, opacity: 0, duration: 1, ease: 'back.out(1.7)',
    }, 0.2)
  }

  // Slogan 逐字揭示
  if (sloganRef.value) {
    const chars = splitText(sloganRef.value)
    tl.from(chars, {
      y: 20, opacity: 0, duration: 0.6, stagger: 0.04, ease: 'power3.out',
    }, '-=0.3')
  }

  // Footer
  if (footerRef.value) {
    tl.from(footerRef.value, {
      opacity: 0, duration: 0.8,
    }, '-=0.2')
  }

  // 右侧面板从右滑入
  if (loginPanelRef.value) {
    tl.from(loginPanelRef.value, {
      x: '100%', duration: 1, ease: 'power4.out',
    }, '-=0.4')
  }

  // 表单头部
  if (formHeaderRef.value) {
    tl.from(formHeaderRef.value, {
      y: 16, opacity: 0, duration: 0.6,
    }, '-=0.2')
  }

  // 表单字段依次淡入
  if (formFieldsRef.value) {
    const children = Array.from(formFieldsRef.value.children) as HTMLElement[]
    tl.from(children, {
      y: 12, opacity: 0, duration: 0.5, stagger: 0.12, ease: 'power3.out',
    }, '-=0.1')
  }

  // 登录按钮
  if (formActionsRef.value) {
    tl.from(formActionsRef.value, {
      y: 8, opacity: 0, duration: 0.5,
    }, '-=0.1')
  }
}

// ---- 登录逻辑（保持不变）----
async function handleLogin() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    const data = await loginApi(form)
    authStore.setAuth(data.token, data.refreshToken, {
      adminId: data.adminId,
      username: data.username,
      name: data.name,
      role: data.role,
    })
    ElMessage.success('登录成功')
    router.replace('/dashboard')
  } catch {
    ElMessage.error('登录失败，请检查用户名和密码')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  runEntranceAnimation()
})
</script>

<style scoped>
.login-page {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

/* ===== 左侧品牌区 ===== */
.brand-panel {
  position: relative;
  flex: 0 0 58%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 40%, #0f2b3d 100%);
  overflow: hidden;
}

.particle-canvas {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.brand-content {
  position: relative;
  z-index: 1;
  text-align: center;
  color: #fff;
}

.brand-logo {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
}

.logo-icon {
  width: 72px;
  height: 72px;
  color: #FF6B4A;
}

.logo-icon svg {
  width: 100%;
  height: 100%;
}

.brand-title {
  font-size: 32px;
  font-weight: 700;
  letter-spacing: 2px;
  margin: 0;
  background: linear-gradient(135deg, #FF6B4A, #FFB347);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.brand-slogan {
  margin-top: 16px;
  font-size: 18px;
  font-weight: 400;
  color: rgba(255, 255, 255, 0.65);
  letter-spacing: 4px;
}

.brand-footer {
  margin-top: 48px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.3);
  letter-spacing: 1px;
}

/* ===== 右侧登录面板 ===== */
.login-panel {
  flex: 0 0 42%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--neutral-bg);
}

.login-card {
  width: 380px;
  max-width: 90%;
}

.login-header {
  margin-bottom: 36px;
}

.login-header h2 {
  font-size: 26px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0 0 8px;
}

.login-header p {
  font-size: 14px;
  color: var(--text-tertiary);
  margin: 0;
}

.login-form :deep(.el-input__wrapper) {
  border-radius: 10px;
  box-shadow: none;
  border: 1px solid var(--el-border-color);
  transition: border-color var(--duration-fast);
}

.login-form :deep(.el-input__wrapper:hover) {
  border-color: var(--el-color-primary);
}

.login-form :deep(.el-input__wrapper.is-focus) {
  border-color: var(--el-color-primary);
  box-shadow: 0 0 0 2px var(--el-color-primary-light-9);
}

.login-btn {
  width: 100%;
  border-radius: 10px;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 4px;
  height: 46px;
  transition: transform var(--duration-fast);
}

.login-btn:hover {
  transform: scale(1.02);
}

.login-btn:active {
  transform: scale(0.98);
}

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .brand-panel {
    display: none;
  }
  .login-panel {
    flex: 1;
  }
}
</style>
