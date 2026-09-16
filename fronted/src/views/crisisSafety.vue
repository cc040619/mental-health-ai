<template>
    <!-- ===== 安全计划页：顶部危机热线卡 + 安全计划编辑器 ===== -->
    <div class="crisis-safety-page">
        <!-- 纸张纹理 -->
        <div class="paper-texture"></div>

        <!-- 顶部胶带装饰 -->
        <div class="tape tape-left"></div>
        <div class="tape tape-right"></div>

        <div class="page-content">
            <!-- 页头 -->
            <div class="page-header">
                <h1 class="page-title"><span class="title-icon">📒</span> 我的安全计划</h1>
                <p class="page-desc">提前记下可以信赖的人和应对方式，难熬时翻一翻，会有人在你身边。</p>
            </div>

            <!-- 顶部危机热线卡（banner 形态，复用全站唯一组件） -->
            <div class="hotline-section">
                <CrisisSupportCard mode="banner" @open-plan="scrollToPlan"/>
            </div>

            <!-- 安全计划编辑器 -->
            <div ref="planRef" class="plan-section">
                <CrisisSafetyPlan @saved="onSaved"/>
            </div>

            <!-- 底部陪伴语 -->
            <div class="bottom-sign">
                <div class="bottom-dashed"></div>
                <span>— 你值得被关心，也可以寻求帮助 🌿</span>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import CrisisSupportCard from '@/components/CrisisSupportCard.vue'
import CrisisSafetyPlan from '@/components/CrisisSafetyPlan.vue'

const router = useRouter()
const planRef = ref(null)

// 危机卡片「填写安全计划」入口 → 滚动到编辑器
const scrollToPlan = () => {
    if (planRef.value) {
        planRef.value.scrollIntoView({behavior: 'smooth', block: 'start'})
    }
}

const onSaved = () => {
    ElMessage.success('已保存，随时可以回来修改')
}
</script>

<style scoped lang="scss">
// ===== 手账风：安全计划页 =====
$bg-page: #FFF8E7;
$bg-card: #FFFEF9;
$primary: #E8927C;
$primary-dark: #D9826B;
$primary-darker: #C4705C;
$accent-yellow: #F7E8A6;
$text-primary: #5C4A3A;
$text-regular: #6B5B4B;
$text-secondary: #8B7D6B;
$border-color: #E8DCC8;
$divider: #E0D5C0;
$shadow-card: 0 4px 16px rgba(180, 140, 100, 0.12);
$radius-lg: 16px;
$font-handwritten: "Ma Shan Zheng", "Comic Sans MS", "Segoe Print", "Bradley Hand", cursive;
$font-rounded: "Nunito", "Quicksand", "PingFang SC", "Microsoft YaHei", sans-serif;

.crisis-safety-page {
    position: relative;
    min-height: calc(100vh - 64px);
    background-color: $bg-page;
    font-family: $font-rounded;
    overflow: hidden;
    padding: 24px 16px 40px;
}

.paper-texture {
    position: absolute;
    inset: 0;
    background-image:
        linear-gradient(rgba(232, 220, 200, 0.18) 1px, transparent 1px),
        linear-gradient(90deg, rgba(232, 220, 200, 0.18) 1px, transparent 1px);
    background-size: 28px 28px;
    pointer-events: none;
    z-index: 0;
}

.tape {
    position: absolute;
    top: 12px;
    width: 120px;
    height: 26px;
    background: linear-gradient(90deg,
    rgba(247, 232, 166, 0.6) 0%,
    rgba(232, 146, 124, 0.4) 50%,
    rgba(247, 232, 166, 0.6) 100%);
    border-radius: 3px;
    box-shadow: 0 2px 6px rgba(180, 140, 100, 0.15);
    z-index: 1;

    &.tape-left {
        left: 8%;
        transform: rotate(-4deg);
    }

    &.tape-right {
        right: 8%;
        transform: rotate(4deg);
    }
}

.page-content {
    position: relative;
    z-index: 2;
    max-width: 760px;
    margin: 0 auto;
}

.page-header {
    text-align: center;
    margin-bottom: 24px;
    margin-top: 12px;
}

.page-title {
    font-family: $font-handwritten;
    font-size: 30px;
    font-weight: 600;
    color: $text-primary;
    margin: 0 0 8px;
    letter-spacing: 1px;
}

.title-icon {
    font-size: 26px;
}

.page-desc {
    font-size: 14px;
    color: $text-secondary;
    line-height: 1.6;
    margin: 0;
}

.hotline-section {
    margin-bottom: 28px;
}

.plan-section {
    background: transparent;
}

.bottom-sign {
    text-align: center;
    margin-top: 36px;
    color: $text-secondary;
    font-size: 13px;
    font-family: $font-handwritten;
}

.bottom-dashed {
    width: 60%;
    margin: 0 auto 12px;
    border-top: 1px dashed $divider;
}

@media (max-width: 768px) {
    .page-title {
        font-size: 24px;
    }

    .tape {
        width: 80px;
    }
}
</style>
