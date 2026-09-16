<template>
    <div>
        <PageHead title="公告管理">
            <template #buttons>
                <el-button type="primary" @click="handleCreate">＋ 新建公告</el-button>
            </template>
        </PageHead>

        <!-- 筛选栏 -->
        <el-card shadow="never" class="filter-card">
            <div class="filter-bar">
                <el-select v-model="filters.type" placeholder="类型" clearable style="width: 140px"
                           @change="onSearch">
                    <el-option label="系统公告" value="SYSTEM" />
                    <el-option label="活动通知" value="ACTIVITY" />
                    <el-option label="温馨提示" value="TIP" />
                </el-select>
                <el-select v-model="filters.status" placeholder="状态" clearable style="width: 120px"
                           @change="onSearch">
                    <el-option label="草稿" :value="0" />
                    <el-option label="已发布" :value="1" />
                    <el-option label="已撤回" :value="2" />
                </el-select>
                <el-input v-model="filters.keyword" placeholder="标题关键字" clearable style="width: 220px"
                          @keyup.enter="onSearch" @clear="onSearch" />
                <el-button type="primary" plain @click="onSearch">查询</el-button>
            </div>
        </el-card>

        <el-table :data="tableData" style="width: 100%; margin-top: 20px" v-loading="loading"
                  :min-height="560">
            <el-table-column prop="title" label="标题" min-width="240" show-overflow-tooltip />
            <el-table-column label="类型" width="110">
                <template #default="scope">
                    <el-tag :type="typeTagMap[scope.row.type]?.tag || 'info'" round effect="plain">
                        {{ typeTagMap[scope.row.type]?.label || scope.row.type }}
                    </el-tag>
                </template>
            </el-table-column>
            <el-table-column label="发布范围" width="130">
                <template #default="scope">
                    <span v-if="scope.row.targetType === 'ALL'">全体用户</span>
                    <el-tooltip v-else :content="scope.row.targetNames" placement="top">
                        <span>指定 {{ (scope.row.targetUserIds || []).length }} 人</span>
                    </el-tooltip>
                </template>
            </el-table-column>
            <el-table-column label="发布时间" width="170">
                <template #default="scope">
                    {{ scope.row.publishedAt ? formatDateTime(scope.row.publishedAt) : (scope.row.publishAt ? '定时 ' + formatDateTime(scope.row.publishAt) : '—') }}
                </template>
            </el-table-column>
            <el-table-column label="状态" width="100">
                <template #default="scope">
                    <el-tag :type="statusTagMap[scope.row.status]?.tag" round>
                        {{ statusTagMap[scope.row.status]?.label }}
                    </el-tag>
                </template>
            </el-table-column>
            <el-table-column prop="readCount" label="阅读人数" width="100" align="center" />
            <el-table-column label="操作" width="230" fixed="right">
                <template #default="scope">
                    <div class="op-cell">
                        <el-button text type="primary" size="small"
                                   :disabled="scope.row.status === 1"
                                   @click="handleEdit(scope.row)">编辑</el-button>
                        <el-button v-if="scope.row.status !== 1" text type="success" size="small"
                                   @click="handlePublish(scope.row)">发布</el-button>
                        <el-button v-if="scope.row.status === 1" text type="warning" size="small"
                                   @click="handleWithdraw(scope.row)">撤回</el-button>
                        <el-button v-if="scope.row.status !== 1" text type="danger" size="small"
                                   @click="handleDelete(scope.row)">删除</el-button>
                    </div>
                </template>
            </el-table-column>
        </el-table>

        <el-pagination style="margin-top: 20px" :current-page="page" :page-size="pageSize"
                       layout="total, prev, pager, next" :total="total" @current-change="onPageChange" />

        <!-- 新建 / 编辑抽屉（草稿保存，发布走表格操作） -->
        <el-drawer v-model="drawerVisible" :title="editingId ? '编辑公告' : '新建公告'"
                   size="800px" :close-on-click-modal="false">
            <el-form label-position="top" @submit.prevent>
                <el-form-item label="公告标题" required>
                    <el-input v-model="form.title" maxlength="200" show-word-limit
                              placeholder="请输入公告标题（≤200字）" />
                </el-form-item>
                <el-form-item label="公告类型">
                    <el-select v-model="form.type" style="width: 200px">
                        <el-option label="系统公告" value="SYSTEM" />
                        <el-option label="活动通知" value="ACTIVITY" />
                        <el-option label="温馨提示" value="TIP" />
                    </el-select>
                </el-form-item>
                <el-form-item label="公告内容" required>
                    <RichTextEditor v-model="form.content" :max-char-count="5000"
                                    placeholder="请输入公告内容..." :min-height="'260px'" />
                </el-form-item>
                <el-form-item label="发布范围">
                    <el-radio-group v-model="form.targetType">
                        <el-radio value="ALL">全体用户</el-radio>
                        <el-radio value="USERS">指定用户</el-radio>
                    </el-radio-group>
                    <el-select v-if="form.targetType === 'USERS'" v-model="form.targetUserIds"
                               multiple filterable remote :remote-method="searchUsers" :loading="userSearching"
                               placeholder="输入昵称/用户名搜索用户" style="width: 100%; margin-top: 8px"
                               :remote-show-suffix="true">
                        <el-option v-for="u in userOptions" :key="u.id" :label="u.nickname || u.username"
                                   :value="u.id" />
                    </el-select>
                </el-form-item>
                <el-form-item label="定时发布（可选，留空 = 保存为草稿，到点自动发布）">
                    <el-date-picker v-model="form.publishAt" type="datetime"
                                    placeholder="选择未来时间" value-format="YYYY-MM-DDTHH:mm:ss"
                                    :disabled-date="(d) => d.getTime() < Date.now() - 86400000"
                                    style="width: 240px" />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="drawerVisible = false">取消</el-button>
                <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
            </template>
        </el-drawer>
    </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import PageHead from '@/components/PageHead.vue'
