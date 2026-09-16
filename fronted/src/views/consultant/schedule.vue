<template>
    <div class="consultant-schedule">
        <PageHead title="日程管理" />

        <el-card shadow="never" class="schedule-card" v-loading="loading">
            <div class="week-toolbar">
                <el-button-group>
                    <el-button @click="shiftWeek(-1)"><el-icon><ArrowLeft /></el-icon></el-button>
                    <el-button @click="goThisWeek">本周</el-button>
                    <el-button @click="shiftWeek(1)"><el-icon><ArrowRight /></el-icon></el-button>
                </el-button-group>
                <el-date-picker v-model="pickerDate" type="date" value-format="YYYY-MM-DD"
                                placeholder="选择周内日期" @change="onPickDate" class="week-picker" />
                <span class="range-text">{{ weekDays[0] }} ~ {{ weekDays[6] }}</span>
                <el-button type="primary" plain :disabled="weekPastAll" @click="openWeekTemplate">
                    本周模板批量生成
                </el-button>
                <div class="legend">
                    <span class="legend-dot free"></span>可约
                    <span class="legend-dot booked"></span>已约
                    <span class="legend-dot disabled"></span>停用
                </div>
            </div>

            <el-table :data="rows" border class="week-table">
                <el-table-column label="时段" width="120" align="center">
                    <template #default="{ row }">{{ row.start }}-{{ row.end }}</template>
                </el-table-column>
                <el-table-column v-for="(day, idx) in weekDays" :key="day"
                                 :label="`${WEEK_NAMES[idx]} ${day.slice(5)}`" align="center" min-width="130">
                    <template #default="{ row }">
                        <!-- 空格：过去日期不可点，其余点击新建该日时段 -->
                        <div v-if="!cell(day, row)" class="slot-cell is-empty"
                             :class="{ 'is-past': isPast(day) }"
                             @click="openCreate(day, row)">
                            <span class="slot-text">{{ isPast(day) ? '-' : '+' }}</span>
                        </div>
                        <div v-else class="slot-cell" :class="[cellClass(day, row), { clickable: cell(day, row).status !== 1 }]"
                             @click="onCellClick(day, row)">
                            <span class="slot-text">{{ cellText(day, row) }}</span>
                        </div>
                    </template>
                </el-table-column>
            </el-table>

            <el-alert type="info" :closable="false" show-icon
                      title="点击空白格新建时段；点击可约/停用格可停用、恢复或删除；已约时段不可操作。排班与管理端共用同一数据源，双方改动实时互见"
                      class="readonly-tip" />
        </el-card>

        <!-- 新建单个时段 -->
        <el-dialog v-model="createVisible" title="新增排班时段" width="420px"
                   :close-on-click-modal="false">
            <el-form label-width="88px">
                <el-form-item label="日期">
                    <el-input :model-value="createForm.date" disabled />
                </el-form-item>
                <el-form-item label="开始时间">
                    <el-time-picker v-model="createForm.startTime" format="HH:mm" value-format="HH:mm"
                                    placeholder="开始时间" :clearable="false" class="time-picker" />
                </el-form-item>
                <el-form-item label="结束时间">
                    <el-time-picker v-model="createForm.endTime" format="HH:mm" value-format="HH:mm"
                                    placeholder="结束时间" :clearable="false" class="time-picker" />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="createVisible = false">取消</el-button>
                <el-button type="primary" :loading="saving" @click="submitCreate">保存</el-button>
            </template>
        </el-dialog>

        <!-- 已有时段操作：停用 / 恢复 / 删除 -->
        <el-dialog v-model="actionVisible" title="时段操作" width="420px">
            <div v-if="activeSlot" class="action-box">
                <p>{{ activeSlot.slotDate }} {{ activeSlot.startTime }} - {{ activeSlot.endTime }}</p>
                <p class="action-state">当前状态：
                    <el-tag size="small" :type="activeSlot.status === 0 ? 'success' : 'info'">
                        {{ activeSlot.status === 0 ? '可约' : '停用' }}
                    </el-tag>
                </p>
            </div>
            <template #footer>
                <el-button v-if="activeSlot && activeSlot.status === 2" type="success"
                           :loading="saving" @click="toggleStatus(0)">恢复可约</el-button>
                <el-button v-if="activeSlot && activeSlot.status === 0" type="warning"
                           :loading="saving" @click="toggleStatus(2)">停用</el-button>
                <el-button type="danger" :loading="saving" @click="removeSlot">删除</el-button>
                <el-button @click="actionVisible = false">关闭</el-button>
            </template>
        </el-dialog>

        <!-- 本周模板批量生成 -->
        <el-dialog v-model="tplVisible" title="本周模板批量生成" width="520px"
                   :close-on-click-modal="false">
            <el-alert type="info" :closable="false" show-icon class="tpl-tip"
                      :title="`将下列每日时段模板应用到 ${weekDays[0]} ~ ${weekDays[6]}，已存在的时段自动跳过（过去日期不生成）`" />
            <div v-for="(item, idx) in tplRows" :key="idx" class="tpl-row">
                <el-time-picker v-model="item.startTime" format="HH:mm" value-format="HH:mm"
                                placeholder="开始" :clearable="false" class="time-picker" />
                <span class="tpl-sep">~</span>
                <el-time-picker v-model="item.endTime" format="HH:mm" value-format="HH:mm"
                                placeholder="结束" :clearable="false" class="time-picker" />
                <el-button link type="danger" @click="tplRows.splice(idx, 1)">移除</el-button>
            </div>
            <el-button link type="primary" @click="addTplRow">+ 添加时段</el-button>
            <template #footer>
                <el-button @click="tplVisible = false">取消</el-button>
                <el-button type="primary" :loading="saving" @click="submitTemplate">应用到整周</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHead from '@/components/PageHead.vue'
