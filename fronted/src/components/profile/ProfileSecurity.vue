<template>
    <div class="profile-security">
        <!-- ===== 手账风：修改密码 ===== -->
        <div class="diary-card">
            <div class="tape tape-orange"></div>
            <p class="card-title">🔒 修改密码</p>
            <p class="card-subtitle">定期更换密码，守护你的小手账 🛡️</p>

            <el-form :model="pwdForm" :rules="pwdRules" ref="pwdFormRef" label-position="top" class="security-form">
                <el-form-item label="原密码" prop="oldPassword">
                    <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入原密码" />
                </el-form-item>
                <el-form-item label="新密码" prop="newPassword">
                    <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="至少 6 位新密码" />
                </el-form-item>
                <el-form-item label="确认新密码" prop="confirmPassword">
                    <el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="再次输入新密码" />
                </el-form-item>
            </el-form>
            <el-button type="primary" :loading="pwdLoading" @click="handleChangePassword">🔐 确认修改</el-button>
            <p class="form-tip">修改成功后需要重新登录</p>
        </div>

        <!-- ===== 手账风：绑定/换绑手机号 ===== -->
        <div class="diary-card">
            <div class="tape tape-yellow"></div>
            <p class="card-title">📱 绑定手机号</p>
            <p class="card-subtitle">通过邮箱验证码验证身份后绑定/换绑手机号</p>

            <el-form :model="phoneForm" :rules="phoneRules" ref="phoneFormRef" label-position="top" class="security-form">
                <el-form-item label="验证码" prop="code">
                    <div class="code-row">
                        <el-input v-model="phoneForm.code" placeholder="输入邮箱验证码" />
                        <el-button :disabled="phoneCooldown > 0" :loading="codeLoading" @click="handleSendPhoneCode" class="code-btn">
                            {{ phoneCooldown > 0 ? `${phoneCooldown}s 后重试` : '发送验证码 📮' }}
                        </el-button>
                    </div>
                    <p class="form-tip">验证码将发送至你绑定的邮箱</p>
                </el-form-item>
                <el-form-item label="新手机号" prop="phone">
                    <el-input v-model="phoneForm.phone" placeholder="请输入新手机号" maxlength="11" />
                </el-form-item>
            </el-form>
            <el-button type="primary" :loading="phoneLoading" @click="handleBindPhone">📱 确认绑定</el-button>
        </div>

        <!-- ===== 手账风：忘记密码 ===== -->
        <div class="diary-card">
            <div class="tape tape-green"></div>
            <p class="card-title">🔑 找回密码</p>
            <p class="card-subtitle">忘记密码？通过邮箱验证码重置</p>

            <el-form :model="resetForm" :rules="resetRules" ref="resetFormRef" label-position="top" class="security-form">
                <el-form-item label="邮箱" prop="email">
                    <el-input v-model="resetForm.email" placeholder="请输入注册邮箱" />
                </el-form-item>
                <el-form-item label="验证码" prop="code">
                    <div class="code-row">
                        <el-input v-model="resetForm.code" placeholder="输入邮箱验证码" />
                        <el-button :disabled="resetCooldown > 0" :loading="resetCodeLoading" @click="handleSendResetCode" class="code-btn">
                            {{ resetCooldown > 0 ? `${resetCooldown}s 后重试` : '发送验证码 📮' }}
                        </el-button>
                    </div>
                </el-form-item>
                <el-form-item label="新密码" prop="newPassword">
                    <el-input v-model="resetForm.newPassword" type="password" show-password placeholder="至少 6 位新密码" />
                </el-form-item>
            </el-form>
            <el-button type="primary" :loading="resetLoading" @click="handleResetPassword">🔑 重置密码</el-button>
        </div>
    </div>
</template>

