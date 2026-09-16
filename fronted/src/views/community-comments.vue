<template>
    <div v-loading="loading">
        <PageHead title="评论管理" />

        <el-tabs v-model="activeTab">
            <!-- 评论列表 Tab -->
            <el-tab-pane label="评论列表" name="comments">
                <div class="filter-bar">
                    <el-select v-model="filters.status" placeholder="状态" clearable style="width: 130px" @change="onSearch">
                        <el-option label="正常" :value="0" />
                        <el-option label="已删除" :value="1" />
                        <el-option label="待审核" :value="2" />
                    </el-select>
                    <el-input v-model="filters.postId" placeholder="帖子ID" clearable style="width: 130px"
                        @keyup.enter="onSearch" @clear="onSearch" />
                    <el-input v-model="filters.keyword" placeholder="内容关键字" clearable style="width: 200px"
                        @keyup.enter="onSearch" @clear="onSearch" />
                    <el-button type="primary" @click="onSearch">查询</el-button>
                    <el-button @click="onReset">重置</el-button>
                </div>

                <el-table :data="comments" style="width: 100%; margin-top: 16px">
                    <el-table-column prop="id" label="ID" width="80" />
                    <el-table-column label="所属帖子" min-width="180" show-overflow-tooltip>
                        <template #default="scope">
                            <span>{{ scope.row.postTitle || `帖子#${scope.row.postId}` }}</span>
                        </template>
                    </el-table-column>
                    <el-table-column label="作者" width="120">
                        <template #default="scope">
                            {{ scope.row.isAnonymous === 1 ? '匿名' : (scope.row.authorName || `用户#${scope.row.authorId}`) }}
                        </template>
                    </el-table-column>
                    <el-table-column label="内容" min-width="260">
                        <template #default="scope">
                            <!-- 楼中楼：缩进展示父评论摘要 -->
                            <div v-if="scope.row.parentId" class="reply-cell">
                                <div class="parent-quote">回复 {{ scope.row.parentContent || `评论#${scope.row.parentId}` }}</div>
                                <div class="comment-content">{{ scope.row.content }}</div>
                            </div>
                            <span v-else>{{ scope.row.content }}</span>
                        </template>
                    </el-table-column>
                    <el-table-column label="状态" width="100">
                        <template #default="scope">
                            <el-tag :type="commentTagType(scope.row.status)" size="small">
                                {{ commentStatusText[scope.row.status] || '未知' }}
                            </el-tag>
                        </template>
                    </el-table-column>
                    <el-table-column label="时间" width="160">
                        <template #default="scope">{{ formatDateTime(scope.row.createdAt) }}</template>
                    </el-table-column>
                    <el-table-column label="操作" width="180" fixed="right">
                        <template #default="scope">
                            <template v-if="scope.row.status === 2">
                                <el-button text type="primary" @click="review(scope.row, true)">通过</el-button>
                                <el-button text type="danger" @click="review(scope.row, false)">驳回</el-button>
                            </template>
                            <el-button v-if="scope.row.status !== 1" text type="danger" @click="onDelete(scope.row)">删除</el-button>
                        </template>
                    </el-table-column>
                </el-table>

                <el-pagination style="margin-top: 20px" :current-page="commentPage" :page-size="size"
                    :total="commentTotal" layout="total, prev, pager, next" @current-change="onCommentPageChange" />
            </el-tab-pane>

            <!-- 黑名单 Tab -->
            <el-tab-pane label="黑名单" name="blacklist">
                <div class="filter-bar">
                    <el-input v-model="blacklistKeyword" placeholder="用户昵称" clearable style="width: 200px"
                        @keyup.enter="loadBlacklist" @clear="loadBlacklist" />
                    <el-button type="primary" @click="loadBlacklist">查询</el-button>
                    <el-button type="danger" plain @click="addVisible = true">屏蔽用户</el-button>
                </div>

                <el-table :data="blacklist" style="width: 100%; margin-top: 16px">
                    <el-table-column prop="userId" label="用户ID" width="110" />
                    <el-table-column label="用户昵称" min-width="160">
                        <template #default="scope">{{ scope.row.nickname || `用户#${scope.row.userId}` }}</template>
                    </el-table-column>
                    <el-table-column prop="reason" label="屏蔽原因" min-width="220" show-overflow-tooltip />
                    <el-table-column label="屏蔽时间" width="160">
                        <template #default="scope">{{ formatDateTime(scope.row.createdAt) }}</template>
                    </el-table-column>
                    <el-table-column label="操作" width="120" fixed="right">
                        <template #default="scope">
                            <el-button text type="warning" @click="onRemove(scope.row)">解除屏蔽</el-button>
                        </template>
                    </el-table-column>
                </el-table>

                <el-pagination style="margin-top: 20px" :current-page="blacklistPage" :page-size="size"
                    :total="blacklistTotal" layout="total, prev, pager, next" @current-change="onBlacklistPageChange" />
            </el-tab-pane>
        </el-tabs>

        <!-- 屏蔽用户弹窗 -->
        <el-dialog v-model="addVisible" title="屏蔽用户" width="440px">
            <el-form label-width="90px">
                <el-form-item label="用户ID" required>
                    <el-input-number v-model="addForm.userId" :min="1" :controls="false" style="width: 200px"
                        placeholder="被屏蔽用户的ID" />
                </el-form-item>
                <el-form-item label="屏蔽原因">
                    <el-input v-model="addForm.reason" type="textarea" :rows="2" maxlength="200" show-word-limit
                        placeholder="选填，仅屏蔽社区发帖/评论，不影响登录" />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="addVisible = false">取消</el-button>
                <el-button type="primary" @click="submitAdd">确定</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup>
import { reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHead from '@/components/PageHead.vue'
import { formatDateTime } from '@/utils/format'
import {
    getAdminCommentPage, reviewAdminComment, deleteAdminComment,
    getAdminBlacklist, addAdminBlacklist, removeAdminBlacklist
} from '@/api/community'

// 评论状态：0正常 1已删除 2待审核
const commentStatusText = { 0: '正常', 1: '已删除', 2: '待审核' }
const commentTagType = (s) => ({ 0: 'success', 1: 'info', 2: 'warning' }[s] || 'info')

const size = 20
const loading = ref(false)
const activeTab = ref('comments')

// ===== 评论列表 =====
const comments = ref([])
const commentPage = ref(1)
const commentTotal = ref(0)
const filters = reactive({ status: null, postId: '', keyword: '' })

const loadComments = () => {
    loading.value = true
    getAdminCommentPage({
        status: filters.status ?? undefined,
        postId: filters.postId || undefined,
        keyword: filters.keyword || undefined,
        page: commentPage.value,
        size
    }).then(data => {
        comments.value = data.records || []
        commentTotal.value = data.total || 0
    }).finally(() => { loading.value = false })
}

const onSearch = () => { commentPage.value = 1; loadComments() }

const onReset = () => {
    filters.status = null
    filters.postId = ''
    filters.keyword = ''
    onSearch()
}

const onCommentPageChange = (p) => { commentPage.value = p; loadComments() }

// 审核通过 → 正常；驳回 → 软删 + count-1（后端同事务）
const review = (row, approve) => {
    const doIt = () => reviewAdminComment(row.id, approve)
        .then(() => {
            ElMessage.success(approve ? '已通过，评论恢复可见' : '已驳回，评论已删除')
            loadComments()
        })
    if (approve) {
        doIt()
    } else {
        ElMessageBox.confirm('确定驳回该评论？驳回后评论将被删除。', '驳回确认', { type: 'warning' })
            .then(doIt)
            .catch(() => {})
    }
}

const onDelete = (row) => {
    ElMessageBox.confirm('确定删除该评论？删除后用户端不可见，帖子评论数同步减少。', '删除确认', { type: 'warning' })
        .then(() => deleteAdminComment(row.id))
        .then(() => { ElMessage.success('已删除'); loadComments() })
        .catch(() => {})
}

// ===== 黑名单 =====
const blacklist = ref([])
const blacklistPage = ref(1)
const blacklistTotal = ref(0)
const blacklistKeyword = ref('')

const loadBlacklist = () => {
    getAdminBlacklist({
        keyword: blacklistKeyword.value || undefined,
        page: blacklistPage.value,
        size
    }).then(data => {
        blacklist.value = data.records || []
        blacklistTotal.value = data.total || 0
    })
}

const onBlacklistPageChange = (p) => { blacklistPage.value = p; loadBlacklist() }

const addVisible = ref(false)
const addForm = reactive({ userId: undefined, reason: '' })

const submitAdd = () => {
    if (!addForm.userId) {
        ElMessage.warning('请输入用户ID')
        return
    }
    addAdminBlacklist({ userId: addForm.userId, reason: addForm.reason.trim() || undefined })
        .then(() => {
            ElMessage.success('已屏蔽，该用户将无法发帖/评论（不影响登录）')
            addVisible.value = false
            addForm.userId = undefined
            addForm.reason = ''
            loadBlacklist()
        })
}

const onRemove = (row) => {
    ElMessageBox.confirm(`确定解除用户「${row.nickname || `#${row.userId}`}」的屏蔽？解除后立即可发帖/评论。`, '解除确认', { type: 'warning' })
        .then(() => removeAdminBlacklist(row.userId))
        .then(() => { ElMessage.success('已解除屏蔽'); loadBlacklist() })
        .catch(() => {})
}

// Tab 切换懒加载：黑名单数据仅在首次进入时请求
watch(activeTab, (tab) => {
    if (tab === 'blacklist' && blacklistTotal.value === 0 && blacklist.value.length === 0) {
        loadBlacklist()
    }
})

loadComments()
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

.reply-cell .parent-quote {
    font-size: 12px;
    color: #6b7280;
    background: #f7f8fa;
    border-left: 2px solid #e5e7eb;
    padding: 2px 8px;
    margin-bottom: 4px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.reply-cell .comment-content {
    padding-left: 12px;
}
</style>
