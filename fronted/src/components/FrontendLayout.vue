<template>
  <div class="frontend-layout">
    <!-- ===== 手账风：固定顶部导航栏，便签纸风格 ===== -->
    <div class="navbar-container">
      <!-- 底部胶带装饰线 -->
      <div class="nav-tape-line"></div>
      <div class="navbar-inner">
        <div class="brand-section">
          <div class="brand-sticker">
            <el-image style="width: 40px; height: 40px" :src="systemLogo || iconUrl" alt="品牌logo" class="brand-logo"/>
          </div>
          <h1 class="brand-name">📖 {{ systemName }}</h1>
        </div>
        <div class="nav-section">
          <router-link to="/" class="nav-link">✨ 首页</router-link>
          <router-link to="/consultation" class="nav-link" v-if="isLoggedIn">💬 AI咨询</router-link>
          <router-link to="/emotion-diary" class="nav-link" v-if="isLoggedIn">📝 情绪日记</router-link>
          <router-link to="/assessment" class="nav-link" v-if="isLoggedIn">🧭 心理测评</router-link>
          <router-link to="/course" class="nav-link" v-if="isLoggedIn">🧘 心理课程</router-link>
          <router-link to="/consultants" class="nav-link" v-if="isLoggedIn">👩‍⚕️ 预约咨询</router-link>
          <router-link to="/daily-practice" class="nav-link" v-if="isLoggedIn">🌸 每日一练</router-link>
          <router-link to="/community" class="nav-link" v-if="isLoggedIn">🌱 暖心社区</router-link>
          <router-link to="/knowledge" class="nav-link">📚 知识库</router-link>
          <span v-if="isLoggedIn" class="nav-link bell-link" @click="goNotifications">
            <el-badge :value="unreadCount" :max="99" :hidden="unreadCount === 0" class="bell-badge">
              <span class="bell-icon">🔔</span>
            </el-badge>
          </span>
          <router-link to="/profile" class="nav-link" v-if="isLoggedIn">👤 个人中心</router-link>
          <el-button v-if="isLoggedIn" class="logout-btn" @click="handleLogout">退出登录</el-button>
          <template v-else>
            <router-link to="/auth/login" class="nav-link">登录</router-link>
            <router-link to="/auth/register" class="nav-link">
              <el-button type="primary">注册</el-button>
            </router-link>
          </template>
        </div>
      </div>
    </div>
    <!-- 导航栏固定后的占位，防止内容被遮挡 -->
    <div class="navbar-spacer"></div>
    <div class="main-content">
      <router-view></router-view>
    </div>
    <div class="footer-container">
      <div class="footer-bottom">
        <p>&copy; 2026 {{ systemName }}. All rights reserved.</p>
      </div>
    </div>
    <!-- 全局音频播放器：切路由不销毁，支持后台播放 -->
    <AudioPlayer/>
    <!-- 危机干预浮动入口：登录用户全站可见，点击展开热线卡片 -->
    <CrisisSupportCard v-if="isLoggedIn" mode="floating" @open-plan="goCrisisSafety"/>
  </div>
</template>
<script setup>
import {ref, onMounted, onBeforeUnmount} from 'vue'
import {logout} from '@/api/admin'
import {getUnreadCount} from '@/api/notify'
import {getPublicSettings} from '@/api/frontend'
import {useRouter} from 'vue-router'
import {ElMessageBox} from 'element-plus'
import AudioPlayer from '@/components/AudioPlayer.vue'
import CrisisSupportCard from '@/components/CrisisSupportCard.vue'

const router = useRouter()

// 系统名称/Logo：读公开设置接口（白名单 key），失败时用默认值兜底，接口挂了不影响页面
const systemName = ref('心理健康AI助手')
const systemLogo = ref('')

// 危机卡片「填写安全计划」入口 → 跳转安全计划页
const goCrisisSafety = () => {
  router.push('/crisis-safety')
}

const iconUrl = new URL('@/assets/images/机器人.png', import.meta.url).href

const isLoggedIn = ref(false)
const unreadCount = ref(0)

// 未读数轮询：进入页面拉取一次，之后每 30 秒节流轮询；离开页面清理定时器
let pollTimer = null
const fetchUnread = () => {
  if (localStorage.getItem('token') === null) return
  getUnreadCount().then(data => {
    unreadCount.value = data?.count || 0
  }).catch(() => {})
}

