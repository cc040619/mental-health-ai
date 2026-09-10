<template>
    <div v-loading="loading">
        <PageHead title="社区举报管理" />

        <!-- 状态筛选 -->
        <div class="filter-bar">
            <span class="filter-label">处理状态：</span>
            <el-select v-model="statusFilter" style="width: 160px" @change="onFilterChange">
                <el-option label="全部" :value="null" />
                <el-option label="待处理" :value="0" />
                <el-option label="已下架" :value="1" />
                <el-option label="已忽略" :value="2" />
            </el-select>
            <el-button @click="loadList" style="margin-left: 12px">刷新</el-button>
        </div>

        <el-table :data="tableData" style="width: 100%; margin-top: 18px">
            <el-table-column prop="id" label="举报ID" width="90" />
            <el-table-column label="类型" width="100">
                <template #default="scope">
                    <el-tag :type="scope.row.targetType === 1 ? 'warning' : 'info'" size="small">
                        {{ scope.row.targetType === 1 ? '帖子' : '评论' }}
                    </el-tag>
                </template>
            </el-table-column>
            <el-table-column label="举报对象" min-width="220">
                <template #default="scope">
                    <div v-if="scope.row.targetType === 1" class="target-cell">
                        <span class="target-title">📄 {{ scope.row.postTitle || `帖子#${scope.row.targetId}` }}</span>
                        <el-button link type="primary" size="small" @click="viewPost(scope.row.targetId)">查看</el-button>
                    </div>
                    <span v-else class="target-comment">💬 {{ scope.row.commentContent || `评论#${scope.row.targetId}` }}</span>
                </template>
            </el-table-column>
            <el-table-column prop="reporterName" label="举报人" width="130">
                <template #default="scope">{{ scope.row.reporterName || `用户#${scope.row.reporterId}` }}</template>
            </el-table-column>
            <el-table-column prop="reason" label="举报原因" min-width="200" show-overflow-tooltip />
            <el-table-column label="状态" width="110">
                <template #default="scope">
                    <el-tag :type="statusTagType(scope.row.status)" size="small">
                        {{ statusText(scope.row.status) }}
                    </el-tag>
                </template>
            </el-table-column>
            <el-table-column label="举报时间" width="160">
                <template #default="scope">{{ formatDateTime(scope.row.createdAt) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="180" fixed="right">
                <template #default="scope">
                    <template v-if="scope.row.status === 0">
                        <el-button text type="danger" @click="handle(scope.row, 'DOWN')">下架内容</el-button>
                        <el-button text type="info" @click="handle(scope.row, 'IGNORE')">忽略</el-button>
                    </template>
                    <span v-else class="handled-text">
                        {{ scope.row.status === 1 ? '已下架' : '已忽略' }}
                        <template v-if="scope.row.handledAt"> · {{ formatDateTime(scope.row.handledAt) }}</template>
                    </span>
                </template>
            </el-table-column>
        </el-table>

        <el-pagination
            style="margin-top: 25px"
            :current-page="page"
            :page-size="size"
            :total="total"
            layout="prev, pager, next"
            @current-change="onPageChange"
        />
    </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import PageHead from '@/components/PageHead.vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getReportList, handleReport } from '@/api/community'
import { formatDateTime } from '@/utils/format'

const loading = ref(false)
const tableData = ref([])
const page = ref(1)
const size = 10
const total = ref(0)
const statusFilter = ref(0) // 默认看待处理

const statusText = (s) => ({ 0: '待处理', 1: '已下架', 2: '已忽略' }[s] || '未知')
const statusTagType = (s) => ({ 0: 'danger', 1: 'warning', 2: 'info' }[s] || 'info')

const loadList = () => {
    loading.value = true
    const params = { page: page.value, size }
    if (statusFilter.value !== null && statusFilter.value !== '') {
        params.status = statusFilter.value
    }
    getReportList(params).then(data => {
        tableData.value = data.records || []
        total.value = data.total || 0
    }).finally(() => {
        loading.value = false
    })
}

const onFilterChange = () => {
    page.value = 1
    loadList()
}

const onPageChange = (p) => {
    page.value = p
    loadList()
}

const viewPost = (postId) => {
    router.push(`/community/post/${postId}`)
}

const handle = (row, action) => {
    const isDown = action === 'DOWN'
    ElMessageBox.confirm(
        isDown ? '下架后该帖子/评论将对普通用户不可见，确定下架吗？' : '忽略后该举报将不再处理，确定忽略吗？',
        isDown ? '下架确认' : '忽略确认',
        { confirmButtonText: '确定', cancelButtonText: '取消', type: isDown ? 'warning' : 'info' }
    ).then(() => {
        handleReport(row.id, action).then(() => {
            ElMessage.success(isDown ? '内容已下架' : '已忽略该举报')
            loadList()
        })
    }).catch(() => {})
}

onMounted(() => {
    loadList()
})
</script>

<style scoped>
.filter-bar {
    margin-top: 18px;
    display: flex;
    align-items: center;
}
.filter-label {
    font-size: 14px;
    color: #606266;
    margin-right: 8px;
}
.target-title {
    font-weight: 600;
    color: #303133;
}
.target-comment {
    color: #606266;
    font-size: 13px;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
}
.handled-text {
    font-size: 12px;
    color: #909399;
}
</style>