import {
    getMySchedule, createMySlots, updateMySlotStatus, deleteMySlot
} from '@/api/consultant'

const WEEK_NAMES = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']

const loading = ref(false)
const saving = ref(false)
const monday = ref(toMonday(new Date()))
const pickerDate = ref(fmtDate(monday.value))
const slots = ref([])

const weekDays = computed(() => {
    const days = []
    for (let i = 0; i < 7; i++) {
        days.push(fmtDate(addDays(monday.value, i)))
    }
    return days
})

// 整周均已过去（今天为周一之前）时禁用周模板入口
const weekPastAll = computed(() => weekDays.value[6] < fmtDate(new Date()))

// 行 = 本周出现过的全部「开始-结束」时段，按开始时间排序去重
const rows = computed(() => {
    const map = new Map()
    slots.value.forEach(s => {
        if (!map.has(s.startTime)) {
            map.set(s.startTime, { start: s.startTime, end: s.endTime })
        }
    })
    return [...map.values()].sort((a, b) => a.start.localeCompare(b.start))
})

const cell = (day, row) => {
    return slots.value.find(s => s.slotDate === day && s.startTime === row.start) || null
}

const cellClass = (day, row) => {
    const s = cell(day, row)
    return { 'is-free': s.status === 0, 'is-booked': s.status === 1, 'is-disabled': s.status === 2 }
}

const cellText = (day, row) => {
    const s = cell(day, row)
    if (s.status === 1) return s.userNickname ? `已约 · ${s.userNickname}` : '已约'
    if (s.status === 2) return '停用'
    return '可约'
}

const isPast = (day) => day < fmtDate(new Date())

const load = () => {
    loading.value = true
    getMySchedule(fmtDate(monday.value)).then(data => {
        slots.value = data || []
    }).finally(() => {
        loading.value = false
    })
}

const shiftWeek = (delta) => {
    monday.value = addDays(monday.value, delta * 7)
    pickerDate.value = fmtDate(monday.value)
    load()
}

const goThisWeek = () => {
    monday.value = toMonday(new Date())
    pickerDate.value = fmtDate(monday.value)
    load()
}

const onPickDate = (val) => {
    if (!val) return
    monday.value = toMonday(new Date(val + 'T00:00:00'))
    load()
}

// ==================== 单格新建 ====================

const createVisible = ref(false)
const createForm = ref({ date: '', startTime: '', endTime: '' })

const openCreate = (day, row) => {
    if (isPast(day)) return
    createForm.value = { date: day, startTime: row?.start || '09:00', endTime: row?.end || '10:00' }
    createVisible.value = true
}

const submitCreate = () => {
    const f = createForm.value
    if (!f.startTime || !f.endTime) {
        ElMessage.warning('请选择起止时间')
        return
    }
    if (f.endTime <= f.startTime) {
        ElMessage.warning('结束时间必须晚于开始时间')
        return
    }
    saving.value = true
    // ponytail: 单格创建复用按日批量接口（slots 长度 1），后端无需单条端点
    createMySlots({ date: f.date, slots: [{ startTime: f.startTime, endTime: f.endTime }] })
        .then(res => {
            ElMessage.success(res?.skipped ? '该时段已存在，已跳过' : '时段已添加')
            createVisible.value = false
            load()
        }).finally(() => { saving.value = false })
}

// ==================== 已有格操作 ====================

const actionVisible = ref(false)
const activeSlot = ref(null)

const onCellClick = (day, row) => {
    const s = cell(day, row)
    if (s.status === 1) {
        ElMessage.info(s.userNickname ? `该时段已被 ${s.userNickname} 预约，不可操作` : '该时段已被预约，不可操作')
        return
    }
    activeSlot.value = s
    actionVisible.value = true
}

