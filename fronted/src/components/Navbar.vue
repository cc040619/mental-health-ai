<template>
    <div class="navbar">
        <div class="flex-box">
            <el-button @click="handleCollapse">
                <el-icon><Expand /></el-icon>
            </el-button>
            <p class="page-title">{{ route.meta.title }}</p>
        </div>
        <div class="flex-box">
            <el-dropdown @command="handleCommand">
                <div class="flex-box">
                    <el-avatar src="https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png" />
                    <p class="user-name">{{ displayName }}</p>
                    <el-tag size="small" type="info" effect="plain" class="role-tag">{{ roleLabel }}</el-tag>
                    <el-icon><ArrowDown /></el-icon>
                </div>
                <template #dropdown>
                    <el-dropdown-menu>
                        <el-dropdown-item command="logout">退出登录</el-dropdown-item>
                    </el-dropdown-menu>
                </template>
            </el-dropdown>
        </div>
    </div>
</template>
<script setup>
import { computed } from 'vue'
import { useAdminStore } from '@/stores/admin'
import { useRouter, useRoute } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { logout } from '@/api/admin'

const router = useRouter()
const route = useRoute()

// 用户信息（登录时写入 localStorage）：昵称优先，角色名按 userType 显示
const userInfo = computed(() => {
    try {
        return JSON.parse(localStorage.getItem('userInfo') || 'null') || {}
    } catch (e) {
        return {}
    }
})
const displayName = computed(() => userInfo.value.nickname || userInfo.value.username || '用户')
const roleLabel = computed(() => ({ 1: '用户', 2: '管理员', 3: '咨询师' }[userInfo.value.userType] || '用户'))

const handleCommand = (command) => {
    console.log(command)
    if (command === 'logout') {
        // 处理退出登录逻辑
        ElMessageBox.confirm('确定退出登录吗？', '提示', {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
        }).then(() => {
            // 确认退出登录
            logout().then(() => {
                // 清除缓存
                localStorage.removeItem('token')
                localStorage.removeItem('userInfo')
                // 跳转到登录页
                router.push('/auth/login')
            })
        })
    }
}

const handleCollapse = () => {
    useAdminStore().toggleCollapse()
}
</script>
<style lang="scss" scoped>
.navbar {
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 15px;
    background: white;
    box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
    border-bottom: 1px solid #e5e7eb;
    .flex-box {
        display: flex;
        align-items: center;
        justify-content: center;
    }
    .page-title {
        margin-left: 20px;
        font-size: 26px;
        font-weight: bold;
        color: #1f2937;
    }
    .role-tag {
        margin: 0 6px 0 8px;
    }
}
</style>
