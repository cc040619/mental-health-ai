<template>
    <div class="consultant-dashboard">
        <!-- 欢迎条 -->
        <div class="welcome-bar">
            <div>
                <h2 class="welcome-title">你好，{{ overview?.consultantName || '咨询师' }}</h2>
                <p class="welcome-date">{{ todayText }}</p>
            </div>
        </div>

        <!-- 五张统计卡（5 等分 flex，避免 24 栅格除不尽） -->
        <div v-loading="loading.overview" class="stat-grid">
            <el-card v-for="card in statCards" :key="card.label" shadow="never" class="stat-card">
                <div class="stat-value" :class="card.cls">{{ card.value }}</div>
                <div class="stat-label">{{ card.label }}</div>
            </el-card>
        </div>

        <!-- 待办提醒三栏 -->
        <el-row :gutter="16" class="todo-row" v-loading="loading.todos">
            <el-col :span="8">
                <el-card shadow="never" class="todo-card">
                    <template #header>
                        <div class="todo-head">
                            <span>即将开始的咨询</span>
                            <el-button link type="primary" @click="goAppointments">全部</el-button>
                        </div>
                    </template>
                    <el-empty v-if="!todos.upcoming?.length" :image-size="60" description="暂无即将开始的咨询" />
                    <div v-for="item in todos.upcoming" :key="item.appointmentId" class="todo-item">
                        <div class="todo-main">
                            <p class="todo-primary">{{ item.startTime }}-{{ item.endTime }} {{ item.userNickname }}</p>
                        </div>
                        <el-button size="small" type="primary" plain @click="goAppointments">开始</el-button>
                    </div>
                </el-card>
            </el-col>
            <el-col :span="8">
                <el-card shadow="never" class="todo-card">
                    <template #header>
                        <div class="todo-head">
                            <span>未回复评价</span>
                            <el-button link type="primary" @click="$router.push('/consultant/reviews')">全部</el-button>
                        </div>
                    </template>
                    <el-empty v-if="!todos.unrepliedReviews?.length" :image-size="60" description="近30天评价均已回复" />
                    <div v-for="item in todos.unrepliedReviews" :key="item.reviewId" class="todo-item">
                        <div class="todo-main">
                            <p class="todo-primary">
                                <el-rate :model-value="item.rating" disabled size="small" />
                                {{ item.userNickname }}
                            </p>
                            <p class="todo-desc">{{ item.content || '用户未填写评价内容' }}</p>
                        </div>
                        <el-button size="small" type="primary" plain
                                   @click="$router.push('/consultant/reviews')">回复</el-button>
                    </div>
                </el-card>
            </el-col>
            <el-col :span="8">
                <el-card shadow="never" class="todo-card">
                    <template #header>
                        <div class="todo-head">
                            <span>待处理预约</span>
                            <el-button link type="primary" @click="goAppointments('PENDING')">全部</el-button>
                        </div>
                    </template>
                    <el-empty v-if="!todos.pendingAppointments?.length" :image-size="60" description="暂无待处理预约" />
                    <div v-for="item in todos.pendingAppointments" :key="item.appointmentId" class="todo-item">
                        <div class="todo-main">
                            <p class="todo-primary">{{ item.userNickname }}</p>
                            <p class="todo-desc">{{ item.appointmentDate }} {{ item.startTime }}</p>
                        </div>
                        <el-button size="small" type="primary" plain :loading="confirmingId === item.appointmentId"
                                   @click="confirmAppointment(item.appointmentId)">确认</el-button>
                    </div>
                </el-card>
            </el-col>
        </el-row>

        <!-- 快捷入口 -->
        <el-row :gutter="16" class="quick-row">
            <el-col :span="8" v-for="q in quickEntries" :key="q.path">
                <el-card shadow="hover" class="quick-card" @click="$router.push(q.path)">
                    <el-icon class="quick-icon"><component :is="q.icon" /></el-icon>
                    <div>
                        <p class="quick-title">{{ q.title }}</p>
                        <p class="quick-desc">{{ q.desc }}</p>
                    </div>
                </el-card>
            </el-col>
        </el-row>
    </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getWorkbenchOverview, getWorkbenchTodos, changeMyAppointmentStatus } from '@/api/consultant'

const router = useRouter()

const overview = ref(null)
const todos = ref({})
const loading = reactive({ overview: false, todos: false })
const confirmingId = ref(null)

