<template>
  <div class="courses-page">
    <PageHead title="课程管理">
      <template #buttons>
        <el-button type="primary" @click="openCreate">新建课程</el-button>
      </template>
    </PageHead>

    <!-- 状态 Tab -->
    <div class="courses-tabs">
      <el-button
        v-for="t in statusTabs"
        :key="t.status"
        :type="activeStatus === t.status ? 'primary' : 'default'"
        :plain="activeStatus !== t.status"
        size="small"
        @click="onTabChange(t.status)"
      >
        {{ t.label }}
        <el-badge v-if="statusCount[t.status]" :value="statusCount[t.status]" :max="999" class="tab-badge" />
      </el-button>
    </div>

    <!-- 搜索栏 -->
    <el-form inline class="courses-search">
      <el-form-item label="标题/讲师">
        <el-input v-model="query.keyword" placeholder="标题或讲师" clearable style="width: 180px" />
      </el-form-item>
      <el-form-item label="分类">
        <el-select v-model="query.category" placeholder="全部" clearable style="width: 130px">
          <el-option v-for="c in categoryOptions" :key="c.value" :label="c.label" :value="c.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="onSearch">查询</el-button>
        <el-button @click="onReset">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 表格 -->
    <el-card shadow="never" class="courses-table-card">
      <el-table :data="courses" v-loading="loading" empty-text="暂无课程" style="width: 100%">
        <el-table-column label="封面" width="80">
          <template #default="{ row }">
            <el-image v-if="row.coverUrl" :src="row.coverUrl" class="cover-thumb" fit="cover" />
            <span v-else class="cover-empty">无</span>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <span>{{ row.title }}</span>
            <el-tooltip v-if="row.rejectReason && row.status === 3" :content="row.rejectReason" placement="top">
              <el-icon class="reject-icon"><WarningFilled /></el-icon>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column label="分类" width="100">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ row.categoryLabel || row.category }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lecturer" label="讲师" width="110">
          <template #default="{ row }">{{ row.lecturer || '—' }}</template>
        </el-table-column>
        <el-table-column prop="chapterCount" label="章节" width="70" />
        <el-table-column label="总时长" width="100">
          <template #default="{ row }">{{ row.totalDurationText || '—' }}</template>
        </el-table-column>
        <el-table-column prop="learnCount" label="学习人数" width="90" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small" effect="light">
              {{ row.statusLabel }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="280" class-name="col-action">
          <template #default="{ row }">
            <el-button text type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="row.status === 0 || row.status === 3" text type="warning" @click="onSubmitReview(row)">提交审核</el-button>
            <el-button v-if="row.status === 1" text type="success" @click="openReview(row)">审核</el-button>
            <el-button v-if="row.status === 2" text type="danger" @click="onOffline(row)">下架</el-button>
            <el-button v-if="row.status === 4" text type="success" @click="onOnline(row)">上架</el-button>
            <el-button text type="info" @click="openStats(row)">学习数据</el-button>
            <el-button text type="info" @click="openComments(row)">评论</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="courses-pager">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :page-sizes="[10, 20]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadCourses"
          @current-change="loadCourses"
        />
      </div>
    </el-card>

    <!-- 编辑抽屉（新建 + 编辑 + 章节管理） -->
    <el-drawer v-model="editVisible" :size="760" :with-header="false" :destroy-on-close="true">
      <div class="drawer-body" v-loading="editLoading">
        <div class="drawer-header">
          <h3>{{ editingCourse?.id ? '编辑课程' : '新建课程' }}</h3>
          <el-button text @click="editVisible = false">关闭</el-button>
        </div>

        <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="90px" class="edit-form">
          <el-form-item label="标题" prop="title">
            <el-input v-model="editForm.title" maxlength="200" show-word-limit />
          </el-form-item>
          <el-form-item label="分类" prop="category">
            <el-select v-model="editForm.category" placeholder="请选择分类" style="width: 100%">
              <el-option v-for="c in categoryOptions" :key="c.value" :label="c.label" :value="c.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="讲师">
            <el-input v-model="editForm.lecturer" maxlength="50" />
          </el-form-item>
          <el-form-item label="封面">
            <el-input v-model="editForm.coverUrl" placeholder="OSS 上传后的 URL" />
          </el-form-item>
          <el-form-item label="简介">
            <el-input v-model="editForm.description" type="textarea" :rows="3" />
          </el-form-item>
          <el-form-item label="适合人群">
            <el-input v-model="editForm.targetAudience" maxlength="200" />
          </el-form-item>
        </el-form>

        <!-- 章节管理（仅编辑模式） -->
        <div v-if="editingCourse?.id" class="chapter-section">
          <div class="chapter-header">
            <h4>章节管理</h4>
            <el-button type="primary" size="small" @click="openChapterForm()">添加章节</el-button>
          </div>
          <el-table :data="editingCourse.chapters" size="small" style="width: 100%">
            <el-table-column label="序号" width="50" type="index" />
            <el-table-column prop="title" label="标题" min-width="120" show-overflow-tooltip />
            <el-table-column label="时长" width="80">
              <template #default="{ row }">{{ row.durationText }}</template>
            </el-table-column>
            <el-table-column label="操作" width="260" class-name="col-action">
              <template #default="{ row, $index }">
                <div class="chapter-actions">
                  <el-button text size="small" :disabled="$index === 0" @click="moveChapter($index, -1)">上移</el-button>
                  <el-button text size="small" :disabled="$index === editingCourse.chapters.length - 1" @click="moveChapter($index, 1)">下移</el-button>
                  <el-button text size="small" type="primary" @click="openChapterForm(row)">编辑</el-button>
                  <el-popconfirm title="确认删除该章节？进度记录会一并清除" @confirm="onDeleteChapter(row)">
                    <template #reference>
                      <el-button text size="small" type="danger">删除</el-button>
                    </template>
                  </el-popconfirm>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div class="drawer-footer">
          <el-button @click="editVisible = false">取消</el-button>
          <el-button type="primary" :loading="saving" @click="onSaveCourse">保存</el-button>
        </div>
      </div>
    </el-drawer>

    <!-- 章节编辑弹窗 -->
    <el-dialog v-model="chapterFormVisible" :title="chapterForm.id ? '编辑章节' : '添加章节'" width="520px" :destroy-on-close="true">
      <el-form ref="chapterFormRef" :model="chapterForm" :rules="chapterRules" label-width="90px">
        <el-form-item label="章节标题" prop="title">
          <el-input v-model="chapterForm.title" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="时长(秒)" prop="duration">
          <el-input-number v-model="chapterForm.duration" :min="0" :max="86400" />
        </el-form-item>
        <el-form-item label="音频地址" prop="audioUrl">
          <el-input v-model="chapterForm.audioUrl" placeholder="OSS 音频 URL（与视频二选一）" />
        </el-form-item>
        <el-form-item label="视频地址" prop="videoUrl">
          <el-input v-model="chapterForm.videoUrl" placeholder="OSS 视频 URL（可选）" />
        </el-form-item>
        <el-form-item label="章节介绍">
          <el-input v-model="chapterForm.intro" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="chapterFormVisible = false">取消</el-button>
        <el-button type="primary" :loading="chapterSaving" @click="onSaveChapter">保存</el-button>
      </template>
    </el-dialog>

    <!-- 审核抽屉 -->
    <el-drawer v-model="reviewVisible" :size="600" :with-header="false" :destroy-on-close="true">
      <div class="drawer-body" v-loading="reviewLoading">
        <div class="drawer-header">
          <h3>课程审核</h3>
          <el-button text @click="reviewVisible = false">关闭</el-button>
        </div>
        <el-descriptions v-if="reviewCourseDetail" :column="1" border size="small" class="review-desc">
          <el-descriptions-item label="标题">{{ reviewCourseDetail.title }}</el-descriptions-item>
          <el-descriptions-item label="分类">{{ reviewCourseDetail.categoryLabel }}</el-descriptions-item>
          <el-descriptions-item label="讲师">{{ reviewCourseDetail.lecturer || '—' }}</el-descriptions-item>
          <el-descriptions-item label="适合人群">{{ reviewCourseDetail.targetAudience || '—' }}</el-descriptions-item>
          <el-descriptions-item label="简介">{{ reviewCourseDetail.description || '—' }}</el-descriptions-item>
          <el-descriptions-item label="总时长">{{ reviewCourseDetail.totalDurationText }}</el-descriptions-item>
        </el-descriptions>
        <h4 class="chapter-section-title">章节列表</h4>
        <el-table :data="reviewCourseDetail?.chapters || []" size="small">
          <el-table-column label="序号" width="55" type="index" />
          <el-table-column prop="title" label="标题" min-width="140" show-overflow-tooltip />
          <el-table-column label="时长" width="80">
            <template #default="{ row }">{{ row.durationText }}</template>
          </el-table-column>
        </el-table>
        <div class="drawer-footer review-footer">
          <el-popconfirm title="确认审核通过并发布？" @confirm="onApprove">
            <template #reference>
              <el-button type="success" :loading="reviewing">审核通过</el-button>
            </template>
          </el-popconfirm>
          <el-button type="danger" @click="rejectDialogVisible = true">驳回</el-button>
        </div>
      </div>
    </el-drawer>

    <!-- 驳回原因弹窗 -->
    <el-dialog v-model="rejectDialogVisible" title="驳回原因" width="480px">
      <el-input v-model="rejectReason" type="textarea" :rows="4" maxlength="500" show-word-limit placeholder="请填写驳回原因（必填）" />
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="reviewing" @click="onReject">确认驳回</el-button>
      </template>
    </el-dialog>

    <!-- 学习数据抽屉 -->
    <el-drawer v-model="statsVisible" :size="760" :with-header="false" :destroy-on-close="true">
      <div class="drawer-body" v-loading="statsLoading">
        <div class="drawer-header">
          <h3>学习数据</h3>
          <el-button text @click="statsVisible = false">关闭</el-button>
        </div>
        <el-row :gutter="16" class="stats-cards">
          <el-col :span="8">
            <el-card shadow="hover" class="stat-card">
              <div class="stat-value">{{ stats?.learnerCount ?? 0 }}</div>
              <div class="stat-label">学习人数</div>
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card shadow="hover" class="stat-card">
              <div class="stat-value">{{ stats?.completionRate ?? 0 }}%</div>
              <div class="stat-label">完成率</div>
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card shadow="hover" class="stat-card">
              <div class="stat-value">{{ formatSeconds(stats?.avgProgressSeconds) }}</div>
              <div class="stat-label">平均学习时长</div>
            </el-card>
          </el-col>
        </el-row>
        <ChartCard title="章节学习热力图" :loading="statsLoading" :is-empty="!stats?.chapterHeatmap?.length" :height="320">
          <div ref="heatmapEl" class="chart-fill"></div>
        </ChartCard>
      </div>
    </el-drawer>

    <!-- 评论管理抽屉 -->
    <el-drawer v-model="commentsVisible" :size="900" :with-header="false" :destroy-on-close="true">
      <div class="drawer-body">
        <div class="drawer-header">
          <h3>评论管理</h3>
          <el-button text @click="commentsVisible = false">关闭</el-button>
        </div>
        <el-form inline class="comments-search">
          <el-form-item label="内容">
            <el-input v-model="commentQuery.keyword" placeholder="评论内容关键字" clearable style="width: 200px" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="onSearchComments">查询</el-button>
          </el-form-item>
        </el-form>
        <el-table :data="comments" v-loading="commentsLoading" size="small" style="width: 100%">
          <el-table-column label="课程" min-width="160" show-overflow-tooltip>
            <template #default="{ row }">{{ row.courseTitle || '—' }}</template>
          </el-table-column>
          <el-table-column label="用户" width="120">
            <template #default="{ row }">
              <div class="comment-user">
                <el-avatar :size="24" :src="row.avatar">{{ (row.nickname || '?').charAt(0) }}</el-avatar>
                <span>{{ row.nickname || '—' }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="content" label="评论内容" min-width="200" show-overflow-tooltip />
          <el-table-column label="回复状态" width="100">
            <template #default="{ row }">
              <el-tag v-if="row.hasReply" type="success" size="small" effect="light">已回复</el-tag>
              <el-tag v-else type="info" size="small" effect="light">待回复</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="回复内容" min-width="200" show-overflow-tooltip>
            <template #default="{ row }">
              <span v-if="row.replyContent">{{ row.replyContent }}</span>
              <span v-else class="text-muted">—</span>
            </template>
          </el-table-column>
          <el-table-column label="时间" width="160">
            <template #default="{ row }">
              <div>{{ fmtTime(row.createdAt) }}</div>
              <div v-if="row.replyAt" class="text-muted">回复：{{ fmtTime(row.replyAt) }}</div>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180" class-name="col-action">
            <template #default="{ row }">
              <el-button text type="primary" @click="openReply(row)">{{ row.hasReply ? '修改回复' : '回复' }}</el-button>
              <el-popconfirm title="确认删除该评论？" @confirm="onDeleteComment(row)">
                <template #reference>
                  <el-button text type="danger">删除</el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
        <div class="courses-pager">
          <el-pagination
            v-model:current-page="commentQuery.page"
            :page-size="commentQuery.size"
            :total="commentTotal"
            layout="total, prev, pager, next"
            @current-change="loadComments"
          />
        </div>
      </div>
    </el-drawer>

    <!-- 回复评论弹窗 -->
    <el-dialog v-model="replyDialogVisible" title="回复评论" width="480px">
      <el-input v-model="replyContent" type="textarea" :rows="4" maxlength="500" show-word-limit placeholder="回复内容（必填，保存即覆盖原回复）" />
      <template #footer>
        <el-button @click="replyDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="replying" @click="onReply">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { WarningFilled } from '@element-plus/icons-vue'
import PageHead from '@/components/PageHead.vue'
import ChartCard from '@/components/ChartCard.vue'
import { horizBarChart } from '@/utils/echartsOptions'
import {
  getAdminCoursePage,
  getAdminCourseStatusCount,
  getAdminCourseDetail,
  createAdminCourse,
  updateAdminCourse,
  submitCourseReview,
  reviewCourse,
  updateCourseStatus,
  addCourseChapter,
  updateCourseChapter,
  deleteCourseChapter,
  sortCourseChapters,
  getCourseStats,
  getAdminCourseComments,
  replyCourseComment,
  deleteCourseComment
} from '@/api/admin'

// 状态枚举：0草稿/1待审核/2已发布/3已驳回/4已下架
const statusTabs = [
  { status: null, label: '全部' },
  { status: 0, label: '草稿' },
  { status: 1, label: '待审核' },
  { status: 2, label: '已发布' },
  { status: 3, label: '已驳回' },
  { status: 4, label: '已下架' }
]

const categoryOptions = [
  { value: 'MEDITATION', label: '正念冥想' },
  { value: 'SLEEP', label: '睡眠助眠' },
  { value: 'EMOTION', label: '情绪管理' },
  { value: 'CBT', label: '认知行为' },
  { value: 'INTERPERSONAL', label: '人际关系' }
]

const statusCount = reactive({})
const activeStatus = ref(null)
const query = reactive({ keyword: '', category: '', page: 1, size: 10 })
const courses = ref([])
const total = ref(0)
const loading = ref(false)

function statusTagType(status) {
  return { 0: 'info', 1: 'warning', 2: 'success', 3: 'danger', 4: 'info' }[status] || 'info'
}

function buildQueryParams() {
  const params = { page: query.page, size: query.size }
  if (query.keyword) params.keyword = query.keyword
  if (query.category) params.category = query.category
  if (activeStatus.value !== null) params.status = activeStatus.value
  return params
}

async function loadCourses() {
  loading.value = true
  try {
    const res = await getAdminCoursePage(buildQueryParams())
    courses.value = res?.records || []
    total.value = res?.total || 0
  } catch (e) {
    ElMessage.error('课程列表加载失败')
  } finally {
    loading.value = false
  }
}

async function loadStatusCount() {
  try {
    const res = await getAdminCourseStatusCount()
    Object.keys(res || {}).forEach(k => { statusCount[k] = res[k] })
  } catch (e) {
    // fail-silent，不影响列表展示
  }
}

function onTabChange(status) {
  activeStatus.value = status
  query.page = 1
  loadCourses()
}

function onSearch() { query.page = 1; loadCourses() }
function onReset() {
  query.keyword = ''
  query.category = ''
  query.page = 1
  loadCourses()
}

// ==================== 编辑/新建 ====================
const editVisible = ref(false)
const editLoading = ref(false)
const saving = ref(false)
const editingCourse = ref(null)
const editFormRef = ref(null)
const editForm = reactive({ title: '', category: '', lecturer: '', coverUrl: '', description: '', targetAudience: '' })
const editRules = {
  title: [{ required: true, message: '请输入课程标题', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }]
}

function openCreate() {
  editingCourse.value = null
  Object.assign(editForm, { title: '', category: '', lecturer: '', coverUrl: '', description: '', targetAudience: '' })
  editVisible.value = true
}

async function openEdit(row) {
  editVisible.value = true
  editLoading.value = true
  editingCourse.value = null
  try {
    const res = await getAdminCourseDetail(row.id)
    editingCourse.value = res
    Object.assign(editForm, {
      title: res.title || '',
      category: res.category || '',
      lecturer: res.lecturer || '',
      coverUrl: res.coverUrl || '',
      description: res.description || '',
      targetAudience: res.targetAudience || ''
    })
  } catch (e) {
    ElMessage.error('课程详情加载失败')
    editVisible.value = false
  } finally {
    editLoading.value = false
  }
}

async function onSaveCourse() {
  await editFormRef.value?.validate()
  saving.value = true
  try {
    if (editingCourse.value?.id) {
      await updateAdminCourse(editingCourse.value.id, { ...editForm })
      ElMessage.success('已保存')
      // 重新加载详情刷新章节列表
      const res = await getAdminCourseDetail(editingCourse.value.id)
      editingCourse.value = res
    } else {
      const id = await createAdminCourse({ ...editForm })
      ElMessage.success('已创建（草稿状态）')
      editVisible.value = false
    }
    loadCourses()
    loadStatusCount()
  } catch (e) {
    ElMessage.error(e?.response?.data?.msg || '保存失败')
  } finally {
    saving.value = false
  }
}

// ==================== 章节管理 ====================
const chapterFormVisible = ref(false)
const chapterSaving = ref(false)
const chapterFormRef = ref(null)
const chapterForm = reactive({ id: null, title: '', duration: 0, audioUrl: '', videoUrl: '', intro: '' })
const chapterRules = {
  title: [{ required: true, message: '请输入章节标题', trigger: 'blur' }],
  duration: [{ type: 'number', min: 0, message: '时长必须为非负数', trigger: 'blur' }]
}

function openChapterForm(chapter) {
  if (chapter) {
    Object.assign(chapterForm, {
      id: chapter.id,
      title: chapter.title,
      duration: chapter.duration ?? 0,
      audioUrl: chapter.audioUrl || '',
      videoUrl: chapter.videoUrl || '',
      intro: chapter.intro || ''
    })
  } else {
    Object.assign(chapterForm, { id: null, title: '', duration: 0, audioUrl: '', videoUrl: '', intro: '' })
  }
  chapterFormVisible.value = true
}

async function onSaveChapter() {
  await chapterFormRef.value?.validate()
  if (!chapterForm.audioUrl && !chapterForm.videoUrl) {
    ElMessage.error('音频或视频地址至少填写一项')
    return
  }
  chapterSaving.value = true
  const courseId = editingCourse.value?.id
  try {
    const payload = {
      title: chapterForm.title,
      duration: chapterForm.duration,
      audioUrl: chapterForm.audioUrl || undefined,
      videoUrl: chapterForm.videoUrl || undefined,
      intro: chapterForm.intro || undefined
    }
    if (chapterForm.id) {
      await updateCourseChapter(chapterForm.id, payload)
      ElMessage.success('章节已更新')
    } else {
      await addCourseChapter(courseId, payload)
      ElMessage.success('章节已添加')
    }
    chapterFormVisible.value = false
    // 重新加载详情
    const res = await getAdminCourseDetail(courseId)
    editingCourse.value = res
    loadCourses()
  } catch (e) {
    ElMessage.error(e?.response?.data?.msg || '保存失败')
  } finally {
    chapterSaving.value = false
  }
}

async function onDeleteChapter(chapter) {
  try {
    await deleteCourseChapter(chapter.id)
    ElMessage.success('章节已删除')
    const res = await getAdminCourseDetail(editingCourse.value.id)
    editingCourse.value = res
    loadCourses()
  } catch (e) {
    ElMessage.error(e?.response?.data?.msg || '删除失败')
  }
}

// 上下移（前端调整数组，单事务提交 sortCourseChapters）
async function moveChapter(index, dir) {
  const chapters = editingCourse.value.chapters
  const target = index + dir
  if (target < 0 || target >= chapters.length) return
  ;[chapters[index], chapters[target]] = [chapters[target], chapters[index]]
  // 重新赋值 sortOrder
  const items = chapters.map((c, i) => ({ id: c.id, sortOrder: i + 1 }))
  try {
    await sortCourseChapters({ items })
    // 刷新本地 sortOrder 显示
    chapters.forEach((c, i) => { c.sortOrder = i + 1 })
    ElMessage.success('排序已更新')
  } catch (e) {
    // 失败回滚到服务端顺序
    const res = await getAdminCourseDetail(editingCourse.value.id)
    editingCourse.value = res
    ElMessage.error('排序失败，已回滚')
  }
}

// ==================== 审核 ====================
const reviewVisible = ref(false)
const reviewLoading = ref(false)
const reviewing = ref(false)
const reviewCourseDetail = ref(null)
const rejectDialogVisible = ref(false)
const rejectReason = ref('')

async function openReview(row) {
  reviewVisible.value = true
  reviewLoading.value = true
  reviewCourseDetail.value = null
  try {
    const res = await getAdminCourseDetail(row.id)
    reviewCourseDetail.value = res
  } catch (e) {
    ElMessage.error('详情加载失败')
    reviewVisible.value = false
  } finally {
    reviewLoading.value = false
  }
}

async function onApprove() {
  reviewing.value = true
  try {
    await reviewCourse(reviewCourseDetail.value.id, { approve: true })
    ElMessage.success('已审核通过并发布')
    reviewVisible.value = false
    loadCourses()
    loadStatusCount()
  } catch (e) {
    ElMessage.error(e?.response?.data?.msg || '审核失败')
  } finally {
    reviewing.value = false
  }
}

async function onReject() {
  if (!rejectReason.value.trim()) {
    ElMessage.error('请填写驳回原因')
    return
  }
  reviewing.value = true
  try {
    await reviewCourse(reviewCourseDetail.value.id, { approve: false, rejectReason: rejectReason.value.trim() })
    ElMessage.success('已驳回')
    rejectDialogVisible.value = false
    reviewVisible.value = false
    rejectReason.value = ''
    loadCourses()
    loadStatusCount()
  } catch (e) {
    ElMessage.error(e?.response?.data?.msg || '驳回失败')
  } finally {
    reviewing.value = false
  }
}

async function onSubmitReview(row) {
  try {
    await submitCourseReview(row.id)
    ElMessage.success('已提交审核')
    loadCourses()
    loadStatusCount()
  } catch (e) {
    ElMessage.error(e?.response?.data?.msg || '提交失败')
  }
}

async function onOffline(row) {
  try {
    await updateCourseStatus(row.id, { status: 4 })
    ElMessage.success('已下架')
    loadCourses()
    loadStatusCount()
  } catch (e) {
    ElMessage.error(e?.response?.data?.msg || '下架失败')
  }
}

async function onOnline(row) {
  try {
    await updateCourseStatus(row.id, { status: 2 })
    ElMessage.success('已上架')
    loadCourses()
    loadStatusCount()
  } catch (e) {
    ElMessage.error(e?.response?.data?.msg || '上架失败')
  }
}

// ==================== 学习数据 ====================
const statsVisible = ref(false)
const statsLoading = ref(false)
const stats = ref(null)
const heatmapEl = ref(null)
let heatmapChart = null

async function openStats(row) {
  statsVisible.value = true
  statsLoading.value = true
  stats.value = null
  try {
    const res = await getCourseStats(row.id)
    stats.value = res
    await nextTick()
    renderHeatmap()
  } catch (e) {
    ElMessage.error('学习数据加载失败')
  } finally {
    statsLoading.value = false
  }
}

function renderHeatmap() {
  if (!heatmapEl.value || !stats.value?.chapterHeatmap?.length) return
  // 抽屉 destroy-on-close 会销毁 DOM，旧实例已失效，每次渲染前重新 init
  heatmapChart?.dispose()
  heatmapChart = echarts.init(heatmapEl.value)
  const items = stats.value.chapterHeatmap.map(i => ({
    name: i.title.length > 14 ? i.title.slice(0, 13) + '…' : i.title,
    value: i.learnerCount || 0
  }))
  heatmapChart.setOption(horizBarChart(items, '#409EFF'))
}

// ==================== 评论管理 ====================
const commentsVisible = ref(false)
const commentsLoading = ref(false)
const comments = ref([])
const commentTotal = ref(0)
const commentQuery = reactive({ courseId: null, keyword: '', page: 1, size: 10 })
const replyDialogVisible = ref(false)
const replyContent = ref('')
const replying = ref(false)
let currentComment = null

function openComments(row) {
  commentQuery.courseId = row.id
  commentQuery.keyword = ''
  commentQuery.page = 1
  commentsVisible.value = true
  loadComments()
}

async function loadComments() {
  commentsLoading.value = true
  try {
    const params = { page: commentQuery.page, size: commentQuery.size }
    if (commentQuery.courseId) params.courseId = commentQuery.courseId
    if (commentQuery.keyword) params.keyword = commentQuery.keyword
    const res = await getAdminCourseComments(params)
    comments.value = res?.records || []
    commentTotal.value = res?.total || 0
  } catch (e) {
    ElMessage.error('评论加载失败')
  } finally {
    commentsLoading.value = false
  }
}

function onSearchComments() { commentQuery.page = 1; loadComments() }

function openReply(comment) {
  currentComment = comment
  replyContent.value = comment.replyContent || ''
  replyDialogVisible.value = true
}

async function onReply() {
  if (!replyContent.value.trim()) {
    ElMessage.error('请输入回复内容')
    return
  }
  replying.value = true
  try {
    await replyCourseComment(currentComment.id, { content: replyContent.value.trim() })
    ElMessage.success('回复已保存')
    replyDialogVisible.value = false
    loadComments()
  } catch (e) {
    ElMessage.error(e?.response?.data?.msg || '回复失败')
  } finally {
    replying.value = false
  }
}

async function onDeleteComment(comment) {
  try {
    await deleteCourseComment(comment.id)
    ElMessage.success('评论已删除')
    loadComments()
  } catch (e) {
    ElMessage.error(e?.response?.data?.msg || '删除失败')
  }
}

// ==================== 通用工具 ====================
function fmtTime(t) {
  if (!t) return '—'
  return String(t).replace('T', ' ').slice(0, 19)
}

function formatSeconds(sec) {
  if (!sec || sec <= 0) return '0秒'
  const m = Math.floor(sec / 60)
  const s = sec % 60
  if (m <= 0) return s + '秒'
  return s === 0 ? m + '分' : m + '分' + s + '秒'
}

function onResize() { heatmapChart?.resize() }

onMounted(() => {
  loadCourses()
  loadStatusCount()
  window.addEventListener('resize', onResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  heatmapChart?.dispose()
})
</script>

<style lang="scss" scoped>
.courses-page {
  padding: 16px;
  background-color: #F7F8FA;
  min-height: 100%;
}

.courses-tabs {
  margin-bottom: 12px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;

  .tab-badge {
    margin-left: 4px;
  }
}

.courses-search {
  background: #FFFFFF;
  padding: 16px 16px 0;
  border: 1px solid #E5E7EB;
  border-radius: 4px;
  margin-bottom: 16px;
}

.courses-table-card {
  border: 1px solid #E5E7EB;
  border-radius: 4px;

  :deep(.el-card__body) {
    padding: 16px;
  }
}

.cover-thumb {
  width: 50px;
  height: 36px;
  border-radius: 4px;
  object-fit: cover;
}

.cover-empty {
  color: #9CA3AF;
  font-size: 12px;
}

.reject-icon {
  color: #F56C6C;
  margin-left: 4px;
  cursor: help;
  vertical-align: middle;
}

.courses-pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.drawer-body {
  padding: 16px 20px;

  .drawer-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 16px;

    h3 {
      margin: 0;
      font-size: 18px;
      color: #1F2937;
    }
  }

  .drawer-footer {
    margin-top: 24px;
    display: flex;
    justify-content: flex-end;
    gap: 8px;
  }
}

.edit-form {
  margin-bottom: 16px;
}

.chapter-section {
  border-top: 1px solid #E5E7EB;
  padding-top: 16px;
  margin-top: 8px;

  .chapter-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 12px;

    h4 {
      margin: 0;
      font-size: 15px;
      color: #1F2937;
    }
  }
}

.chapter-section-title {
  margin: 16px 0 8px;
  font-size: 14px;
  color: #1F2937;
}

.review-desc {
  margin-bottom: 16px;
}

.review-footer {
  justify-content: space-between;
}

.stats-cards {
  margin-bottom: 16px;

  .stat-card {
    text-align: center;

    .stat-value {
      font-size: 26px;
      font-weight: 600;
      color: #1F2937;
      line-height: 1.4;
    }

    .stat-label {
      font-size: 13px;
      color: #6B7280;
      margin-top: 4px;
    }
  }
}

.chart-fill {
  width: 100%;
  height: 100%;
  min-height: 280px;
}

.comments-search {
  margin-bottom: 12px;
  padding: 0;
}

.comment-user {
  display: flex;
  align-items: center;
  gap: 6px;
}

.text-muted {
  color: #9CA3AF;
  font-size: 12px;
}

:deep(.col-action .cell) {
  white-space: nowrap;
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: nowrap;
}

/* 章节操作列：强制四按钮单行不换行，清零 Element Plus 默认兄弟 margin */
.chapter-actions {
  display: flex;
  align-items: center;
  flex-wrap: nowrap;
  gap: 8px;
  white-space: nowrap;
}

.chapter-actions :deep(.el-button) {
  margin-left: 0 !important;
  padding: 4px 6px;
}
</style>
