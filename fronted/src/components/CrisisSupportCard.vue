<template>
    <!-- ===== 危机干预卡片：全站唯一实现，三态复用 ===== -->
    <!-- dialog 模式：el-dialog 包裹，对话页/测评报告弹窗用 -->
    <el-dialog v-if="mode === 'dialog'" v-model="dialogVisible" width="520px" append-to-body
               :show-close="true" :close-on-click-modal="false" align-center class="crisis-dialog">
        <template #header>
            <span class="dialog-title">🆘 {{ crisisCopy.title }}</span>
        </template>
        <div class="crisis-card">
            <p class="card-subtitle">{{ crisisCopy.subtitle }}</p>
            <div class="hotline-hint">{{ crisisCopy.hotlineHint }}</div>
            <div class="hotline-list">
                <div v-for="h in crisisHotlines" :key="h.phone" class="hotline-item">
                    <div class="hotline-info">
                        <div class="hotline-name">{{ h.name }}</div>
                        <div class="hotline-phone">{{ h.phone }}</div>
                        <div class="hotline-note">{{ h.note }}</div>
                    </div>
                    <div class="hotline-actions">
                        <a :href="telLink(h.phone)" class="action-btn call-btn">📞 {{ crisisCopy.callBtn }}</a>
                        <button class="action-btn copy-btn" @click="onCopy(h.phone)">📋 {{ crisisCopy.copyBtn }}</button>
                    </div>
                </div>
            </div>
            <div class="plan-entry" @click="emit('open-plan')">
                <span class="plan-icon">📒</span>
                <div class="plan-text">
                    <div class="plan-title">{{ crisisCopy.planEntry }}</div>
                    <div class="plan-hint">{{ crisisCopy.planHint }}</div>
                </div>
                <span class="plan-arrow">›</span>
            </div>
            <p class="card-footer">{{ crisisCopy.footer }}</p>
        </div>
    </el-dialog>

    <!-- floating 模式：右下角常驻按钮，点击展开卡片 -->
    <div v-else-if="mode === 'floating'" class="crisis-floating">
        <transition name="crisis-pop">
            <div v-if="expanded" class="crisis-card floating-card">
                <button class="card-close" @click="expanded = false" aria-label="关闭">✕</button>
                <p class="card-subtitle">{{ crisisCopy.subtitle }}</p>
                <div class="hotline-hint">{{ crisisCopy.hotlineHint }}</div>
                <div class="hotline-list">
                    <div v-for="h in crisisHotlines" :key="h.phone" class="hotline-item">
                        <div class="hotline-info">
                            <div class="hotline-name">{{ h.name }}</div>
                            <div class="hotline-phone">{{ h.phone }}</div>
                            <div class="hotline-note">{{ h.note }}</div>
                        </div>
                        <div class="hotline-actions">
                            <a :href="telLink(h.phone)" class="action-btn call-btn">📞 {{ crisisCopy.callBtn }}</a>
                            <button class="action-btn copy-btn" @click="onCopy(h.phone)">📋 {{ crisisCopy.copyBtn }}</button>
                        </div>
                    </div>
                </div>
                <div class="plan-entry" @click="emit('open-plan')">
                    <span class="plan-icon">📒</span>
                    <div class="plan-text">
                        <div class="plan-title">{{ crisisCopy.planEntry }}</div>
                        <div class="plan-hint">{{ crisisCopy.planHint }}</div>
                    </div>
                    <span class="plan-arrow">›</span>
                </div>
                <p class="card-footer">{{ crisisCopy.footer }}</p>
            </div>
        </transition>
        <button class="crisis-fab" @click="expanded = !expanded" aria-label="危机干预">
            <span class="fab-pulse"></span>
            <span class="fab-icon">🆘</span>
            <span class="fab-text">危机干预</span>
        </button>
    </div>

    <!-- banner 模式：顶部横幅（社区危机帖/首页用） -->
    <div v-else class="crisis-card banner-card">
        <span class="banner-tape">📎</span>
        <p class="card-subtitle">{{ crisisCopy.subtitle }}</p>
        <div class="hotline-hint">{{ crisisCopy.hotlineHint }}</div>
        <div class="hotline-list">
            <div v-for="h in crisisHotlines" :key="h.phone" class="hotline-item">
                <div class="hotline-info">
                    <div class="hotline-name">{{ h.name }}</div>
                    <div class="hotline-phone">{{ h.phone }}</div>
                    <div class="hotline-note">{{ h.note }}</div>
                </div>
                <div class="hotline-actions">
                    <a :href="telLink(h.phone)" class="action-btn call-btn">📞 {{ crisisCopy.callBtn }}</a>
                    <button class="action-btn copy-btn" @click="onCopy(h.phone)">📋 {{ crisisCopy.copyBtn }}</button>
                </div>
            </div>
        </div>
        <div class="plan-entry" @click="emit('open-plan')">
            <span class="plan-icon">📒</span>
            <div class="plan-text">
                <div class="plan-title">{{ crisisCopy.planEntry }}</div>
                <div class="plan-hint">{{ crisisCopy.planHint }}</div>
            </div>
            <span class="plan-arrow">›</span>
        </div>
        <p class="card-footer">{{ crisisCopy.footer }}</p>
    </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { crisisHotlines as defaultHotlines, crisisCopy as defaultCopy, telLink, copyPhone } from '@/config/crisis'