const todayText = computed(() => {
    const d = new Date()
    const week = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][d.getDay()]
    return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日 ${week}`
})

const statCards = computed(() => [
    { label: '今日预约', value: overview.value?.todayAppointments ?? 0 },
    { label: '待处理预约', value: overview.value?.pendingAppointments ?? 0, cls: 'text-orange' },
    { label: '本月咨询次数', value: overview.value?.monthConsultCount ?? 0 },
    {
        label: '总收入（元）',
        value: overview.value?.priceUnset ? '未设置' : (overview.value?.totalIncome ?? 0),
        cls: overview.value?.priceUnset ? 'text-gray' : 'text-green'
    },
    {
        label: `平均评分（${overview.value?.reviewCount ?? 0}条评价）`,
        value: overview.value?.avgRating != null ? Number(overview.value.avgRating).toFixed(1) : '0.0'
    }
])

const quickEntries = [
    { path: '/consultant/appointments', title: '发起咨询', desc: '查看今日待开始的咨询', icon: 'ChatDotRound' },
    { path: '/consultant/schedule', title: '查看日程', desc: '一周排班与预约安排', icon: 'Calendar' },
    { path: '/consultant/publish-article', title: '发布文章', desc: '分享专业心理知识', icon: 'EditPen' }
]

const loadOverview = () => {
    loading.overview = true
    getWorkbenchOverview().then(data => {
        overview.value = data
    }).finally(() => {
        loading.overview = false
    })
}

const loadTodos = () => {
    loading.todos = true
    getWorkbenchTodos().then(data => {
        todos.value = data || {}
    }).finally(() => {
        loading.todos = false
    })
}

const goAppointments = (status) => {
    router.push({ path: '/consultant/appointments', query: status ? { status } : {} })
}

const confirmAppointment = (id) => {
    ElMessageBox.confirm('确认通过该预约申请？确认后来访者将收到通知。', '确认预约', {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        type: 'info'
    }).then(() => {
        confirmingId.value = id
        changeMyAppointmentStatus(id, 'CONFIRMED').then(() => {
            ElMessage.success('已确认预约')
            loadTodos()
            loadOverview()
        }).finally(() => {
            confirmingId.value = null
        })
    }).catch(() => {})
}

onMounted(() => {
    loadOverview()
    loadTodos()
})
</script>

<style lang="scss" scoped>
.consultant-dashboard {
    .welcome-bar {
        background: linear-gradient(135deg, #eef4ff 0%, #f7f8fa 100%);
        border: 1px solid #e5e7eb;
        border-radius: 10px;
        padding: 20px 24px;
        margin-bottom: 16px;
        .welcome-title {
            font-size: 22px;
            font-weight: 600;
            color: #1f2937;
            margin-bottom: 6px;
        }
        .welcome-date {
            font-size: 14px;
            color: #6b7280;
        }
    }
    .stat-grid {
        display: flex;
        gap: 16px;
        margin-bottom: 16px;
    }
    .todo-row,
    .quick-row {
        margin-bottom: 0;
    }
    .el-col {
        margin-bottom: 16px;
    }
    .stat-card {
        flex: 1;
        min-width: 0;
        border-color: #e5e7eb;
        border-radius: 10px;
        text-align: center;
        .stat-value {
            font-size: 30px;
            font-weight: 700;
            color: #2563eb;
            line-height: 1.3;
        }
        .stat-label {
            font-size: 13px;
            color: #6b7280;
            margin-top: 4px;
        }
        .text-orange { color: #f59e0b; }
        .text-green { color: #10b981; }
        .text-gray { color: #9ca3af; font-size: 22px; }
    }
    .todo-card {
        border-color: #e5e7eb;
        border-radius: 10px;
        min-height: 300px;
        .todo-head {
            display: flex;
            align-items: center;
            justify-content: space-between;
            font-weight: 600;
            color: #1f2937;
        }
        .todo-item {
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding: 10px 0;
            border-bottom: 1px solid #f3f4f6;
            .todo-main {
                min-width: 0;
                flex: 1;
                padding-right: 8px;
            }
            .todo-primary {
                font-size: 14px;
                color: #1f2937;
                margin-bottom: 4px;
                display: flex;
                align-items: center;
            }
            .todo-desc {
                font-size: 12px;
                color: #9ca3af;
                white-space: nowrap;
                overflow: hidden;
                text-overflow: ellipsis;
            }
        }
    }
    .quick-card {
        border-color: #e5e7eb;
        border-radius: 10px;
        cursor: pointer;
        :deep(.el-card__body) {
            display: flex;
            align-items: center;
            gap: 14px;
        }
        .quick-icon {
            font-size: 34px;
            color: #2563eb;
            background: #eef4ff;
            border-radius: 10px;
            padding: 10px;
        }
        .quick-title {
            font-size: 16px;
            font-weight: 600;
            color: #1f2937;
            margin-bottom: 4px;
        }
        .quick-desc {
            font-size: 13px;
            color: #9ca3af;
        }
    }
}
</style>
