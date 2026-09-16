<template>
    <el-aside :width="isCollapse ? '64px' : '264px'">
        <div class="brand">
            <el-image style="width: 50px; height: 50px; margin-right: 10px;" :src="iconUrl" alt="logo" />
            <div v-show="!isCollapse" class="info-card">
                <h1 class="brand-title">心理健康AI助手</h1>
                <p class="brand-subtitle">管理后台</p>
            </div>
        </div>
        <el-menu
            :collapse="isCollapse"
            :collapse-transition="false"
            :default-active="route.path"
            class="menu-style"
        >
            <!-- meta.group 有值的一级路由聚合为二级目录（如 AI 配置），其余保持平铺 -->
            <template v-for="g in menuGroups" :key="g.type === 'group' ? g.title : g.route.path">
                <el-sub-menu v-if="g.type === 'group'" :index="g.title">
                    <template #title>
                        <el-icon><component :is="g.icon" /></el-icon>
                        <span>{{ g.title }}</span>
                    </template>
                    <el-menu-item v-for="child in g.children" :key="child.path" :index="child.path" @click="selectMenu">
                        <el-icon><component :is="child.meta.icon" /></el-icon>
                        <span>{{ child.meta.title }}</span>
                    </el-menu-item>
                </el-sub-menu>
                <el-menu-item v-else :index="g.route.path" @click="selectMenu">
                    <el-icon><component :is="g.route.meta.icon" /></el-icon>
                    <span>{{ g.route.meta.title }}</span>
                </el-menu-item>
            </template>
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

/** 平铺一级项 + group 聚合二级目录（保持路由声明顺序） */
const menuGroups = computed(() => {
    const items = router.options.routes[0].children.filter(i => !i.meta?.hidden)
    const result = []
    const groupMap = new Map()
    for (const item of items) {
        const groupTitle = item.meta?.group
        if (!groupTitle) {
            result.push({ type: 'item', route: item })
            continue
        }
        if (!groupMap.has(groupTitle)) {
            const entry = { type: 'group', title: groupTitle, icon: item.meta.icon, children: [] }
            groupMap.set(groupTitle, entry)
            result.push(entry)
        }
        groupMap.get(groupTitle).children.push(item)
    }
    return result
})

const selectMenu = (key) => {
    const currentRoute = router.options.routes[0]
    router.push(`${currentRoute.path}/${key.index}`)
}

</script>
<style lang="scss" scoped>
// 侧边栏纵向 flex：brand 固定顶部，菜单区独立滚动
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
