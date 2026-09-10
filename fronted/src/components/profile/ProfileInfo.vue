<template>
    <div class="profile-info">
        <!-- ===== 手账风：便签卡片 - 个人资料 ===== -->
        <div class="diary-card">
            <div class="tape tape-orange"></div>
            <p class="card-title">📌 个人资料</p>
            <p class="card-subtitle">在这里完善你的小档案，让手账本更懂你 ✏️</p>

            <!-- 头像上传 -->
            <div class="avatar-section">
                <div class="avatar-wrapper">
                    <el-image :src="avatarPreview" class="avatar-img" fit="cover">
                        <template #error>
                            <div class="avatar-placeholder">📷</div>
                        </template>
                    </el-image>
                    <el-upload
                        class="avatar-uploader"
                        :show-file-list="false"
                        :http-request="handleAvatarUpload"
                        accept="image/*"
                        :before-upload="beforeAvatarUpload"
                    >
                        <div class="upload-btn">更换头像 📷</div>
                    </el-upload>
                </div>
                <p class="avatar-tip">支持 JPG/PNG，大小不超过 2MB</p>
            </div>

            <!-- 资料表单 -->
            <el-form :model="form" label-position="top" class="profile-form">
                <el-form-item label="昵称">
                    <el-input v-model="form.nickname" placeholder="写下你的昵称 🌷" maxlength="50" show-word-limit />
                </el-form-item>

                <el-form-item label="性别">
                    <el-radio-group v-model="form.gender">
                        <el-radio :value="1">男生</el-radio>
                        <el-radio :value="2">女生</el-radio>
                        <el-radio :value="0">保密</el-radio>
                    </el-radio-group>
                </el-form-item>

                <el-form-item label="生日">
                    <el-date-picker
                        v-model="form.birthday"
                        type="date"
                        placeholder="选择你的生日 🎂"
                        format="YYYY-MM-DD"
                        value-format="YYYY-MM-DD"
                        style="width: 100%"
                    />
                </el-form-item>

                <el-form-item label="个人简介">
                    <el-input
                        v-model="form.bio"
                        type="textarea"
                        :rows="4"
                        placeholder="写点什么介绍自己吧～ 📝"
                        maxlength="500"
                        show-word-limit
                    />
                </el-form-item>
            </el-form>

            <div class="form-actions">
                <el-button type="primary" :loading="saving" @click="handleSave">💾 保存资料</el-button>
                <el-button @click="handleReset">↺ 重置</el-button>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { getProfile, updateProfile } from '@/api/user'
import service from '@/utils/request'

const emit = defineEmits(['updated'])

const form = ref({
    nickname: '',
    avatar: '',
    gender: 0,
    birthday: null,
    bio: ''
})

const saving = ref(false)
// 头像预览：优先用已上传的 URL，其次本地选择后的 blob
const avatarPreview = computed(() => form.value.avatar || '')

// 加载用户资料
const loadProfile = async () => {
    try {
        const data = await getProfile()
        form.value = {
            nickname: data.nickname || '',
            avatar: data.avatar || '',
            gender: data.gender != null ? data.gender : 0,
            birthday: data.birthday || null,
            bio: data.bio || ''
        }
    } catch (e) {
        // 错误已由拦截器统一提示
    }
}

// 上传前校验
const beforeAvatarUpload = (file) => {
    const isImage = file.type.startsWith('image/')
    const isLt2M = file.size / 1024 / 1024 < 2
    if (!isImage) {
        ElMessage.error('只能上传图片文件')
        return false
    }
    if (!isLt2M) {
        ElMessage.error('图片大小不能超过 2MB')
        return false
    }
    return true
}

// 自定义上传：复用 request.js 自动带 token，返回 filePath
const handleAvatarUpload = async (options) => {
    const { file } = options
    const formData = new FormData()
    formData.append('file', file)
    formData.append('businessType', 'USER_AVATAR')
    formData.append('businessField', 'avatar')
    try {
        const res = await service.post('/file/upload', formData, {
            headers: { 'Content-Type': 'multipart/form-data' }
        })
        form.value.avatar = res.filePath
        ElMessage.success('头像上传成功')
    } catch (e) {
        // 错误已由拦截器统一提示
    }
}