import { getCrisisConfig } from '@/api/frontend'

// 危机干预卡片（全站唯一实现）：dialog/banner/floating 三态由 props.mode 控制
// 对话页 SSE crisis 事件 / 测评报告 / 社区危机帖 / 首页均引用本组件，禁止各自实现
const props = defineProps({
    mode: { type: String, default: 'dialog' }, // dialog / banner / floating
    visible: { type: Boolean, default: false }, // dialog 模式 v-model:visible
    keyword: { type: String, default: '' } // 命中词（仅日志用，不展示给用户保护隐私）
})
const emit = defineEmits(['update:visible', 'open-plan'])

const expanded = ref(false) // floating 模式展开状态

// dialog 模式 v-model 代理
const dialogVisible = computed({
    get: () => props.visible,
    set: (v) => emit('update:visible', v)
})

// 热线与提示文案：默认用 config/crisis.js 常量兜底，挂载后静默拉取管理端配置
const crisisHotlines = ref(defaultHotlines)
const crisisCopy = ref({ ...defaultCopy })

onMounted(() => {
    getCrisisConfig().then(cfg => {
        if (Array.isArray(cfg?.hotlines) && cfg.hotlines.length) {
            crisisHotlines.value = cfg.hotlines
        }
        if (cfg?.promptText) {
            crisisCopy.value = { ...defaultCopy, hotlineHint: cfg.promptText }
        }
    }).catch(() => {
        // 接口失败保持常量兜底，卡片正常展示
    })
})

// 复制号码：Clipboard API 不可用时降级选中文本
const onCopy = async (phone) => {
    const ok = await copyPhone(phone)
    if (ok) {
        ElMessage.success('已复制号码，请拨打')
    } else {
        ElMessage.warning('复制失败，请手动记录号码拨打')
    }
}
</script>

<style scoped lang="scss">
// ===== 手账风：危机卡片局部主题变量（暖橙警示强调）=====
$bg-page: #FFF8E7;
$bg-card: #FFFEF9;
$primary: #E8927C;
$primary-dark: #D9826B;
$primary-darker: #C4705C;
$primary-light: rgba(232, 146, 124, 0.15);
$accent-yellow: #F7E8A6;
$accent-warm: #F5C6A0;
$text-primary: #5C4A3A;
$text-regular: #6B5B4B;
$text-secondary: #8B7D6B;
$border-color: #E8DCC8;
$divider: #E0D5C0;
$shadow-card: 0 4px 16px rgba(180, 140, 100, 0.18);
$shadow-button: 0 2px 0 $primary-darker, 0 3px 8px rgba(180, 140, 100, 0.2);
$radius-md: 12px;
$radius-lg: 16px;
$font-handwritten: "Ma Shan Zheng", "Comic Sans MS", "Segoe Print", "Bradley Hand", cursive;
$font-rounded: "Nunito", "Quicksand", "PingFang SC", "Microsoft YaHei", sans-serif;

