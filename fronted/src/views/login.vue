<template>
    <div class="container">
        <div class="title">
            <router-link to="/" class="back-home">← 返回首页</router-link>
            <div class="title-text">
                <!-- 🎨 手绘装饰 emoji -->
                <h2>📖 登录您的账户 ✨</h2>
                <p>请输入您的登录信息</p>
            </div>
        </div>
        <div class="form-container">
            <!-- 🎨 登录方式切换 — 手账风标签页 -->
            <div class="tab-switch">
                <span
                    class="tab-item"
                    :class="{ active: loginMode === 'account' }"
                    @click="switchMode('account')"
                >👤 账号登录</span>
                <span
                    class="tab-item"
                    :class="{ active: loginMode === 'email' }"
                    @click="switchMode('email')"
                >✉️ 邮箱登录</span>
            </div>

            <!-- 🎨 账号密码登录表单 -->
            <el-form
                v-if="loginMode === 'account'"
                ref="accountFormRef"
                :model="accountForm"
                :rules="accountRules"
                label-position="top"
            >
                <el-form-item label="用户名或邮箱" prop="username">
                    <el-input v-model="accountForm.username" size="large" placeholder="请输入用户名">
                        <!-- 🎨 手绘小图标前缀 -->
                        <template #prefix>👤</template>
                    </el-input>
                </el-form-item>
                <el-form-item label="密码" prop="password">
                    <el-input v-model="accountForm.password" size="large" placeholder="请输入密码" type="password" show-password @keyup.enter="submitAccountForm(accountFormRef)">
                        <template #prefix>🔒</template>
                    </el-input>
                </el-form-item>
                <el-button class="btn" size="large" type="primary" @click="submitAccountForm(accountFormRef)">登录</el-button>
            </el-form>

            <!-- 🎨 QQ邮箱验证码登录表单 -->
            <el-form
                v-else
                ref="emailFormRef"
                :model="emailForm"
                :rules="emailRules"
                label-position="top"
            >
                <el-form-item label="QQ邮箱" prop="email">
                    <el-input v-model="emailForm.email" size="large" placeholder="请输入QQ邮箱">
                        <template #prefix>✉️</template>
                    </el-input>
                </el-form-item>
                <el-form-item label="验证码" prop="code">
                    <div class="code-row">
                        <el-input
                            v-model="emailForm.code"
                            size="large"
                            placeholder="请输入6位验证码"
                            maxlength="6"
                            @keyup.enter="submitEmailForm(emailFormRef)"
                        >
                            <template #prefix>🔑</template>
                        </el-input>
                        <el-button
                            class="send-btn"
                            size="large"
                            type="primary"
                            :disabled="countdown > 0"
                            @click="handleSendCode"
                        >
                            {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
                        </el-button>
                    </div>
                </el-form-item>
                <el-button class="btn" size="large" type="primary" @click="submitEmailForm(emailFormRef)">登录</el-button>
            </el-form>

            <div class="footer">
                <p>还没有账户？<router-link to="/auth/register">去注册</router-link> 🧡</p>
            </div>
        </div>
    </div>
</template>
<script setup>
import { ref, reactive, onBeforeUnmount } from 'vue'
import { login, sendEmailCode, emailLogin } from '@/api/admin'
import { ElMessage } from 'element-plus'
import { useRouter, useRoute } from 'vue-router'

// 当前登录方式：account 账号密码 / email 邮箱验证码
const loginMode = ref('account')

const accountFormRef = ref()
const emailFormRef = ref()

// 账号密码表单
const accountForm = reactive({
    username: '',
    password: ''
})
const accountRules = reactive({
    username: [
        { required: true, message: '请输入用户名', trigger: 'blur' }
    ],
    password: [
        { required: true, message: '请输入密码', trigger: 'blur' }
    ]
})

// 邮箱验证码表单
const emailForm = reactive({
    email: '',
    code: ''
})
const emailRules = reactive({
    email: [
        { required: true, message: '请输入邮箱', trigger: 'blur' },
        { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
    ],
    code: [
        { required: true, message: '请输入验证码', trigger: 'blur' },
        { min: 6, max: 6, message: '验证码为6位', trigger: 'blur' }
    ]
})

// 验证码倒计时
const countdown = ref(0)
let timer = null
const startCountdown = () => {
    countdown.value = 60
    timer = setInterval(() => {
        countdown.value--
        if (countdown.value <= 0) {
            clearInterval(timer)
            timer = null
        }
    }, 1000)
}
onBeforeUnmount(() => {
    if (timer) clearInterval(timer)
})

// 切换登录方式
const switchMode = (mode) => {
    loginMode.value = mode
}

// 处理登录成功的统一逻辑
const router = useRouter()
const route = useRoute()
const handleLoginSuccess = (data) => {
    if (!data.token) {
        ElMessage.error('登录失败')
        return
    }
    localStorage.setItem('token', data.token)
    localStorage.setItem('userInfo', JSON.stringify(data.userInfo))
    ElMessage.success('登录成功')
    // 登录过期跳转携带的回跳地址：仅允许站内路径（防开放重定向）
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : ''
    const safeRedirect = redirect.startsWith('/') && !redirect.startsWith('//') ? redirect : ''
    if (safeRedirect) {
        router.push(safeRedirect)
    } else if (data.userInfo.userType === 2) {
        router.push('/back/dashboard')
    } else if (data.userInfo.userType === 3) {
        router.push('/consultant/dashboard')
    } else {
        router.push('/')
    }
}

// 账号密码登录
const submitAccountForm = async (formEl) => {
    if (!formEl) return
    await formEl.validate((valid) => {
        if (valid) {
            login(accountForm).then(data => {
                handleLoginSuccess(data)
            }).catch(() => {})
        }
    })
}

// 发送邮箱验证码
const handleSendCode = async () => {
    if (!emailForm.email) {
        ElMessage.warning('请先输入邮箱')
        return
    }
    // 简单的邮箱格式校验
    const emailReg = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/
    if (!emailReg.test(emailForm.email)) {
        ElMessage.warning('邮箱格式不正确')
        return
    }
    try {
        await sendEmailCode({ email: emailForm.email })
        ElMessage.success('验证码已发送，请查收邮箱')
        startCountdown()
    } catch (e) {
        // 响应拦截器已统一提示
    }
}

// 邮箱验证码登录
const submitEmailForm = async (formEl) => {
    if (!formEl) return
    await formEl.validate((valid) => {
        if (valid) {
            emailLogin(emailForm).then(data => {
                handleLoginSuccess(data)
            }).catch(() => {})
        }
    })
}
</script>
<style scoped lang="scss">
/* ===== 🎨 手绘手账治愈风 — 登录页 ===== */
/* Design Read: 心理健康应用登录页，手账治愈风格，SCSS + Element Plus :deep() 深度覆盖 */
/* Dials: VARIANCE 7 / MOTION 3 / DENSITY 3 */
/* Accent Lock: #E8927C 暖珊瑚橙 — 全页统一，不漂移 */

$warm-orange: #E8927C;
$warm-orange-dark: #D67860;
$warm-orange-light: #F4B5A0;
$paper-bg: #FDF6E3;
$card-bg: #FFFEF9;
$warm-gray: #8B7E6E;
$warm-gray-light: #C4B5A4;
$deep-brown: #5C4A3A;

$handwritten: "Comic Sans MS", "Segoe Print", "Ma Shan Zheng", "Yuanti SC", "Hannotate SC", cursive;
$rounded: "Comic Sans MS", "Segoe UI Rounded", "Hiragino Sans GB", "Microsoft YaHei", sans-serif;

.container {
    width: 400px;
    max-width: 420px;
    margin: 0 auto;
    padding: 28px 24px;
    box-sizing: border-box;

    /* 🎨 米黄色纸张质感背景 + 淡网格线 + 纸纹噪点 */
    background-color: $paper-bg;
    background-image:
        linear-gradient(rgba(232, 146, 124, 0.05) 1px, transparent 1px),
        linear-gradient(90deg, rgba(232, 146, 124, 0.05) 1px, transparent 1px),
        radial-gradient(circle at 20% 80%, rgba(210, 180, 140, 0.04), transparent 50%),
        radial-gradient(circle at 80% 20%, rgba(255, 220, 180, 0.05), transparent 50%);
    background-size: 24px 24px, 24px 24px, 100% 100%, 100% 100%;
    border-radius: 20px;

    /* 🎨 柔和暖色阴影 — 纸张卷曲感 */
    box-shadow:
        0 2px 4px rgba(139, 126, 110, 0.08),
        0 8px 24px rgba(139, 126, 110, 0.12),
        0 20px 60px rgba(139, 126, 110, 0.06);

    .title {
        .back-home {
            /* 🎨 返回首页 — 手账风虚线下划线 + 箭头微动 */
            display: inline-flex;
            align-items: center;
            gap: 4px;
            margin-bottom: 36px;
            color: $warm-gray;
            font-family: $rounded;
            font-size: 14px;
            text-decoration: none;
            cursor: pointer;
            transition: color 0.3s ease;

            &:hover {
                color: $warm-orange;
            }

            .el-icon {
                font-size: 14px;
                transition: transform 0.3s ease;
            }

            &:hover .el-icon {
                transform: translateX(-3px);
            }

            span {
                border-bottom: 1px dashed $warm-gray-light;
                transition: border-color 0.3s ease;
            }

            &:hover span {
                border-color: $warm-orange;
            }
        }

        .title-text {
            text-align: center;

            /* 🎨 标题 — 手写感字体 + 暖橙色 */
            h2 {
                font-family: $handwritten;
                font-size: 32px;
                color: $warm-orange-dark;
                margin-bottom: 8px;
                letter-spacing: 1px;
                text-shadow: 1px 1px 0 rgba(232, 146, 124, 0.1);
            }

            p {
                font-family: $rounded;
                font-size: 16px;
                color: $warm-gray;
            }
        }
    }

    .form-container {
        position: relative;
        margin-top: 20px;
        padding: 28px 22px 20px;
        background-color: $card-bg;
        border-radius: 18px;

        /* 🎨 柔和暖色阴影 — 便签纸悬浮感 */
        box-shadow:
            0 1px 3px rgba(139, 126, 110, 0.06),
            0 4px 12px rgba(232, 146, 124, 0.1),
            0 12px 32px rgba(139, 126, 110, 0.08);

        /* 🎨 顶部胶带装饰 — 半透明暖橙色胶带条，略微旋转 */
        &::before {
            content: '';
            position: absolute;
            top: -10px;
            left: 50%;
            transform: translateX(-50%) rotate(-2deg);
            width: 90px;
            height: 22px;
            background: rgba(232, 146, 124, 0.35);
            border-radius: 2px;
            box-shadow: 0 1px 3px rgba(139, 126, 110, 0.1);
        }

        /* 🎨 右上角回形针装饰 */
        &::after {
            content: '📎';
            position: absolute;
            top: -8px;
            right: 14px;
            font-size: 20px;
            transform: rotate(15deg);
        }

        /* 🎨 登录方式切换标签页 — 手账风虚线分隔 + 暖橙下划线 */
        .tab-switch {
            display: flex;
            justify-content: space-around;
            margin-bottom: 24px;
            padding-bottom: 12px;
            border-bottom: 2px dashed $warm-gray-light;

            .tab-item {
                font-family: $handwritten;
                font-size: 16px;
                color: $warm-gray;
                cursor: pointer;
                padding: 4px 12px;
                border-radius: 10px;
                transition: all 0.3s ease;
                user-select: none;

                &:hover {
                    color: $warm-orange;
                }

                &.active {
                    color: $warm-orange-dark;
                    font-weight: bold;
                    position: relative;

                    &::after {
                        content: '';
                        position: absolute;
                        bottom: -14px;
                        left: 50%;
                        transform: translateX(-50%);
                        width: 40px;
                        height: 3px;
                        background: $warm-orange;
                        border-radius: 2px;
                    }
                }
            }
        }

        /* 🎨 el-form-item label — 手写感字体 */
        :deep(.el-form-item__label) {
            font-family: $handwritten;
            font-size: 15px;
            color: $deep-brown;
            padding-bottom: 6px;
            line-height: 1.6;
        }

        /* 🎨 el-input 深度定制 — 大圆角 + 虚线柔和描边 */
        :deep(.el-input__wrapper) {
            border-radius: 14px;
            background-color: #FFFCF5;
            box-shadow: 0 0 0 1.5px dashed $warm-gray-light;
            padding: 4px 12px;
            transition: all 0.3s ease;

            &:hover {
                box-shadow: 0 0 0 1.5px dashed $warm-orange;
            }

            &.is-focus {
                /* 🎨 focus 时暖橙色边框 + 柔和光晕 */
                box-shadow:
                    0 0 0 1.5px solid $warm-orange,
                    0 0 12px rgba(232, 146, 124, 0.2);
                background-color: #FFFEF9;
            }
        }

        :deep(.el-input__inner) {
            font-family: $rounded;
            color: $deep-brown;

            &::placeholder {
                color: $warm-gray-light;
            }
        }

        /* 🎨 输入框前缀 emoji 图标 */
        :deep(.el-input__prefix) {
            font-size: 16px;
            opacity: 0.8;
        }

        :deep(.el-form-item) {
            margin-bottom: 20px;
        }

        /* 🎨 验证码行 — 输入框 + 获取验证码按钮 */
        .code-row {
            display: flex;
            gap: 10px;
            width: 100%;

            :deep(.el-input) {
                flex: 1;
            }

            /* 🎨 获取验证码按钮 — 手写感字体 + 暖橙描边 */
            .send-btn {
                flex-shrink: 0;
                width: 130px;
                border: 1.5px dashed $warm-orange;
                background-color: #FFFCF5;
                color: $warm-orange-dark;
                font-family: $handwritten;
                font-size: 14px;
                border-radius: 14px;
                transition: all 0.25s ease;

                &:hover:not(:disabled) {
                    background-color: rgba(232, 146, 124, 0.12);
                    border-style: solid;
                }

                &:disabled {
                    color: $warm-gray-light;
                    border-color: $warm-gray-light;
                    cursor: not-allowed;
                    background-color: #F5F0E8;
                }
            }
        }

        /* 🎨 登录按钮 — 暖橙渐变 + 手写字体 + 便签立体感 */
        :deep(.el-button.btn) {
            margin-top: 24px;
            width: 100%;
            border: none;
            border-radius: 14px;
            font-family: $handwritten;
            font-size: 18px;
            letter-spacing: 2px;
            background: linear-gradient(135deg, $warm-orange 0%, $warm-orange-dark 100%);
            color: #fff;
            box-shadow:
                0 3px 0 $warm-orange-dark,
                0 4px 12px rgba(232, 146, 124, 0.3);
            transition: all 0.25s ease;

            /* 🎨 hover — 轻微上浮 + 阴影加深 */
            &:hover {
                transform: translateY(-2px);
                background: linear-gradient(135deg, $warm-orange-light 0%, $warm-orange 100%);
                box-shadow:
                    0 5px 0 $warm-orange-dark,
                    0 8px 20px rgba(232, 146, 124, 0.4);
            }

            /* 🎨 active — 按下效果 */
            &:active {
                transform: translateY(1px);
                box-shadow:
                    0 1px 0 $warm-orange-dark,
                    0 2px 6px rgba(232, 146, 124, 0.3);
            }
        }

        /* 🎨 底部链接 — 虚线分隔 + 暖橙色注册链接 */
        .footer {
            margin-top: 20px;
            padding: 16px 0 0;
            text-align: center;
            border-top: 2px dashed $warm-gray-light;

            p {
                font-family: $rounded;
                font-size: 14px;
                color: $warm-gray;

                a {
                    color: $warm-orange;
                    font-family: $handwritten;
                    font-size: 15px;
                    text-decoration: underline dashed $warm-orange;
                    text-underline-offset: 3px;
                    transition: all 0.3s ease;

                    /* 🎨 hover — 虚线变实线 */
                    &:hover {
                        text-decoration: underline solid $warm-orange-dark;
                        color: $warm-orange-dark;
                    }
                }
            }
        }
    }
}

/* 🎨 响应式 — 小屏适配 */
@media (max-width: 480px) {
    .container {
        width: 100%;
        max-width: 420px;
    }
}
</style>
