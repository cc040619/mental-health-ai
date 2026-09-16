<template>
    <div>
        <PageHead title="消息推送" />

        <el-row :gutter="20">
            <!-- 推送表单 -->
            <el-col :span="9">
                <el-card shadow="never" class="push-card">
                    <template #header>
                        <span class="card-title">发起推送</span>
                    </template>
                    <el-form label-position="top" @submit.prevent>
                        <el-form-item label="推送类型" required>
                            <el-select v-model="form.type" style="width: 100%">
                                <el-option label="系统公告" value="ANNOUNCEMENT" />
                                <el-option label="预约提醒" value="APPOINTMENT" />
                                <el-option label="互动消息" value="INTERACTION" />
                                <el-option label="测评提醒" value="ASSESSMENT" />
                            </el-select>
                        </el-form-item>
                        <el-form-item label="推送标题" required>
                            <el-input v-model="form.title" maxlength="200" show-word-limit
                                      placeholder="请输入推送标题（≤200字）" />
                        </el-form-item>
                        <el-form-item label="推送内容" required>
                            <el-input v-model="form.content" type="textarea" :rows="5" maxlength="1000"
                                      show-word-limit placeholder="请输入推送内容（≤1000字）" />
                        </el-form-item>
                        <el-form-item label="推送目标" required>
                            <el-radio-group v-model="form.targetType">
                                <el-radio value="ALL">全体用户</el-radio>
                                <el-radio value="USERS">指定用户</el-radio>
                            </el-radio-group>
                            <el-select v-if="form.targetType === 'USERS'" v-model="form.targetUserIds"
                                       multiple filterable remote :remote-method="searchUsers"
                                       :loading="userSearching" placeholder="输入昵称/用户名搜索用户"
                                       style="width: 100%; margin-top: 8px">
                                <el-option v-for="u in userOptions" :key="u.id"
                                           :label="u.nickname || u.username" :value="u.id" />
                            </el-select>
                        </el-form-item>
                        <el-button type="primary" style="width: 100%" :loading="pushing"
                                   @click="handlePush">推送（{{ targetDesc }}）</el-button>
                    </el-form>
                </el-card>
            </el-col>

            <!-- 推送记录 -->
            <el-col :span="15">
                <el-card shadow="never">
                    <template #header>
                        <span class="card-title">推送记录</span>
                    </template>
                    <el-table :data="records" v-loading="loading" :min-height="480">
                        <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
                        <el-table-column label="类型" width="110">
                            <template #default="scope">
                                <el-tag effect="plain" round>{{ typeLabel(scope.row.type) }}</el-tag>
                            </template>
                        </el-table-column>
                        <el-table-column label="目标" width="90" align="center">
                            <template #default="scope">
                                {{ scope.row.targetType === 'ALL' ? '全体' : '指定' }} · {{ scope.row.targetCount }}
                            </template>
                        </el-table-column>
                        <el-table-column prop="readCount" label="阅读数" width="80" align="center" />
                        <el-table-column label="阅读率" min-width="140">
                            <template #default="scope">
                                <el-progress :percentage="scope.row.readRate || 0" :stroke-width="8"
                                             :color="progressColor" />
                            </template>
                        </el-table-column>
                        <el-table-column label="时间" width="170">
                            <template #default="scope">{{ formatDateTime(scope.row.createdAt) }}</template>
                        </el-table-column>
                    </el-table>
                    <el-pagination style="margin-top: 16px" :current-page="page" :page-size="pageSize"
                                   layout="total, prev, pager, next" :total="total"
                                   @current-change="onPageChange" />
                </el-card>
            </el-col>
        </el-row>
    </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import PageHead from '@/components/PageHead.vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pushNotification, getPushRecords } from '@/api/notify'
import { getAdminUserList } from '@/api/admin'
import { formatDateTime } from '@/utils/format'

const typeMap = {
    ANNOUNCEMENT: '系统公告',
    APPOINTMENT: '预约提醒',
    INTERACTION: '互动消息',
    ASSESSMENT: '测评提醒'
}
const typeLabel = (t) => typeMap[t] || t
const progressColor = [
    { color: '#f56c6c', percentage: 30 },
    { color: '#e6a23c', percentage: 60 },
    { color: '#67c23a', percentage: 100 }
]

const form = reactive({ type: 'ANNOUNCEMENT', title: '', content: '', targetType: 'ALL', targetUserIds: [] })
const pushing = ref(false)
const userOptions = ref([])
const userSearching = ref(false)

const records = ref([])
const loading = ref(false)
const page = ref(1)
const pageSize = 10
const total = ref(0)

const targetDesc = computed(() =>
    form.targetType === 'ALL' ? '全体用户' : `已选 ${form.targetUserIds.length} 人`)

// 远程搜索用户（复用用户管理列表接口）
const searchUsers = (keyword) => {
    userSearching.value = true
    getAdminUserList({ keyword: keyword || '', page: 1, size: 20 }).then(data => {
        userOptions.value = data?.records || []
    }).finally(() => {
        userSearching.value = false
    })
}

const handlePush = () => {
    if (!form.title.trim()) {
        ElMessage.warning('请输入推送标题')
        return
    }
    if (!form.content.trim()) {
        ElMessage.warning('请输入推送内容')
        return
    }
    if (form.targetType === 'USERS' && form.targetUserIds.length === 0) {
        ElMessage.warning('指定用户推送需选择目标用户')
        return
    }
    const desc = form.targetType === 'ALL'
        ? '全体用户'
        : `选定的 ${form.targetUserIds.length} 位用户`
    ElMessageBox.confirm(`站内消息将推送给${desc}，确定推送吗？`, '确认推送', {
        confirmButtonText: '确定推送',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(() => {
        pushing.value = true
        pushNotification({
            type: form.type,
            title: form.title.trim(),
            content: form.content,
            targetType: form.targetType,
            targetUserIds: form.targetType === 'USERS' ? form.targetUserIds : null
        }).then(count => {
            ElMessage.success(`推送成功，共送达 ${count} 位用户`)
            form.title = ''
            form.content = ''
            form.targetUserIds = []
            load()
        }).finally(() => {
            pushing.value = false
        })
    }).catch(() => {})
}

const load = () => {
    loading.value = true
    getPushRecords({ page: page.value, size: pageSize }).then(data => {
        records.value = data.records || []
        total.value = data.total || 0
    }).finally(() => {
        loading.value = false
    })
}

const onPageChange = (p) => {
    page.value = p
    load()
}

onMounted(load)
</script>

<style scoped>
.push-card {
    height: 100%;
}

.card-title {
    font-weight: 600;
}
</style>
