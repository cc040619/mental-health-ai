<template>
    <div class="auth-layout">
        <!-- 左侧品牌区：像手账本的封面页 -->
        <div class="left-section">
            <!-- 装饰性纸胶带 -->
            <div class="tape tape-top"></div>
            <div class="tape tape-bottom"></div>

            <div class="content">
                <!-- 标题区带手绘装饰 emoji -->
                <h2 class="title">
                    <span class="title-deco">✨</span>
                    心理AI助手
                    <span class="title-deco">📖</span>
                </h2>

                <!-- 引言做成便签卡片 -->
                <div class="quote-card">
                    <p class="text">每个深夜，每个焦虑的时刻，我们都在这里。不必独自承受，让心与心的连接温暖您的每一天</p>
                </div>

                <!-- 机器人头像容器：贴纸风格 -->
                <div class="robot-sticker">
                    <div class="robot">
                        <el-image style="width: 90px;height:90px" :src="iconUrl" alt="心理AI助手" />
                    </div>
                    <!-- 装饰图钉 -->
                    <span class="pin pin-top">📌</span>
                </div>

                <!-- 底部装饰虚线与手写签名 -->
                <div class="footer-deco">
                    <div class="dashed-line"></div>
                    <span class="signature">— 你的情绪小树洞 🌱</span>
                </div>
            </div>
        </div>

        <!-- 右侧表单区：保持 router-view，仅做背景氛围 -->
        <div class="right-section">
            <!-- ===== 手账风：右侧底板装饰层（纯装饰，pointer-events:none 不影响表单交互）===== -->
            <div class="right-deco-layer">
                <span class="deco-tape deco-tape-1"></span>
                <span class="deco-tape deco-tape-2"></span>
                <div class="deco-note deco-note-1">
                    <span class="deco-note-text">🌿 深呼吸，你值得被温柔对待</span>
                </div>
                <div class="deco-note deco-note-2">
                    <span class="deco-note-text">☕ 今天也要好好照顾自己呀</span>
                </div>
                <span class="deco-doodle deco-doodle-1">✿</span>
                <span class="deco-doodle deco-doodle-2">♡</span>
                <span class="deco-doodle deco-doodle-3">☆</span>
                <span class="deco-stamp">📎</span>
            </div>
            <!-- 表单内容层：确保在装饰层之上 -->
            <div class="right-form-wrapper">
                <router-view></router-view>
            </div>
        </div>
    </div>
</template>
<script setup>

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

