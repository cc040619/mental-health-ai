<template>
    <!-- ===== 手账风：整体页面背景为米黄纸张 ===== -->
    <div class="home-container">
        <!-- ===== 纸张纹理层：网格线 + 纸纹噪点（纯装饰） ===== -->
        <div class="paper-texture"></div>

        <!-- ===== 手账风：漂浮装饰 emoji 粒子 ===== -->
        <div class="floating-decorations">
            <span class="float-emoji float-1">🌿</span>
            <span class="float-emoji float-2">✨</span>
            <span class="float-emoji float-3">🌸</span>
            <span class="float-emoji float-4">💛</span>
            <span class="float-emoji float-5">☕</span>
            <span class="float-emoji float-6">🍃</span>
            <span class="float-emoji float-7">🦋</span>
            <span class="float-emoji float-8">⭐</span>
        </div>

        <!-- ===== 顶部胶带装饰 ===== -->
        <div class="tape tape-tape-left"></div>
        <div class="tape tape-tape-right"></div>

        <div class="content">
            <div class="text">
                <!-- ===== 标题区：手写体 + emoji 装饰 + 逐行淡入动画 ===== -->
                <h2 class="title">
                    <span class="title-icon">📖</span>
                    一次温暖的对话<br/>
                    <span class="highlight-text">化孤独为慰藉 <span class="title-sparkle">✦</span></span>
                </h2>
                <!-- ===== 虚线分隔 ===== -->
                <div class="dashed-divider"></div>
                <p class="description">每个深夜，每个焦虑的时刻，我们都在这里。不必独自承受，让心与心的连接温暖您的每一天</p>
                <!-- ===== 按钮：暖橙渐变主按钮 + 虚线边框次按钮 ===== -->
                <div class="hero-actions">
                    <el-button class="btn-primary" size="large" @click="router.push('/consultation')">开始倾诉，获得陪伴 🌿</el-button>
                    <el-button class="btn-secondary" size="large" @click="router.push('/emotion-diary')">记录心情，释放情感 ✏️</el-button>
                </div>
            </div>
            <!-- ===== 机器人容器：贴纸风格 + 呼吸光晕 ===== -->
            <div class="robot-sticker">
                <!-- 角落图钉装饰 -->
                <span class="robot-pin">📌</span>
                <!-- 呼吸光晕 -->
                <div class="robot-glow"></div>
                <div class="robot">
                    <el-image style="width: 150px; height: 150px" :src="iconUrl" alt="机器人" class="robot-image" />
                </div>
            </div>
        </div>

        <!-- ===== 首页轮播图：启用中按排序展示（空数据隐藏） ===== -->
        <div v-if="banners.length" class="banner-carousel">
            <el-carousel :interval="4000" height="220px" arrow="hover" indicator-position="outside">
                <el-carousel-item v-for="b in banners" :key="b.id">
                    <div class="banner-item" @click="openBanner(b)"
                         :style="{ cursor: b.linkUrl ? 'pointer' : 'default' }">
                        <el-image :src="b.imageUrl" fit="cover" class="banner-image" />
                        <span v-if="b.title" class="banner-title">{{ b.title }}</span>
                    </div>
                </el-carousel-item>
            </el-carousel>
        </div>

        <!-- ===== 底部手写签名装饰 ===== -->
        <div class="bottom-deco">
            <div class="bottom-dashed"></div>
            <span class="bottom-signature">— 你的情绪小树洞 🌱</span>
        </div>

        <!-- ===== 首次设置密码提醒弹窗（仅无密码用户且本会话未弹过时展示） ===== -->
        <el-dialog
            v-model="pwdReminderVisible"
            title="🔒 温馨提示"
            width="400px"
            :close-on-click-modal="false"
            class="pwd-reminder-dialog"
        >
            <div class="pwd-reminder-content">
                <p class="pwd-reminder-text">您还没有设置密码哦～</p>
                <p class="pwd-reminder-sub">设置密码后可使用账号密码登录，账户更安全 🛡️</p>
            </div>
            <template #footer>
                <el-button @click="handleLaterPwd">稍后设置</el-button>
                <el-button type="primary" @click="handleSetPwdNow">立刻设置</el-button>
            </template>
        </el-dialog>
    </div>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getBanners } from '@/api/notify'

