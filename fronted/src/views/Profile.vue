<template>
    <div class="profile-container">
        <!-- ===== 手账风：纸张纹理背景层 ===== -->
        <div class="paper-texture"></div>

        <!-- ===== 手账风：顶部标题区 ===== -->
        <div class="header-section">
            <div class="header-content">
                <div class="header-inner">
                    <span class="header-emoji">👤</span>
                    <h1 class="header-title">个人中心</h1>
                </div>
                <p class="header-subtitle">{{ greetingText }}</p>
            </div>
            <div class="header-tape"></div>
        </div>

        <!-- ===== 手账风：左侧菜单 + 右侧内容 ===== -->
        <div class="profile-body">
            <!-- 左侧菜单 -->
            <aside class="side-menu">
                <div
                    v-for="item in menuList"
                    :key="item.key"
                    class="menu-item"
                    :class="{ active: activeMenu === item.key }"
                    @click="activeMenu = item.key"
                >
                    <span class="menu-emoji">{{ item.emoji }}</span>
                    <span class="menu-text">{{ item.text }}</span>
                </div>
            </aside>

            <!-- 右侧内容区 -->
            <section class="content-area">
                <ProfileInfo v-if="activeMenu === 'info'" @updated="handleProfileUpdated" />
                <ProfileSecurity v-else-if="activeMenu === 'security'" @password-changed="handlePasswordChanged" @phone-bound="handlePhoneBound" />
                <ProfileFavorites v-else-if="activeMenu === 'favorites'" />
                <ProfileHistory v-else-if="activeMenu === 'history'" />
                <ProfileDeactivate v-else-if="activeMenu === 'deactivate'" @deactivated="handleDeactivated" />
            </section>
        </div>
    </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import ProfileInfo from '@/components/profile/ProfileInfo.vue'
import ProfileSecurity from '@/components/profile/ProfileSecurity.vue'
import ProfileFavorites from '@/components/profile/ProfileFavorites.vue'
import ProfileHistory from '@/components/profile/ProfileHistory.vue'
import ProfileDeactivate from '@/components/profile/ProfileDeactivate.vue'

const router = useRouter()

const menuList = [
    { key: 'info', emoji: '📋', text: '个人资料' },
    { key: 'security', emoji: '🔒', text: '账号安全' },
    { key: 'favorites', emoji: '⭐', text: '我的收藏' },
    { key: 'history', emoji: '📜', text: '浏览历史' },
    { key: 'deactivate', emoji: '🚪', text: '账号注销' }
]

const activeMenu = ref('info')

// 从首页"立刻设置密码"跳转过来时，自动展开账号安全
onMounted(() => {
    if (sessionStorage.getItem('openSecurityTab') === '1') {
        activeMenu.value = 'security'
        sessionStorage.removeItem('openSecurityTab')
    }
})

const greetingText = computed(() => {
    try {
        const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
        const name = userInfo.nickname || userInfo.username || '朋友'
        return `欢迎回来，${name} 🌿`
    } catch (e) {
        return '欢迎回来 🌿'
    }
})

// 资料更新后，同步更新 localStorage 中的 userInfo（保持导航栏昵称一致）
const handleProfileUpdated = (profile) => {
    try {
        const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
        userInfo.nickname = profile.nickname
        userInfo.avatar = profile.avatar
        localStorage.setItem('userInfo', JSON.stringify(userInfo))
    } catch (e) {
        // 忽略解析异常
    }
}

// 修改密码 / 重置密码后清除登录态，跳转登录页
const handlePasswordChanged = () => {
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
    router.push('/auth/login')
}

// 手机号绑定成功后无需跳转，提示已成功即可
const handlePhoneBound = () => {
    // 组件内已提示成功，此处无需额外处理
}

// 账号注销后清除登录态，跳转首页
const handleDeactivated = () => {
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
    setTimeout(() => {
        router.push('/')
    }, 800)
}
</script>

<style lang="scss" scoped>
// ========== 手绘手账治愈风 - 局部主题变量 ==========
$bg-page: #FFF8E7;
$bg-card: #FFFEF9;
$primary: #E8927C;
$primary-dark: #D9826B;
$primary-darker: #C4705C;
$primary-light: rgba(232, 146, 124, 0.15);
$accent-yellow: #F7E8A6;
$text-primary: #5C4A3A;
$text-regular: #6B5B4B;
$text-secondary: #8B7D6B;
$border-color: #E8DCC8;
$border-light: #EFE6D5;
$divider: #E0D5C0;
$shadow-card: 0 4px 16px rgba(180, 140, 100, 0.12);
$shadow-sticker: 0 2px 6px rgba(180, 140, 100, 0.18);
$radius-md: 12px;
$radius-lg: 16px;
$radius-xl: 24px;
$font-handwritten: "Ma Shan Zheng", "Comic Sans MS", "Segoe Print", "Bradley Hand", cursive;
$font-rounded: "Nunito", "Quicksand", "PingFang SC", "Microsoft YaHei", sans-serif;

