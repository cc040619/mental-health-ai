<template>
    <div v-loading="loading">
        <PageHead title="敏感词管理">
            <template #buttons>
                <el-button type="primary" plain @click="importVisible = true">批量导入</el-button>
                <el-button type="primary" @click="openSave()">新增敏感词</el-button>
            </template>
        </PageHead>

        <!-- 筛选栏 -->
        <div class="filter-bar">
            <el-select v-model="filters.category" placeholder="分类" clearable style="width: 140px" @change="onSearch">
                <el-option v-for="c in categories" :key="c.value" :label="c.label" :value="c.value" />
            </el-select>
            <el-input v-model="filters.keyword" placeholder="敏感词" clearable style="width: 200px"
                @keyup.enter="onSearch" @clear="onSearch" />
            <el-button type="primary" @click="onSearch">查询</el-button>
            <el-button @click="onReset">重置</el-button>
        </div>

        <el-table :data="tableData" style="width: 100%; margin-top: 18px">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="word" label="敏感词" min-width="200" />
            <el-table-column label="分类" width="120">
                <template #default="scope">
                    <el-tag size="small" effect="plain">{{ categoryText[scope.row.category] || scope.row.category }}</el-tag>
                </template>
            </el-table-column>
            <el-table-column label="状态" width="110">
                <template #default="scope">
                    <el-switch :model-value="scope.row.status === 1" @change="onToggleStatus(scope.row)" />
                </template>
            </el-table-column>
            <el-table-column label="添加时间" width="160">
                <template #default="scope">{{ formatDateTime(scope.row.createdAt) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="140" fixed="right">
                <template #default="scope">
                    <el-button text type="primary" @click="openSave(scope.row)">编辑</el-button>
                    <el-button text type="danger" @click="onDelete(scope.row)">删除</el-button>
                </template>
            </el-table-column>
        </el-table>

        <el-pagination style="margin-top: 25px" :current-page="page" :page-size="size" :total="total"
            layout="total, prev, pager, next" @current-change="onPageChange" />

        <el-alert class="tip-alert" type="info" :closable="false"
            title="词库变更保存后即时生效，无需重启服务。危机词库（自残/自杀类干预词）为安全底线，不在管理端维护。" />

        <!-- 新增/编辑弹窗 -->
        <el-dialog v-model="saveVisible" :title="form.id ? '编辑敏感词' : '新增敏感词'" width="440px">
            <el-form label-width="80px">
                <el-form-item label="敏感词" required>
                    <el-input v-model="form.word" maxlength="50" show-word-limit placeholder="≤50字" />
                </el-form-item>
                <el-form-item label="分类">
                    <el-select v-model="form.category" style="width: 100%">
                        <el-option v-for="c in categories" :key="c.value" :label="c.label" :value="c.value" />
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

        <!-- 批量导入弹窗 -->
        <el-dialog v-model="importVisible" title="批量导入敏感词" width="520px">
            <el-form label-width="80px">
                <el-form-item label="统一分类">
                    <el-select v-model="importForm.category" style="width: 100%">
                        <el-option v-for="c in categories" :key="c.value" :label="c.label" :value="c.value" />
                    </el-select>
                </el-form-item>
                <el-form-item label="词列表">
                    <el-input v-model="importForm.text" type="textarea" :rows="10"
                        placeholder="每行一个敏感词，单次最多1000个，重复词自动跳过" />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="importVisible = false">取消</el-button>
                <el-button type="primary" :loading="importing" @click="submitImport">导入</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHead from '@/components/PageHead.vue'
import { formatDateTime } from '@/utils/format'
import {
    getAdminSensitiveWords, addAdminSensitiveWord, updateAdminSensitiveWord,
    deleteAdminSensitiveWord, importAdminSensitiveWords
} from '@/api/community'

const categoryText = { POLITICAL: '政治', VIOLENT: '暴力', VULGAR: '低俗', AD: '广告', OTHER: '其他' }
const categories = Object.entries(categoryText).map(([value, label]) => ({ value, label }))

const size = 20
const loading = ref(false)
const tableData = ref([])
const page = ref(1)
const total = ref(0)
const filters = reactive({ category: null, keyword: '' })

const loadList = () => {
    loading.value = true
    getAdminSensitiveWords({
        category: filters.category || undefined,
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
    filters.category = null
    filters.keyword = ''
    onSearch()
}

const onPageChange = (p) => { page.value = p; loadList() }

// ===== 新增/编辑 =====
const saveVisible = ref(false)
const form = reactive({ id: null, word: '', category: 'OTHER', status: 1 })

const openSave = (row) => {
    form.id = row?.id ?? null
    form.word = row?.word ?? ''
    form.category = row?.category ?? 'OTHER'
    form.status = row?.status ?? 1
    saveVisible.value = true
}

const submitSave = () => {
    if (!form.word.trim()) {
        ElMessage.warning('请填写敏感词')
        return
    }
    const payload = { word: form.word.trim(), category: form.category, status: form.status }
    const request = form.id ? updateAdminSensitiveWord(form.id, payload) : addAdminSensitiveWord(payload)
    request.then(() => {
        ElMessage.success('已保存，词库即时生效（无需重启）')
        saveVisible.value = false
        loadList()
    })
}

// 启停用切换
const onToggleStatus = (row) => {
    const target = row.status === 1 ? 0 : 1
    updateAdminSensitiveWord(row.id, { word: row.word, category: row.category, status: target })
        .then(() => {
            row.status = target
            ElMessage.success(target === 1 ? '已启用，词库即时生效' : '已停用，该词不再拦截')
        })
}

const onDelete = (row) => {
    ElMessageBox.confirm(`确定删除敏感词「${row.word}」？删除后即时生效。`, '删除确认', { type: 'warning' })
        .then(() => deleteAdminSensitiveWord(row.id))
        .then(() => { ElMessage.success('已删除，词库即时生效'); loadList() })
        .catch(() => {})
}

// ===== 批量导入 =====
const importVisible = ref(false)
const importing = ref(false)
const importForm = reactive({ category: 'OTHER', text: '' })

const submitImport = () => {
    const words = importForm.text.split('\n').map(w => w.trim()).filter(Boolean)
    if (words.length === 0) {
        ElMessage.warning('请粘贴要导入的词，每行一个')
        return
    }
    importing.value = true
    importAdminSensitiveWords({ words, category: importForm.category })
        .then(result => {
            ElMessage.success(`导入完成：成功 ${result.success} 个，重复跳过 ${result.duplicate} 个，无效 ${result.invalid} 个`)
            importVisible.value = false
            importForm.text = ''
            onSearch()
        })
        .finally(() => { importing.value = false })
}
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

.tip-alert {
    margin-top: 16px;
}
</style>