import RichTextEditor from '@/components/RichTextEditor.vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
    getAdminAnnouncements, createAnnouncement, updateAnnouncement,
    publishAnnouncement, withdrawAnnouncement, deleteAnnouncement
} from '@/api/notify'
import { getAdminUserList } from '@/api/admin'
import { formatDateTime } from '@/utils/format'

const statusTagMap = {
    0: { label: '草稿', tag: 'info' },
    1: { label: '已发布', tag: 'success' },
    2: { label: '已撤回', tag: 'warning' }
}
const typeTagMap = {
    SYSTEM: { label: '系统公告', tag: 'primary' },
    ACTIVITY: { label: '活动通知', tag: 'success' },
    TIP: { label: '温馨提示', tag: 'warning' }
}

const tableData = ref([])
const loading = ref(false)
const page = ref(1)
const pageSize = 10
const total = ref(0)
const filters = reactive({ type: null, status: null, keyword: '' })

const drawerVisible = ref(false)
const saving = ref(false)
const editingId = ref(null)
const form = reactive({ title: '', content: '', type: 'SYSTEM', targetType: 'ALL', targetUserIds: [], publishAt: null })
const userOptions = ref([])
const userSearching = ref(false)

const load = () => {
    loading.value = true
    const params = { page: page.value, size: pageSize }
    if (filters.type) params.type = filters.type
    if (filters.status !== null && filters.status !== undefined && filters.status !== '') params.status = filters.status
    if (filters.keyword.trim()) params.keyword = filters.keyword.trim()
    getAdminAnnouncements(params).then(data => {
        tableData.value = data.records || []
        total.value = data.total || 0
    }).finally(() => {
        loading.value = false
    })
}

const onSearch = () => {
    page.value = 1
    load()
}

const onPageChange = (p) => {
    page.value = p
    load()
}

// 远程搜索用户（复用用户管理列表接口）
const searchUsers = (keyword) => {
    userSearching.value = true
    getAdminUserList({ keyword: keyword || '', page: 1, size: 20 }).then(data => {
        userOptions.value = data?.records || []
    }).finally(() => {
        userSearching.value = false
    })
}

const resetForm = () => {
    form.title = ''
    form.content = ''
    form.type = 'SYSTEM'
    form.targetType = 'ALL'
    form.targetUserIds = []
    form.publishAt = null
}

const handleCreate = () => {
    editingId.value = null
    resetForm()
    userOptions.value = []
    drawerVisible.value = true
}

const handleEdit = (row) => {
    editingId.value = row.id
    form.title = row.title
    form.content = row.content
    form.type = row.type || 'SYSTEM'
    form.targetType = row.targetType || 'ALL'
    form.targetUserIds = row.targetUserIds || []
    form.publishAt = row.publishAt || null
    // 回显已选用户为可搜索选项
    userOptions.value = (row.targetUserIds || []).map((id, i) => ({
        id,
        nickname: (row.targetNames || '').split('、')[i] || `用户${id}`
    }))
    drawerVisible.value = true
}

const handleSave = () => {
    if (!form.title.trim()) {
        ElMessage.warning('请输入公告标题')
        return
    }
    if (!form.content || form.content.replace(/<[^>]*>/g, '').trim() === '') {
        ElMessage.warning('请输入公告内容')
        return
    }
    if (form.targetType === 'USERS' && form.targetUserIds.length === 0) {
        ElMessage.warning('指定用户公告需选择目标用户')
        return
    }
    saving.value = true
    const payload = {
        title: form.title.trim(),
        content: form.content,
        type: form.type,
        targetType: form.targetType,
        targetUserIds: form.targetType === 'USERS' ? form.targetUserIds : null,
        publishAt: form.publishAt || null
    }
    const request = editingId.value
        ? updateAnnouncement(editingId.value, payload)
        : createAnnouncement(payload)
    request.then(() => {
        ElMessage.success('已保存。草稿可在列表中点击「发布」上线')
        drawerVisible.value = false
        load()
    }).finally(() => {
        saving.value = false
    })
}

// 发布（草稿/已撤回 → 已发布）
const handlePublish = (row) => {
    ElMessageBox.confirm(`确定发布公告「${row.title}」吗？发布后用户端立即可见。`, '发布公告', {
        confirmButtonText: '确定发布',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(() => {
        publishAnnouncement(row.id).then(() => {
            ElMessage.success('已发布')
            load()
        })
    }).catch(() => {})
}

// 撤回（已发布 → 已撤回，用户端立即不可见）
const handleWithdraw = (row) => {
    ElMessageBox.confirm(`撤回后用户端将立即不可见，确定撤回公告「${row.title}」吗？`, '撤回公告', {
        confirmButtonText: '确定撤回',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(() => {
        withdrawAnnouncement(row.id).then(() => {
            ElMessage.success('已撤回')
            load()
        })
    }).catch(() => {})
}

// 删除（仅草稿/已撤回；后端级联清理阅读记录）
const handleDelete = (row) => {
    ElMessageBox.confirm(`删除后不可恢复，确定删除公告「${row.title}」吗？`, '删除公告', {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'error'
    }).then(() => {
        deleteAnnouncement(row.id).then(() => {
            ElMessage.success('已删除')
            load()
        })
    }).catch(() => {})
}

onMounted(load)
</script>

<style scoped>
.filter-card {
    border-radius: 8px;
}

.filter-bar {
    display: flex;
    gap: 12px;
    align-items: center;
}

.op-cell {
    display: flex;
    flex-wrap: nowrap;
    gap: 2px;
}
</style>