.auth-layout {
    display: flex;
    min-height: 100vh;
    // ===== 手账风：米黄色纸张背景 + 淡网格线 + 纸纹噪点 =====
    background:
        radial-gradient(circle at 20% 30%, rgba(232, 146, 124, 0.06) 0%, transparent 50%),
        radial-gradient(circle at 80% 70%, rgba(184, 216, 190, 0.08) 0%, transparent 40%),
        linear-gradient(rgba(255, 248, 231, 0.9), rgba(255, 248, 231, 0.9)),
        repeating-linear-gradient(
            0deg,
            transparent,
            transparent 31px,
            rgba(232, 146, 124, 0.08) 32px
        ),
        repeating-linear-gradient(
            90deg,
            transparent,
            transparent 31px,
            rgba(232, 146, 124, 0.08) 32px
        ),
        $bg-page;

    .left-section {
        position: relative;
        display: flex;
        justify-content: center;
        align-items: center;
        flex: 1;
        min-height: 100vh;
        // ===== 手账风：从青绿渐变改为暖米色纸张 + 柔和暖色渐变 =====
        background:
            linear-gradient(135deg, rgba(255, 254, 249, 0.95) 0%, rgba(255, 248, 231, 0.95) 100%),
            linear-gradient(90deg, rgba(232, 146, 124, 0.08) 0%, rgba(247, 232, 166, 0.08) 100%);
        border-right: 1px dashed $divider;
        overflow: hidden;

        // ===== 手账风：纸胶带装饰 =====
        .tape {
            position: absolute;
            width: 120px;
            height: 32px;
            border: 1px dashed rgba(255, 255, 255, 0.5);
            box-shadow: 0 2px 6px rgba(180, 140, 100, 0.1);

            &-top {
                top: 24px;
                left: 40px;
                background: rgba(232, 146, 124, 0.35);
                transform: rotate(-3deg);
            }

            &-bottom {
                bottom: 32px;
                right: 40px;
                background: rgba(247, 232, 166, 0.45);
                transform: rotate(2deg);
            }
        }

        .content {
            position: relative;
            z-index: 1;
            display: flex;
            flex-direction: column;
            justify-content: center;
            align-items: center;
            padding: 48px 32px;
            max-width: 560px;

            // ===== 手账风：标题改为手写体 + 暖橙色 =====
            .title {
                margin-bottom: 28px;
                font-family: $font-handwritten;
                font-size: 44px;
                font-weight: normal;
                color: $primary;
                text-shadow: 1px 1px 0 rgba(255, 255, 255, 0.8);
                letter-spacing: 2px;

                .title-deco {
                    display: inline-block;
                    margin: 0 8px;
                    font-size: 24px;
                    vertical-align: middle;
                    animation: gentle-float 3s ease-in-out infinite;

                    &:last-child {
                        animation-delay: 0.5s;
                    }
                }
            }

            // ===== 手账风：引言做成便签卡片（白底 + 大圆角 + 暖色阴影 + 胶带）=====
            .quote-card {
                padding: 28px 32px;
                background: $bg-card;
                border-radius: $radius-xl;
                box-shadow: $shadow-card, inset 0 0 0 1px rgba(232, 146, 124, 0.08);
                transform: rotate(-0.8deg);
                margin-bottom: 36px;
                position: relative;

                // 卡片顶部小胶带
                &::before {
                    content: '';
                    position: absolute;
                    top: -12px;
                    left: 50%;
                    transform: translateX(-50%) rotate(-1deg);
                    width: 80px;
                    height: 24px;
                    background: rgba(232, 146, 124, 0.4);
                    border: 1px dashed rgba(255, 255, 255, 0.6);
                    box-shadow: 0 2px 5px rgba(180, 140, 100, 0.1);
                }

                .text {
                    margin: 0;
                    width: auto;
                    font-family: $font-rounded;
                    font-size: 18px;
                    line-height: 1.8;
                    color: $text-regular;
                    text-align: center;
                }
            }

            // ===== 手账风：机器人头像改为贴纸风格 =====
            .robot-sticker {
                position: relative;
                display: inline-block;
                margin-bottom: 28px;

                .robot {
                    display: flex;
                    justify-content: center;
                    align-items: center;
                    width: 160px;
                    height: 160px;
                    border-radius: 50%;
                    border: 3px solid rgba(232, 146, 124, 0.3);
                    background: linear-gradient(135deg, rgba(255, 255, 255, 0.9) 0%, rgba(255, 248, 231, 0.9) 100%);
                    box-shadow: $shadow-card, 0 0 0 6px rgba(232, 146, 124, 0.08);
                    transition: transform 0.3s ease;

                    &:hover {
                        transform: scale(1.03) rotate(-1deg);
                    }
                }

                // 角落图钉装饰
                .pin {
                    position: absolute;
                    font-size: 24px;
                    filter: drop-shadow(0 2px 2px rgba(0, 0, 0, 0.1));

                    &-top {
                        top: 6px;
                        right: 18px;
                        transform: rotate(15deg);
                    }
                }
            }

            // ===== 手账风：底部虚线分隔 + 手写签名 =====
            .footer-deco {
                display: flex;
                flex-direction: column;
                align-items: center;
                gap: 12px;

                .dashed-line {
                    width: 120px;
                    height: 1px;
                    border-top: 2px dashed $primary;
                    opacity: 0.5;
                }

                .signature {
                    font-family: $font-handwritten;
                    font-size: 16px;
                    color: $text-secondary;
                    letter-spacing: 1px;
                }
            }
        }
    }

    .right-section {
        position: relative;
        display: flex;
        justify-content: center;
        align-items: center;
        flex: 1;
        min-height: 100vh;
        overflow: hidden;
        // ===== 手账风：右侧米白纸纹背景，让表单卡片自然浮现 =====
        background:
            radial-gradient(circle at 90% 10%, rgba(232, 146, 124, 0.04) 0%, transparent 40%),
            radial-gradient(circle at 10% 90%, rgba(184, 216, 190, 0.05) 0%, transparent 40%),
            linear-gradient(rgba(255, 254, 249, 0.95), rgba(255, 254, 249, 0.95)),
            repeating-linear-gradient(
                0deg,
                transparent,
                transparent 31px,
                rgba(232, 146, 124, 0.05) 32px
            ),
            $bg-page;

        // ===== 手账风：表单内容层（z-index:1 确保在装饰层之上）=====
        .right-form-wrapper {
            position: relative;
            z-index: 1;
            width: 100%;
            display: flex;
            justify-content: center;
            align-items: center;
        }

        // ===== 手账风：右侧底板装饰层（纯装饰，不影响交互）=====
        .right-deco-layer {
            position: absolute;
            inset: 0;
            pointer-events: none;
            z-index: 0;
            overflow: hidden;

            // 装饰：纸胶带条 - 右上角
            .deco-tape {
                position: absolute;
                border-radius: 3px;
            }
            .deco-tape-1 {
                top: 40px;
                right: 30px;
                width: 100px;
                height: 24px;
                background: rgba(232, 146, 124, 0.25);
                transform: rotate(12deg);
                box-shadow: 0 1px 3px rgba(180, 140, 100, 0.1);
            }
            // 装饰：纸胶带条 - 左下角
            .deco-tape-2 {
                bottom: 60px;
                left: 30px;
                width: 90px;
                height: 22px;
                background: rgba(247, 232, 166, 0.45);
                transform: rotate(-8deg);
                box-shadow: 0 1px 3px rgba(180, 140, 100, 0.1);
            }

            // 装饰：治愈小便签卡片
            .deco-note {
                position: absolute;
                padding: 10px 16px;
                background: $bg-card;
                border-radius: 12px;
                box-shadow: 0 2px 8px rgba(180, 140, 100, 0.1);
                border: 1px dashed rgba(232, 146, 124, 0.2);

                .deco-note-text {
                    font-size: 13px;
                    font-family: $font-handwritten;
                    color: $text-secondary;
                    white-space: nowrap;
                }
            }
            .deco-note-1 {
                top: 10%;
                right: 8%;
                transform: rotate(3deg);
            }
            .deco-note-2 {
                bottom: 15%;
                left: 8%;
                transform: rotate(-2deg);
            }

            // 装饰：手绘涂鸦符号
            .deco-doodle {
                position: absolute;
                font-family: $font-handwritten;
            }
            .deco-doodle-1 {
                top: 55%;
                right: 12%;
                font-size: 28px;
                color: rgba(184, 216, 190, 0.4);
                transform: rotate(15deg);
            }
            .deco-doodle-2 {
                top: 25%;
                left: 12%;
                font-size: 20px;
                color: rgba(245, 198, 198, 0.5);
                transform: rotate(-10deg);
            }
            .deco-doodle-3 {
                bottom: 35%;
                right: 18%;
                font-size: 18px;
                color: rgba(247, 232, 166, 0.6);
                transform: rotate(20deg);
            }

            // 装饰：回形针
            .deco-stamp {
                position: absolute;
                top: 8%;
                left: 10%;
                font-size: 28px;
                transform: rotate(-25deg);
                opacity: 0.35;
                filter: drop-shadow(0 1px 2px rgba(0, 0, 0, 0.1));
            }
        }
    }
}

// ===== 手账风：轻柔浮动动画（克制使用）=====
@keyframes gentle-float {
    0%, 100% {
        transform: translateY(0) rotate(0deg);
    }
    50% {
        transform: translateY(-4px) rotate(2deg);
    }
}

// ===== 手账风：响应式适配 =====
@media (max-width: 768px) {
    .auth-layout {
        flex-direction: column;

        .left-section {
            min-height: auto;
            padding: 48px 24px;
            border-right: none;
            border-bottom: 1px dashed $divider;

            .content {
                padding: 0;
                max-width: 100%;

                .title {
                    font-size: 32px;
                }

                .quote-card {
                    margin-bottom: 24px;
                    transform: rotate(0deg);

                    .text {
                        font-size: 16px;
                    }
                }
            }
        }

        .right-section {
            min-height: auto;
            padding: 32px 16px;

            // 小屏隐藏便签卡片，避免与表单重叠
            .right-deco-layer {
                .deco-note {
                    display: none;
                }
            }
        }
    }
}
</style>