const router = useRouter()

// 首页轮播（后端已过滤启用中并按排序返回，空数据不渲染）
const banners = ref([])

// ===== 首次设置密码提醒 =====
const pwdReminderVisible = ref(false)

const handleLaterPwd = () => {
    // 本会话不再弹，下次登录（新会话）再提醒
    sessionStorage.setItem('pwdReminderShown', '1')
    pwdReminderVisible.value = false
}

const handleSetPwdNow = () => {
    pwdReminderVisible.value = false
    sessionStorage.setItem('pwdReminderShown', '1')
    // 标记进入个人中心后自动展开账号安全（设置密码）
    sessionStorage.setItem('openSecurityTab', '1')
    router.push('/profile')
}

const checkPwdReminder = () => {
    try {
        const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
        // 无密码用户且本会话未弹过提醒
        if (userInfo.hasPassword === false && !sessionStorage.getItem('pwdReminderShown')) {
            pwdReminderVisible.value = true
        }
    } catch (e) {
        // 忽略解析异常
    }
}

onMounted(() => {
    getBanners().then(data => {
        banners.value = data || []
    }).catch(() => {})
    // 首次密码设置提醒（延迟到轮播加载后，避免抢焦点）
    checkPwdReminder()
})

// 轮播跳转（后端已白名单校验仅 http/https）
const openBanner = (b) => {
    if (b.linkUrl) window.open(b.linkUrl, '_blank')
}



const iconUrl = new URL('@/assets/images/机器人.png', import.meta.url).href
</script>
<style scoped lang="scss">
// ===== 手账风：局部主题变量（项目暂无全局变量文件时使用）=====
$bg-page: #FFF8E7;
$bg-card: #FFFEF9;
$primary: #E8927C;
$primary-dark: #D9826B;
$primary-darker: #C4705C;
$primary-light: rgba(232, 146, 124, 0.15);
$accent-pink: #F5C6C6;
$accent-green: #B8D8BE;
$accent-yellow: #F7E8A6;
$text-primary: #5C4A3A;
$text-regular: #6B5B4B;
$text-secondary: #8B7D6B;
$text-placeholder: #B8A898;
$border-color: #E8DCC8;
$divider: #E0D5C0;
$shadow-card: 0 4px 16px rgba(180, 140, 100, 0.12);
$shadow-button: 0 3px 0 $primary-darker, 0 4px 8px rgba(180, 140, 100, 0.2);
$radius-sm: 8px;
$radius-md: 12px;
$radius-lg: 16px;
$radius-xl: 24px;
$font-handwritten: "Ma Shan Zheng", "Comic Sans MS", "Segoe Print", "Bradley Hand", cursive;
$font-rounded: "Nunito", "Quicksand", "PingFang SC", "Microsoft YaHei", sans-serif;

// ===== 动画关键帧 =====
// 漂浮 emoji 上下浮动
@keyframes float {
    0%, 100% { transform: translateY(0) rotate(0deg); opacity: 0.6; }
    50% { transform: translateY(-25px) rotate(8deg); opacity: 0.9; }
}
// 文字逐行淡入上浮
@keyframes fadeInUp {
    from { opacity: 0; transform: translateY(20px); }
    to { opacity: 1; transform: translateY(0); }
}
// 呼吸光晕
@keyframes breathe {
    0%, 100% { transform: scale(1); opacity: 0.4; }
    50% { transform: scale(1.08); opacity: 0.7; }
}
// 背景纹理缓慢位移
@keyframes drift {
    from { background-position: 0 0; }
    to { background-position: 40px 40px; }
}
// 星星闪烁
@keyframes sparkle {
    0%, 100% { opacity: 0.3; transform: scale(0.8) rotate(0deg); }
    50% { opacity: 1; transform: scale(1.2) rotate(180deg); }
}
// 胶带微晃
@keyframes tapeSway {
    0%, 100% { transform: rotate(-3deg); }
    50% { transform: rotate(-2deg); }
}
@keyframes tapeSwayReverse {
    0%, 100% { transform: rotate(3deg); }
    50% { transform: rotate(2deg); }
}

