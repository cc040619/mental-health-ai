<template>
  <div class="ai-blocks-page">
    <PageHead title="拦截记录" />
    <el-card shadow="never" class="panel">
      <el-table :data="records" v-loading="loading" size="default" border>
        <el-table-column prop="createdAt" label="时间" width="180">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column prop="triggerWord" label="触发词" width="140" align="center">
          <template #default="{ row }">
            <el-tag type="danger" size="small">{{ row.triggerWord }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="处理动作" width="110" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.action === 'BLOCK'" type="danger" size="small">整条屏蔽</el-tag>
            <el-tag v-else-if="row.action === 'REPLACE'" type="warning" size="small">打码替换</el-tag>
            <el-tag v-else type="info" size="small">仅记录</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sessionId" label="会话ID" width="100" align="center" />
        <el-table-column prop="contentSummary" label="内容摘要" min-width="300" show-overflow-tooltip />
      </el-table>
      <div class="pager">
        <el-pagination
          v-model:current-page="page"
          :page-size="size"
          :total="total"
          layout="total, prev, pager, next"
          background
          @current-change="load"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import PageHead from '@/components/PageHead.vue'
import { getAiBlocks } from '@/api/admin'

const records = ref([])
const page = ref(1)
const size = ref(20)
const total = ref(0)
const loading = ref(false)

const formatTime = (t) => (t ? String(t).replace('T', ' ').slice(0, 19) : '')

const load = async () => {
  loading.value = true
  try {
    const res = await getAiBlocks({ page: page.value, size: size.value })
    records.value = res.data.records || []
    total.value = Number(res.data.total || 0)
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style lang="scss" scoped>
.panel {
  border-radius: 8px;
}
.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}
</style>
