<template>
  <div class="logs-page">
    <PageHead title="操作日志" />

    <!-- 筛选栏 -->
    <el-form inline class="logs-search">
      <el-form-item label="操作人">
        <el-input v-model="query.operator" placeholder="操作人账号" clearable style="width: 140px" />
      </el-form-item>
      <el-form-item label="操作类型">
        <el-select v-model="query.actionType" placeholder="全部" clearable style="width: 120px">
          <el-option v-for="(label, type) in ACTION_TYPES" :key="type" :label="label" :value="type" />
        </el-select>
      </el-form-item>
      <el-form-item label="内容关键字">
        <el-input v-model="query.keyword" placeholder="内容关键字" clearable style="width: 160px" />
      </el-form-item>
      <el-form-item label="时间范围">
        <el-date-picker
          v-model="query.dateRange"
          type="daterange"
          value-format="YYYY-MM-DD"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          style="width: 220px"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="onSearch">查询</el-button>
        <el-button @click="onReset">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 日志表格 -->
    <el-card shadow="never" class="logs-table-card">
      <el-table :data="logs" v-loading="loading" empty-text="暂无日志" style="width: 100%">
        <el-table-column prop="operatorName" label="操作人" width="120">
          <template #default="{ row }">{{ row.operatorName || '—' }}</template>
        </el-table-column>
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="typeTag(row.actionType)" size="small" effect="light">
              {{ ACTION_TYPES[row.actionType] || row.actionType || '—' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="actionContent" label="操作内容" min-width="360" show-overflow-tooltip>
          <template #default="{ row }">{{ row.actionContent || '—' }}</template>
        </el-table-column>
        <el-table-column prop="ip" label="IP" width="140">
          <template #default="{ row }">{{ row.ip || '—' }}</template>
        </el-table-column>
        <el-table-column prop="createdAt" label="操作时间" width="170">
          <template #default="{ row }">{{ fmtTime(row.createdAt) }}</template>
        </el-table-column>
      </el-table>

      <div class="logs-pager">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :page-sizes="[10, 20]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadLogs"
          @current-change="loadLogs"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import PageHead from '@/components/PageHead.vue'
import { getOperationLogs } from '@/api/admin'

// 操作类型口径与后端 AOP/登录登出记录一致
const ACTION_TYPES = {
  CREATE: '新增',
  UPDATE: '修改',
  DELETE: '删除',
  LOGIN: '登录',
  LOGOUT: '登出',
  CONFIG: '配置'
}

const query = reactive({
  operator: '',
  actionType: undefined,
  keyword: '',
  dateRange: null,
  page: 1,
  size: 10
})

const logs = ref([])
const total = ref(0)
const loading = ref(false)

function buildParams() {
  const params = { page: query.page, size: query.size }
  if (query.operator) params.operator = query.operator
  if (query.actionType) params.actionType = query.actionType
  if (query.keyword) params.keyword = query.keyword
  if (Array.isArray(query.dateRange) && query.dateRange.length === 2) {
    params.startDate = query.dateRange[0]
    params.endDate = query.dateRange[1]
  }
  return params
}

async function loadLogs() {
  loading.value = true
  try {
    const res = await getOperationLogs(buildParams())
    logs.value = res?.list || []
    total.value = res?.total || 0
  } catch (e) {
    ElMessage.error('日志加载失败')
  } finally {
    loading.value = false
  }
}

function onSearch() {
  query.page = 1
  loadLogs()
}

function onReset() {
  query.operator = ''
  query.actionType = undefined
  query.keyword = ''
  query.dateRange = null
  query.page = 1
  loadLogs()
}

function typeTag(type) {
  switch (type) {
    case 'CREATE': return 'success'
    case 'UPDATE': return 'primary'
    case 'DELETE': return 'danger'
    case 'LOGIN': return 'success'
    case 'LOGOUT': return 'info'
    case 'CONFIG': return 'warning'
    default: return 'info'
  }
}

function fmtTime(t) {
  if (!t) return '—'
  return String(t).replace('T', ' ').slice(0, 19)
}

onMounted(loadLogs)
</script>

<style lang="scss" scoped>
.logs-page {
  padding: 16px;
  background-color: #F7F8FA;
  min-height: 100%;
}

.logs-search {
  background: #FFFFFF;
  padding: 16px 16px 0;
  border: 1px solid #E5E7EB;
  border-radius: 4px;
  margin-bottom: 16px;
}

.logs-table-card {
  border: 1px solid #E5E7EB;
  border-radius: 4px;

  :deep(.el-card__body) {
    padding: 16px;
  }
}

.logs-pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