const toggleStatus = (status) => {
    saving.value = true
    updateMySlotStatus(activeSlot.value.id, status).then(() => {
        ElMessage.success(status === 2 ? '已停用' : '已恢复可约')
        actionVisible.value = false
        load()
    }).finally(() => { saving.value = false })
}

const removeSlot = () => {
    ElMessageBox.confirm('确定删除该时段吗？删除后用户端不再显示。', '删除时段', {
        confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning'
    }).then(() => {
        saving.value = true
        deleteMySlot(activeSlot.value.id).then(() => {
            ElMessage.success('已删除')
            actionVisible.value = false
            load()
        }).finally(() => { saving.value = false })
    }).catch(() => {})
}

// ==================== 周模板批量 ====================

const tplVisible = ref(false)
const tplRows = ref([])

const addTplRow = (start = '09:00', end = '10:00') => tplRows.value.push({ startTime: start, endTime: end })

const openWeekTemplate = () => {
    tplRows.value = [{ startTime: '09:00', endTime: '10:00' }]
    tplVisible.value = true
}

const submitTemplate = () => {
    const valid = tplRows.value.filter(r => r.startTime && r.endTime && r.endTime > r.startTime)
    if (valid.length === 0) {
        ElMessage.warning('请至少添加一个结束晚于开始的时段')
        return
    }
    saving.value = true
    createMySlots({ weekStart: weekDays.value[0], slots: valid }).then(res => {
        ElMessage.success(`已生成 ${res?.created ?? 0} 个时段，跳过重复 ${res?.skipped ?? 0} 个`)
        tplVisible.value = false
        load()
    }).finally(() => { saving.value = false })
}

// 归一到周一
function toMonday(d) {
    const day = d.getDay() === 0 ? 7 : d.getDay()
    return addDays(d, 1 - day)
}
function addDays(d, n) {
    const nd = new Date(d)
    nd.setDate(nd.getDate() + n)
    return nd
}
function fmtDate(d) {
    const m = String(d.getMonth() + 1).padStart(2, '0')
    const day = String(d.getDate()).padStart(2, '0')
    return `${d.getFullYear()}-${m}-${day}`
}

onMounted(load)
</script>

<style lang="scss" scoped>
.consultant-schedule {
    .schedule-card {
        border-color: #e5e7eb;
    }
    .week-toolbar {
        display: flex;
        align-items: center;
        gap: 12px;
        margin-bottom: 16px;
        flex-wrap: wrap;
        .week-picker {
            width: 170px;
        }
        .range-text {
            font-weight: 600;
            color: #1f2937;
        }
        .legend {
            margin-left: auto;
            font-size: 13px;
            color: #6b7280;
            display: flex;
            align-items: center;
            gap: 6px;
        }
        .legend-dot {
            display: inline-block;
            width: 12px;
            height: 12px;
            border-radius: 3px;
            margin-left: 10px;
            &.free { background: #d1fae5; }
            &.booked { background: #fed7aa; }
            &.disabled { background: #e5e7eb; }
        }
    }
    .week-table {
        .slot-cell {
            border-radius: 6px;
            padding: 6px 8px;
            font-size: 12px;
            &.is-free {
                background: #d1fae5;
                color: #047857;
            }
            &.is-booked {
                background: #fed7aa;
                color: #b45309;
            }
            &.is-disabled {
                background: #e5e7eb;
                color: #6b7280;
            }
            // 可约/停用格可点击操作
            &.clickable {
                cursor: pointer;
                outline: 1px dashed transparent;
                transition: outline-color .15s;
                &:hover { outline-color: #9ca3af; }
            }
            // 空格：非过去日期悬停出现虚线引导
            &.is-empty {
                cursor: pointer;
                color: #c0c4cc;
                border: 1px dashed #e5e7eb;
                border-radius: 6px;
                &:hover { color: #2563eb; border-color: #93c5fd; }
                &.is-past { cursor: default; }
                &.is-past:hover { color: #c0c4cc; border-color: #e5e7eb; }
            }
        }
    }
    .readonly-tip {
        margin-top: 14px;
    }
    .text-muted {
        color: #c0c4cc;
    }
    .time-picker {
        width: 100%;
    }
    .action-box {
        p { margin: 8px 0; color: #374151; }
        .action-state { font-size: 14px; }
    }
    .tpl-tip {
        margin-bottom: 14px;
    }
    .tpl-row {
        display: flex;
        align-items: center;
        gap: 8px;
        margin-bottom: 10px;
        .tpl-sep { color: #9ca3af; }
        .time-picker { flex: 1; }
    }
}
</style>
