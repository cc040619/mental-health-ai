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
                <el-option label="已警告" :value="3" />
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
            <el-table-column label="被举报人" width="130">
                <template #default="scope">
                    <span v-if="scope.row.authorName">{{ scope.row.authorName }}</span>
                    <span v-else class="handled-text">—</span>
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
            <el-table-column label="操作" width="120" fixed="right">
                <template #default="scope">
                    <el-button v-if="scope.row.status === 0" text type="primary" @click="openHandle(scope.row)">处理</el-button>
                    <span v-else class="handled-text">
                        {{ statusText(scope.row.status) }}
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

        <!-- 处理弹窗（动作单选） -->
        <el-dialog v-model="handleVisible" title="处理举报" width="460px">
            <el-form label-width="90px">
                <el-form-item label="举报对象">
                    {{ handleRow.targetType === 1 ? (handleRow.postTitle || `帖子#${handleRow.targetId}`) : (handleRow.commentContent || `评论#${handleRow.targetId}`) }}
                </el-form-item>
                <el-form-item label="处理动作">
                    <el-radio-group v-model="handleAction">
                        <el-radio value="DELETE_POST">删除内容（帖软删/评软删+计数回减）</el-radio>
                        <el-radio value="WARN_USER">警告用户（不删除内容）</el-radio>
                        <el-radio value="IGNORE">忽略举报</el-radio>
                    </el-radio-group>
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="handleVisible = false">取消</el-button>
                <el-button type="primary" @click="submitHandle">确定</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import PageHead from '@/components/PageHead.vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getReportList, handleReport } from '@/api/community'
import { formatDateTime } from '@/utils/format'

const router = useRouter()

const loading = ref(false)
const tableData = ref([])
const page = ref(1)
const size = 10
const total = ref(0)
const statusFilter = ref(0) // 默认看待处理

// 0待处理 1已下架 2已忽略 3已警告
const statusText = (s) => ({ 0: '待处理', 1: '已下架', 2: '已忽略', 3: '已警告' }[s] || '未知')
const statusTagType = (s) => ({ 0: 'danger', 1: 'warning', 2: 'info', 3: 'warning' }[s] || 'info')

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

// ===== 处理弹窗 =====
const handleVisible = ref(false)
const handleRow = ref({})
const handleAction = ref('DELETE_POST')

const openHandle = (row) => {
    handleRow.value = row
    handleAction.value = 'DELETE_POST'
    handleVisible.value = true
}

const submitHandle = () => {
    const action = handleAction.value
    const tips = {
        DELETE_POST: ['删除确认', '删除后该内容对用户不可见，确定删除吗？'],
        WARN_USER: ['警告确认', '仅警告用户，不删除内容，确定警告吗？'],
        IGNORE: ['忽略确认', '忽略后该举报不再处理，确定忽略吗？']
    }
    const [title, message] = tips[action]
    handleVisible.value = false
    ElMessageBox.confirm(message, title, { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
        .then(() => handleReport(handleRow.value.id, action))
        .then(() => {
            ElMessage.success({ DELETE_POST: '内容已删除', WARN_USER: '已警告用户', IGNORE: '已忽略该举报' }[action])
            loadList()
        })
        .catch(() => {})
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