// ===== 卡片基础样式（三态共享）=====
.crisis-card {
    background: linear-gradient(180deg, #FFF9F0 0%, $bg-card 100%);
    border: 2px solid $primary;
    border-radius: $radius-lg;
    box-shadow: $shadow-card;
    padding: 22px 22px 18px;
    font-family: $font-rounded;
    color: $text-primary;
    position: relative;
}

.card-subtitle {
    font-size: 15px;
    color: $text-regular;
    line-height: 1.7;
    margin: 0 0 14px;
    font-weight: 500;
}

.hotline-hint {
    background: $accent-yellow;
    border-left: 3px solid $primary-dark;
    padding: 8px 12px;
    border-radius: 8px;
    font-size: 13px;
    color: $text-primary;
    margin-bottom: 14px;
    line-height: 1.6;
}

.hotline-list {
    display: flex;
    flex-direction: column;
    gap: 10px;
    margin-bottom: 14px;
}

.hotline-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 12px;
    padding: 10px 12px;
    background: $bg-page;
    border: 1px dashed $border-color;
    border-radius: $radius-md;
    transition: all 0.2s ease;

    &:hover {
        border-color: $primary;
        background: $primary-light;
    }
}

.hotline-info {
    flex: 1;
    min-width: 0;
}

.hotline-name {
    font-size: 14px;
    font-weight: 600;
    color: $text-primary;
    margin-bottom: 2px;
}

.hotline-phone {
    font-family: $font-handwritten;
    font-size: 20px;
    font-weight: 700;
    color: $primary-dark;
    letter-spacing: 1px;
    margin-bottom: 2px;
}

.hotline-note {
    font-size: 11px;
    color: $text-secondary;
    line-height: 1.4;
}

.hotline-actions {
    display: flex;
    flex-direction: column;
    gap: 6px;
    flex-shrink: 0;
}

.action-btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 4px;
    padding: 6px 14px;
    font-size: 13px;
    border-radius: $radius-md;
    text-decoration: none;
    cursor: pointer;
    border: none;
    font-family: $font-rounded;
    font-weight: 600;
    transition: all 0.2s ease;
    white-space: nowrap;
}

.call-btn {
    background: linear-gradient(135deg, $primary, $primary-dark);
    color: #fff;
    box-shadow: $shadow-button;

    &:hover {
        transform: translateY(-1px);
        box-shadow: 0 3px 0 $primary-darker, 0 5px 12px rgba(180, 140, 100, 0.28);
    }

    &:active {
        transform: translateY(1px);
    }
}

.copy-btn {
    background: $bg-card;
    color: $primary-dark;
    border: 1px dashed $primary;

    &:hover {
        background: $primary-light;
    }
}

.plan-entry {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 10px 12px;
    background: $bg-page;
    border: 1px solid $border-color;
    border-radius: $radius-md;
    cursor: pointer;
    transition: all 0.2s ease;
    margin-bottom: 14px;

    &:hover {
        border-color: $primary;
        background: $primary-light;
    }
}

.plan-icon {
    font-size: 20px;
}

.plan-text {
    flex: 1;
}

.plan-title {
    font-size: 14px;
    font-weight: 600;
    color: $text-primary;
}

.plan-hint {
    font-size: 11px;
    color: $text-secondary;
    margin-top: 2px;
}

.plan-arrow {
    font-size: 22px;
    color: $primary;
    font-weight: bold;
}

