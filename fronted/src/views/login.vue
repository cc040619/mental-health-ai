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
            <el-form
                ref="ruleFormRef"
                :model="formData"
                :rules="rules"
                label-position="top"
            >
                <el-form-item label="用户名或邮箱" prop="username">
                    <el-input v-model="formData.username" size="large" placeholder="请输入用户名">
                        <!-- 🎨 手绘小图标前缀 -->
                        <template #prefix>👤</template>
                    </el-input>
                </el-form-item>
                <el-form-item label="密码" prop="password">
                    <el-input v-model="formData.password" size="large" placeholder="请输入密码" type="password" show-password @keyup.enter="submitForm(ruleFormRef)">
                        <template #prefix>🔒</template>
                    </el-input>
                </el-form-item>
                <el-button class="btn" size="large" type="primary" @click="submitForm(ruleFormRef)">登录</el-button>
            </el-form>
            <div class="footer">
                <p>还没有账户？<router-link to="/auth/register">去注册</router-link> 🧡</p>
            </div>
        </div>
    </div>
</template>
<script setup>
import { ref, reactive } from 'vue'
import { login } from '@/api/admin'
import { useRouter } from 'vue-router'

const ruleFormRef = ref()

const formData = reactive({
    username: '',
    password: ''
})
const rules = reactive({
    username: [
        { required: true, message: '请输入用户名', trigger: 'blur' }
    ],
    password: [
        { required: true, message: '请输入密码', trigger: 'blur' }
    ]
})

// 登录
const router = useRouter()
const submitForm = async (formEl) => {
    if (!formEl) return
    await formEl.validate((valid, fields) => {
        if (valid) {
            console.log(formData, 'formData')
            login(formData).then(data => {
                // 判断token是否存在
                if (!data.token) {
                    return console.error('登录失败')
                }
                // 登录成功，保存token和用户信息
                localStorage.setItem('token', data.token)
                localStorage.setItem('userInfo', JSON.stringify(data.userInfo))
                // 根据用户角色决定跳转的路径
                if (data.userInfo.userType === 2) {
                    router.push('/back/dashboard')
                } else {
                    router.push('/')
                }
            })
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
