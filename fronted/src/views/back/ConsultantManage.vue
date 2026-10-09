<template>
  <div class="consultants-page">
    <PageHead title="咨询师管理">
      <template #buttons>
        <el-button type="primary" @click="openCreate">新增咨询师</el-button>
      </template>
    </PageHead>

    <!-- 状态 Tab + 搜索 -->
    <el-card shadow="never" class="filter-card">
      <el-tabs v-model="activeStatus" @tab-change="onStatusChange">
        <el-tab-pane v-for="t in STATUS_TABS" :key="t.name" :name="t.name">
          <template #label>
            <span class="status-tab-label">
              {{ t.label }}
              <span class="tab-count" :class="{ 'tab-count-pending': t.name === '0' }">
                {{ counts[t.countKey] ?? 0 }}
              </span>
            </span>
          </template>
        </el-tab-pane>
      </el-tabs>
      <el-form inline class="search-form">
        <el-form-item label="关键字">
          <el-input v-model="keyword" placeholder="姓名 / 擅长领域" clearable style="width: 220px"
                    @keyup.enter="onSearch" @clear="onSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSearch">查询</el-button>
          <el-button @click="onReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 列表 -->
    <el-card shadow="never" class="table-card">
      <el-table :data="tableData" v-loading="loading" style="width: 100%"
                :empty-text="activeStatus === '0' ? '暂无待审核咨询师，新增咨询师后将在此等待审核' : '暂无咨询师'">
        <el-table-column label="咨询师" min-width="170">
          <template #default="{ row }">
            <div class="consultant-cell">
              <el-avatar :size="40" :src="row.avatar">{{ (row.name || '?').charAt(0) }}</el-avatar>
              <span class="consultant-name">{{ row.name }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="职称" width="130">
          <template #default="{ row }">{{ row.title || '—' }}</template>
        </el-table-column>
        <el-table-column label="擅长领域" min-width="200">
          <template #default="{ row }">
            <el-tag v-for="s in splitSpecialties(row.specialties)" :key="s" size="small"
                    type="warning" effect="plain" round class="specialty-tag">
              {{ s }}
            </el-tag>
            <span v-if="!splitSpecialties(row.specialties).length">—</span>
          </template>
        </el-table-column>
        <el-table-column prop="yearsOfPractice" label="从业年限" width="90" />
        <el-table-column label="评分" width="110">
          <template #default="{ row }">
            <el-rate :model-value="Number(row.rating || 0)" disabled size="small" :max="5"
                    show-score score-template="{value}" />
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tooltip v-if="row.status === 0 && row.rejectReason" :content="row.rejectReason"
                        placement="top" :show-after="100">
              <el-tag type="danger" effect="dark" size="small">已驳回</el-tag>
            </el-tooltip>
            <el-tag v-else :type="statusTagType(row.status)" size="small" effect="light">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="入驻时间" width="170">
          <template #default="{ row }">{{ fmtTime(row.joinedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" min-width="280" fixed="right" class-name="col-action">
          <template #default="{ row }">
            <el-button text type="primary" @click="openDetail(row)">详情</el-button>
            <el-button v-if="row.status === 0" text type="warning" @click="openReview(row)">审核</el-button>
            <el-button text type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button text type="success" @click="openSchedule(row)">排班</el-button>
            <el-popconfirm v-if="row.status === 1" title="禁用后用户端将不可见、不可新约，确认禁用？"
                           @confirm="onToggleStatus(row, 2)">
              <template #reference>
                <el-button text type="danger">禁用</el-button>
              </template>
            </el-popconfirm>
            <el-popconfirm v-else-if="row.status === 2" title="确认恢复该咨询师？恢复后用户端可见。"
                           @confirm="onToggleStatus(row, 1)">
              <template #reference>
                <el-button text type="success">启用</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="page"
          :page-size="pageSize"
          :page-sizes="[10, 20]"
          :total="total"
          layout="total, sizes, prev, pager, next"
          @current-change="loadList"
          @size-change="onSizeChange"
        />
      </div>
    </el-card>

    <!-- ============ 审核抽屉 ============ -->
    <el-drawer v-model="reviewVisible" size="720px" :title="`资质审核 · ${reviewInfo?.name || ''}`" destroy-on-close>
      <div v-loading="reviewLoading" class="drawer-body">
        <template v-if="reviewInfo">
          <div class="review-alert" v-if="reviewInfo.rejectReason">
            该咨询师为「已驳回待修改」状态，上次驳回原因：{{ reviewInfo.rejectReason }}
          </div>
          <el-descriptions :column="2" border size="small" class="info-desc">
            <el-descriptions-item label="姓名">{{ reviewInfo.name }}</el-descriptions-item>
            <el-descriptions-item label="职称">{{ reviewInfo.title || '—' }}</el-descriptions-item>
            <el-descriptions-item label="从业年限">{{ reviewInfo.yearsOfPractice }} 年</el-descriptions-item>
            <el-descriptions-item label="价格">{{ formatPrice(reviewInfo.price) }}</el-descriptions-item>
            <el-descriptions-item label="擅长领域" :span="2">
              <el-tag v-for="s in splitSpecialties(reviewInfo.specialties)" :key="s" size="small"
                      type="warning" effect="plain" round class="specialty-tag">{{ s }}</el-tag>
              <span v-if="!splitSpecialties(reviewInfo.specialties).length">—</span>
            </el-descriptions-item>
          </el-descriptions>

          <div class="section-title">个人简介</div>
          <div class="intro-box">{{ reviewInfo.intro || '暂未填写简介' }}</div>

          <div class="section-title">资质证明
            <span class="section-hint">（{{ (reviewInfo.qualificationImages || []).length }} 张，点击可预览大图）</span>
          </div>
          <div v-if="(reviewInfo.qualificationImages || []).length" class="image-wall">
            <el-image v-for="(url, i) in reviewInfo.qualificationImages" :key="i"
                      :src="url" :preview-src-list="reviewInfo.qualificationImages"
                      :initial-index="i" fit="cover" class="wall-image"
                      preview-teleported hide-on-click-modal />
          </div>
          <el-empty v-else description="未上传资质证明" :image-size="70" />
        </template>
      </div>
      <template #footer>
        <div class="drawer-footer">
          <el-button type="danger" plain @click="openReject">驳回</el-button>
          <el-popconfirm title="确认审核通过？通过后该咨询师将在用户端可见。" @confirm="onApprove">
            <template #reference>
              <el-button type="primary">审核通过</el-button>
            </template>
          </el-popconfirm>
        </div>
      </template>
    </el-drawer>

    <!-- 驳回原因弹窗 -->
    <el-dialog v-model="rejectVisible" title="驳回审核" width="480px" :close-on-click-modal="false" append-to-body>
      <el-input v-model="rejectReason" type="textarea" :rows="4" maxlength="500" show-word-limit
                placeholder="请填写驳回原因（必填，咨询师修改后可重新提交）" />
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="danger" :loading="rejectSaving" @click="onReject">确认驳回</el-button>
      </template>
    </el-dialog>

    <!-- ============ 详情抽屉 ============ -->
    <el-drawer v-model="detailVisible" size="720px" :title="`咨询师详情 · ${detailInfo?.name || ''}`" destroy-on-close>
      <div v-loading="detailLoading" class="drawer-body">
        <template v-if="detailInfo">
          <div class="detail-header">
            <el-avatar :size="52" :src="detailInfo.avatar">{{ (detailInfo.name || '?').charAt(0) }}</el-avatar>
            <div class="detail-header-info">
              <div class="detail-name">
                {{ detailInfo.name }}
                <el-tag :type="statusTagType(detailInfo.status)" size="small" effect="light">
                  {{ detailInfo.status === 0 && detailInfo.rejectReason ? '已驳回' : statusLabel(detailInfo.status) }}
                </el-tag>
              </div>
              <div class="detail-sub">{{ detailInfo.title || '—' }} · 从业 {{ detailInfo.yearsOfPractice }} 年</div>
            </div>
          </div>

          <el-descriptions :column="2" border size="small" class="info-desc">
            <el-descriptions-item label="擅长领域" :span="2">
              <el-tag v-for="s in splitSpecialties(detailInfo.specialties)" :key="s" size="small"
                      type="warning" effect="plain" round class="specialty-tag">{{ s }}</el-tag>
              <span v-if="!splitSpecialties(detailInfo.specialties).length">—</span>
            </el-descriptions-item>
            <el-descriptions-item label="入驻时间">{{ fmtTime(detailInfo.joinedAt) }}</el-descriptions-item>
            <el-descriptions-item label="审核时间">{{ fmtTime(detailInfo.reviewedAt) }}</el-descriptions-item>
            <el-descriptions-item label="排序权重">{{ detailInfo.sortWeight ?? 0 }}</el-descriptions-item>
            <el-descriptions-item label="价格">{{ formatPrice(detailInfo.price) }}</el-descriptions-item>
            <el-descriptions-item label="简介" :span="2">{{ detailInfo.intro || '暂未填写简介' }}</el-descriptions-item>
          </el-descriptions>

          <el-tabs v-model="activeDetailTab" @tab-change="onDetailTabChange" class="detail-tabs">
            <el-tab-pane label="资质文件" name="qual">
              <div v-if="(detailInfo.qualificationImages || []).length" class="image-wall">
                <el-image v-for="(url, i) in detailInfo.qualificationImages" :key="i"
                          :src="url" :preview-src-list="detailInfo.qualificationImages"
                          :initial-index="i" fit="cover" class="wall-image"
                          preview-teleported hide-on-click-modal />
              </div>
              <el-empty v-else description="未上传资质证明" :image-size="80" />
            </el-tab-pane>

            <el-tab-pane label="咨询记录" name="appt">
              <el-table :data="apptData.records" v-loading="apptData.loading" size="small" style="width: 100%">
                <el-table-column label="咨询时间" min-width="150">
                  <template #default="{ row }">
                    {{ row.appointmentDate }} {{ row.startTime }}-{{ row.endTime }}
                  </template>
                </el-table-column>
                <el-table-column prop="userNickname" label="用户" width="100" />
                <el-table-column label="状态" width="90">
                  <template #default="{ row }">
                    <el-tag size="small" :type="apptStatusType(row.status)">{{ row.statusLabel }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="评价" width="80">
                  <template #default="{ row }">
                    <el-tag size="small" :type="row.evaluated ? 'success' : 'info'" effect="plain">
                      {{ row.evaluated ? '已评价' : '未评价' }}
                    </el-tag>
                  </template>
                </el-table-column>
              </el-table>
              <div class="tab-pager">
                <el-pagination v-model:current-page="apptData.page" :page-size="10" :total="apptData.total"
                               layout="total, prev, pager, next" small @current-change="loadAppointments" />
              </div>
            </el-tab-pane>

            <el-tab-pane label="评价列表" name="review">
              <div v-loading="reviewTabData.loading" class="review-list">
                <div v-for="r in reviewTabData.records" :key="r.id" class="review-item">
                  <div class="review-item-head">
                    <el-rate :model-value="r.rating" disabled size="small" />
                    <span class="review-item-time">{{ fmtTime(r.createdAt) }}</span>
                  </div>
                  <div class="review-item-content">{{ r.content || '（用户未填写文字评价）' }}</div>
                </div>
                <el-empty v-if="!reviewTabData.records.length && !reviewTabData.loading"
                          description="暂无评价" :image-size="80" />
              </div>
              <div class="tab-pager">
                <el-pagination v-model:current-page="reviewTabData.page" :page-size="10"
                               :total="reviewTabData.total" layout="total, prev, pager, next" small
                               @current-change="loadReviewTab" />
              </div>
            </el-tab-pane>

            <el-tab-pane label="收入统计" name="income">
              <div v-loading="incomeLoading" class="income-grid">
                <div class="income-card">
                  <div class="income-value">{{ income?.totalCompleted ?? 0 }}</div>
                  <div class="income-label">累计完成（次）</div>
                </div>
                <div class="income-card">
                  <div class="income-value">{{ income?.monthCompleted ?? 0 }}</div>
                  <div class="income-label">近30天完成（次）</div>
                </div>
                <div class="income-card">
                  <div class="income-value">{{ Number(income?.avgRating || 0).toFixed(1) }}</div>
                  <div class="income-label">平均评分</div>
                </div>
                <div class="income-card">
                  <div class="income-value" :class="{ 'income-unset': income && income.totalIncome == null }">
                    {{ income && income.totalIncome != null ? `¥${income.totalIncome}` : '未设置' }}
                  </div>
                  <div class="income-label">估算总收入（完成数 × 单价）</div>
                </div>
              </div>
            </el-tab-pane>
          </el-tabs>
        </template>
      </div>
    </el-drawer>

    <!-- ============ 新增/编辑弹窗 ============ -->
    <el-dialog v-model="formVisible" :title="editingId ? '编辑咨询师' : '新增咨询师'"
               width="680px" top="5vh" :close-on-click-modal="false">
      <el-form label-position="top" @submit.prevent>
        <div class="form-grid">
          <el-form-item label="头像" class="form-col-avatar">
            <el-upload class="avatar-uploader" :show-file-list="false" accept="image/*"
                       :before-upload="beforeImageUpload" :http-request="handleAvatarUpload">
              <el-avatar v-if="form.avatar" :src="form.avatar" :size="64" />
              <div v-else class="avatar-add"><el-icon><Plus /></el-icon><span>上传头像</span></div>
            </el-upload>
          </el-form-item>
          <div class="form-col-main">
            <el-form-item label="姓名" required>
              <el-input v-model="form.name" maxlength="50" placeholder="请输入姓名" />
            </el-form-item>
            <el-form-item label="职称">
              <el-input v-model="form.title" maxlength="50" placeholder="如：心理咨询师 / 心理治疗师 / 专家" />
            </el-form-item>
          </div>
        </div>
        <el-form-item label="擅长领域（可多选或自定义输入）">
          <el-select v-model="specialtyList" multiple filterable allow-create default-first-option
                     placeholder="选择或输入擅长领域" style="width: 100%">
            <el-option v-for="s in SPECIALTY_OPTIONS" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <div class="form-grid">
          <el-form-item label="从业年限" required>
            <el-input-number v-model="form.yearsOfPractice" :min="0" :max="60" style="width: 100%" />
          </el-form-item>
          <el-form-item label="排序权重（越大越靠前）">
            <el-input-number v-model="form.sortWeight" :min="0" :max="999999" style="width: 100%" />
          </el-form-item>
          <el-form-item label="咨询价格（元/小时，付费预留，留空=未设置）">
            <el-input-number v-model="form.price" :min="0" :precision="2" :value-on-clear="null"
                             controls-position="right" placeholder="未设置" style="width: 100%" />
          </el-form-item>
        </div>
        <el-form-item v-if="!editingId" label="登录用户名（选填，留空自动生成；3-50 位字母/数字/下划线）">
          <el-input v-model="form.username" maxlength="50"
                    placeholder="如 doc_zhangsan，留空则系统自动生成登录账号" />
        </el-form-item>
        <el-form-item label="个人简介">
          <el-input v-model="form.intro" type="textarea" :rows="3" maxlength="1000" show-word-limit
                    placeholder="请输入个人简介（选填，1000 字以内）" />
        </el-form-item>
        <el-form-item :label="`资质证明图片（最多 9 张）`">
          <el-upload list-type="picture-card" :file-list="qualFileList" accept="image/*" :limit="9"
                     :before-upload="beforeImageUpload" :http-request="handleQualUpload"
                     :on-remove="handleQualRemove"
                     :on-exceed="() => ElMessage.warning('资质图片最多上传 9 张')">
            <div class="qual-trigger"><el-icon><Plus /></el-icon><span>上传资质</span></div>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- ============ 排班周视图抽屉 ============ -->
    <el-drawer v-model="schedVisible" size="1040px" :title="`排班管理 · ${schedConsultant.name || ''}`" destroy-on-close>
      <div class="drawer-body">
        <div class="sched-toolbar">
          <el-button-group>
            <el-button @click="shiftWeek(-1)">上一周</el-button>
            <el-button @click="goCurrentWeek">本周</el-button>
            <el-button @click="shiftWeek(1)">下一周</el-button>
          </el-button-group>
          <el-date-picker v-model="weekPickerValue" type="week" format="YYYY 年第 ww 周"
                          value-format="YYYY-MM-DD" :clearable="false" placeholder="选择周"
                          style="width: 180px" @change="onWeekPickerChange" />
          <div class="sched-toolbar-right">
            <el-button @click="openDayAdd">按日添加</el-button>
            <el-button type="primary" @click="openTplAdd">本周模板批量生成</el-button>
          </div>
        </div>

        <div class="sched-legend">
          <span class="legend-item"><i class="legend-dot dot-free"></i>可约（点击停用）</span>
          <span class="legend-item"><i class="legend-dot dot-booked"></i>已约（不可操作）</span>
          <span class="legend-item"><i class="legend-dot dot-disabled"></i>停用（点击恢复）</span>
          <span class="legend-item"><i class="legend-dot dot-empty"></i>空格（点击添加时段）</span>
        </div>

        <div v-loading="schedLoading" class="sched-grid">
          <div class="grid-head grid-time-head">时间</div>
          <div v-for="(d, i) in weekDays" :key="d" class="grid-head" :class="{ 'head-past': isPast(d) }">
            <div>{{ WEEK_NAMES[i] }}</div>
            <div class="head-date">{{ d.slice(5) }}</div>
          </div>

          <template v-for="row in timeRows" :key="row.start">
            <div class="grid-time">{{ row.start }}<br />{{ row.end }}</div>
            <div v-for="d in weekDays" :key="d + row.start"
                 class="grid-cell"
                 :class="[cellClass(cell(d, row)), { 'cell-past': isPast(d) }]"
                 @click="onCellClick(d, row)">
              <span v-if="cell(d, row)">{{ cellStatusText(cell(d, row).status) }}</span>
              <span v-else class="cell-plus">+</span>
            </div>
          </template>
          <el-empty v-if="!timeRows.length && !schedLoading" description="本周暂无排班，可使用「按日添加」或「本周模板批量生成」"
                    :image-size="70" class="grid-empty" />
        </div>
      </div>
    </el-drawer>

    <!-- 周模板批量生成 -->
    <el-dialog v-model="tplVisible" title="本周模板批量生成" width="560px" :close-on-click-modal="false" append-to-body>
      <div class="slot-tip">设置一天的时段模板，将应用到本周一至周日（已存在的时段自动跳过）。</div>
      <div v-for="(item, idx) in tplRows" :key="idx" class="slot-row">
        <el-time-picker v-model="item.startTime" value-format="HH:mm:ss" format="HH:mm"
                        placeholder="开始时间" style="width: 150px" />
        <span>-</span>
        <el-time-picker v-model="item.endTime" value-format="HH:mm:ss" format="HH:mm"
                        placeholder="结束时间" style="width: 150px" />
        <el-button text type="danger" @click="tplRows.splice(idx, 1)">删除</el-button>
      </div>
      <el-button text type="primary" @click="tplRows.push({ startTime: '', endTime: '' })">+ 添加时段</el-button>
      <template #footer>
        <el-button @click="tplVisible = false">取消</el-button>
        <el-button type="primary" :loading="tplSaving" @click="submitTpl">应用到整周</el-button>
      </template>
    </el-dialog>

    <!-- 按日添加 -->
    <el-dialog v-model="dayVisible" title="按日添加时段" width="560px" :close-on-click-modal="false" append-to-body>
      <el-form label-position="top">
        <el-form-item label="选择日期">
          <el-date-picker v-model="dayDate" type="date" value-format="YYYY-MM-DD"
                          :disabled-date="disablePastDate" placeholder="选择日期" style="width: 220px" />
        </el-form-item>
      </el-form>
      <div v-for="(item, idx) in dayRows" :key="idx" class="slot-row">
        <el-time-picker v-model="item.startTime" value-format="HH:mm:ss" format="HH:mm"
                        placeholder="开始时间" style="width: 150px" />
        <span>-</span>
        <el-time-picker v-model="item.endTime" value-format="HH:mm:ss" format="HH:mm"
                        placeholder="结束时间" style="width: 150px" />
        <el-button text type="danger" @click="dayRows.splice(idx, 1)">删除</el-button>
      </div>
      <el-button text type="primary" @click="dayRows.push({ startTime: '', endTime: '' })">+ 添加时段</el-button>
      <template #footer>
        <el-button @click="dayVisible = false">取消</el-button>
        <el-button type="primary" :loading="daySaving" @click="submitDay">生成时段</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import PageHead from '@/components/PageHead.vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
    getAdminConsultantPage, getConsultantStatusCounts, getAdminConsultantDetail,
    reviewConsultant, getConsultantAppointments, getAdminConsultantReviews, getConsultantIncome,
    createConsultant, updateConsultant, changeConsultantStatus,
    createConsultantSlots, getConsultantWeekSlots, updateSlotStatus, uploadConsultantImage
} from '@/api/consultant'

const SPECIALTY_OPTIONS = ['情绪管理', '人际关系', '学业压力', '睡眠问题', '职业发展', '亲子关系']

// name 即接口 status 参数：all=全部，0/1/2 为状态码
const STATUS_TABS = [
    { name: 'all', label: '全部', countKey: 'total' },
    { name: '0', label: '待审核', countKey: '0' },
    { name: '1', label: '正常', countKey: '1' },
    { name: '2', label: '禁用', countKey: '2' }
]
const STATUS_META = { 0: ['待审核', 'warning'], 1: ['正常', 'success'], 2: ['禁用', 'danger'] }
const WEEK_NAMES = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']

// ==================== 列表 ====================
const tableData = ref([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const keyword = ref('')
const activeStatus = ref('all')
const counts = reactive({ total: 0, 0: 0, 1: 0, 2: 0 })

const loadList = () => {
    loading.value = true
    const params = { page: page.value, size: pageSize.value }
    if (keyword.value) params.keyword = keyword.value
    if (activeStatus.value !== 'all') params.status = Number(activeStatus.value)
    getAdminConsultantPage(params).then(data => {
        tableData.value = data.records || []
        total.value = data.total || 0
    }).finally(() => {
        loading.value = false
    })
}

const loadCounts = () => {
    getConsultantStatusCounts().then(data => {
        counts.total = data.total || 0
        counts[0] = Number(data[0] || 0)
        counts[1] = Number(data[1] || 0)
        counts[2] = Number(data[2] || 0)
    }).catch(() => {})
}

const onStatusChange = () => {
    page.value = 1
    loadList()
}
const onSearch = () => {
    page.value = 1
    loadList()
}
const onReset = () => {
    keyword.value = ''
    activeStatus.value = 'all'
    page.value = 1
    loadList()
}
const onSizeChange = (size) => {
    pageSize.value = size
    page.value = 1
    loadList()
}

const onToggleStatus = (row, status) => {
    changeConsultantStatus(row.id, status).then(() => {
        ElMessage.success(status === 1 ? '已恢复正常' : '已禁用')
        loadList()
        loadCounts()
    }).catch(() => {})
}

// ==================== 审核 ====================
const reviewVisible = ref(false)
const reviewLoading = ref(false)
const reviewInfo = ref(null)
const reviewId = ref(null)

const rejectVisible = ref(false)
const rejectReason = ref('')
const rejectSaving = ref(false)

const openReview = (row) => {
    reviewId.value = row.id
    reviewInfo.value = null
    reviewVisible.value = true
    reviewLoading.value = true
    getAdminConsultantDetail(row.id).then(data => {
        reviewInfo.value = data
    }).finally(() => {
        reviewLoading.value = false
    })
}

const onApprove = () => {
    reviewConsultant(reviewId.value, { approve: true }).then(() => {
        ElMessage.success('已审核通过')
        reviewVisible.value = false
        loadList()
        loadCounts()
    }).catch(() => {})
}

const openReject = () => {
    rejectReason.value = ''
    rejectVisible.value = true
}

const onReject = () => {
    const reason = rejectReason.value.trim()
    if (!reason) {
        ElMessage.warning('请填写驳回原因')
        return
    }
    rejectSaving.value = true
    reviewConsultant(reviewId.value, { approve: false, rejectReason: reason })
        .then(() => {
            ElMessage.success('已驳回')
            rejectVisible.value = false
            reviewVisible.value = false
            loadList()
            loadCounts()
        }).finally(() => {
            rejectSaving.value = false
        })
}

// ==================== 详情抽屉（Tab 懒加载） ====================
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailInfo = ref(null)
const activeDetailTab = ref('qual')
const detailId = ref(null)
const tabsLoaded = reactive({ qual: false, appt: false, review: false, income: false })

const apptData = reactive({ records: [], total: 0, page: 1, loading: false })
const reviewTabData = reactive({ records: [], total: 0, page: 1, loading: false })
const income = ref(null)
const incomeLoading = ref(false)

const openDetail = (row) => {
    detailId.value = row.id
    detailInfo.value = null
    activeDetailTab.value = 'qual'
    Object.keys(tabsLoaded).forEach(k => { tabsLoaded[k] = false })
    apptData.records = []; apptData.total = 0; apptData.page = 1
    reviewTabData.records = []; reviewTabData.total = 0; reviewTabData.page = 1
    income.value = null
    detailVisible.value = true
    detailLoading.value = true
    tabsLoaded.qual = true
    getAdminConsultantDetail(row.id).then(data => {
        detailInfo.value = data
    }).finally(() => {
        detailLoading.value = false
    })
}

const onDetailTabChange = (name) => {
    if (tabsLoaded[name]) return
    tabsLoaded[name] = true
    if (name === 'appt') loadAppointments()
    else if (name === 'review') loadReviewTab()
    else if (name === 'income') loadIncome()
}

const loadAppointments = () => {
    apptData.loading = true
    getConsultantAppointments(detailId.value, { page: apptData.page, size: 10 }).then(data => {
        apptData.records = data.records || []
        apptData.total = data.total || 0
    }).finally(() => {
        apptData.loading = false
    })
}

const loadReviewTab = () => {
    reviewTabData.loading = true
    getAdminConsultantReviews(detailId.value, { page: reviewTabData.page, size: 10 }).then(data => {
        reviewTabData.records = data.records || []
        reviewTabData.total = data.total || 0
    }).finally(() => {
        reviewTabData.loading = false
    })
}

const loadIncome = () => {
    incomeLoading.value = true
    getConsultantIncome(detailId.value).then(data => {
        income.value = data
    }).finally(() => {
        incomeLoading.value = false
    })
}

// ==================== 新增 / 编辑 ====================
const formVisible = ref(false)
const saving = ref(false)
const editingId = ref(null)
const form = reactive({
    name: '', avatar: '', title: '', yearsOfPractice: 0,
    intro: '', sortWeight: 0, price: null, username: ''
})
const specialtyList = ref([])
const qualFileList = ref([])

const emptyForm = () => ({
    name: '', avatar: '', title: '', yearsOfPractice: 0,
    intro: '', sortWeight: 0, price: null, username: ''
})

const openCreate = () => {
    editingId.value = null
    Object.assign(form, emptyForm())
    specialtyList.value = []
    qualFileList.value = []
    formVisible.value = true
}

const openEdit = (row) => {
    getAdminConsultantDetail(row.id).then(data => {
        editingId.value = data.id
        Object.assign(form, {
            name: data.name || '',
            avatar: data.avatar || '',
            title: data.title || '',
            yearsOfPractice: data.yearsOfPractice ?? 0,
            intro: data.intro || '',
            sortWeight: data.sortWeight ?? 0,
            price: data.price ?? null,
            username: ''
        })
        specialtyList.value = splitSpecialties(data.specialties)
        qualFileList.value = (data.qualificationImages || []).map((url, i) => ({
            name: `qualification-${i + 1}`,
            url,
            response: { filePath: url }
        }))
        formVisible.value = true
    })
}

const beforeImageUpload = (file) => {
    if (!file.type.startsWith('image/')) {
        ElMessage.error('只能上传图片文件')
        return false
    }
    if (file.size / 1024 / 1024 > 5) {
        ElMessage.error('图片大小不能超过 5MB')
        return false
    }
    return true
}

const handleAvatarUpload = async ({ file, onSuccess, onError }) => {
    try {
        const res = await uploadConsultantImage(file)
        form.avatar = res.filePath
        onSuccess?.(res)
    } catch (e) {
        onError?.(e)
    }
}

const handleQualUpload = async ({ file, onSuccess, onError }) => {
    try {
        const res = await uploadConsultantImage(file)
        qualFileList.value.push({ name: file.name, url: res.filePath, response: res })
        onSuccess?.(res)
    } catch (e) {
        onError?.(e)
    }
}

const handleQualRemove = (file) => {
    const path = file.response?.filePath || file.url
    qualFileList.value = qualFileList.value
        .filter(f => (f.response?.filePath || f.url) !== path)
}

const handleSave = () => {
    if (!form.name.trim()) {
        ElMessage.warning('请输入姓名')
        return
    }
    if (form.yearsOfPractice == null) {
        ElMessage.warning('请填写从业年限')
        return
    }
    const payload = {
        name: form.name.trim(),
        avatar: form.avatar || null,
        title: form.title || null,
        specialties: specialtyList.value.join(','),
        yearsOfPractice: form.yearsOfPractice,
        intro: form.intro || null,
        qualificationImages: qualFileList.value.map(f => f.response?.filePath || f.url),
        sortWeight: form.sortWeight ?? 0,
        price: form.price === '' || form.price == null ? null : Number(form.price)
    }
    if (editingId.value) {
        payload.id = editingId.value
    } else {
        // 仅新增时可指定登录用户名，留空由后端自动生成 doc_xxxxxxxx
        payload.username = form.username.trim() || null
    }
    saving.value = true
    const request = editingId.value ? updateConsultant(payload) : createConsultant(payload)
    request.then((data) => {
        ElMessage.success(editingId.value ? '咨询师信息已更新' : '咨询师已新增，等待审核')
        formVisible.value = false
        loadList()
        loadCounts()
        // 初始密码仅在创建成功时一次性展示，后端不再保留明文
        if (!editingId.value && data?.initialPassword) {
            ElMessageBox.alert(
                `<p>登录账号：<b>${data.username}</b></p>
                 <p>初始密码：<b>${data.initialPassword}</b></p>
                 <p style="color:#9ca3af;font-size:12px;margin-top:8px;">
                 请妥善转交咨询师，密码仅显示一次；账号需审核通过后方可登录。</p>`,
                '咨询师账号已创建',
                { dangerouslyUseHTMLString: true, confirmButtonText: '我已记录' }
            ).catch(() => {})
        }
    }).finally(() => {
        saving.value = false
    })
}

// ==================== 排班周视图 ====================
const schedVisible = ref(false)
const schedLoading = ref(false)
const schedConsultant = ref({})
const weekPickerValue = ref('')
const weekDate = ref('')
const weekSlotList = ref([])

const tplVisible = ref(false)
const tplRows = ref([])
const tplSaving = ref(false)
const dayVisible = ref(false)
const dayDate = ref('')
const dayRows = ref([])
const daySaving = ref(false)

const weekDays = computed(() =>
    weekDate.value ? Array.from({ length: 7 }, (_, i) => addDays(weekDate.value, i)) : [])

const cellMap = computed(() => {
    const m = {}
    weekSlotList.value.forEach(s => { m[s.slotDate + '|' + s.startTime] = s })
    return m
})

const timeRows = computed(() => {
    const endMap = new Map()
    weekSlotList.value.forEach(s => {
        if (!endMap.has(s.startTime)) endMap.set(s.startTime, s.endTime)
    })
    return [...endMap.keys()].sort().map(start => ({ start, end: endMap.get(start) }))
})

const cell = (day, row) => cellMap.value[day + '|' + row.start]

const openSchedule = (row) => {
    schedConsultant.value = row
    const monday = toMonday(todayStr())
    weekDate.value = monday
    weekPickerValue.value = monday
    weekSlotList.value = []
    schedVisible.value = true
    loadWeek()
}

const loadWeek = () => {
    schedLoading.value = true
    getConsultantWeekSlots(schedConsultant.value.id, weekDate.value).then(data => {
        weekSlotList.value = data || []
    }).finally(() => {
        schedLoading.value = false
    })
}

const shiftWeek = (delta) => {
    weekDate.value = addDays(weekDate.value, delta * 7)
    weekPickerValue.value = weekDate.value
    loadWeek()
}

const goCurrentWeek = () => {
    weekDate.value = toMonday(todayStr())
    weekPickerValue.value = weekDate.value
    loadWeek()
}

const onWeekPickerChange = (val) => {
    if (!val) return
    weekDate.value = toMonday(val)
    loadWeek()
}

const onCellClick = async (day, row) => {
    if (isPast(day)) {
        ElMessage.info('过去日期的排班不可操作')
        return
    }
    const slot = cell(day, row)
    try {
        if (!slot) {
            await ElMessageBox.confirm(`在 ${day}（${WEEK_NAMES[weekDays.value.indexOf(day)]}）添加 ${row.start}-${row.end} 时段？`,
                '添加时段', { type: 'primary', confirmButtonText: '添加', cancelButtonText: '取消' })
            const res = await createConsultantSlots(schedConsultant.value.id, {
                date: day,
                slots: [{ startTime: row.start, endTime: row.end }]
            })
            ElMessage.success(`已添加 1 个时段`)
            loadWeek()
            return
        }
        if (slot.status === 1) {
            ElMessage.warning('已被预约的时段不可停用或恢复')
            return
        }
        const toDisable = slot.status === 0
        await ElMessageBox.confirm(
            toDisable ? '停用后该时段将不可被预约，确认停用？' : '确认将该时段恢复为可约？',
            '排班操作', { type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消' })
        await updateSlotStatus(slot.id, toDisable ? 2 : 0)
        ElMessage.success(toDisable ? '已停用' : '已恢复')
        loadWeek()
    } catch (e) {
        // 用户取消确认框：静默
    }
}

const openTplAdd = () => {
    tplRows.value = [{ startTime: '', endTime: '' }]
    tplVisible.value = true
}

const submitTpl = () => {
    const slots = collectSlots(tplRows.value)
    if (!slots) return
    tplSaving.value = true
    createConsultantSlots(schedConsultant.value.id, { weekStart: weekDate.value, slots })
        .then(res => {
            ElMessage.success(`已生成 ${res.created} 个时段，跳过 ${res.skipped} 个重复时段`)
            tplVisible.value = false
            loadWeek()
        }).finally(() => {
            tplSaving.value = false
        })
}

const openDayAdd = () => {
    dayDate.value = weekDate.value
    dayRows.value = [{ startTime: '', endTime: '' }]
    dayVisible.value = true
}

const submitDay = () => {
    if (!dayDate.value) {
        ElMessage.warning('请选择日期')
        return
    }
    const slots = collectSlots(dayRows.value)
    if (!slots) return
    daySaving.value = true
    createConsultantSlots(schedConsultant.value.id, { date: dayDate.value, slots })
        .then(res => {
            ElMessage.success(`已生成 ${res.created} 个时段，跳过 ${res.skipped} 个重复时段`)
            dayVisible.value = false
            // 若添加日期不属于当前查看周，切到该周
            const monday = toMonday(dayDate.value)
            if (monday !== weekDate.value) {
                weekDate.value = monday
                weekPickerValue.value = monday
            }
            loadWeek()
        }).finally(() => {
            daySaving.value = false
        })
}

const disablePastDate = (date) => {
    const today = new Date()
    today.setHours(0, 0, 0, 0)
    return date.getTime() < today.getTime()
}

// ==================== 工具 ====================
function fmtTime(t) {
    if (!t) return '—'
    return String(t).replace('T', ' ').slice(0, 16)
}

function splitSpecialties(str) {
    return str ? str.split(',').map(s => s.trim()).filter(Boolean) : []
}

function statusLabel(code) {
    return STATUS_META[code]?.[0] || '未知'
}

function statusTagType(code) {
    return STATUS_META[code]?.[1] || 'info'
}

function apptStatusType(code) {
    return { PENDING: 'warning', CONFIRMED: 'primary', COMPLETED: 'success', CANCELED: 'info' }[code] || 'info'
}

function formatPrice(price) {
    return price == null || price === '' ? '未设置' : `¥${Number(price).toFixed(2)} / 小时`
}

function cellStatusText(status) {
    return { 0: '可约', 1: '已约', 2: '停用' }[status]
}

function cellClass(slot) {
    if (!slot) return 'cell-empty'
    return { 0: 'cell-free', 1: 'cell-booked', 2: 'cell-disabled' }[slot.status] || 'cell-empty'
}

function todayStr() {
    const d = new Date()
    return fmtDate(d)
}

function fmtDate(d) {
    const m = String(d.getMonth() + 1).padStart(2, '0')
    const day = String(d.getDate()).padStart(2, '0')
    return `${d.getFullYear()}-${m}-${day}`
}

function addDays(dateStr, delta) {
    const d = new Date(dateStr + 'T00:00:00')
    d.setDate(d.getDate() + delta)
    return fmtDate(d)
}

/** 归一到周一（Mon=0 ... Sun=6） */
function toMonday(dateStr) {
    const d = new Date(dateStr + 'T00:00:00')
    const offset = (d.getDay() + 6) % 7
    d.setDate(d.getDate() - offset)
    return fmtDate(d)
}

function isPast(dateStr) {
    return dateStr < todayStr()
}

/** 收集并校验时段表单，不合法返回 null */
function collectSlots(rows) {
    const slots = rows
        .filter(s => s.startTime && s.endTime)
        .map(s => ({ startTime: s.startTime, endTime: s.endTime }))
    if (!slots.length) {
        ElMessage.warning('请至少添加一个完整时段')
        return null
    }
    if (slots.some(s => s.startTime >= s.endTime)) {
        ElMessage.warning('存在结束时间早于（或等于）开始时间的时段，请检查')
        return null
    }
    return slots
}

// 初始加载
loadList()
loadCounts()
</script>

<style lang="scss" scoped>
.consultants-page {
    padding: 16px;
    background-color: #F7F8FA;
    min-height: 100%;
}

.filter-card {
    border: 1px solid #E5E7EB;
    border-radius: 4px;
    margin-bottom: 16px;

    :deep(.el-card__body) {
        padding: 0 16px;
    }
}

.search-form {
    margin-bottom: 4px;
}

.status-tab-label {
    font-size: 14px;

    .tab-count {
        display: inline-block;
        margin-left: 4px;
        padding: 0 7px;
        min-width: 22px;
        line-height: 18px;
        border-radius: 9px;
        background: #F3F4F6;
        color: #6B7280;
        font-size: 12px;
        text-align: center;
    }

    .tab-count-pending {
        background: #FEF3C7;
        color: #B45309;
    }
}

.table-card {
    border: 1px solid #E5E7EB;
    border-radius: 4px;

    :deep(.el-card__body) {
        padding: 16px;
    }
}

.consultant-cell {
    display: flex;
    align-items: center;
    gap: 10px;

    .consultant-name {
        color: #1F2937;
        font-size: 14px;
    }
}

.specialty-tag {
    margin: 2px 4px 2px 0;
}

.pager {
    margin-top: 16px;
    display: flex;
    justify-content: flex-end;
}

:deep(.col-action .cell) {
    white-space: nowrap;
    display: flex;
    align-items: center;
    gap: 2px;
}

/* ============ 抽屉通用 ============ */
.drawer-body {
    padding: 0 20px 16px;
}

.drawer-footer {
    display: flex;
    justify-content: flex-end;
    gap: 12px;
}

.section-title {
    margin: 18px 0 10px;
    font-size: 14px;
    font-weight: 600;
    color: #1F2937;

    .section-hint {
        font-weight: 400;
        font-size: 12px;
        color: #9CA3AF;
    }
}

.intro-box {
    background: #F7F8FA;
    border: 1px solid #E5E7EB;
    border-radius: 4px;
    padding: 12px;
    color: #374151;
    font-size: 13px;
    line-height: 1.7;
    white-space: pre-wrap;
}

.review-alert {
    margin-bottom: 14px;
    padding: 10px 12px;
    background: #FEF2F2;
    border: 1px solid #FECACA;
    border-radius: 4px;
    color: #B91C1C;
    font-size: 13px;
    line-height: 1.6;
}

.info-desc {
    margin-bottom: 8px;
}

.image-wall {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;

    .wall-image {
        width: 128px;
        height: 128px;
        border-radius: 4px;
        border: 1px solid #E5E7EB;
        cursor: pointer;
    }
}

/* ============ 详情 ============ */
.detail-header {
    display: flex;
    align-items: center;
    gap: 14px;
    margin-bottom: 16px;

    .detail-name {
        font-size: 17px;
        font-weight: 600;
        color: #1F2937;
        display: flex;
        align-items: center;
        gap: 8px;
    }

    .detail-sub {
        margin-top: 4px;
        color: #6B7280;
        font-size: 13px;
    }
}

.detail-tabs {
    margin-top: 8px;
}

.tab-pager {
    margin-top: 12px;
    display: flex;
    justify-content: flex-end;
}

.review-list {
    min-height: 120px;
}

.review-item {
    padding: 12px 0;
    border-bottom: 1px solid #F3F4F6;

    .review-item-head {
        display: flex;
        align-items: center;
        justify-content: space-between;
        margin-bottom: 6px;
    }

    .review-item-time {
        color: #9CA3AF;
        font-size: 12px;
    }

    .review-item-content {
        color: #374151;
        font-size: 13px;
        line-height: 1.7;
    }
}

.income-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 14px;
    padding-top: 8px;
}

.income-card {
    border: 1px solid #E5E7EB;
    border-radius: 4px;
    padding: 20px;
    text-align: center;
    background: #FFFFFF;

    .income-value {
        font-size: 26px;
        font-weight: 600;
        color: #1F2937;
    }

    .income-unset {
        color: #9CA3AF;
        font-size: 18px;
    }

    .income-label {
        margin-top: 8px;
        color: #6B7280;
        font-size: 13px;
    }
}

/* ============ 编辑弹窗 ============ */
.form-grid {
    display: grid;
    grid-template-columns: 1fr 1fr 1.2fr;
    gap: 0 16px;

    .form-col-avatar {
        grid-column: 1;
    }

    .form-col-main {
        grid-column: 2 / span 2;
    }
}

.avatar-uploader {
    :deep(.el-upload) {
        border: 1px dashed #D1D5DB;
        border-radius: 6px;
        cursor: pointer;
        width: 64px;
        height: 64px;
        display: flex;
        align-items: center;
        justify-content: center;
        overflow: hidden;
        transition: border-color 0.2s;

        &:hover {
            border-color: var(--el-color-primary);
        }
    }
}

.avatar-add {
    display: flex;
    flex-direction: column;
    align-items: center;
    color: #9CA3AF;
    font-size: 12px;
    gap: 2px;
}

.qual-trigger {
    display: flex;
    flex-direction: column;
    align-items: center;
    color: #9CA3AF;
    font-size: 12px;
    gap: 2px;
}

/* ============ 排班周视图 ============ */
.sched-toolbar {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 12px;

    .sched-toolbar-right {
        margin-left: auto;
        display: flex;
        gap: 8px;
    }
}

.sched-legend {
    display: flex;
    gap: 18px;
    margin-bottom: 10px;
    color: #6B7280;
    font-size: 12px;

    .legend-item {
        display: flex;
        align-items: center;
        gap: 5px;
    }

    .legend-dot {
        width: 12px;
        height: 12px;
        border-radius: 3px;
        display: inline-block;
    }

    .dot-free { background: #D8F0DF; border: 1px solid #86C99B; }
    .dot-booked { background: #FDE7CE; border: 1px solid #F0B27A; }
    .dot-disabled { background: #F3F4F6; border: 1px solid #D1D5DB; }
    .dot-empty { background: #FFFFFF; border: 1px dashed #D1D5DB; }
}

.sched-grid {
    display: grid;
    grid-template-columns: 88px repeat(7, 1fr);
    border: 1px solid #E5E7EB;
    border-radius: 4px;
    overflow: hidden;

    .grid-head {
        background: #F7F8FA;
        border-bottom: 1px solid #E5E7EB;
        border-left: 1px solid #E5E7EB;
        padding: 8px 4px;
        text-align: center;
        color: #374151;
        font-size: 13px;
        font-weight: 500;

        &:first-child {
            border-left: none;
        }

        .head-date {
            font-size: 12px;
            color: #9CA3AF;
            font-weight: 400;
            margin-top: 2px;
        }

        &.head-past {
            color: #9CA3AF;
        }
    }

    .grid-time-head {
        background: #F7F8FA;
    }

    .grid-time {
        padding: 6px 4px;
        font-size: 12px;
        color: #6B7280;
        text-align: center;
        line-height: 1.5;
        border-top: 1px solid #E5E7EB;
        background: #F7F8FA;
    }

    .grid-cell {
        height: 54px;
        border-left: 1px solid #E5E7EB;
        border-top: 1px solid #E5E7EB;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 12px;
        cursor: pointer;
        user-select: none;
        transition: filter 0.15s;

        &:hover {
            filter: brightness(0.97);
        }

        .cell-plus {
            color: #C4CCD8;
            font-size: 16px;
        }
    }

    .cell-free {
        background: #E9F7EF;
        color: #1E8449;
    }

    .cell-booked {
        background: #FDEBD8;
        color: #B06A1F;
        cursor: not-allowed;

        &:hover {
            filter: none;
        }
    }

    .cell-disabled {
        background: #F3F4F6;
        color: #9CA3AF;
    }

    .cell-empty {
        background: #FFFFFF;

        &:hover {
            background: #EFF6FF;
        }
    }

    .cell-past {
        opacity: 0.55;
        cursor: not-allowed;

        &:hover {
            filter: none;
        }
    }

    .grid-empty {
        grid-column: 1 / -1;
        padding: 30px 0;
    }
}

.slot-tip {
    margin-bottom: 12px;
    padding: 8px 12px;
    background: #F7F8FA;
    border-radius: 4px;
    color: #6B7280;
    font-size: 13px;
}

.slot-row {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 10px;
}
</style>
