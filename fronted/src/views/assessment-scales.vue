<template>
  <div class="scales-page">
    <PageHead title="量表管理">
      <template #buttons>
        <el-button type="primary" @click="openCreate">新建量表</el-button>
      </template>
    </PageHead>

    <el-form inline class="search-bar">
      <el-form-item label="名称/编码">
        <el-input v-model="query.keyword" placeholder="名称或编码" clearable style="width: 180px" @keyup.enter="onSearch" />
      </el-form-item>
      <el-form-item label="类别">
        <el-select v-model="query.category" placeholder="全部" clearable style="width: 130px">
          <el-option v-for="c in categories" :key="c.value" :label="c.label" :value="c.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.enabled" placeholder="全部" clearable style="width: 110px">
          <el-option label="启用" :value="1" />
          <el-option label="停用" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="onSearch">查询</el-button>
        <el-button @click="onReset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-card shadow="never" class="table-card">
      <el-table :data="list" v-loading="loading" empty-text="暂无量表">
        <el-table-column prop="name" label="名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="code" label="编码" width="110" />
        <el-table-column label="类别" width="100">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ row.categoryLabel || row.category }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="questionCount" label="题目数" width="80" />
        <el-table-column prop="estimatedMinutes" label="预计时长" width="90">
          <template #default="{ row }">{{ row.estimatedMinutes }} 分钟</template>
        </el-table-column>
        <el-table-column prop="assessmentCount" label="测评次数" width="90" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.enabled === 1 ? 'success' : 'info'" size="small">{{ row.enabledLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button text type="primary" @click="openQuestions(row)">题目配置</el-button>
            <el-button text :type="row.enabled === 1 ? 'warning' : 'success'" @click="toggleEnabled(row)">
              {{ row.enabled === 1 ? '停用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total"
        :page-sizes="[10, 20]" layout="total, sizes, prev, pager, next, jumper" class="pagination"
        @size-change="fetchData" @current-change="fetchData" />
    </el-card>

    <!-- 基本信息编辑抽屉 -->
    <el-drawer v-model="editVisible" :title="editId ? '编辑量表' : '新建量表'" size="520px" destroy-on-close>
      <el-form :model="editForm" :rules="editRules" ref="editFormRef" label-width="90px">
        <el-form-item label="编码" prop="code">
          <el-input v-model="editForm.code" placeholder="如 PHQ-9" :disabled="!!editId" />
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="editForm.name" />
        </el-form-item>
        <el-form-item label="类别" prop="category">
          <el-select v-model="editForm.category" style="width: 100%">
            <el-option v-for="c in categories" :key="c.value" :label="c.label" :value="c.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="editForm.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="指导语">
          <el-input v-model="editForm.guidance" type="textarea" :rows="3" placeholder="测评前展示给用户的指导语" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="onSaveScale">保存</el-button>
      </template>
    </el-drawer>

    <!-- 题目配置抽屉 -->
    <el-drawer v-model="qVisible" title="题目与维度配置" size="80%" destroy-on-close>
      <div class="q-config">
        <div class="q-section">
          <div class="q-section-head">
            <span>题目列表</span>
            <div>
              <el-button size="small" @click="addSelfQuestion">+ 自建题目</el-button>
              <el-button size="small" type="primary" plain @click="bankVisible = true">+ 从题库添加</el-button>
            </div>
          </div>
          <el-table :data="questions" border size="small">
            <el-table-column label="序号" width="60" type="index" />
            <el-table-column label="题干" min-width="200">
              <template #default="{ row }">{{ row.text }}</template>
            </el-table-column>
            <el-table-column label="选项" min-width="200">
              <template #default="{ row }">
                <el-tag v-for="(o, i) in row.options" :key="i" size="small" class="opt-tag">{{ o.label }}({{ o.score }})</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="200">
              <template #default="{ $index, row }">
                <el-button text @click="moveQ($index, -1)" :disabled="$index === 0">上移</el-button>
                <el-button text @click="moveQ($index, 1)" :disabled="$index === questions.length - 1">下移</el-button>
                <el-button text type="primary" @click="editQ($index)">编辑</el-button>
                <el-button text type="danger" @click="delQ($index)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div class="q-section">
          <div class="q-section-head"><span>维度配置</span><el-button size="small" @click="dimensions.push({name: '', questionIndexes: []})">+ 维度</el-button></div>
          <div v-for="(d, i) in dimensions" :key="i" class="dim-row">
            <el-input v-model="d.name" placeholder="维度名称" style="width: 140px" />
            <el-select v-model="d.questionIndexes" multiple collapse-tags collapse-tags-tooltip
              placeholder="包含题目" style="flex: 1; min-width: 200px; margin: 0 8px">
              <el-option v-for="(q, qi) in questions" :key="qi" :label="`第${qi+1}题`" :value="qi" />
            </el-select>
            <el-button text type="danger" @click="dimensions.splice(i, 1)">删除</el-button>
          </div>
        </div>

        <div class="q-section">
          <div class="q-section-head"><span>结果解释模板</span></div>
          <div class="tpl-grid">
            <div v-for="lv in levelOptions" :key="lv.value" class="tpl-item">
              <label>{{ lv.label }}</label>
              <el-input v-model="templates[lv.value]" type="textarea" :rows="2" :placeholder="`${lv.label}结果解释`" />
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="qVisible = false">取消</el-button>
        <el-button type="primary" @click="onSaveQuestions">保存题目配置</el-button>
      </template>
    </el-drawer>

    <!-- 题目编辑弹窗 -->
    <el-dialog v-model="qEditVisible" title="编辑题目" width="560px" destroy-on-close>
      <el-form :model="qForm" label-width="80px">
        <el-form-item label="题干">
          <el-input v-model="qForm.text" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="选项">
          <div v-for="(o, i) in qForm.options" :key="i" class="option-row">
            <el-input v-model="o.label" placeholder="选项文本" style="width: 280px" />
            <el-input-number v-model="o.score" :min="0" style="width: 110px; margin-left: 8px" />
            <el-button text type="danger" @click="qForm.options.splice(i, 1)">删除</el-button>
          </div>
          <el-button size="small" @click="qForm.options.push({label: '', score: 0})">+ 选项</el-button>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="qEditVisible = false">取消</el-button>
        <el-button type="primary" @click="onSaveQ">确定</el-button>
      </template>
    </el-dialog>

    <!-- 题库选择弹窗 -->
    <el-dialog v-model="bankVisible" title="从题库添加题目" width="700px" destroy-on-close>
      <el-input v-model="bankKw" placeholder="搜索题干" clearable style="margin-bottom: 12px" @input="fetchBank" />
      <el-table :data="bankList" @selection-change="onBankSelect" size="small" max-height="360">
        <el-table-column type="selection" width="50" />
        <el-table-column prop="stem" label="题干" min-width="240" show-overflow-tooltip />
        <el-table-column label="选项" min-width="160">
          <template #default="{ row }">
            <el-tag v-for="(o, i) in parseOpts(row.options)" :key="i" size="small" class="opt-tag">{{ o.label }}({{ o.score }})</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="bankVisible = false">取消</el-button>
        <el-button type="primary" @click="addFromBank">添加选中</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHead from '@/components/PageHead.vue'
import {
  assessmentScalePage, createAssessmentScale, updateAssessmentScale,
  assessmentScaleDetail, saveScaleQuestions, updateScaleEnabled, questionBankPage
} from '@/api/admin'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, keyword: '', category: '', enabled: null })