.profile-container {
    position: relative;
    min-height: 100vh;
    background: $bg-page;
    font-family: $font-rounded;
    color: $text-primary;
    padding-bottom: 40px;
}

// ===== 手账风：纸张纹理背景层 =====
.paper-texture {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    pointer-events: none;
    z-index: 0;
    background-image:
        linear-gradient(rgba(232, 220, 200, 0.25) 1px, transparent 1px),
        linear-gradient(90deg, rgba(232, 220, 200, 0.25) 1px, transparent 1px),
        radial-gradient(ellipse at center, rgba(180, 140, 100, 0.04) 0%, transparent 70%);
    background-size: 28px 28px, 28px 28px, 100% 100%;
}

// ===== 手账风：顶部标题区 =====
.header-section {
    position: relative;
    background: linear-gradient(135deg, $accent-yellow 0%, $primary-light 100%);
    padding: 40px 40px 32px;
    border-bottom: 2px dashed $divider;
    overflow: hidden;
    z-index: 1;

    .header-content {
        max-width: 1100px;
        margin: 0 auto;
        position: relative;
        z-index: 2;

        .header-inner {
            display: flex;
            align-items: center;
            gap: 12px;
            .header-emoji {
                font-size: 32px;
            }
            .header-title {
                font-family: $font-handwritten;
                font-size: 32px;
                color: $text-primary;
                margin: 0;
                letter-spacing: 1px;
                text-shadow: 1px 1px 0 rgba(255, 255, 255, 0.6);
            }
        }
        .header-subtitle {
            margin: 8px 0 0 44px;
            font-size: 14px;
            color: $text-secondary;
        }
    }

    .header-tape {
        position: absolute;
        top: 14px;
        right: -30px;
        width: 180px;
        height: 38px;
        background: rgba(232, 146, 124, 0.35);
        transform: rotate(8deg);
        border-radius: 2px;
        box-shadow: 0 2px 4px rgba(180, 140, 100, 0.15);
        &::before, &::after {
            content: '';
            position: absolute;
            top: 0;
            bottom: 0;
            width: 8px;
            background: rgba(255, 255, 255, 0.4);
        }
        &::before { left: 0; clip-path: polygon(0 0, 100% 0, 80% 50%, 100% 100%, 0 100%); }
        &::after { right: 0; clip-path: polygon(0 0, 100% 0, 100% 100%, 20% 100%, 0 50%); }
    }
}

// ===== 手账风：左侧菜单 + 右侧内容 =====
.profile-body {
    position: relative;
    z-index: 1;
    max-width: 1100px;
    margin: 0 auto;
    padding: 28px 20px;
    display: flex;
    gap: 24px;
    align-items: flex-start;
}

// ===== 手账风：左侧菜单（便签贴纸风） =====
.side-menu {
    width: 200px;
    flex-shrink: 0;
    background: $bg-card;
    border-radius: $radius-lg;
    padding: 14px 12px;
    box-shadow: $shadow-card;
    border: 1px solid $border-light;
    position: sticky;
    top: 80px;

    .menu-item {
        display: flex;
        align-items: center;
        gap: 10px;
        padding: 12px 14px;
        margin-bottom: 6px;
        border-radius: $radius-md;
        cursor: pointer;
        color: $text-regular;
        font-size: 15px;
        font-family: $font-rounded;
        border: 1px dashed transparent;
        transition: all 0.25s ease;

        .menu-emoji {
            font-size: 18px;
        }

        &:hover {
            background: $primary-light;
            color: $primary-dark;
            border-color: $border-color;
        }

        &.active {
            background: linear-gradient(135deg, $primary-light, rgba(247, 232, 166, 0.25));
            color: $primary-dark;
            font-weight: 600;
            border-color: $primary;
            box-shadow: $shadow-sticker;
        }
    }
}

// ===== 右侧内容区 =====
.content-area {
    flex: 1;
    min-width: 0;
}

// ===== 响应式适配 =====
@media (max-width: 768px) {
    .profile-body {
        flex-direction: column;
        padding: 20px 12px;
    }
    .side-menu {
        width: 100%;
        position: static;
        display: flex;
        flex-wrap: wrap;
        gap: 8px;
        .menu-item {
            margin-bottom: 0;
            flex: 1;
            min-width: 90px;
            justify-content: center;
        }
    }
    .header-section {
        padding: 28px 20px;
        .header-content .header-inner .header-title { font-size: 26px; }
    }
}
</style>
