<template>
    <div class="profile-deactivate">
        <div class="diary-card">
            <div class="tape tape-red"></div>
            <p class="card-title">⚠️ 账号注销</p>
            <p class="card-subtitle">请仔细阅读以下说明，确认后再进行操作</p>

            <!-- 数据删除说明 -->
            <div class="notice-box">
                <h4 class="notice-title">📌 注销说明</h4>
                <ul class="notice-list">
                    <li>注销后，你的账号将立即被<span class="warn-text">禁用</span>，无法再登录系统。</li>
                    <li>为保护数据安全，我们采用<span class="warn-text">软删除</span>方式：你的情绪日记、AI 咨询记录等数据不会被立即物理删除，但账号将无法使用。</li>
                    <li>收藏、浏览历史等记录将保留，账号将无法访问。</li>
                    <li>注销后如需恢复账号，请联系管理员处理。</li>
                    <li>此操作<span class="warn-text">不可自助撤回</span>，请谨慎操作。</li>
                </ul>
            </div>

            <!-- 二次确认：输入「确认注销」 -->
            <div class="confirm-section">
                <p class="confirm-label">请输入 <span class="confirm-keyword">确认注销</span> 以继续：</p>
                <el-input v-model="confirmText" placeholder="请输入：确认注销" class="confirm-input" />
            </div>

            <div class="form-actions">
                <el-button type="danger" :loading="loading" :disabled="confirmText !== '确认注销'" @click="handleDeactivate">
                    🚪 确认注销账号
                </el-button>
                <router-link to="/profile" class="cancel-link">再想想，返回个人中心</router-link>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deactivateAccount } from '@/api/user'

const emit = defineEmits(['deactivated'])

const confirmText = ref('')
const loading = ref(false)

const handleDeactivate = () => {
    ElMessageBox.confirm('这是最后一次确认，注销后账号将无法登录，确定继续吗？', '最终确认', {
        confirmButtonText: '我已了解，确认注销',
        cancelButtonText: '取消',
        type: 'warning',
        confirmButtonClass: 'el-button--danger'
    }).then(async () => {
        loading.value = true
        try {
            await deactivateAccount()
            ElMessage.success('账号已注销，即将跳转首页 🌷')
            emit('deactivated')
        } catch (e) {
            // 错误已由拦截器统一提示
        } finally {
            loading.value = false
        }
    }).catch(() => {})
}
</script>

<style lang="scss" scoped>
// ========== 手绘手账治愈风 - 局部主题变量 ==========
$bg-card: #FFFEF9;
$primary: #E8927C;
$primary-dark: #D9826B;
$primary-darker: #C4705C;
$primary-light: rgba(232, 146, 124, 0.15);
$accent-yellow: #F7E8A6;
$text-primary: #5C4A3A;
$text-regular: #6B5B4B;
$text-secondary: #8B7D6B;
$text-placeholder: #B8A898;
$border-color: #E8DCC8;
$border-light: #EFE6D5;
$divider: #E0D5C0;
$shadow-card: 0 4px 16px rgba(180, 140, 100, 0.12);
$radius-md: 12px;
$radius-xl: 24px;
$font-handwritten: "Ma Shan Zheng", "Comic Sans MS", "Segoe Print", "Bradley Hand", cursive;
$font-rounded: "Nunito", "Quicksand", "PingFang SC", "Microsoft YaHei", sans-serif;

// 危险色（仍偏暖，不使用冷色）
$danger: #C4705C;
$danger-dark: #A85A48;

.profile-deactivate {
    font-family: $font-rounded;
    color: $text-primary;
}

.diary-card {
    position: relative;
    background: $bg-card;
    border-radius: $radius-xl;
    padding: 32px 28px 28px;
    box-shadow: $shadow-card;
    border: 1px solid $border-light;

    .card-title {
        margin: 0 0 6px;
        font-size: 22px;
        font-weight: 600;
        color: $danger-dark;
        font-family: $font-handwritten;
        letter-spacing: 0.5px;
    }
    .card-subtitle {
        margin: 0 0 24px;
        font-size: 13px;
        color: $text-secondary;
    }
}

.tape {
    position: absolute;
    top: -10px;
    left: 36px;
    width: 110px;
    height: 30px;
    border-radius: 2px;
    background: rgba(196, 112, 92, 0.4);
    transform: rotate(-4deg);
    box-shadow: 0 2px 4px rgba(180, 140, 100, 0.15);
    z-index: 2;
    &::before, &::after {
        content: '';
        position: absolute;
        top: 0;
        bottom: 0;
        width: 6px;
        background: rgba(255, 255, 255, 0.4);
    }
    &::before { left: 0; clip-path: polygon(0 0, 100% 0, 75% 50%, 100% 100%, 0 100%); }
    &::after { right: 0; clip-path: polygon(0 0, 100% 0, 100% 100%, 25% 100%, 0 50%); }
}

.notice-box {
    background: rgba(247, 232, 166, 0.25);
    border-left: 4px solid $accent-yellow;
    border-radius: 0 $radius-md $radius-md 0;
    padding: 16px 18px;
    margin-bottom: 24px;

    .notice-title {
        margin: 0 0 10px;
        font-size: 15px;
        font-weight: 600;
        color: $text-primary;
        font-family: $font-handwritten;
    }
    .notice-list {
        margin: 0;
        padding-left: 18px;
        color: $text-regular;
        font-size: 13px;
        line-height: 1.9;
        li { margin-bottom: 4px; }
    }
    .warn-text {
        color: $danger-dark;
        font-weight: 600;
    }
}

.confirm-section {
    margin-bottom: 24px;
    .confirm-label {
        margin: 0 0 10px;
        font-size: 14px;
        color: $text-regular;
    }
    .confirm-keyword {
        color: $danger-dark;
        font-weight: 700;
        padding: 2px 6px;
        background: rgba(196, 112, 92, 0.12);
        border-radius: 4px;
    }
    :deep(.el-input__wrapper) {
        background: $bg-card;
        border-radius: $radius-md;
        box-shadow: 0 0 0 1px $border-color inset;
        &:hover { box-shadow: 0 0 0 1px $danger inset; }
        &.is-focus { box-shadow: 0 0 0 1px $danger inset, 0 0 0 3px rgba(196, 112, 92, 0.2); }
    }
    :deep(.el-input__inner) {
        color: $text-primary;
        font-family: $font-rounded;
        &::placeholder { color: $text-placeholder; }
    }
}

.form-actions {
    display: flex;
    align-items: center;
    gap: 16px;
    padding-top: 20px;
    border-top: 1px dashed $divider;

    :deep(.el-button--danger) {
        font-family: $font-rounded;
        background: linear-gradient(135deg, $danger, $danger-dark);
        border: none;
        color: #fff;
        border-radius: $radius-md;
        font-weight: 600;
        box-shadow: 0 3px 0 $danger-dark, 0 4px 8px rgba(168, 90, 72, 0.25);
        &:hover:not(:disabled) {
            transform: translateY(-1px);
            box-shadow: 0 4px 0 $danger-dark, 0 6px 12px rgba(168, 90, 72, 0.3);
        }
        &:active:not(:disabled) { transform: translateY(1px); }
        &:disabled {
            opacity: 0.5;
            cursor: not-allowed;
            background: #d8c9bb;
            box-shadow: none;
        }
    }
    .cancel-link {
        color: $text-secondary;
        text-decoration: none;
        font-size: 14px;
        border-bottom: 1px dashed $border-color;
        &:hover { color: $primary-dark; border-color: $primary; }
    }
}
</style>
