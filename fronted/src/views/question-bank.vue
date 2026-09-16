<template>
  <div class="question-bank-page">
    <PageHead title="题目库">
      <template #buttons>
        <el-button type="primary" @click="openEdit()">新增题目</el-button>
      </template>
    </PageHead>

    <el-form inline class="search-bar">
      <el-form-item label="题干">
        <el-input v-model="query.keyword" placeholder="题干关键字" clearable style="width: 200px" @keyup.enter="onSearch" />
      </el-form-item>
      <el-form-item label="题型">
        <el-select v-model="query.type" placeholder="全部" clearable style="width: 130px">
          <el-option label="单选" value="SINGLE" />
          <el-option label="多选" value="MULTIPLE" />
          <el-option label="量表题" value="SCALE" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="onSearch">查询</el-button>
        <el-button @click="onReset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-card shadow="never" class="table-card">
      <el-table :data="list" v-loading="loading" empty-text="暂无题目" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="stem" label="题干" min-width="240" show-overflow-tooltip />
        <el-table-column label="题型" width="100">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ typeLabel(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="选项" min-width="200">
          <template #default="{ row }">
            <div class="options-preview">
              <el-tag v-for="(o, i) in parseOptions(row.options)" :key="i" size="small" class="opt-tag">
                {{ o.label }}({{ o.score }})
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="dimensionTag" label="维度标签" width="120">
          <template #default="{ row }">{{ row.dimensionTag || '—' }}</template>
        </el-table-column>
        <el-table-column prop="category" label="分类" width="120">
          <template #default="{ row }">{{ row.category || '—' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button text type="danger" @click="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        class="pagination"
        @size-change="fetchData"
        @current-change="fetchData"
      />
    </el-card>

    <!-- 新增/编辑题目弹窗 -->
    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑题目' : '新增题目'" width="600px" destroy-on-close>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="90px">
        <el-form-item label="题型" prop="type">
          <el-select v-model="form.type" style="width: 100%">
            <el-option label="单选" value="SINGLE" />
            <el-option label="多选" value="MULTIPLE" />
            <el-option label="量表题" value="SCALE" />
          </el-select>
        </el-form-item>
        <el-form-item label="题干" prop="stem">
          <el-input v-model="form.stem" type="textarea" :rows="2" placeholder="请输入题干" />
        </el-form-item>
        <el-form-item label="选项">
          <div class="options-editor">
            <div v-for="(opt, i) in form.options" :key="i" class="option-row">
              <el-input v-model="opt.label" placeholder="选项文本" style="width: 280px" />
              <el-input-number v-model="opt.score" :min="0" placeholder="分值" style="width: 120px; margin-left: 8px" />
              <el-button text type="danger" @click="removeOption(i)">删除</el-button>
            </div>
            <el-button size="small" @click="addOption">+ 添加选项</el-button>
          </div>
        </el-form-item>
        <el-form-item label="维度标签">
          <el-input v-model="form.dimensionTag" placeholder="可选，默认维度标签" />
        </el-form-item>
        <el-form-item label="分类">
          <el-input v-model="form.category" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="onSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHead from '@/components/PageHead.vue'
import { questionBankPage, createQuestion, updateQuestion, deleteQuestion } from '@/api/admin'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, keyword: '', type: '' })

const dialogVisible = ref(false)
const editingId = ref(null)
const formRef = ref(null)
const form = reactive({ type: 'SINGLE', stem: '', options: [{ label: '', score: 0 }, { label: '', score: 0 }], dimensionTag: '', category: '' })
const rules = {
  type: [{ required: true, message: '请选择题型', trigger: 'change' }],
  stem: [{ required: true, message: '请输入题干', trigger: 'blur' }]
}

const typeLabel = (t) => ({ SINGLE: '单选', MULTIPLE: '多选', SCALE: '量表题' }[t] || t)

const parseOptions = (json) => {
  try { return JSON.parse(json) } catch { return [] }
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await questionBankPage(query)
    list.value = res.records || []
    total.value = res.total || 0
  } finally { loading.value = false }
}

const onSearch = () => { query.page = 1; fetchData() }
const onReset = () => { query.keyword = ''; query.type = ''; query.page = 1; fetchData() }

const openEdit = (row) => {
  editingId.value = row ? row.id : null
  if (row) {
    form.type = row.type
    form.stem = row.stem
    form.options = parseOptions(row.options)
    form.dimensionTag = row.dimensionTag || ''
    form.category = row.category || ''
  } else {
    Object.assign(form, { type: 'SINGLE', stem: '', options: [{ label: '', score: 0 }, { label: '', score: 0 }], dimensionTag: '', category: '' })
  }
  dialogVisible.value = true
}

const addOption = () => form.options.push({ label: '', score: 0 })
const removeOption = (i) => {
  if (form.options.length <= 2) { ElMessage.warning('至少保留 2 个选项'); return }
  form.options.splice(i, 1)
}

const onSave = async () => {
  await formRef.value.validate()
  if (form.options.length < 2) { ElMessage.error('至少需要 2 个选项'); return }
  const opts = form.options.filter(o => o.label && o.label.trim())
  if (opts.length < 2) { ElMessage.error('选项文本不能为空'); return }
  const payload = { ...form, options: JSON.stringify(opts) }
  try {
    if (editingId.value) {
      await updateQuestion(editingId.value, payload)
      ElMessage.success('已更新')
    } else {
      await createQuestion(payload)
      ElMessage.success('已创建')
    }
    dialogVisible.value = false
    fetchData()
  } catch (e) {}
}

const onDelete = async (row) => {
  await ElMessageBox.confirm(`确认删除题目「${row.stem.slice(0, 20)}…」？题库题删除不影响已建量表`, '提示', { type: 'warning' })
  await deleteQuestion(row.id)
  ElMessage.success('已删除')
  fetchData()
}

onMounted(fetchData)
</script>

<style scoped>
.question-bank-page { padding: 20px; }
.search-bar { margin-bottom: 16px; }
.table-card { background: #fff; }
.pagination { margin-top: 16px; text-align: right; }
.options-preview { display: flex; flex-wrap: wrap; gap: 4px; }
.opt-tag { margin-right: 0; }
.options-editor .option-row { display: flex; align-items: center; margin-bottom: 8px; }
</style>
