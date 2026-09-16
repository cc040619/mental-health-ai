<template>
  <div class="ai-samples-page">
    <PageHead title="对话质量抽检">
      <template #buttons>
        <el-select v-model="generateCount" style="width: 90px">
          <el-option v-for="n in 20" :key="n" :label="`${n} 个`" :value="n" />
        </el-select>
        <el-button type="primary" :loading="generating" @click="onGenerate">发起抽检</el-button>
      </template>
    </PageHead>

    <el-card shadow="never" class="panel">
      <!-- 状态筛选 -->
      <el-radio-group v-model="queryStatus" style="margin-bottom: 14px" @change="onFilter">
        <el-radio-button :value="null">全部</el-radio-button>
        <el-radio-button :value="0">待标注</el-radio-button>
        <el-radio-button :value="1">好</el-radio-button>
        <el-radio-button :value="2">一般</el-radio-button>
        <el-radio-button :value="3">坏</el-radio-button>
      </el-radio-group>

      <el-table :data="records" v-loading="loading" size="default" border min-height="500px">
        <el-table-column prop="nickname" label="用户昵称" width="130" show-overflow-tooltip>
          <template #default="{ row }">{{ row.nickname || '已注销' }}</template>
        </el-table-column>
        <el-table-column prop="sessionTitle" label="会话标题" min-width="160" show-overflow-tooltip />
        <el-table-column label="场景" width="100" align="center">
          <template #default="{ row }">{{ row.sceneLabel || sceneLabel(row.scene) || '-' }}</template>
        </el-table-column>
        <el-table-column prop="messageCount" label="消息数" width="80" align="center" />
        <el-table-column label="标注状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.status === 0" type="warning" size="small">待标注</el-tag>
            <el-tag v-else-if="row.status === 1" type="success" size="small">好</el-tag>
            <el-tag v-else-if="row.status === 2" type="info" size="small">一般</el-tag>
            <el-tag v-else type="danger" size="small">坏</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="评分" width="80" align="center">
          <template #default="{ row }">{{ row.score ? `${row.score} 分` : '-' }}</template>
        </el-table-column>
        <el-table-column prop="createdAt" label="抽检时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openAnnotate(row)">标注</el-button>
            <el-button link type="primary" size="small" @click="openSession(row)">查看会话</el-button>
          </template>
        </el-table-column>
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

    <!-- 标注弹窗 -->
    <el-dialog v-model="annotateVisible" title="抽检标注" width="480px" destroy-on-close>
      <el-form label-width="70px">
        <el-form-item label="状态">
          <el-radio-group v-model="annotateForm.status">
            <el-radio :value="1">好</el-radio>
            <el-radio :value="2">一般</el-radio>
            <el-radio :value="3">坏</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="评分">
          <el-rate v-model="annotateForm.score" :max="5" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="annotateForm.note" type="textarea" :rows="4" maxlength="500" show-word-limit placeholder="标注依据、问题描述等（可空）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="annotateVisible = false">取消</el-button>
        <el-button type="primary" :loading="annotating" @click="onAnnotate">提交标注</el-button>
      </template>
    </el-dialog>

    <!-- 查看会话抽屉 -->
    <el-drawer v-model="sessionVisible" title="会话详情" size="640px" destroy-on-close>
      <div v-if="sessionDetail" v-loading="sessionLoading" class="session-detail">
        <div class="session-meta">
          <span>{{ sessionDetail.userNickname || '已注销' }}</span>
          <el-tag size="small">{{ sessionDetail.sceneLabel || sceneLabel(sessionDetail.scene) }}</el-tag>
          <span>{{ sessionDetail.msgCount }} 条消息</span>
          <span>{{ formatTime(sessionDetail.startedAt) }}</span>
        </div>
        <div class="msg-list">
          <div
            v-for="msg in sessionDetail.messages || []"
            :key="msg.id"
            class="msg-row"
            :class="{ ai: msg.senderType === 2 }"
          >
            <div class="bubble">
              {{ msg.content }}
              <span v-if="msg.emotionTag" class="emotion">（{{ msg.emotionTag }}）</span>
            </div>
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import PageHead from '@/components/PageHead.vue'
import { annotateAiSample, generateAiSamples, getAiSampleDetail, getAiSamples } from '@/api/admin'

const SCENE_LABELS = {
  EMOTION: '情绪疏导', RELATIONSHIP: '人际关系', ACADEMIC: '学业压力',
  SLEEP: '睡眠问题', OTHER: '其他'
}
const sceneLabel = (code) => SCENE_LABELS[code] || code

const records = ref([])
const page = ref(1)
const size = ref(20)
const total = ref(0)
const loading = ref(false)
const queryStatus = ref(null)
const generateCount = ref(5)
const generating = ref(false)

const formatTime = (t) => (t ? String(t).replace('T', ' ').slice(0, 19) : '')

const load = async () => {
  loading.value = true
  try {
    const res = await getAiSamples({ status: queryStatus.value, page: page.value, size: size.value })
    records.value = res.data.records || []
    total.value = Number(res.data.total || 0)
  } finally {
    loading.value = false
  }
}

const onFilter = () => {
  page.value = 1
  load()
}

const onGenerate = async () => {
  generating.value = true
  try {
    const res = await generateAiSamples({ count: generateCount.value })
    ElMessage.success(`已生成 ${res.data.generated} 条待标注样本`)
    page.value = 1
    await load()
  } finally {
    generating.value = false
  }
}

// 标注
const annotateVisible = ref(false)
const annotating = ref(false)
const annotateId = ref(null)
const annotateForm = reactive({ status: 1, score: 3, note: '' })

const openAnnotate = (row) => {
  annotateId.value = row.id
  annotateForm.status = row.status === 0 ? 1 : row.status
  annotateForm.score = row.score || 3
  annotateForm.note = row.note || ''
  annotateVisible.value = true
}

const onAnnotate = async () => {
  annotating.value = true
  try {
    await annotateAiSample(annotateId.value, {
      status: annotateForm.status,
      score: annotateForm.score,
      note: annotateForm.note || undefined
    })
    ElMessage.success('标注完成')
    annotateVisible.value = false
    await load()
  } finally {
    annotating.value = false
  }
}

// 查看会话
const sessionVisible = ref(false)
const sessionLoading = ref(false)
const sessionDetail = ref(null)

const openSession = async (row) => {
  sessionVisible.value = true
  sessionLoading.value = true
  try {
    const res = await getAiSampleDetail(row.id)
    sessionDetail.value = res.data.session
  } finally {
    sessionLoading.value = false
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
.session-detail {
  .session-meta {
    display: flex;
    align-items: center;
    gap: 12px;
    padding-bottom: 12px;
    margin-bottom: 12px;
    border-bottom: 1px solid #e5e7eb;
    font-size: 13px;
    color: #6b7280;
  }
  .msg-list {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }
  .msg-row {
    display: flex;
    justify-content: flex-start;
    &.ai {
      justify-content: flex-end;
    }
    .bubble {
      max-width: 78%;
      padding: 10px 14px;
      border-radius: 10px;
      background: #f3f4f6;
      font-size: 14px;
      line-height: 1.6;
      color: #1f2937;
      white-space: pre-wrap;
      word-break: break-word;
      .emotion {
        font-size: 12px;
        color: #6b7280;
      }
    }
  }
  .msg-row.ai .bubble {
    background: #eff6ff;
  }
}
</style>