const categories = [
  { value: 'EMOTION', label: '情绪' },
  { value: 'PERSONALITY', label: '人格' },
  { value: 'STRESS', label: '压力' },
  { value: 'SLEEP', label: '睡眠' },
  { value: 'INTERPERSONAL', label: '人际关系' }
]
const levelOptions = [
  { value: 'NORMAL', label: '正常' },
  { value: 'MILD', label: '轻度' },
  { value: 'MODERATE', label: '中度' },
  { value: 'SEVERE', label: '重度' }
]

const fetchData = async () => {
  loading.value = true
  try {
    const res = await assessmentScalePage(query)
    list.value = res.records || []
    total.value = res.total || 0
  } finally { loading.value = false }
}
const onSearch = () => { query.page = 1; fetchData() }
const onReset = () => { query.keyword = ''; query.category = ''; query.enabled = null; query.page = 1; fetchData() }

// 基本信息编辑
const editVisible = ref(false)
const editId = ref(null)
const editForm = reactive({ code: '', name: '', category: 'EMOTION', description: '', guidance: '' })
const editFormRef = ref(null)
const editRules = {
  code: [{ required: true, message: '请输入编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  category: [{ required: true, message: '请选择类别', trigger: 'change' }]
}

const openCreate = () => { editId.value = null; Object.assign(editForm, { code: '', name: '', category: 'EMOTION', description: '', guidance: '' }); editVisible.value = true }
const openEdit = async (row) => {
  editId.value = row.id
  const res = await assessmentScaleDetail(row.id)
  Object.assign(editForm, { code: res.code, name: res.name, category: res.category, description: res.description || '', guidance: res.guidance || '' })
  editVisible.value = true
}
const onSaveScale = async () => {
  await editFormRef.value.validate()
  try {
    if (editId.value) {
      await updateAssessmentScale(editId.value, editForm)
      ElMessage.success('已更新')
    } else {
      await createAssessmentScale(editForm)
      ElMessage.success('已创建')
    }
    editVisible.value = false
    fetchData()
  } catch (e) {}
}

const toggleEnabled = async (row) => {
  const target = row.enabled === 1 ? 0 : 1
  await ElMessageBox.confirm(`确认${target === 1 ? '启用' : '停用'}量表「${row.name}」？`, '提示', { type: 'warning' })
  await updateScaleEnabled(row.id, target)
  ElMessage.success('操作成功')
  fetchData()
}

// 题目配置
const qVisible = ref(false)
const qScaleId = ref(null)
const questions = ref([])
const dimensions = ref([])
const templates = reactive({ NORMAL: '', MILD: '', MODERATE: '', SEVERE: '' })

const openQuestions = async (row) => {
  qScaleId.value = row.id
  const res = await assessmentScaleDetail(row.id)
  questions.value = (res.questions || []).map((q, i) => ({ ...q, index: i }))
  dimensions.value = (res.dimensions || []).map(d => ({ name: d.name, questionIndexes: d.questionIndexes || [] }))
  editForm.guidance = res.guidance || ''
  const tpl = res.interpretationTemplates || {}
  templates.NORMAL = tpl.NORMAL || ''
  templates.MILD = tpl.MILD || ''
  templates.MODERATE = tpl.MODERATE || ''
  templates.SEVERE = tpl.SEVERE || ''
  qVisible.value = true
}

const addSelfQuestion = () => { questions.value.push({ text: '', options: [{ label: '', score: 0 }, { label: '', score: 0 }] }) }
const moveQ = (i, dir) => {
  const j = i + dir
  if (j < 0 || j >= questions.value.length) return
  ;[questions.value[i], questions.value[j]] = [questions.value[j], questions.value[i]]
}
const delQ = (i) => questions.value.splice(i, 1)

// 题目编辑弹窗
const qEditVisible = ref(false)
const qEditIndex = ref(-1)
const qForm = reactive({ text: '', options: [] })
const editQ = (i) => {
  qEditIndex.value = i
  const q = questions.value[i]
  Object.assign(qForm, { text: q.text, options: JSON.parse(JSON.stringify(q.options || [])) })
  qEditVisible.value = true
}
const onSaveQ = () => {
  if (!qForm.text) { ElMessage.error('题干不能为空'); return }
  if (qForm.options.length < 2) { ElMessage.error('至少 2 个选项'); return }
  questions.value[qEditIndex.value] = { ...questions.value[qEditIndex.value], text: qForm.text, options: JSON.parse(JSON.stringify(qForm.options)) }
  qEditVisible.value = false
}

// 题库选择
const bankVisible = ref(false)
const bankList = ref([])
const bankKw = ref('')
const bankSelected = ref([])
const fetchBank = async () => {
  const res = await questionBankPage({ keyword: bankKw.value, size: 50 })
  bankList.value = res.records || []
}
const parseOpts = (j) => { try { return JSON.parse(j) } catch { return [] } }
const onBankSelect = (rows) => { bankSelected.value = rows }
const addFromBank = () => {
  bankSelected.value.forEach(q => {
    questions.value.push({ text: q.stem, options: parseOpts(q.options) })
  })
  bankVisible.value = false
  ElMessage.success(`已添加 ${bankSelected.value.length} 题`)
}

const onSaveQuestions = async () => {
  if (!questions.value.length) { ElMessage.error('至少需要 1 道题'); return }
  for (let i = 0; i < questions.value.length; i++) {
    const q = questions.value[i]
    if (!q.text || !q.text.trim()) { ElMessage.error(`第 ${i + 1} 题题干为空`); return }
    if (!q.options || q.options.length < 2) { ElMessage.error(`第 ${i + 1} 题至少 2 个选项`); return }
    for (const o of q.options) {
      if (!o.label || o.score == null) { ElMessage.error(`第 ${i + 1} 题选项不完整`); return }
    }
  }
  const payload = {
    questions: questions.value.map((q, i) => ({ index: i, text: q.text, options: q.options })),
    dimensions: dimensions.value.filter(d => d.name),
    guidance: editForm.guidance,
    interpretationTemplates: { ...templates }
  }
  await saveScaleQuestions(qScaleId.value, payload)
  ElMessage.success('已保存题目配置')
  qVisible.value = false
  fetchData()
}

onMounted(fetchData)
</script>

<style scoped>
.scales-page { padding: 20px; }
.search-bar { margin-bottom: 16px; }
.table-card { background: #fff; }
.pagination { margin-top: 16px; text-align: right; }
.opt-tag { margin-right: 4px; margin-bottom: 2px; }
.q-config { padding: 0 8px; }
.q-section { margin-bottom: 24px; }
.q-section-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; font-weight: 600; }
.dim-row { display: flex; align-items: center; margin-bottom: 8px; }
.tpl-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.tpl-item label { display: block; margin-bottom: 4px; font-size: 13px; color: #606266; }
.option-row { display: flex; align-items: center; margin-bottom: 8px; }
</style>