const goNotifications = () => {
  router.push('/notifications')
}

// 通知中心已读操作后即时刷新徽标（浏览器原生事件，避免为一个数字引全局 store）
const onUnreadChanged = () => fetchUnread()

// 登出（弹窗确认）
const handleLogout = () => {
  ElMessageBox.confirm('确定要退出登录吗？', '退出登录', {
    confirmButtonText: '确定退出',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    logout().then(() => {
      // 清除缓存
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      // 跳转到登录页
      router.push('/auth/login')
    })
  }).catch(() => {
    // 用户取消退出，不做处理
  })
}

onMounted(() => {
  isLoggedIn.value = localStorage.getItem('token') !== null
  if (isLoggedIn.value) {
    fetchUnread()
    pollTimer = setInterval(fetchUnread, 30000)
    window.addEventListener('notify-unread-changed', onUnreadChanged)
  }
  // 拉取公开系统设置（静默失败，保留默认值）
  getPublicSettings().then(data => {
    if (data?.systemName) systemName.value = data.systemName
    if (data?.systemLogo) systemLogo.value = data.systemLogo
  }).catch(() => {})
})

onBeforeUnmount(() => {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
  window.removeEventListener('notify-unread-changed', onUnreadChanged)
})
</script>
<style scoped lang="scss">
// ===== 手账风：导航栏局部主题变量 =====
$bg-page: #FFF8E7;
$bg-nav: #FFFEF9;
$primary: #E8927C;
$primary-dark: #D9826B;
$primary-darker: #C4705C;
$primary-light: rgba(232, 146, 124, 0.12);
$accent-yellow: #F7E8A6;
$text-primary: #5C4A3A;
$text-regular: #6B5B4B;
$text-secondary: #8B7D6B;
$border-color: #E8DCC8;
$divider: #E0D5C0;
$shadow-nav: 0 2px 12px rgba(180, 140, 100, 0.15);
$shadow-sticker: 0 2px 6px rgba(180, 140, 100, 0.18);
$shadow-button: 0 2px 0 $primary-darker, 0 3px 8px rgba(180, 140, 100, 0.2);
$radius-sm: 8px;
$radius-md: 12px;
$radius-lg: 16px;
$font-handwritten: "Ma Shan Zheng", "Comic Sans MS", "Segoe Print", "Bradley Hand", cursive;
$font-rounded: "Nunito", "Quicksand", "PingFang SC", "Microsoft YaHei", sans-serif;

