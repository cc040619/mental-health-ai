<template>
  <div class="settings-page">
    <PageHead title="危机干预" />

    <el-tabs v-model="activeTab" class="crisis-tabs">
      <!-- Tab 1：热线与干预文案 -->
      <el-tab-pane label="热线与文案" name="hotline">
        <el-card shadow="never" class="settings-card" v-loading="loading">
          <template #header>
            <span class="card-title">危机干预卡片配置</span>
            <span class="card-subtitle">保存后用户端危机卡片展示新值（缓存 5 分钟内自动刷新）</span>
          </template>

          <div class="section-label">求助热线</div>
          <el-table :data="hotlines" size="small" class="hotline-table">
            <el-table-column label="名称" min-width="180">
              <template #default="{ row }">
                <el-input v-model="row.name" maxlength="50" placeholder="如：全国心理援助热线" />
              </template>
            </el-table-column>
            <el-table-column label="号码" width="180">
              <template #default="{ row }">
                <el-input v-model="row.phone" maxlength="30" placeholder="如：12356" />
              </template>
            </el-table-column>
            <el-table-column label="说明（可选）" min-width="220">
              <template #default="{ row }">
                <el-input v-model="row.note" maxlength="100" placeholder="展示在号码下方的一行说明" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="80" class-name="col-action">
              <template #default="{ $index }">
                <el-button text type="danger" :disabled="hotlines.length <= 1" @click="hotlines.splice($index, 1)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-button class="add-row-btn" text type="primary" :disabled="hotlines.length >= 10" @click="hotlines.push({ name: '', phone: '', note: '' })">
            + 添加热线（最多 10 条）
          </el-button>

          <div class="section-label">干预提示文案</div>
          <el-input
            v-model="promptText"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="展示在热线列表上方的引导语，如：如果你正处于危机之中，请立即拨打下面任一条热线……"
          />

          <div class="save-bar">
            <el-button type="primary" :loading="saving" @click="onSaveHotline">保存设置</el-button>
          </div>
        </el-card>
      </el-tab-pane>

      <!-- Tab 2：危机词库 -->
      <el-tab-pane label="危机词库" name="words">
        <el-card shadow="never" class="settings-card">
          <template #header>
            <span class="card-title">危机词管理</span>
            <span class="card-subtitle">命中危机词的内容会打干预标记；新增/删除不重启即生效</span>
          </template>

          <div class="words-toolbar">
            <el-input v-model="wordQuery.keyword" placeholder="按词语搜索" clearable style="width: 200px" @keyup.enter="onSearchWords" />
            <el-button type="primary" @click="onSearchWords">查询</el-button>
            <div class="toolbar-right">
              <el-button type="primary" plain @click="openAddDialog">新增危机词</el-button>
              <el-button type="danger" plain @click="onRestoreDefault">恢复默认词库</el-button>
            </div>
          </div>

          <el-alert type="warning" :closable="false" show-icon class="guard-tip"
                    title="安全护栏：启用危机词不足 5 个时，删除/停用会被拒绝；如需整体重建请使用「恢复默认词库」" />

          <el-table :data="words" v-loading="wordsLoading" size="small" empty-text="暂无危机词">
            <el-table-column prop="word" label="危机词" min-width="160" />
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small" effect="light">
                  {{ row.status === 1 ? '启用' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="创建时间" width="170">
              <template #default="{ row }">{{ fmtTime(row.createdAt) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="150" class-name="col-action">
              <template #default="{ row }">
                <el-popconfirm
                  :title="row.status === 1 ? '停用后将不再命中该词，确认停用？' : '确认启用该危机词？'"
                  @confirm="onToggleWord(row)"
                >
                  <template #reference>
                    <el-button text :type="row.status === 1 ? 'warning' : 'success'">{{ row.status === 1 ? '停用' : '启用' }}</el-button>
                  </template>
                </el-popconfirm>
                <el-popconfirm title="确认删除该危机词？" @confirm="onDeleteWord(row)">
                  <template #reference>
                    <el-button text type="danger">删除</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>

          <div class="words-pager">
            <el-pagination
              v-model:current-page="wordQuery.page"
              v-model:page-size="wordQuery.size"
              :page-sizes="[10, 20]"
              :total="wordTotal"
              layout="total, sizes, prev, pager, next"
              @size-change="loadWords"
              @current-change="loadWords"
            />
          </div>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 新增危机词弹窗 -->
    <el-dialog v-model="addVisible" title="新增危机词" width="420px" :close-on-click-modal="false">
      <el-input v-model="newWord" maxlength="50" show-word-limit placeholder="输入危机词（≤50 字），保存后立即生效" @keyup.enter="onAddWord" />
      <template #footer>
        <el-button @click="addVisible = false">取消</el-button>
        <el-button type="primary" :loading="adding" @click="onAddWord">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHead from '@/components/PageHead.vue'
import {
  getAdminSettings,
  saveAdminSettings,
  getCrisisWords,
  addCrisisWord,
  updateCrisisWord,
  deleteCrisisWord,
  restoreCrisisWords
} from '@/api/admin'

const activeTab = ref('hotline')

// ==================== 热线与文案 ====================
const loading = ref(false)
const saving = ref(false)
const hotlines = ref([{ name: '', phone: '', note: '' }])
const promptText = ref('')

async function loadHotline() {
  loading.value = true
  try {
    const list = (await getAdminSettings('CRISIS')) || []
    list.forEach(item => {
      if (item.configKey === 'HOTLINE_NUMBERS') {
        try {
          const arr = JSON.parse(item.configValue || '[]')
          if (Array.isArray(arr) && arr.length) hotlines.value = arr.map(h => ({ name: h.name || '', phone: h.phone || '', note: h.note || '' }))
        } catch (e) {
          // JSON 解析失败保留默认空行，保存时会重新整体覆盖
        }
      } else if (item.configKey === 'CRISIS_PROMPT_TEXT') {
        promptText.value = item.configValue || ''
      }
    })
  } catch (e) {
    ElMessage.error('配置加载失败')
  } finally {
    loading.value = false
  }
}

async function onSaveHotline() {
  // 过滤整行为空的记录，校验必填项
  const rows = hotlines.value.filter(h => (h.name || '').trim() || (h.phone || '').trim())
  if (!rows.length) {
    ElMessage.warning('至少保留一条热线')
    return
  }
  for (const h of rows) {
    if (!h.name.trim() || !h.phone.trim()) {
      ElMessage.warning('热线的名称和号码不能为空')
      return
    }
  }
  saving.value = true
  try {
    await saveAdminSettings({
      items: [
        { configKey: 'HOTLINE_NUMBERS', configValue: JSON.stringify(rows.map(h => ({ name: h.name.trim(), phone: h.phone.trim(), note: (h.note || '').trim() }))) },
        { configKey: 'CRISIS_PROMPT_TEXT', configValue: promptText.value.trim() }
      ]
    })
    ElMessage.success('保存成功')
    loadHotline()
  } catch (e) {
    // 拦截器已提示
  } finally {
    saving.value = false
  }
}

// ==================== 危机词库 ====================
const wordQuery = reactive({ keyword: '', page: 1, size: 10 })
const words = ref([])
const wordTotal = ref(0)
const wordsLoading = ref(false)

async function loadWords() {
  wordsLoading.value = true
  try {
    const params = { page: wordQuery.page, size: wordQuery.size }
    if (wordQuery.keyword) params.keyword = wordQuery.keyword
    const res = await getCrisisWords(params)
    words.value = res?.list || []
    wordTotal.value = res?.total || 0
  } catch (e) {
    ElMessage.error('危机词加载失败')
  } finally {
    wordsLoading.value = false
  }
}

function onSearchWords() {
  wordQuery.page = 1
  loadWords()
}

const addVisible = ref(false)
const newWord = ref('')
const adding = ref(false)

function openAddDialog() {
  newWord.value = ''
  addVisible.value = true
}

async function onAddWord() {
  const word = newWord.value.trim()
  if (!word) {
    ElMessage.warning('请输入危机词')
    return
  }
  adding.value = true
  try {
    await addCrisisWord({ word })
    ElMessage.success('已新增并即时生效')
    addVisible.value = false
    onSearchWords()
  } catch (e) {
    // 拦截器已提示（重复等）
  } finally {
    adding.value = false
  }
}

async function onToggleWord(row) {
  try {
    await updateCrisisWord(row.id, { status: row.status === 1 ? 0 : 1 })
    ElMessage.success(row.status === 1 ? '已停用' : '已启用')
    loadWords()
  } catch (e) {
    // 护栏拒绝等信息由拦截器提示
  }
}

async function onDeleteWord(row) {
  try {
    await deleteCrisisWord(row.id)
    ElMessage.success('已删除')
    loadWords()
  } catch (e) {
    // 护栏拒绝等信息由拦截器提示
  }
}

// 恢复默认：二次确认 + 全量重建
function onRestoreDefault() {
  ElMessageBox.confirm(
    '将清空当前危机词库，并从系统默认词库（crisis-words.txt）全量重建，已自定义的词会丢失。确定继续吗？',
    '恢复默认词库',
    { confirmButtonText: '确认重建', cancelButtonText: '取消', type: 'warning' }
  ).then(async () => {
    try {
      const res = await restoreCrisisWords()
      ElMessage.success(res?.message || '已恢复默认词库')
      onSearchWords()
    } catch (e) {
      // 拦截器已提示
    }
  }).catch(() => {})
}

function fmtTime(t) {
  if (!t) return '—'
  return String(t).replace('T', ' ').slice(0, 19)
}

onMounted(() => {
  loadHotline()
  loadWords()
})
</script>

<style lang="scss" scoped>
.settings-page {
  padding: 16px;
  background-color: #F7F8FA;
  min-height: 100%;
}

.crisis-tabs {
  :deep(.el-tabs__header) {
    margin-bottom: 0;
  }

  :deep(.el-tabs__item.is-active) {
    font-weight: 600;
  }
}

.settings-card {
  border: 1px solid #E5E7EB;
  border-radius: 4px;
  border-top-left-radius: 0;
  max-width: 1000px;

  :deep(.el-card__header) {
    padding: 14px 16px;
    border-bottom: 1px solid #E5E7EB;
  }

  :deep(.el-card__body) {
    padding: 16px;
  }
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #1F2937;
}

.card-subtitle {
  margin-left: 12px;
  font-size: 12px;
  color: #6B7280;
}

.section-label {
  font-size: 13px;
  font-weight: 600;
  color: #1F2937;
  margin: 4px 0 10px;
}

.hotline-table {
  :deep(.el-input__inner) {
    font-size: 13px;
  }
}

.add-row-btn {
  margin: 8px 0 16px;
}

.save-bar {
  margin-top: 16px;
}

.words-toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;

  .toolbar-right {
    margin-left: auto;
    display: flex;
    gap: 10px;
  }
}

.guard-tip {
  margin-bottom: 12px;
}

.words-pager {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
}

// 操作列按钮自然流式排列，不截断为省略号
:deep(.col-action .cell) {
  white-space: nowrap;
  display: flex;
  align-items: center;
  gap: 4px;
}
</style>
