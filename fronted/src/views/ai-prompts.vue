<template>
  <div class="ai-prompts-page">
    <PageHead title="提示词管理">
      <template #buttons>
        <el-button @click="onRestore">恢复默认（生成草稿）</el-button>
      </template>
    </PageHead>

    <el-row :gutter="16">
      <!-- 左侧 key 导航 -->
      <el-col :span="6">
        <el-card shadow="never" class="panel nav-panel">
          <div
            v-for="item in groups"
            :key="item.promptKey"
            class="nav-item"
            :class="{ active: currentKey === item.promptKey }"
            @click="selectKey(item.promptKey)"
          >
            <span class="nav-label">{{ item.label }}</span>
            <span class="nav-version">{{ item.activeVersion != null ? `v${item.activeVersion}` : '默认' }}</span>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧详情 -->
      <el-col :span="18">
        <el-card shadow="never" class="panel">
          <template #header>
            <div class="detail-head">
              <span class="panel-title">{{ currentMeta.label }}</span>
              <div class="detail-actions">
                <el-button type="primary" size="small" @click="openEdit">编辑新建版本</el-button>
              </div>
            </div>
          </template>

          <div class="active-block">
            <div class="active-meta">
              <el-tag v-if="currentMeta.activeVersion != null" type="success" size="small">
                生效 v{{ currentMeta.activeVersion }}
              </el-tag>
              <el-tag v-else type="info" size="small">使用系统默认（无生效版本）</el-tag>
              <span v-if="currentMeta.activeName" class="active-name">{{ currentMeta.activeName }}</span>
            </div>
            <pre class="prompt-content">{{ activeContent || '加载中…' }}</pre>
          </div>

          <!-- 版本历史 -->
          <div class="history-title">版本历史</div>
          <el-table :data="versions" size="small" border>
            <el-table-column prop="version" label="版本" width="70" align="center">
              <template #default="{ row }">v{{ row.version }}</template>
            </el-table-column>
            <el-table-column prop="name" label="名称" min-width="140" show-overflow-tooltip />
            <el-table-column prop="summary" label="内容摘要" min-width="240" show-overflow-tooltip />
            <el-table-column label="状态" width="90" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.status === 1" type="success" size="small">生效</el-tag>
                <el-tag v-else type="info" size="small">历史</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="时间" width="170">
              <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="140" align="center">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="preview(row)">预览</el-button>
                <el-button v-if="row.status !== 1" link type="warning" size="small" @click="activate(row)">激活</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- 编辑新建版本 -->
    <el-dialog v-model="editVisible" :title="`编辑提示词 - ${currentMeta.label}`" width="720px" destroy-on-close>
      <el-form label-width="70px">
        <el-form-item label="名称">
          <el-input v-model="editForm.name" placeholder="如：加入危机识别细则" maxlength="100" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="editForm.content" type="textarea" :rows="14" maxlength="8000" show-word-limit />
        </el-form-item>
        <el-form-item label="试跑">
          <div class="test-run">
            <el-input v-model="editForm.testMessage" placeholder="测试消息（默认：你好）" maxlength="1000" style="width: 320px" />
            <el-button :loading="testing" @click="onTestRun">试跑一轮</el-button>
          </div>
          <div v-if="testReply" class="test-reply">{{ testReply }}</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSaveDraft">保存为草稿</el-button>
      </template>
    </el-dialog>

    <!-- 预览全文 -->
    <el-dialog v-model="previewVisible" :title="`预览 v${previewRow?.version} - ${currentMeta.label}`" width="720px">
      <pre class="prompt-content">{{ previewRow?.content }}</pre>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHead from '@/components/PageHead.vue'
import {
  activateAiPrompt, getAiPromptVersions, getAiPrompts, previewAiPrompt,
  restoreAiPrompt, saveAiPrompt, testAiPrompt
} from '@/api/admin'

const groups = ref([])
const currentKey = ref('')
const activeContent = ref('')
const versions = ref([])

const currentMeta = computed(() =>
  groups.value.find(g => g.promptKey === currentKey.value) || { label: '' })

const formatTime = (t) => (t ? String(t).replace('T', ' ').slice(0, 19) : '')

const loadGroups = async (keepKey = true) => {
  const res = await getAiPrompts()
  groups.value = res.data || []
  if (!keepKey || !groups.value.some(g => g.promptKey === currentKey.value)) {
    currentKey.value = groups.value[0]?.promptKey || ''
  }
  await Promise.all([loadActive(), loadVersions()])
}

const loadActive = async () => {
  activeContent.value = ''
  const meta = currentMeta.value
  if (meta.activeVersion == null) return
  const res = await previewAiPrompt(currentKey.value, meta.activeVersion)
  activeContent.value = res.data.content
}

