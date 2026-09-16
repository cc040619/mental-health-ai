<template>
    <div class="consultant-sessions">
        <PageHead title="咨询会话" subtitle="会话即您名下的预约，进入会话可与来访者留言沟通并记录咨询过程" />

        <el-card shadow="never" class="filter-card">
            <div class="filter-row">
                <el-radio-group v-model="filters.status" @change="reload">
                    <el-radio-button label="">全部</el-radio-button>
                    <el-radio-button label="PENDING">待确认</el-radio-button>
                    <el-radio-button label="CONFIRMED">进行中</el-radio-button>
                    <el-radio-button label="COMPLETED">已完成</el-radio-button>
                </el-radio-group>
                <el-input v-model="filters.keyword" placeholder="搜索来访者昵称" clearable
                          style="width: 220px" @keyup.enter="reload" @clear="reload">
                    <template #prefix><el-icon><Search /></el-icon></template>
                </el-input>
            </div>
        </el-card>

        <el-card shadow="never" class="table-card" v-loading="loading">
            <el-table :data="records" style="width: 100%">
                <el-table-column label="来访者" min-width="120">
                    <template #default="{ row }">{{ row.userNickname || `用户 #${row.userId}` }}</template>
                </el-table-column>
                <el-table-column prop="appointmentDate" label="预约日期" width="120" />
                <el-table-column label="时间" width="130">
                    <template #default="{ row }">{{ row.startTime }}-{{ row.endTime }}</template>
                </el-table-column>
                <el-table-column label="状态" width="110">
                    <template #default="{ row }">
                        <el-tag :type="statusTagType(row.status)">{{ row.statusLabel }}</el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="最后留言" min-width="220" show-overflow-tooltip>
                    <template #default="{ row }">
                        <span v-if="row.lastMessageContent" class="last-msg">
                            {{ row.lastMessageContent }}
                        </span>
                        <span v-else class="text-muted">暂无留言</span>
                    </template>
                </el-table-column>
                <el-table-column label="留言数" width="90" align="center">
                    <template #default="{ row }">
                        <el-badge :value="row.messageCount" :max="99" :hidden="!row.messageCount"
                                  type="primary" />
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="120" fixed="right">
                    <template #default="{ row }">
                        <el-button link type="primary" @click="enterDetail(row)">进入会话</el-button>
                    </template>
                </el-table-column>
            </el-table>

            <el-empty v-if="!loading && records.length === 0" description="暂无会话记录" />

            <div class="pager">
                <el-pagination background layout="total, prev, pager, next"
                               :total="total" :page-size="size" :current-page="page"
                               @current-change="onPageChange" />
            </div>
        </el-card>
    </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import PageHead from '@/components/PageHead.vue'
import { getSessions } from '@/api/consultant'

const router = useRouter()

const filters = reactive({ status: '', keyword: '' })
const records = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)

const statusTagType = (status) => ({
    PENDING: 'warning',
    CONFIRMED: 'primary',
    COMPLETED: 'success',
    CANCELED: 'info'
}[status] || 'info')

const load = () => {
    loading.value = true
    getSessions({
        status: filters.status || undefined,
        keyword: filters.keyword || undefined,
        page: page.value,
        size: size.value
    }).then(data => {
        records.value = data.records || []
        total.value = Number(data.total) || 0
    }).finally(() => {
        loading.value = false
    })
}

const reload = () => {
    page.value = 1
    load()
}

const onPageChange = (p) => {
    page.value = p
    load()
}

const enterDetail = (row) => {
    router.push(`/consultant/sessions/${row.appointmentId}`)
}

onMounted(load)
</script>

<style lang="scss" scoped>
.consultant-sessions {
    .filter-card {
        border-color: #e5e7eb;
        margin-bottom: 16px;
        .filter-row {
            display: flex;
            justify-content: space-between;
            align-items: center;
            gap: 12px;
            flex-wrap: wrap;
        }
    }
    .table-card {
        border-color: #e5e7eb;
    }
    .pager {
        display: flex;
        justify-content: flex-end;
        margin-top: 16px;
    }
    .text-muted {
        color: #9ca3af;
    }
    .last-msg {
        color: #4b5563;
    }
}
</style>