.home-container {
    // ===== 手账风：米黄纸张背景 =====
    background: $bg-page;
    color: $text-primary;
    font-family: $font-rounded;
    padding: 5rem 0;
    min-height: max(calc(100vh - 285px), 480px); // 有轮播图时内容可撑开容器
    display: flex;
    align-items: center;
    justify-content: center;
    position: relative;
    overflow: hidden;

    // ===== 纸张纹理层 =====
    .paper-texture {
        position: absolute;
        inset: 0;
        z-index: 0;
        // 网格线
        background-image:
            linear-gradient(rgba(212, 180, 140, 0.08) 1px, transparent 1px),
            linear-gradient(90deg, rgba(212, 180, 140, 0.08) 1px, transparent 1px);
        background-size: 28px 28px;
        // 纸纹噪点
        &::before {
            content: '';
            position: absolute;
            inset: 0;
            background: radial-gradient(circle at 20% 30%, rgba(180, 140, 100, 0.03) 0%, transparent 50%),
                        radial-gradient(circle at 80% 70%, rgba(232, 146, 124, 0.03) 0%, transparent 50%),
                        radial-gradient(circle at 50% 50%, rgba(200, 170, 130, 0.02) 0%, transparent 70%);
            animation: drift 20s linear infinite;
        }
    }

    // ===== 漂浮装饰 emoji =====
    .floating-decorations {
        position: absolute;
        inset: 0;
        z-index: 0;
        pointer-events: none;
        .float-emoji {
            position: absolute;
            font-size: 1.5rem;
            opacity: 0.6;
            animation: float 6s ease-in-out infinite;
        }
        .float-1 { top: 10%; left: 5%; animation-delay: 0s; }
        .float-2 { top: 18%; right: 8%; animation-delay: 0.8s; font-size: 1.2rem; }
        .float-3 { top: 65%; left: 8%; animation-delay: 1.5s; font-size: 1.3rem; }
        .float-4 { top: 75%; right: 12%; animation-delay: 0.3s; }
        .float-5 { top: 30%; left: 15%; animation-delay: 2s; font-size: 1.1rem; }
        .float-6 { top: 50%; right: 5%; animation-delay: 1.2s; font-size: 1.4rem; }
        .float-7 { top: 85%; left: 30%; animation-delay: 2.5s; font-size: 1.3rem; }
        .float-8 { top: 8%; left: 45%; animation-delay: 1.8s; font-size: 1rem; }
    }

    // ===== 顶部胶带装饰 =====
    .tape {
        position: absolute;
        z-index: 1;
        width: 100px;
        height: 24px;
        background: rgba(232, 146, 124, 0.25);
        border-left: 1px dashed rgba(232, 146, 124, 0.3);
        border-right: 1px dashed rgba(232, 146, 124, 0.3);
    }
    .tape-tape-left {
        top: 12px;
        left: 10%;
        animation: tapeSway 4s ease-in-out infinite;
    }
    .tape-tape-right {
        top: 12px;
        right: 10%;
        background: rgba(247, 232, 166, 0.3);
        border-color: rgba(247, 232, 166, 0.4);
        animation: tapeSwayReverse 5s ease-in-out infinite;
    }

    .content {
        position: relative;
        z-index: 2;
        display: flex;
        justify-content: center;
        align-items: center;
        gap: 40px;

        .text {
            width: 500px;
            // ===== 标题：手写体 + 暖橙色 + emoji 装饰 + 淡入动画 =====
            .title {
                font-family: $font-handwritten;
                font-size: 42px;
                font-weight: bold;
                margin-bottom: 15px;
                color: $text-primary;
                line-height: 1.35;
                animation: fadeInUp 0.8s ease-out;
                .title-icon {
                    font-size: 36px;
                    vertical-align: middle;
                    margin-right: 6px;
                    display: inline-block;
                    animation: sparkle 3s ease-in-out infinite;
                }
                .highlight-text {
                    color: $primary;
                    position: relative;
                    display: inline-block;
                    .title-sparkle {
                        font-size: 28px;
                        vertical-align: middle;
                        margin-left: 4px;
                        display: inline-block;
                        animation: sparkle 2.5s ease-in-out infinite;
                        color: $accent-yellow;
                    }
                    // 手绘下划线效果
                    &::after {
                        content: '';
                        position: absolute;
                        bottom: -4px;
                        left: 0;
                        width: 100%;
                        height: 8px;
                        background: rgba(232, 146, 124, 0.18);
                        border-radius: 50%;
                        z-index: -1;
                    }
                }
            }

            // ===== 虚线分隔 =====
            .dashed-divider {
                width: 80px;
                border-top: 2px dashed $divider;
                margin: 10px 0 15px 0;
            }

            .description {
                font-family: $font-rounded;
                font-size: 17px;
                color: $text-regular;
                line-height: 1.7;
                animation: fadeInUp 1s ease-out 0.2s both;
            }

            // ===== 按钮：手账风深度定制 =====
            .hero-actions {
                margin-top: 30px;
                display: flex;
                gap: 16px;
                flex-wrap: wrap;
                animation: fadeInUp 1.2s ease-out 0.4s both;

                // 主按钮：暖橙渐变 + 立体便签效果
                :deep(.btn-primary) {
                    background: linear-gradient(135deg, #{$primary}, #{$primary-dark}) !important;
                    border: none !important;
                    border-radius: $radius-lg !important;
                    color: #fff !important;
                    font-family: $font-rounded;
                    font-size: 16px;
                    font-weight: 600;
                    padding: 12px 24px;
                    box-shadow: $shadow-button;
                    transition: all 0.3s ease;
                    &:hover {
                        transform: translateY(-2px);
                        box-shadow: 0 5px 0 $primary-darker, 0 8px 16px rgba(180, 140, 100, 0.3);
                        background: linear-gradient(135deg, #{$primary-dark}, #{$primary-darker}) !important;
                    }
                    &:active {
                        transform: translateY(1px);
                        box-shadow: 0 1px 0 $primary-darker, 0 2px 4px rgba(180, 140, 100, 0.15);
                    }
                }

                // 次按钮：虚线边框 + 透明底
                :deep(.btn-secondary) {
                    background: transparent !important;
                    border: 2px dashed $primary !important;
                    border-radius: $radius-lg !important;
                    color: $primary !important;
                    font-family: $font-rounded;
                    font-size: 16px;
                    font-weight: 600;
                    padding: 12px 24px;
                    transition: all 0.3s ease;
                    &:hover {
                        background: $primary-light !important;
                        border-style: solid !important;
                        color: $primary-dark !important;
                        transform: translateY(-2px);
                        box-shadow: 0 4px 12px rgba(180, 140, 100, 0.15);
                    }
                    &:active {
                        transform: translateY(1px);
                    }
                }
            }
        }

        // ===== 机器人容器：贴纸风格 + 呼吸光晕 =====
        .robot-sticker {
            position: relative;
            width: 280px;
            height: 280px;
            display: flex;
            justify-content: center;
            align-items: center;

            // 图钉装饰
            .robot-pin {
                position: absolute;
                top: -10px;
                right: 30px;
                font-size: 22px;
                z-index: 3;
                transform: rotate(-15deg);
                filter: drop-shadow(0 2px 4px rgba(0,0,0,0.15));
            }

            // 呼吸光晕
            .robot-glow {
                position: absolute;
                width: 240px;
                height: 240px;
                border-radius: 50%;
                background: radial-gradient(circle, rgba(232, 146, 124, 0.2) 0%, transparent 70%);
                animation: breathe 3s ease-in-out infinite;
                z-index: 0;
            }

            .robot {
                position: relative;
                z-index: 1;
                display: flex;
                justify-content: center;
                align-items: center;
                width: 260px;
                height: 260px;
                border-radius: 50%;
                // 手账风：米白底 + 暖色描边
                background: $bg-card;
                border: 3px solid $primary-light;
                box-shadow: $shadow-card, inset 0 1px 0 rgba(255, 255, 255, 0.5);
                transition: transform 0.4s ease, box-shadow 0.4s ease;
                // 顶部胶带装饰
                &::before {
                    content: '';
                    position: absolute;
                    top: -8px;
                    left: 50%;
                    transform: translateX(-50%) rotate(-3deg);
                    width: 70px;
                    height: 18px;
                    background: rgba(232, 146, 124, 0.3);
                    border-left: 1px dashed rgba(232, 146, 124, 0.4);
                    border-right: 1px dashed rgba(232, 146, 124, 0.4);
                    border-radius: 2px;
                }
                &:hover {
                    transform: rotate(5deg) scale(1.03);
                    box-shadow: 0 8px 24px rgba(180, 140, 100, 0.2), inset 0 1px 0 rgba(255, 255, 255, 0.5);
                }
                .robot-image {
                    transition: transform 0.3s ease;
                }
                &:hover .robot-image {
                    transform: scale(1.08);
                }
            }
        }
    }

    // ===== 底部手写签名装饰 =====
    .bottom-deco {
        position: absolute;
        bottom: 20px;
        left: 50%;
        transform: translateX(-50%);
        z-index: 1;
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 6px;
        .bottom-dashed {
            width: 120px;
            border-top: 2px dashed $divider;
        }
        .bottom-signature {
            font-family: $font-handwritten;
            font-size: 14px;
            color: $text-secondary;
        }
    }

    // ===== 响应式 =====
    @media (max-width: 768px) {
        // 小屏 content 竖排变高，轮播回文档流防重叠
        flex-direction: column;
        .banner-carousel {
            position: static;
            transform: none;
            margin-top: 20px;
        }
        .content {
            flex-direction: column;
            gap: 24px;
            .text {
                width: 90%;
                text-align: center;
                .dashed-divider {
                    margin-left: auto;
                    margin-right: auto;
                }
                .hero-actions {
                    justify-content: center;
                }
            }
        }
        .floating-decorations {
            .float-emoji {
                font-size: 1rem;
            }
        }
    }

    // ===== 首页轮播图（手账风纸卡）=====
    // 绝对定位脱流：不参与 flex 居中计算，保证标题/机器人位置不变；
    // top 依据 content 垂直居中（高约 280px）+ 40px 间距锚定在按钮下方空白区
    .banner-carousel {
        position: absolute;
        top: calc(50% + 180px);
        left: 50%;
        transform: translateX(-50%);
        width: min(820px, 90%); // 与 .content 内容宽对齐（500 + 40 + 280）
        background: $bg-card;
        border: 2px dashed $border-color;
        border-radius: $radius-lg;
        padding: 12px;
        box-shadow: $shadow-card;
        z-index: 2;

        .banner-item {
            position: relative;
            width: 100%;
            height: 100%;
            border-radius: $radius-md;
            overflow: hidden;

            .banner-image {
                width: 100%;
                height: 100%;
                display: block;
            }

            .banner-title {
                position: absolute;
                left: 0;
                right: 0;
                bottom: 0;
                padding: 8px 16px;
                background: linear-gradient(transparent, rgba(92, 74, 58, 0.55));
                color: #fff;
                font-size: 14px;
            }
        }
    }
}

// ===== 首次设置密码提醒弹窗 =====
.pwd-reminder-content {
    text-align: center;
    padding: 8px 0;
    .pwd-reminder-text {
        font-size: 18px;
        font-weight: 600;
        color: $text-primary;
        margin: 0 0 8px;
        font-family: $font-handwritten;
    }
    .pwd-reminder-sub {
        font-size: 14px;
        color: $text-secondary;
        margin: 0;
    }
}
</style>
