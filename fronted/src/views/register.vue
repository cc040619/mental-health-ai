<template>
    <div class="container">
        <!-- 🎨 右上角返回登录入口 -->
        <router-link to="/auth/login" class="back-login">← 返回登录</router-link>
        <div class="title">
            <div class="title-text">
                <!-- 🎨 手绘装饰 emoji -->
                <h2>✨ 创建您的账户 📖</h2>
                <p>邮箱验证码一键注册</p>
            </div>
        </div>
        <div class="form-container">
            <el-form label-position="top" :model="formData" :rules="rules" ref="submitFormRef">
                <el-form-item label="邮箱" prop="email">
                    <el-input v-model="formData.email" placeholder="请输入QQ邮箱" size="large" @keyup.enter="handleSendCode">
                        <template #prefix>✉️</template>
                    </el-input>
                </el-form-item>
                <el-form-item label="验证码" prop="code">
                    <div class="code-row">
                        <el-input
                            v-model="formData.code"
                            placeholder="请输入6位验证码"
                            size="large"
                            maxlength="6"
                            @keyup.enter="submitForm(submitFormRef)"
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
                <el-form-item>
                    <el-button class="btn" type="primary" size="large" @click="submitForm(submitFormRef)">注册并登录</el-button>
                </el-form-item>
            </el-form>
        </div>
    </div>

</template>
<script setup>
import { ref, reactive, onBeforeUnmount } from 'vue'
import { sendRegisterCode, emailRegister } from '@/api/frontend'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'

const router = useRouter()

const formData = reactive({
    email: '',
    code: ''
})

const rules = reactive({
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

// 发送注册验证码
const handleSendCode = async () => {
    if (!formData.email) {
        ElMessage.warning('请先输入邮箱')
        return
    }
    const emailReg = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/
    if (!emailReg.test(formData.email)) {
        ElMessage.warning('邮箱格式不正确')
        return
    }
    try {
        await sendRegisterCode({ email: formData.email })
        ElMessage.success('验证码已发送，请查收邮箱')
        startCountdown()
    } catch (e) {
        // 响应拦截器已统一提示
    }
}

// 提交注册并登录
const submitFormRef = ref(null)
const submitForm = async (formEl) => {
    if (!formEl) return
    await formEl.validate(async (valid) => {
        if (!valid) return
        try {
            const data = await emailRegister({ email: formData.email, code: formData.code })
            // 审核开启：后端返回 message，无 token
            if (!data || !data.token) {
                ElMessage.info(data?.message || '已提交注册申请，审核通过后可登录')
                router.push('/auth/login')
                return
            }
            // 注册并登录成功
            localStorage.setItem('token', data.token)
            localStorage.setItem('userInfo', JSON.stringify(data.userInfo))
            ElMessage.success('注册成功，已自动登录 🎉')
            router.push('/')
        } catch (e) {
            // 响应拦截器已统一提示（如"该邮箱已注册，请直接登录"）
        }
    })
}
</script>

<style scoped lang="scss">
/* ===== 🎨 手绘手账治愈风 — 注册页 ===== */
/* Design Read: 心理健康应用注册页，手账治愈风格，SCSS + Element Plus :deep() 深度覆盖 */
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
    position: relative;
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

    /* 🎨 左上角返回登录链接 — 手写感小字 */
    .back-login {
        position: absolute;
        top: 16px;
        left: 20px;
        font-family: $rounded;
        font-size: 13px;
        color: $warm-gray;
        text-decoration: none;
        cursor: pointer;
        transition: color 0.3s ease;

        &:hover {
            color: $warm-orange;
        }
    }

    .title {
        .title-text {
            text-align: center;
            padding-top: 20px;

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
            margin-bottom: 18px;
        }

        /* 🎨 验证码行 — 输入框 + 获取验证码按钮 */
        .code-row {
            display: flex;
            gap: 10px;
            width: 100%;

            :deep(.el-input) {
                flex: 1;
            }

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

        /* 🎨 注册按钮 — 暖橙渐变 + 手写字体 + 便签立体感 */
        :deep(.el-button.btn) {
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

        /* 🎨 按钮所在 form-item 去掉底部间距 */
        :deep(.el-form-item:last-child) {
            margin-bottom: 0;
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