.card-footer {
    font-size: 12px;
    color: $text-secondary;
    line-height: 1.6;
    margin: 0;
    padding-top: 10px;
    border-top: 1px dashed $divider;
    text-align: center;
}

// ===== dialog 模式 =====
:deep(.crisis-dialog) {
    .el-dialog__header {
        background: linear-gradient(135deg, $primary, $primary-dark);
        border-radius: $radius-lg $radius-lg 0 0;
        padding: 14px 20px;
        margin: 0;
    }

    .dialog-title {
        color: #fff;
        font-family: $font-handwritten;
        font-size: 18px;
        font-weight: 600;
    }

    .el-dialog__headerbtn .el-dialog__close {
        color: #fff;
        font-size: 18px;
    }

    .el-dialog__body {
        padding: 0;
    }

    .el-dialog {
        border-radius: $radius-lg;
        overflow: hidden;
        border: 2px solid $primary-dark;
    }
}

// ===== floating 模式：右下角常驻按钮 =====
.crisis-floating {
    position: fixed;
    right: 20px;
    bottom: 80px;
    z-index: 999;
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    gap: 12px;
}

.floating-card {
    width: 340px;
    max-width: calc(100vw - 40px);
    max-height: 70vh;
    overflow-y: auto;
}

.card-close {
    position: absolute;
    top: 8px;
    right: 10px;
    width: 24px;
    height: 24px;
    border: none;
    background: $primary-light;
    color: $primary-dark;
    border-radius: 50%;
    cursor: pointer;
    font-size: 14px;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all 0.2s ease;

    &:hover {
        background: $primary;
        color: #fff;
    }
}

.crisis-fab {
    position: relative;
    width: 56px;
    height: 56px;
    border-radius: 50%;
    border: 2px solid $primary-dark;
    background: linear-gradient(135deg, $primary, $primary-dark);
    color: #fff;
    cursor: pointer;
    box-shadow: 0 4px 16px rgba(196, 112, 92, 0.4);
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 1px;
    transition: all 0.25s ease;
    overflow: visible;

    &:hover {
        transform: scale(1.08);
        box-shadow: 0 6px 20px rgba(196, 112, 92, 0.5);
    }
}

.fab-pulse {
    position: absolute;
    inset: -4px;
    border-radius: 50%;
    border: 2px solid $primary;
    animation: crisis-pulse 2s ease-out infinite;
    pointer-events: none;
}

@keyframes crisis-pulse {
    0% {
        transform: scale(1);
        opacity: 0.8;
    }
    100% {
        transform: scale(1.4);
        opacity: 0;
    }
}

.fab-icon {
    font-size: 20px;
    line-height: 1;
}

.fab-text {
    font-size: 9px;
    font-weight: 600;
    line-height: 1;
    font-family: $font-rounded;
}

// ===== banner 模式：顶部横幅 =====
.banner-card {
    position: relative;
    margin: 0;
    overflow: hidden;
}

.banner-tape {
    position: absolute;
    top: -2px;
    left: 20px;
    font-size: 18px;
    transform: rotate(-8deg);
    background: $accent-yellow;
    padding: 2px 8px;
    border-radius: 4px;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

// ===== 展开动画 =====
.crisis-pop-enter-active,
.crisis-pop-leave-active {
    transition: all 0.3s ease;
    transform-origin: bottom right;
}

.crisis-pop-enter-from,
.crisis-pop-leave-to {
    opacity: 0;
    transform: translateY(10px) scale(0.95);
}

// ===== 响应式 =====
@media (max-width: 768px) {
    .floating-card {
        width: calc(100vw - 40px);
    }

    .hotline-item {
        flex-direction: column;
        align-items: stretch;

        .hotline-actions {
            flex-direction: row;
            justify-content: stretch;

            .action-btn {
                flex: 1;
            }
        }
    }

    .crisis-fab {
        width: 52px;
        height: 52px;
    }
}
</style>