<script setup>
import { ref, reactive, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { changePassword, bindPhone, sendPhoneBindCode, resetPassword, sendEmailCode } from '@/api/user'

const emit = defineEmits(['passwordChanged', 'phoneBound'])

// ==================== 修改密码 ====================
const pwdFormRef = ref()
const pwdForm = reactive({
    oldPassword: '',
    newPassword: '',
    confirmPassword: ''
})
const pwdRules = {
    oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
    newPassword: [
        { required: true, message: '请输入新密码', trigger: 'blur' },
        { min: 6, max: 50, message: '密码长度必须在 6 到 50 个字符之间', trigger: 'blur' }
    ],
    confirmPassword: [
        { required: true, message: '请再次输入新密码', trigger: 'blur' },
        { validator: (rule, value, callback) => value !== pwdForm.newPassword ? callback(new Error('两次输入的密码不一致')) : callback(), trigger: 'blur' }
    ]
}
const pwdLoading = ref(false)

const handleChangePassword = async () => {
    if (!pwdFormRef.value) return
    await pwdFormRef.value.validate(async (valid) => {
        if (!valid) return
        pwdLoading.value = true
        try {
            await changePassword({
                oldPassword: pwdForm.oldPassword,
                newPassword: pwdForm.newPassword
            })
            ElMessage.success('密码修改成功，请重新登录 🌷')
            emit('passwordChanged')
        } catch (e) {
            // 错误已由拦截器统一提示
        } finally {
            pwdLoading.value = false
        }
    })
}

// ==================== 绑定手机号 ====================
const phoneFormRef = ref()
const phoneForm = reactive({ code: '', phone: '' })
const phoneRules = {
    code: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
    phone: [
        { required: true, message: '请输入手机号', trigger: 'blur' },
        { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
    ]
}
const phoneLoading = ref(false)
const codeLoading = ref(false)
const phoneCooldown = ref(0)
let phoneTimer = null

const handleSendPhoneCode = async () => {
    codeLoading.value = true
    try {
        await sendPhoneBindCode()
        ElMessage.success('验证码已发送至你的邮箱 📮')
        phoneCooldown.value = 60
        phoneTimer = setInterval(() => {
            phoneCooldown.value--
            if (phoneCooldown.value <= 0) clearInterval(phoneTimer)
        }, 1000)
    } catch (e) {
        // 错误已由拦截器统一提示
    } finally {
        codeLoading.value = false
    }
}

const handleBindPhone = async () => {
    if (!phoneFormRef.value) return
    await phoneFormRef.value.validate(async (valid) => {
        if (!valid) return
        phoneLoading.value = true
        try {
            await bindPhone({ code: phoneForm.code, phone: phoneForm.phone })
            ElMessage.success('手机号绑定成功 📱')
            emit('phoneBound', phoneForm.phone)
            phoneForm.code = ''
            phoneForm.phone = ''
        } catch (e) {
            // 错误已由拦截器统一提示
        } finally {
            phoneLoading.value = false
        }
    })
}

// ==================== 忘记密码 ====================
const resetFormRef = ref()
const resetForm = reactive({ email: '', code: '', newPassword: '' })
const resetRules = {
    email: [
        { required: true, message: '请输入邮箱', trigger: 'blur' },
        { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
    ],
    code: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
    newPassword: [
        { required: true, message: '请输入新密码', trigger: 'blur' },
        { min: 6, max: 50, message: '密码长度必须在 6 到 50 个字符之间', trigger: 'blur' }
    ]
}
const resetLoading = ref(false)
const resetCodeLoading = ref(false)
const resetCooldown = ref(0)
let resetTimer = null

const handleSendResetCode = async () => {
    if (!resetFormRef.value) return
    await resetFormRef.value.validateField('email', async (valid) => {
        if (!valid) return
        resetCodeLoading.value = true
        try {
            await sendEmailCode(resetForm.email)
            ElMessage.success('验证码已发送至邮箱 📮')
            resetCooldown.value = 60
            resetTimer = setInterval(() => {
                resetCooldown.value--
                if (resetCooldown.value <= 0) clearInterval(resetTimer)
            }, 1000)
        } catch (e) {
            // 错误已由拦截器统一提示
        } finally {
            resetCodeLoading.value = false
        }
    })
}

const handleResetPassword = async () => {
    if (!resetFormRef.value) return
    await resetFormRef.value.validate(async (valid) => {
        if (!valid) return
        resetLoading.value = true
        try {
            await resetPassword({
                email: resetForm.email,
                code: resetForm.code,
                newPassword: resetForm.newPassword
            })
            ElMessage.success('密码重置成功，请使用新密码登录 🌷')
            emit('passwordChanged')
        } catch (e) {
            // 错误已由拦截器统一提示
        } finally {
            resetLoading.value = false
        }
    })
}

onUnmounted(() => {
    if (phoneTimer) clearInterval(phoneTimer)
    if (resetTimer) clearInterval(resetTimer)
})
</script>

<style lang="scss" scoped>
// ========== 手绘手账治愈风 - 局部主题变量 ==========
$bg-card: #FFFEF9;
$primary: #E8927C;
$primary-dark: #D9826B;
$primary-darker: #C4705C;
$primary-light: rgba(232, 146, 124, 0.15);
$accent-yellow: #F7E8A6;
$accent-green: #B8D8BE;
$text-primary: #5C4A3A;
$text-regular: #6B5B4B;
$text-secondary: #8B7D6B;
$text-placeholder: #B8A898;
$border-color: #E8DCC8;
$border-light: #EFE6D5;
$divider: #E0D5C0;
$shadow-card: 0 4px 16px rgba(180, 140, 100, 0.12);
$shadow-button: 0 3px 0 $primary-darker, 0 4px 8px rgba(180, 140, 100, 0.2);
$radius-md: 12px;
$radius-xl: 24px;
$font-handwritten: "Ma Shan Zheng", "Comic Sans MS", "Segoe Print", "Bradley Hand", cursive;
$font-rounded: "Nunito", "Quicksand", "PingFang SC", "Microsoft YaHei", sans-serif;

.profile-security {
    font-family: $font-rounded;
    color: $text-primary;
    display: flex;
    flex-direction: column;
    gap: 24px;
}

// ===== 手账风：便签卡片 =====
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
        color: $text-primary;
        font-family: $font-handwritten;
        letter-spacing: 0.5px;
    }
    .card-subtitle {
        margin: 0 0 24px;
        font-size: 13px;
        color: $text-secondary;
    }
}

// ===== 手账风：胶带装饰 =====
.tape {
    position: absolute;
    top: -10px;
    width: 110px;
    height: 30px;
    border-radius: 2px;
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
.tape-orange { left: 36px; background: rgba(232, 146, 124, 0.4); transform: rotate(-4deg); }
.tape-yellow { right: 36px; background: rgba(247, 232, 166, 0.55); transform: rotate(3deg); }
.tape-green { left: 36px; background: rgba(184, 216, 190, 0.5); transform: rotate(-2deg); }

// ===== 手账风：表单 =====
.security-form {
    max-width: 480px;
    margin-bottom: 20px;
    :deep(.el-form-item__label) {
        font-family: $font-rounded;
        color: $text-regular;
        font-weight: 600;
        font-size: 14px;
        padding-bottom: 4px;
    }
    :deep(.el-input__wrapper) {
        background: $bg-card;
        border-radius: $radius-md;
        box-shadow: 0 0 0 1px $border-color inset;
        &:hover { box-shadow: 0 0 0 1px $primary inset; }
        &.is-focus { box-shadow: 0 0 0 1px $primary inset, 0 0 0 3px rgba(232, 146, 124, 0.2); }
    }
    :deep(.el-input__inner) {
        color: $text-primary;
        font-family: $font-rounded;
        &::placeholder { color: $text-placeholder; }
    }
}

.code-row {
    display: flex;
    gap: 10px;
    width: 100%;
    :deep(.el-input) { flex: 1; }
    .code-btn {
        font-family: $font-rounded;
        background: $primary-light;
        border: 1px dashed $primary;
        color: $primary-dark;
        border-radius: $radius-md;
        white-space: nowrap;
        &:hover:not(:disabled) {
            background: $primary;
            color: #fff;
            border-style: solid;
        }
        &.is-disabled {
            background: #f5f0e8;
            border-color: $divider;
            color: $text-secondary;
            cursor: not-allowed;
        }
    }
}

.form-tip {
    margin: 8px 0 0;
    font-size: 12px;
    color: $text-secondary;
}

// ===== 手账风：主按钮 =====
:deep(.el-button--primary) {
    font-family: $font-rounded;
    background: linear-gradient(135deg, $primary, $primary-dark);
    border: none;
    color: #fff;
    border-radius: $radius-md;
    font-weight: 600;
    box-shadow: $shadow-button;
    &:hover {
        transform: translateY(-1px);
        box-shadow: 0 4px 0 $primary-darker, 0 6px 12px rgba(180, 140, 100, 0.25);
    }
    &:active { transform: translateY(1px); }
}
</style>