// 保存资料
const handleSave = async () => {
    saving.value = true
    try {
        const data = await updateProfile({
            nickname: form.value.nickname,
            avatar: form.value.avatar,
            gender: form.value.gender,
            birthday: form.value.birthday,
            bio: form.value.bio
        })
        ElMessage.success('资料保存成功 🌷')
        emit('updated', data)
    } catch (e) {
        // 错误已由拦截器统一提示
    } finally {
        saving.value = false
    }
}

// 重置为已加载的资料
const handleReset = () => {
    loadProfile()
}

onMounted(() => {
    loadProfile()
})
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
$text-placeholder: #B8A898;
$border-color: #E8DCC8;
$border-light: #EFE6D5;
$divider: #E0D5C0;
$shadow-card: 0 4px 16px rgba(180, 140, 100, 0.12);
$shadow-button: 0 3px 0 $primary-darker, 0 4px 8px rgba(180, 140, 100, 0.2);
$radius-sm: 8px;
$radius-md: 12px;
$radius-lg: 16px;
$radius-xl: 24px;
$font-handwritten: "Ma Shan Zheng", "Comic Sans MS", "Segoe Print", "Bradley Hand", cursive;
$font-rounded: "Nunito", "Quicksand", "PingFang SC", "Microsoft YaHei", sans-serif;

.profile-info {
    font-family: $font-rounded;
    color: $text-primary;
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

// ===== 手账风：卡片顶部胶带装饰 =====
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
.tape-orange {
    left: 36px;
    background: rgba(232, 146, 124, 0.4);
    transform: rotate(-4deg);
}

// ===== 手账风：头像区 =====
.avatar-section {
    display: flex;
    flex-direction: column;
    align-items: center;
    margin-bottom: 28px;
    padding-bottom: 24px;
    border-bottom: 1px dashed $divider;

    .avatar-wrapper {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 14px;
    }
    .avatar-img {
        width: 110px;
        height: 110px;
        border-radius: 50%;
        border: 3px dashed $primary;
        box-shadow: 0 4px 12px rgba(180, 140, 100, 0.18);
        background: $bg-page;
    }
    .avatar-placeholder {
        width: 100%;
        height: 100%;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 40px;
        background: $bg-page;
        border-radius: 50%;
    }
    .upload-btn {
        font-family: $font-rounded;
        color: $primary-dark;
        background: $primary-light;
        border: 1px dashed $primary;
        border-radius: $radius-md;
        padding: 6px 18px;
        font-size: 14px;
        cursor: pointer;
        transition: all 0.25s ease;
        &:hover {
            background: $primary;
            color: #fff;
            border-style: solid;
        }
    }
    .avatar-tip {
        margin-top: 10px;
        font-size: 12px;
        color: $text-secondary;
    }
}

// ===== 手账风：表单 =====
.profile-form {
    max-width: 480px;
    :deep(.el-form-item__label) {
        font-family: $font-rounded;
        color: $text-regular;
        font-weight: 600;
        font-size: 14px;
        padding-bottom: 4px;
    }
    :deep(.el-input__wrapper),
    :deep(.el-textarea__inner) {
        background: $bg-card;
        border-radius: $radius-md;
        box-shadow: 0 0 0 1px $border-color inset;
        &:hover {
            box-shadow: 0 0 0 1px $primary inset;
        }
        &.is-focus,
        &:focus {
            box-shadow: 0 0 0 1px $primary inset, 0 0 0 3px rgba(232, 146, 124, 0.2);
        }
    }
    :deep(.el-input__inner),
    :deep(.el-textarea__inner) {
        color: $text-primary;
        font-family: $font-rounded;
        &::placeholder {
            color: $text-placeholder;
        }
    }
    :deep(.el-radio__label) {
        color: $text-regular;
        font-family: $font-rounded;
    }
    :deep(.el-radio__input.is-checked .el-radio__inner) {
        background: $primary;
        border-color: $primary;
    }
    :deep(.el-radio__input.is-checked + .el-radio__label) {
        color: $primary-dark;
    }
}

// ===== 手账风：操作按钮 =====
.form-actions {
    display: flex;
    gap: 12px;
    margin-top: 24px;
    padding-top: 20px;
    border-top: 1px dashed $divider;

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
        &:active {
            transform: translateY(1px);
        }
    }
    :deep(.el-button:not(.el-button--primary)) {
        font-family: $font-rounded;
        background: transparent;
        border: 1.5px dashed $border-color;
        color: $text-regular;
        border-radius: $radius-md;
        &:hover {
            color: $primary-dark;
            border-color: $primary;
            background: $primary-light;
        }
    }
}
</style>
