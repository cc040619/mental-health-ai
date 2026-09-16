<template>
    <div v-loading="loading">
        <PageHead title="板块管理">
            <template #buttons>
                <el-button type="primary" @click="openSave()">新增板块</el-button>
            </template>
        </PageHead>

        <el-table :data="tableData" style="width: 100%; margin-top: 4px">
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column label="图标" width="80">
                <template #default="scope">
                    <el-image v-if="scope.row.icon" :src="scope.row.icon" style="width: 32px; height: 32px"
                        fit="cover" />
                    <span v-else>—</span>
                </template>
            </el-table-column>
            <el-table-column prop="name" label="名称" width="140" />
            <el-table-column prop="code" label="编码" width="150" />
            <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
            <el-table-column prop="postCount" label="帖子数" width="90" align="center" />
            <el-table-column label="版主" width="130">
                <template #default="scope">
                    {{ scope.row.moderatorName || '—' }}
                </template>
            </el-table-column>
            <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
            <el-table-column label="状态" width="100">
                <template #default="scope">
                    <el-tag :type="scope.row.status === 1 ? 'success' : 'info'" size="small">
                        {{ scope.row.status === 1 ? '启用' : '停用' }}
                    </el-tag>
                </template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
                <template #default="scope">
                    <el-button text type="primary" @click="openSave(scope.row)">编辑</el-button>
                    <el-button text type="danger" @click="onDelete(scope.row)">删除</el-button>
                </template>
            </el-table-column>
        </el-table>

        <!-- 新增/编辑弹窗（编辑时编码不可改） -->
        <el-dialog v-model="saveVisible" :title="form.id ? '编辑板块' : '新增板块'" width="520px">
            <el-form label-width="90px">
                <el-form-item label="板块编码" required>
                    <el-input v-model="form.code" :disabled="!!form.id" maxlength="30" placeholder="大写字母/数字/下划线，如 TREES" />
                </el-form-item>
                <el-form-item label="板块名称" required>
                    <el-input v-model="form.name" maxlength="50" placeholder="如 情绪树洞" />
                </el-form-item>
                <el-form-item label="图标地址">
                    <el-input v-model="form.icon" maxlength="200" placeholder="选填，图片URL" />
                </el-form-item>
                <el-form-item label="描述">
                    <el-input v-model="form.description" type="textarea" :rows="2" maxlength="200" show-word-limit />
                </el-form-item>
                <el-form-item label="排序">
                    <el-input-number v-model="form.sortOrder" :min="0" :max="9999" />
                </el-form-item>
                <el-form-item label="版主">
                    <el-select v-model="form.moderatorId" filterable remote clearable :remote-method="searchUsers"
                        :loading="userSearching" placeholder="搜索用户昵称（选填）" style="width: 100%">
                        <el-option v-for="u in userOptions" :key="u.id" :label="u.nickname || `用户#${u.id}`"
                            :value="u.id" />
                    </el-select>
                </el-form-item>
                <el-form-item label="状态">
                    <el-radio-group v-model="form.status">
                        <el-radio :value="1">启用</el-radio>
                        <el-radio :value="0">停用</el-radio>
                    </el-radio-group>
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="saveVisible = false">取消</el-button>
                <el-button type="primary" @click="submitSave">确定</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHead from '@/components/PageHead.vue'
import { getAdminBoards, saveAdminBoard, deleteAdminBoard } from '@/api/community'
import { getAdminUserList } from '@/api/admin'

const loading = ref(false)
const tableData = ref([])

const loadList = () => {
    loading.value = true
    getAdminBoards().then(data => {
        tableData.value = data || []
    }).finally(() => { loading.value = false })
}

// ===== 新增/编辑 =====
const saveVisible = ref(false)
const form = reactive({ id: null, code: '', name: '', icon: '', description: '', sortOrder: 0, moderatorId: null, status: 1 })

const openSave = (row) => {
    form.id = row?.id ?? null
    form.code = row?.code ?? ''
    form.name = row?.name ?? ''
    form.icon = row?.icon ?? ''
    form.description = row?.description ?? ''
    form.sortOrder = row?.sortOrder ?? 0
    form.moderatorId = row?.moderatorId ?? null
    form.status = row?.status ?? 1
    saveVisible.value = true
    if (row?.moderatorId) {
        // 回显版主选项
        userOptions.value = [{ id: row.moderatorId, nickname: row.moderatorName }]
    } else {
        userOptions.value = []
    }
}

const submitSave = () => {
    if (!form.code.trim() || !form.name.trim()) {
        ElMessage.warning('请填写板块编码与名称')
        return
    }
    saveAdminBoard({
        id: form.id,
        code: form.code.trim(),
        name: form.name.trim(),
        icon: form.icon.trim() || undefined,
        description: form.description.trim() || undefined,
        sortOrder: form.sortOrder,
        moderatorId: form.moderatorId,
        status: form.status
    }).then(() => {
        ElMessage.success(form.id ? '板块已更新，用户端导航已同步' : '板块已新增，用户端导航已同步')
        saveVisible.value = false
        loadList()
    })
}

const onDelete = (row) => {
    ElMessageBox.confirm(
        `确定删除板块「${row.name}」？该板块下有 ${row.postCount} 篇帖子${row.postCount > 0 ? '，需先迁移帖子才能删除' : ''}。`,
        '删除确认', { type: 'warning' })
        .then(() => deleteAdminBoard(row.id))
        .then(() => { ElMessage.success('板块已删除，用户端导航已同步'); loadList() })
        .catch(() => {})
}

// ===== 版主远程搜索（复用用户管理列表接口） =====
const userOptions = ref([])
const userSearching = ref(false)

const searchUsers = (keyword) => {
    if (!keyword || !keyword.trim()) {
        userOptions.value = []
        return
    }
    userSearching.value = true
    getAdminUserList({ keyword: keyword.trim(), page: 1, size: 20 })
        .then(data => { userOptions.value = data.records || [] })
        .finally(() => { userSearching.value = false })
}

onMounted(loadList)
</script>
