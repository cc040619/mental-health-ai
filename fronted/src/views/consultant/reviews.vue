<template>
    <div class="consultant-reviews">
        <PageHead title="评价回复" />

        <el-card shadow="never" class="table-card" v-loading="loading">
            <el-table :data="records" style="width: 100%">
                <el-table-column label="来访者" min-width="120">
                    <template #default="{ row }">{{ row.userNickname || `用户 #${row.userId}` }}</template>
                </el-table-column>
                <el-table-column label="评分" width="170">
                    <template #default="{ row }">
                        <el-rate :model-value="row.rating" disabled />
                    </template>
                </el-table-column>
                <el-table-column label="评价内容" min-width="220" show-overflow-tooltip>
                    <template #default="{ row }">{{ row.content || '用户未填写评价内容' }}</template>
                </el-table-column>
                <el-table-column prop="appointmentDate" label="咨询日期" width="120" />
                <el-table-column label="评价时间" width="160">
                    <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
                </el-table-column>
                <el-table-column label="回复状态" width="100">
                    <template #default="{ row }">
                        <el-tag v-if="row.replyContent" type="success" size="small">已回复</el-tag>
                        <el-tag v-else type="warning" size="small">未回复</el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="100" fixed="right">
                    <template #default="{ row }">
                        <el-button link type="primary" @click="openReply(row)">
                            {{ row.replyContent ? '修改回复' : '回复' }}
                        </el-button>
                    </template>
                </el-table-column>
            </el-table>

            <div class="pager">
                <el-pagination background layout="total, prev, pager, next"
                               :total="total" :page-size="size" :current-page="page"
                               @current-change="onPageChange" />
            </div>
        </el-card>

        <!-- 回复弹窗 -->
        <el-dialog v-model="dialogVisible" title="回复评价" width="560px" :close-on-click-modal="false">
            <div v-if="current" class="reply-origin">
                <el-rate :model-value="current.rating" disabled size="small" />
                <p class="origin-content">{{ current.content || '用户未填写评价内容' }}</p>
                <p v-if="current.replyContent" class="origin-reply">原回复：{{ current.replyContent }}</p>
            </div>
            <el-input v-model="replyContent" type="textarea" :rows="4" maxlength="500" show-word-limit
                      placeholder="请输入回复内容（500 字以内，将公开展示在咨询师详情页）" />
            <template #footer>
                <el-button @click="dialogVisible = false">取消</el-button>
                <el-button type="primary" :loading="saving" @click="saveReply">保存回复</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import PageHead from '@/components/PageHead.vue'
import { getMyReviews, replyMyReview } from '@/api/consultant'

const records = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)

const dialogVisible = ref(false)
const saving = ref(false)
const current = ref(null)
const replyContent = ref('')

const formatTime = (t) => (t ? String(t).replace('T', ' ').slice(0, 16) : '-')

const load = () => {
    loading.value = true
    getMyReviews({ page: page.value, size: size.value }).then(data => {
        records.value = data.records || []
        total.value = Number(data.total) || 0
    }).finally(() => {
        loading.value = false
    })
}

const onPageChange = (p) => {
    page.value = p
    load()
}

const openReply = (row) => {
    current.value = row
    replyContent.value = row.replyContent || ''
    dialogVisible.value = true
}

const saveReply = () => {
    const content = replyContent.value.trim()
    if (!content) {
        ElMessage.warning('请输入回复内容')
        return
    }
    saving.value = true
    replyMyReview(current.value.id, content).then(() => {
        ElMessage.success('回复已保存')
        dialogVisible.value = false
        load()
    }).finally(() => {
        saving.value = false
    })
}

onMounted(load)
</script>

<style lang="scss" scoped>
.consultant-reviews {
    .table-card {
        border-color: #e5e7eb;
    }
    .pager {
        display: flex;
        justify-content: flex-end;
        margin-top: 16px;
    }
    .reply-origin {
        background: #f7f8fa;
        border: 1px solid #e5e7eb;
        border-radius: 8px;
        padding: 12px;
        margin-bottom: 14px;
        .origin-content {
            font-size: 14px;
            color: #1f2937;
            margin-top: 8px;
        }
        .origin-reply {
            font-size: 13px;
            color: #6b7280;
            margin-top: 8px;
        }
    }
}
</style>