const loadVersions = async () => {
  const res = await getAiPromptVersions(currentKey.value)
  versions.value = res.data || []
}

const selectKey = (key) => {
  currentKey.value = key
  loadActive()
  loadVersions()
}

// 编辑新建版本
const editVisible = ref(false)
const saving = ref(false)
const testing = ref(false)
const testReply = ref('')
const editForm = reactive({ name: '', content: '', testMessage: '' })

const openEdit = async () => {
  editForm.name = ''
  editForm.testMessage = ''
  testReply.value = ''
  // 编辑默认带入当前生效内容（无生效版本则系统默认文案由后端返回空时前端提示）
  editForm.content = activeContent.value || ''
  editVisible.value = true
}

const onSaveDraft = async () => {
  if (!editForm.name.trim()) {
    ElMessage.warning('请填写模板名称')
    return
  }
  if (!editForm.content.trim()) {
    ElMessage.warning('提示词内容不能为空')
    return
  }
  saving.value = true
  try {
    const res = await saveAiPrompt({
      promptKey: currentKey.value,
      name: editForm.name,
      content: editForm.content
    })
    ElMessage.success(res.data.message)
    editVisible.value = false
    await loadGroups()
  } finally {
    saving.value = false
  }
}

const onTestRun = async () => {
  testing.value = true
  try {
    const res = await testAiPrompt({
      content: editForm.content,
      message: editForm.testMessage || undefined
    })
    testReply.value = res.data.reply
  } finally {
    testing.value = false
  }
}

// 激活（二次确认）
const activate = async (row) => {
  const confirmed = await ElMessageBox.confirm(
    `确认激活「${currentMeta.label}」v${row.version} 吗？生效后下一条对话即使用新提示词。`,
    '激活确认',
    { type: 'warning', confirmButtonText: '确认激活', cancelButtonText: '取消' }
  )
  if (!confirmed) return
  const res = await activateAiPrompt(row.id)
  ElMessage.success(res.data.message)
  await loadGroups()
}

// 恢复默认
const onRestore = async () => {
  const confirmed = await ElMessageBox.confirm(
    `将以系统内置默认文案为「${currentMeta.label}」生成一个新草稿版本（不自动激活），确认吗？`,
    '恢复默认',
    { type: 'info', confirmButtonText: '确认', cancelButtonText: '取消' }
  )
  if (!confirmed) return
  const res = await restoreAiPrompt(currentKey.value)
  ElMessage.success(res.data.message)
  await loadGroups()
}

// 预览
const previewVisible = ref(false)
const previewRow = ref(null)
const preview = async (row) => {
  const res = await previewAiPrompt(currentKey.value, row.version)
  previewRow.value = res.data
  previewVisible.value = true
}

onMounted(() => loadGroups(false))
</script>

<style lang="scss" scoped>
.ai-prompts-page {
  .panel {
    border-radius: 8px;
  }
  .panel-title {
    font-size: 15px;
    font-weight: 600;
    color: #1f2937;
  }
  .nav-panel {
    :deep(.el-card__body) {
      padding: 8px;
    }
  }
  .nav-item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 10px 12px;
    border-radius: 6px;
    cursor: pointer;
    font-size: 14px;
    color: #1f2937;
    &:hover {
      background: #f7f8fa;
    }
    &.active {
      background: #eff6ff;
      color: #2563eb;
      .nav-version {
        color: #2563eb;
      }
    }
    .nav-version {
      font-size: 12px;
      color: #6b7280;
    }
  }
  .detail-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
  .active-block {
    margin-bottom: 18px;
    .active-meta {
      display: flex;
      align-items: center;
      gap: 10px;
      margin-bottom: 10px;
      .active-name {
        font-size: 13px;
        color: #6b7280;
      }
    }
  }
  .prompt-content {
    margin: 0;
    padding: 12px 14px;
    background: #f7f8fa;
    border: 1px solid #e5e7eb;
    border-radius: 6px;
    font-size: 13px;
    line-height: 1.7;
    color: #1f2937;
    white-space: pre-wrap;
    word-break: break-all;
    max-height: 320px;
    overflow-y: auto;
  }
  .history-title {
    margin: 4px 0 10px;
    font-size: 14px;
    font-weight: 600;
    color: #1f2937;
  }
  .test-run {
    display: flex;
    gap: 10px;
    width: 100%;
  }
  .test-reply {
    width: 100%;
    margin-top: 10px;
    padding: 10px 12px;
    background: #f7f8fa;
    border: 1px solid #e5e7eb;
    border-radius: 6px;
    font-size: 13px;
    line-height: 1.6;
    color: #1f2937;
    white-space: pre-wrap;
  }
}
</style>
