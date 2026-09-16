<template>
    <el-aside :width="isCollapse ? '64px' : '264px'">
        <div class="brand">
            <el-image style="width: 50px; height: 50px; margin-right: 10px;" :src="iconUrl" alt="logo" />
            <div v-show="!isCollapse" class="info-card">
                <h1 class="brand-title">心理健康AI助手</h1>
                <p class="brand-subtitle">咨询师工作台</p>
            </div>
        </div>
        <el-menu
            :collapse="isCollapse"
            :collapse-transition="false"
            :default-active="activeMenu"
            class="menu-style"
            @select="selectMenu"
        >
            <el-menu-item v-for="m in menus" :key="m.path" :index="m.path">
                <el-icon><component :is="m.icon" /></el-icon>
                <span>{{ m.title }}</span>
            </el-menu-item>
        </el-menu>
    </el-aside>
</template>
<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAdminStore } from '@/stores/admin'

const route = useRoute()
const router = useRouter()

const iconUrl = new URL('@/assets/images/机器人.png', import.meta.url).href

const isCollapse = computed(() => useAdminStore().isCollapse)

// 咨询师端菜单（与 consultantRoutes 保持一致，平铺无二级目录）
const menus = [
    { path: '/consultant/dashboard', title: '工作台', icon: 'Odometer' },
    { path: '/consultant/appointments', title: '我的预约', icon: 'Calendar' },
    { path: '/consultant/schedule', title: '日程管理', icon: 'Clock' },
    { path: '/consultant/settings', title: '预约设置', icon: 'Setting' },
    { path: '/consultant/reviews', title: '评价回复', icon: 'Star' },
    { path: '/consultant/publish-article', title: '发布文章', icon: 'EditPen' },
    { path: '/consultant/sessions', title: '咨询会话', icon: 'ChatDotRound' }
]

// 会话详情页路由 /consultant/sessions/:appointmentId 不在菜单中，
// 需把高亮回退到「咨询会话」菜单项，否则进入详情后菜单全部失活。
const activeMenu = computed(() => {
    if (route.path.startsWith('/consultant/sessions/')) {
        return '/consultant/sessions'
    }
    return route.path
})

const selectMenu = (path) => {
    router.push(path)
}
</script>
<style lang="scss" scoped>
// 侧边栏纵向 flex：brand 固定顶部，菜单区独立滚动（与管理端 Sidebar 同款简约白）
.el-aside {
    display: flex;
    flex-direction: column;
    overflow: hidden;
}
.brand {
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 10px;
    background-color: #fff;
    border-bottom: 1px solid #e5e7eb;
    flex-shrink: 0;
    .info-card {
        .brand-title {
            font-size: 20px;
            font-weight: bold;
            margin-bottom: 5px;
            color: #1f2937;
        }
        .brand-subtitle {
            font-size: 14px;
            color: #6b7280;
        }
    }
}
.menu-style {
    flex: 1;
    overflow-y: auto;
}
</style>
