<template>
    <div>
        <PageHead title="轮播图管理">
            <template #buttons>
                <el-button type="primary" @click="handleCreate">＋ 新增轮播图</el-button>
            </template>
        </PageHead>

        <el-table :data="tableData" style="width: 100%; margin-top: 25px" v-loading="loading"
                  :min-height="480">
            <el-table-column label="缩略图" width="200">
                <template #default="scope">
                    <el-image :src="scope.row.imageUrl" fit="cover" preview-teleported
                              :preview-src-list="[scope.row.imageUrl]"
                              style="width: 168px; height: 62px; border-radius: 6px" />
                </template>
            </el-table-column>
            <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip>
                <template #default="scope">{{ scope.row.title || '—' }}</template>
            </el-table-column>
            <el-table-column label="跳转链接" min-width="200" show-overflow-tooltip>
                <template #default="scope">
                    <a v-if="scope.row.linkUrl" :href="scope.row.linkUrl" target="_blank"
                       style="color: var(--el-color-primary)">{{ scope.row.linkUrl }}</a>
                    <span v-else>—</span>
                </template>
            </el-table-column>
            <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
            <el-table-column label="状态" width="100">
                <template #default="scope">
                    <el-tag :type="scope.row.status === 1 ? 'success' : 'info'" round>
                        {{ scope.row.status === 1 ? '启用' : '停用' }}
                    </el-tag>
                </template>
            </el-table-column>
            <el-table-column label="操作" width="180" fixed="right">
                <template #default="scope">
                    <div class="op-cell">
                        <el-button text type="primary" size="small" @click="handleEdit(scope.row)">编辑</el-button>
                        <el-button text :type="scope.row.status === 1 ? 'warning' : 'success'" size="small"
                                   @click="handleToggleStatus(scope.row)">
                            {{ scope.row.status === 1 ? '停用' : '启用' }}
                        </el-button>
                        <el-button text type="danger" size="small" @click="handleDelete(scope.row)">删除</el-button>
                    </div>
                </template>
            </el-table-column>
        </el-table>

        <!-- 新增 / 编辑弹窗 -->
        <el-dialog v-model="dialogVisible" :title="editingId ? '编辑轮播图' : '新增轮播图'"
                   width="520px" :close-on-click-modal="false">
            <el-form label-position="top" @submit.prevent>
                <el-form-item label="轮播图片" required>
                    <el-upload :show-file-list="false" accept="image/*"
                               :http-request="handleUpload" :before-upload="beforeUpload">
                        <el-image v-if="form.imageUrl" :src="form.imageUrl" fit="cover"
                                  style="width: 320px; height: 118px; border-radius: 6px" />
                        <el-button v-else type="primary" plain :loading="uploading">上传图片</el-button>
                    </el-upload>
                    <el-button v-if="form.imageUrl" text type="danger" size="small"
                               @click="form.imageUrl = ''">移除图片</el-button>
                </el-form-item>
                <el-form-item label="标题（可选）">
                    <el-input v-model="form.title" maxlength="100" placeholder="请输入标题（≤100字）" />
                </el-form-item>
                <el-form-item label="跳转链接（可选，仅 http/https）">
                    <el-input v-model="form.linkUrl" placeholder="https://..." />
                </el-form-item>
                <el-form-item label="排序（数字越小越靠前）">
                    <el-input-number v-model="form.sortOrder" :min="0" :max="999" />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="dialogVisible = false">取消</el-button>
                <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import PageHead from '@/components/PageHead.vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
    getAdminBanners, createBanner, updateBanner, updateBannerStatus,
    deleteBanner, uploadBannerImage
} from '@/api/notify'

const tableData = ref([])
const loading = ref(false)

const dialogVisible = ref(false)
const saving = ref(false)
const uploading = ref(false)
const editingId = ref(null)
const form = reactive({ title: '', imageUrl: '', linkUrl: '', sortOrder: 0 })

const load = () => {
    loading.value = true
    getAdminBanners().then(data => {
        tableData.value = data || []
    }).finally(() => {
        loading.value = false
    })
}

// 上传走 OSS（businessType=BANNER），返回完整 URL
const beforeUpload = (file) => {
    const isImage = file.type.startsWith('image/')
    const isLt5M = file.size / 1024 / 1024 < 5
    if (!isImage) ElMessage.error('仅支持图片文件')
    if (!isLt5M) ElMessage.error('图片大小不能超过 5MB')
    return isImage && isLt5M
}

const handleUpload = async ({ file, onSuccess, onError }) => {
    try {
        uploading.value = true
        const res = await uploadBannerImage(file)
        form.imageUrl = res.filePath
        onSuccess?.(res)
    } catch (e) {
        onError?.(e)
    } finally {
        uploading.value = false
    }
}

const handleCreate = () => {
    editingId.value = null
    form.title = ''
    form.imageUrl = ''
    form.linkUrl = ''
    form.sortOrder = 0
    dialogVisible.value = true
}

const handleEdit = (row) => {
    editingId.value = row.id
    form.title = row.title || ''
    form.imageUrl = row.imageUrl
    form.linkUrl = row.linkUrl || ''
    form.sortOrder = row.sortOrder ?? 0
    dialogVisible.value = true
}

const handleSave = () => {
    if (!form.imageUrl) {
        ElMessage.warning('请上传轮播图片')
        return
    }
    saving.value = true
    const payload = {
        title: form.title.trim() || null,
        imageUrl: form.imageUrl,
        linkUrl: form.linkUrl.trim() || null,
        sortOrder: form.sortOrder
    }
    const request = editingId.value
        ? updateBanner(editingId.value, payload)
        : createBanner(payload)
    request.then(() => {
        ElMessage.success('已保存，首页轮播即时生效')
        dialogVisible.value = false
        load()
    }).finally(() => {
        saving.value = false
    })
}

// 启停（停用后首页立即不显示，后端已删缓存）
const handleToggleStatus = (row) => {
    const action = row.status === 1 ? '停用' : '启用'
    ElMessageBox.confirm(`确定${action}轮播图「${row.title || row.id}」吗？`, `${action}轮播图`, {
        confirmButtonText: `确定${action}`,
        cancelButtonText: '取消',
        type: 'warning'
    }).then(() => {
        updateBannerStatus(row.id, row.status === 1 ? 0 : 1).then(() => {
            ElMessage.success(`已${action}`)
            load()
        })
    }).catch(() => {})
}

const handleDelete = (row) => {
    ElMessageBox.confirm(`删除后不可恢复，确定删除轮播图「${row.title || row.id}」吗？`, '删除轮播图', {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'error'
    }).then(() => {
        deleteBanner(row.id).then(() => {
            ElMessage.success('已删除')
            load()
        })
    }).catch(() => {})
}

onMounted(load)
</script>

<style scoped>
.op-cell {
    display: flex;
    flex-wrap: nowrap;
    gap: 2px;
}
</style>
