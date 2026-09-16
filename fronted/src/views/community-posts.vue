<template>
    <div v-loading="loading">
        <PageHead title="帖子管理" />

        <!-- 筛选栏 -->
        <div class="filter-bar">
            <el-select v-model="filters.board" placeholder="板块" clearable style="width: 150px" @change="onSearch">
                <el-option v-for="b in boardOptions" :key="b.code" :label="b.label" :value="b.code" />
            </el-select>
            <el-select v-model="filters.status" placeholder="状态" clearable style="width: 130px" @change="onSearch">
                <el-option v-for="(text, code) in statusText" :key="code" :label="text" :value="Number(code)" />
            </el-select>
            <el-input v-model="filters.keyword" placeholder="标题关键字" clearable style="width: 200px"
                @keyup.enter="onSearch" @clear="onSearch" />
            <el-button type="primary" @click="onSearch">查询</el-button>
            <el-button @click="onReset">重置</el-button>
        </div>

        <el-table :data="tableData" style="width: 100%; margin-top: 18px">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column label="标题" min-width="220" show-overflow-tooltip>
                <template #default="scope">
                    <span>{{ scope.row.title }}</span>
                </template>
            </el-table-column>
            <el-table-column label="作者" width="130">
                <template #default="scope">
                    {{ scope.row.isAnonymous === 1 ? '匿名' : (scope.row.authorName || `用户#${scope.row.authorId}`) }}
                </template>
            </el-table-column>
            <el-table-column label="板块" width="110">
                <template #default="scope">
                    <el-tag size="small" effect="plain">{{ scope.row.boardLabel || scope.row.board }}</el-tag>
                </template>
            </el-table-column>
            <el-table-column prop="likeCount" label="赞" width="60" align="center" />
            <el-table-column prop="commentCount" label="评" width="60" align="center" />
            <el-table-column prop="favoriteCount" label="藏" width="60" align="center" />
            <el-table-column label="状态" width="100">
                <template #default="scope">
                    <el-tag :type="statusTagType(scope.row.status)" size="small">
                        {{ statusText[scope.row.status] || '未知' }}
                    </el-tag>
                </template>
            </el-table-column>
            <el-table-column label="标记" width="90" align="center">
                <template #default="scope">
                    <el-tag v-if="scope.row.isTop === 1" type="danger" size="small" effect="dark">置顶</el-tag>
                    <el-tag v-if="scope.row.isFeatured === 1" type="warning" size="small" effect="dark">精</el-tag>
                </template>
            </el-table-column>
            <el-table-column label="发布时间" width="160">
                <template #default="scope">{{ formatDateTime(scope.row.createdAt) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="280" fixed="right">
                <template #default="scope">
                    <div class="op-cell">
                        <el-button v-if="scope.row.status === 4" text type="primary" @click="openReview(scope.row)">审核</el-button>
                        <el-button v-if="scope.row.status !== 1" text type="danger" @click="onDelete(scope.row)">删除</el-button>
                        <el-button v-if="scope.row.status === 0 || scope.row.status === 2" text type="warning"
                            @click="onTakeDown(scope.row)">下架</el-button>
                        <el-button text :type="scope.row.isTop === 1 ? 'info' : 'success'" @click="onToggleTop(scope.row)">
                            {{ scope.row.isTop === 1 ? '取消置顶' : '置顶' }}
                        </el-button>
                        <el-button text :type="scope.row.isFeatured === 1 ? 'info' : 'success'"
                            @click="onToggleFeature(scope.row)">
                            {{ scope.row.isFeatured === 1 ? '取消加精' : '加精' }}
                        </el-button>
                    </div>
                </template>
            </el-table-column>
        </el-table>

        <el-pagination style="margin-top: 25px" :current-page="page" :page-size="size" :total="total"
            layout="total, prev, pager, next" @current-change="onPageChange" />

        <!-- 审核弹窗 -->
        <el-dialog v-model="reviewVisible" title="帖子审核" width="480px">
            <el-form label-width="90px">
                <el-form-item label="帖子">{{ reviewing.title }}</el-form-item>
                <el-form-item label="审核结论">
                    <el-radio-group v-model="reviewForm.approve">
                        <el-radio :value="true">通过</el-radio>
                        <el-radio :value="false">驳回</el-radio>
                    </el-radio-group>
                </el-form-item>
                <el-form-item v-if="reviewForm.approve === false" label="驳回原因" required>
                    <el-input v-model="reviewForm.rejectReason" type="textarea" :rows="3" maxlength="500"
                        show-word-limit placeholder="驳回原因将展示给作者（个人中心可见）" />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="reviewVisible = false">取消</el-button>
                <el-button type="primary" @click="submitReview">确定</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHead from '@/components/PageHead.vue'
