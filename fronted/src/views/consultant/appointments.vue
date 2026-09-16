<template>
    <div class="consultant-appointments">
        <PageHead title="我的预约" />

        <el-card shadow="never" class="filter-card">
            <el-form :inline="true" @submit.prevent>
                <el-form-item label="状态">
                    <el-select v-model="filters.status" placeholder="全部状态" clearable style="width: 150px"
                               @change="reload">
                        <el-option v-for="s in STATUS_OPTIONS" :key="s.value" :label="s.label" :value="s.value" />
                    </el-select>
                </el-form-item>
                <el-form-item label="预约日期">
                    <el-date-picker v-model="filters.date" type="date" value-format="YYYY-MM-DD"
                                    placeholder="选择日期" clearable @change="reload" />
                </el-form-item>
                <el-form-item>
                    <el-button @click="resetFilters">重置</el-button>
                </el-form-item>
            </el-form>
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
                <el-table-column label="问题摘要" min-width="200" show-overflow-tooltip>
                    <template #default="{ row }">
                        <template v-if="row.status === 'CANCELED' && row.cancelReason">
                            <span class="cancel-reason">取消原因：{{ row.cancelReason }}</span>
                        </template>
                        <span v-else>{{ row.problemDesc || '未填写' }}</span>
                    </template>
                </el-table-column>
                <el-table-column label="状态" width="110">
                    <template #default="{ row }">
                        <el-tag :type="statusTagType(row.status)">{{ row.statusLabel }}</el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="评价" width="90">
                    <template #default="{ row }">
                        <el-tag v-if="row.evaluated" size="small" type="success">已评价</el-tag>
                        <span v-else class="text-muted">-</span>
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="200" fixed="right">
                    <template #default="{ row }">
                        <el-button v-if="row.status === 'PENDING'" link type="primary"
                                   @click="changeStatus(row, 'CONFIRMED')">确认</el-button>
                        <el-button v-if="row.status === 'CONFIRMED'" link type="success"
                                   @click="changeStatus(row, 'COMPLETED')">完成</el-button>
                        <el-button v-if="row.status === 'PENDING' || row.status === 'CONFIRMED'"
                                   link type="danger" @click="onCancel(row)">取消</el-button>
                        <span v-if="row.status === 'COMPLETED' || row.status === 'CANCELED'"
                              class="text-muted">-</span>
                    </template>
                </el-table-column>
            </el-table>

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
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHead from '@/components/PageHead.vue'
import { getMyAppointments, changeMyAppointmentStatus, cancelMyAppointment } from '@/api/consultant'

const route = useRoute()

const STATUS_OPTIONS = [
    { value: 'PENDING', label: '待确认' },
    { value: 'CONFIRMED', label: '已确认' },
    { value: 'COMPLETED', label: '已完成' },
    { value: 'CANCELED', label: '已取消' }
]

const filters = reactive({
    status: route.query.status || '',
    date: ''
})
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
    getMyAppointments({
        status: filters.status || undefined,
        date: filters.date || undefined,
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

const resetFilters = () => {
    filters.status = ''
    filters.date = ''
    reload()
}

const onPageChange = (p) => {
    page.value = p
    load()
}

const changeStatus = (row, target) => {
    const isConfirm = target === 'CONFIRMED'
    ElMessageBox.confirm(
        isConfirm ? `确认通过 ${row.userNickname} 的预约申请（${row.appointmentDate} ${row.startTime}）？`
                  : `确认将 ${row.appointmentDate} ${row.startTime} 的咨询标记为已完成？`,
        isConfirm ? '确认预约' : '完成咨询',
        { confirmButtonText: '确定', cancelButtonText: '取消', type: 'info' }
    ).then(() => {
        changeMyAppointmentStatus(row.id, target).then(() => {
            ElMessage.success(isConfirm ? '已确认预约' : '已完成咨询')
            load()
        })
    }).catch(() => {})
}

// 咨询师取消（特权，不受用户取消时限约束）：原因必填，来访者侧可见
const onCancel = (row) => {
    ElMessageBox.prompt(
        `取消 ${row.appointmentDate} ${row.startTime} 与 ${row.userNickname || '来访者'} 的预约后，时段将重新开放。`,
        '取消预约（需填写原因）',
        {
            confirmButtonText: '确认取消',
            cancelButtonText: '再想想',
            type: 'warning',
            inputType: 'textarea',
            inputPlaceholder: '请填写取消原因（必填，500 字以内），将展示给来访者',
            inputValidator: (v) => (v && v.trim() ? true : '取消原因不能为空')
        }
    ).then(({ value }) => {
        cancelMyAppointment(row.id, value.trim()).then(() => {
            ElMessage.success('已取消预约，时段已释放')
            load()
        })
    }).catch(() => {})
}

onMounted(load)
</script>

<style lang="scss" scoped>
.consultant-appointments {
    .filter-card {
        border-color: #e5e7eb;
        margin-bottom: 16px;
        :deep(.el-card__body) {
            padding-bottom: 2px;
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
    .cancel-reason {
        color: #b45309;
    }
}
</style>