// ===== 手账风：整体布局 =====
.frontend-layout {
  // 页面背景保持米黄纸张色
  background-color: $bg-page;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

// ===== 手账风：导航栏固定在顶部 =====
.navbar-container {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1000;
  // 导航栏用更白的米白色，与首页 #FFF8E7 米黄底形成层次辨识度
  background: $bg-nav;
  // 底部虚线描边 + 暖色阴影，增强手账便签感与层次分离
  border-bottom: 2px dashed $border-color;
  box-shadow: $shadow-nav;
  height: 64px;
  font-family: $font-rounded;

  // ===== 手账风：底部胶带装饰线 =====
  .nav-tape-line {
    position: absolute;
    bottom: -3px;
    left: 10%;
    width: 80%;
    height: 6px;
    background: linear-gradient(90deg,
    transparent 0%,
    rgba(232, 146, 124, 0.25) 15%,
    rgba(247, 232, 166, 0.35) 50%,
    rgba(232, 146, 124, 0.25) 85%,
    transparent 100%
    );
    border-radius: 3px;
    pointer-events: none;
  }

  // 导航栏全宽铺满，不再限宽
  .navbar-inner {
    height: 100%;
    margin: 0 auto;
    padding: 0 24px;
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  // ===== 手账风：品牌区域 =====
  .brand-section {
    display: flex;
    align-items: center;
    gap: 10px;

    // logo 贴纸风格：圆角卡片 + 暖色描边 + 微旋转
    .brand-sticker {
      width: 46px;
      height: 46px;
      border-radius: $radius-sm;
      background: $bg-page;
      border: 1.5px solid $border-color;
      display: flex;
      align-items: center;
      justify-content: center;
      box-shadow: $shadow-sticker;
      transform: rotate(-2deg);
      transition: transform 0.3s ease;

      &:hover {
        transform: rotate(0deg) scale(1.05);
      }
    }

    // 品牌名手写体
    .brand-name {
      font-family: $font-handwritten;
      font-size: 22px;
      font-weight: 600;
      color: $text-primary;
      margin: 0;
      white-space: nowrap;
      letter-spacing: 1px;
    }
  }

  // ===== 手账风：导航链接区域 =====
  .nav-section {
    display: flex;
    align-items: center;
    gap: 16px;

    .nav-link {
      font-family: $font-rounded;
      color: $text-regular;
      font-size: 15px;
      font-weight: 500;
      text-decoration: none;
      padding: 6px 12px;
      border-radius: $radius-sm;
      position: relative;
      transition: all 0.25s ease;
      white-space: nowrap;

      // hover 底部暖橙下划线动画
      &::after {
        content: '';
        position: absolute;
        bottom: 0;
        left: 50%;
        width: 0;
        height: 2px;
        background: $primary;
        border-radius: 1px;
        transition: all 0.3s ease;
        transform: translateX(-50%);
      }

      &:hover {
        color: $primary;
        background: $primary-light;

        &::after {
          width: 60%;
        }
      }

      // 当前页面激活状态
      &.router-link-exact-active {
        color: $primary;
        font-weight: 600;

        &::after {
          width: 70%;
          height: 2.5px;
          background: $primary-dark;
        }
      }
    }

    // ===== 手账风：通知铃铛 =====
    .bell-link {
      cursor: pointer;
      display: inline-flex;
      align-items: center;
      padding: 6px 8px;

      .bell-icon {
        font-size: 18px;
        line-height: 1;
      }

      :deep(.el-badge__content) {
        background: $primary;
        border: none;
        font-family: $font-rounded;
        font-weight: 600;
        box-shadow: 0 1px 4px rgba(196, 112, 92, 0.4);
      }
    }

    // ===== 手账风：退出按钮定制 =====
    :deep(.logout-btn) {
      font-family: $font-rounded;
      background: transparent;
      border: 1.5px dashed $primary;
      color: $primary;
      border-radius: $radius-md;
      padding: 6px 16px;
      font-size: 14px;
      transition: all 0.25s ease;

      &:hover {
        background: $primary-light;
        border-style: solid;
        color: $primary-dark;
      }
    }

    // ===== 手账风：注册按钮定制（暖橙渐变 + 立体便签感）=====
    :deep(.el-button--primary) {
      font-family: $font-rounded;
      background: linear-gradient(135deg, $primary, $primary-dark);
      border: none;
      color: #fff;
      border-radius: $radius-md;
      padding: 8px 20px;
      font-size: 14px;
      font-weight: 600;
      box-shadow: $shadow-button;
      transition: all 0.25s ease;

      &:hover {
        transform: translateY(-1px);
        box-shadow: 0 3px 0 $primary-darker, 0 5px 12px rgba(180, 140, 100, 0.25);
      }

      &:active {
        transform: translateY(1px);
        box-shadow: 0 1px 0 $primary-darker, 0 2px 4px rgba(180, 140, 100, 0.15);
      }
    }
  }
}

// ===== 导航栏固定后的占位高度 =====
.navbar-spacer {
  height: 64px;
}

// ===== 手账风：主内容区 =====
.main-content {
  flex: 1;
}

// ===== 手账风：底部页脚 =====
.footer-container {
  background: linear-gradient(180deg, #F5EBD8 0%, #EDE2D0 100%);
  color: $text-secondary;
  padding: 20px 0;
  margin-top: auto;
  border-top: 2px dashed $border-color;

  .footer-bottom {
    max-width: 1200px;
    margin: 0 auto;
    padding: 0 10px;
    text-align: center;
    font-family: $font-rounded;
    font-size: 13px;
  }
}

// ===== 手账风：响应式适配 =====
@media (max-width: 768px) {
  .navbar-container {
    height: 56px;

    .navbar-inner {
      padding: 0 16px;
    }

    .brand-section {
      .brand-sticker {
        width: 38px;
        height: 38px;
      }

      .brand-name {
        font-size: 17px;
      }
    }

    .nav-section {
      gap: 12px;

      .nav-link {
        font-size: 13px;
        padding: 4px 8px;
      }
    }
  }

  .navbar-spacer {
    height: 56px;
  }
}
</style>