import { formatDateTime } from '@/utils/format'
import { getBoards, getAdminPostPage, reviewAdminPost, updateAdminPostStatus, updateAdminPostTop, updateAdminPostFeature } from '@/api/community'

// 帖子状态：0正常 1已删除 2举报待审 3已下架 4待审核 5已驳回
const statusText = { 0: '正常', 1: '已删除', 2: '举报待审', 3: '已下架', 4: '待审核', 5: '已驳回' }
const statusTagType = (s) => ({ 0: 'success', 1: 'info', 2: 'warning', 3: 'danger', 4: 'warning', 5: 'danger' }[s] || 'info')

const loading = ref(false)
const boardOptions = ref([])
const tableData = ref([])
const page = ref(1)
const size = 20
const total = ref(0)
const filters = reactive({ board: null, status: null, keyword: '' })

const loadList = () => {
    loading.value = true
    getAdminPostPage({
        board: filters.board || undefined,
        status: filters.status ?? undefined,
        keyword: filters.keyword || undefined,
        page: page.value,
        size
    }).then(data => {
        tableData.value = data.records || []
        total.value = data.total || 0
    }).finally(() => { loading.value = false })
}

const onSearch = () => { page.value = 1; loadList() }

const onReset = () => {
    filters.board = null
    filters.status = null
    filters.keyword = ''
    onSearch()
}

const onPageChange = (p) => { page.value = p; loadList() }

// ===== 审核 =====
const reviewVisible = ref(false)
const reviewing = ref({})
const reviewForm = reactive({ approve: true, rejectReason: '' })

const openReview = (row) => {
    reviewing.value = row
    reviewForm.approve = false
    reviewForm.rejectReason = ''
    reviewVisible.value = true
}

const submitReview = () => {
    if (reviewForm.approve === false && !reviewForm.rejectReason.trim()) {
        ElMessage.warning('驳回时必须填写原因')
        return
    }
    reviewAdminPost(reviewing.value.id, {
        approve: reviewForm.approve,
        rejectReason: reviewForm.approve ? undefined : reviewForm.rejectReason.trim()
    }).then(() => {
        ElMessage.success(reviewForm.approve ? '已通过，帖子已发布' : '已驳回，原因将展示给作者')
        reviewVisible.value = false
        loadList()
    })
}

// ===== 删除 / 下架 =====
const onDelete = (row) => {
    ElMessageBox.confirm(`确定删除帖子「${row.title}」？删除后用户端不可见，该帖评论同步隐藏。`, '删除确认', { type: 'warning' })
        .then(() => updateAdminPostStatus(row.id, 1))
        .then(() => { ElMessage.success('已删除，用户端已同步'); loadList() })
        .catch(() => {})
}

const onTakeDown = (row) => {
    ElMessageBox.confirm(`确定下架帖子「${row.title}」？`, '下架确认', { type: 'warning' })
        .then(() => updateAdminPostStatus(row.id, 3))
        .then(() => { ElMessage.success('已下架，用户端已同步'); loadList() })
        .catch(() => {})
}

// ===== 置顶 / 加精 =====
const onToggleTop = (row) => {
    const target = row.isTop === 1 ? 0 : 1
    updateAdminPostTop(row.id, target).then(() => {
        row.isTop = target
        ElMessage.success(target === 1 ? '已置顶，用户端列表已同步' : '已取消置顶，用户端已同步')
    })
}

const onToggleFeature = (row) => {
    const target = row.isFeatured === 1 ? 0 : 1
    updateAdminPostFeature(row.id, target).then(() => {
        row.isFeatured = target
        ElMessage.success(target === 1 ? '已加精，用户端已同步' : '已取消加精，用户端已同步')
    })
}

onMounted(() => {
    getBoards().then(data => { boardOptions.value = data || [] })
    loadList()
})
</script>

<style scoped>
.filter-bar {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 16px;
    background: #ffffff;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
}

/* 操作列按钮单行排列：去掉默认内边距与间距，禁止换行 */
.op-cell {
    display: flex;
    align-items: center;
    flex-wrap: nowrap;
}

.op-cell :deep(.el-button) {
    padding: 0;
    margin-left: 0;
    margin-right: 12px;
}

.op-cell :deep(.el-button:last-child) {
    margin-right: 0;
}
</style>
