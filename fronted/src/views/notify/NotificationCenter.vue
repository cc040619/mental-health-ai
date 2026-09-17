<template>
  <div class="notify-container">
    <!-- 便签标题 -->
    <div class="page-header">
      <div class="header-tape"></div>
      <h1 class="page-title">🔔 消息通知</h1>
      <p class="page-subtitle">那些想对你说的话，都安静地躺在这里 🤍</p>
    </div>

    <div class="notify-card">
      <!-- 顶部操作行 -->
      <div class="toolbar">
        <div class="tabs">
          <button v-for="t in tabs" :key="t.code"
                  class="tab-btn" :class="{ active: activeTab === t.code }"
                  @click="switchTab(t.code)">
            {{ t.label }}
          </button>
        </div>
        <div class="toolbar-actions">
          <router-link to="/notifications/settings" class="soft-btn">⚙️ 消息设置</router-link>
          <el-button v-if="activeTab !== 'ANNOUNCEMENT'" class="read-all-btn" @click="onReadAll">
            ✅ 全部已读
          </el-button>
        </div>
      </div>

      <!-- 列表 -->
      <div v-if="loading" class="loading-tip">载入中... 🌿</div>
      <div v-else-if="items.length === 0" class="empty-tip">
        <div class="empty-emoji">📭</div>
        <p>暂无消息，安安静静也很好</p>
      </div>
      <div v-else class="notify-list">
        <div v-for="item in items" :key="item.kind + '-' + item.id"
             class="notify-item" :class="{ unread: !item.isRead, fading: fadingKeys.has(item.kind + '-' + item.id) }"
             @click="onClickItem(item)">
          <span class="unread-dot" v-if="!item.isRead"></span>
          <span class="type-emoji">{{ typeEmoji(item) }}</span>
          <div class="item-body">
            <div class="item-head">
              <span class="item-title">{{ item.title }}</span>
              <span class="item-type">{{ typeLabel(item) }}</span>
            </div>
            <p class="item-content" v-html="item.content"></p>
            <span class="item-time">{{ formatRelative(item.createdAt) }}</span>
          </div>
          <span class="item-arrow" v-if="item.jumpable">›</span>
        </div>
      </div>

      <!-- 分页 -->
      <div v-if="total > pageSize" class="pager">
        <el-pagination background layout="prev, pager, next"
          :current-page="page" :page-size="pageSize" :total="total"
          @current-change="onPageChange" />
      </div>
    </div>

    <!-- 公告内容弹窗 -->
    <el-dialog v-model="announcementVisible" :title="currentAnnouncement.title"
               width="640px" class="announcement-dialog" append-to-body>
      <div class="announcement-content" v-html="currentAnnouncement.content"></div>
      <template #footer>
        <span class="dialog-time" v-if="currentAnnouncement.publishedAt">
          发布于 {{ formatDateTime(currentAnnouncement.publishedAt) }}
        </span>
        <el-button type="primary" @click="announcementVisible = false">我知道了</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  getNotificationList, getAnnouncements,
  markNotificationRead, markAllNotificationRead, markAnnouncementRead
} from '@/api/notify'
import { formatRelative, formatDateTime } from '@/utils/format'

const router = useRouter()

const tabs = [
  { code: 'all', label: '全部' },
  { code: 'INTERACTION', label: '💬 互动' },
  { code: 'ANNOUNCEMENT', label: '📢 公告' },
  { code: 'APPOINTMENT', label: '📅 预约' },
  { code: 'CONSULTANT', label: '👩‍⚕️ 咨询师' },
  { code: 'ASSESSMENT', label: '🧭 测评提醒' }
]

const activeTab = ref('all')
const items = ref([])
const loading = ref(false)
const page = ref(1)
const pageSize = 10
const total = ref(0)

// 「全部」Tab 合并通知 + 公告后内存分页（ponytail: 各取前 50 合并，毕设量级足够；量级上来后改后端聚合）
const mergedAll = ref([])
const fadingKeys = ref(new Set())

const announcementVisible = ref(false)
const currentAnnouncement = ref({ title: '', content: '', publishedAt: null })

const TYPE_META = {
  INTERACTION: { emoji: '💬', label: '互动' },
  APPOINTMENT: { emoji: '📅', label: '预约' },
  ASSESSMENT: { emoji: '🧭', label: '测评提醒' },
  ANNOUNCEMENT: { emoji: '📢', label: '公告' },
  CONSULTANT: { emoji: '👩‍⚕️', label: '咨询师' }
}

const typeEmoji = (item) => item.kind === 'a' ? '📢' : (TYPE_META[item.type]?.emoji || '🔔')
const typeLabel = (item) => item.kind === 'a' ? '公告' : (TYPE_META[item.type]?.label || '通知')

const load = () => {
  loading.value = true
  const finish = (list, totalCount) => {
    items.value = list
    total.value = totalCount
    loading.value = false
  }

  if (activeTab.value === 'ANNOUNCEMENT') {
    getAnnouncements({ page: page.value, size: pageSize }).then(data => {
      finish((data.records || []).map(a => ({ ...a, kind: 'a', jumpable: true })), data.total || 0)
    }).catch(() => loading.value = false)
    return
  }

  if (activeTab.value === 'all') {
    Promise.all([
      getNotificationList({ page: 1, size: 50 }),
      getAnnouncements({ page: 1, size: 50 })
    ]).then(([notifyData, annoData]) => {
      const notifications = (notifyData.records || []).map(n => ({ ...n, kind: 'n' }))
      const announcements = (annoData.records || []).map(a => ({ ...a, kind: 'a', jumpable: true }))
      mergedAll.value = [...notifications, ...announcements]
        .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
      renderMergedPage()
      loading.value = false
    }).catch(() => loading.value = false)
    return
  }

  getNotificationList({ type: activeTab.value, page: page.value, size: pageSize }).then(data => {
    finish((data.records || []).map(n => ({ ...n, kind: 'n' })), data.total || 0)
  }).catch(() => loading.value = false)
}

const renderMergedPage = () => {
  const from = (page.value - 1) * pageSize
  items.value = mergedAll.value.slice(from, from + pageSize)
  total.value = mergedAll.value.length
}

const switchTab = (code) => {
  activeTab.value = code
  page.value = 1
  mergedAll.value = []
  load()
}

const onPageChange = (p) => {
  page.value = p
  if (activeTab.value === 'all') {
    renderMergedPage()
  } else {
    load()
  }
}

// 标记本地已读 + 淡出动画，300ms 后提交状态
const fadeThenRun = (item, fn) => {
  const key = item.kind + '-' + item.id
  fadingKeys.value.add(key)
  fadingKeys.value = new Set(fadingKeys.value)
  setTimeout(() => {
    fadingKeys.value.delete(key)
    fadingKeys.value = new Set(fadingKeys.value)
    item.isRead = true
    fn && fn()
  }, 300)
}

const emitUnreadChanged = () => {
  window.dispatchEvent(new Event('notify-unread-changed'))
}

const onClickItem = (item) => {
  // 公告：已读后弹窗展示正文（正文后端已白名单清洗）
  if (item.kind === 'a') {
    if (!item.isRead) {
      markAnnouncementRead(item.id).then(() => {
        item.isRead = true
      }).catch(() => {})
    }
    currentAnnouncement.value = item
    announcementVisible.value = true
    return
  }

  // 通知：未读则先标记，再按关联类型跳转
  const doJump = () => {
    if (item.relatedType === 'POST' && item.relatedId) {
      router.push(`/community/post/${item.relatedId}`)
    } else if (item.relatedType === 'ASSESSMENT') {
      router.push('/assessment')
    } else if (item.relatedType === 'APPOINTMENT' && item.relatedId) {
      router.push(`/appointments/${item.relatedId}`)
    }
  }
  item.jumpable = item.relatedType === 'POST' || item.relatedType === 'ASSESSMENT' || item.relatedType === 'APPOINTMENT'

  if (!item.isRead) {
    markNotificationRead(item.id).then(() => {
      emitUnreadChanged()
      fadeThenRun(item, doJump)
    }).catch(() => {})
  } else {
    doJump()
  }
}

const onReadAll = () => {
  markAllNotificationRead().then(() => {
    items.value.forEach(i => { if (i.kind === 'n') i.isRead = true })
    mergedAll.value.forEach(i => { if (i.kind === 'n') i.isRead = true })
    emitUnreadChanged()
  })
}

onMounted(load)
</script>

<style lang="scss" scoped>
$bg-page: #FFF8E7;
$bg-card: #FFFEF9;
$primary: #E8927C;
$primary-dark: #D9826B;
$primary-darker: #C4705C;
$primary-light: rgba(232, 146, 124, 0.15);
$text-primary: #5C4A3A;
$text-regular: #6B5B4B;
$text-secondary: #8B7D6B;
$border-color: #E8DCC8;
$shadow-card: 0 4px 16px rgba(180, 140, 100, 0.12);
$shadow-button: 0 3px 0 $primary-darker, 0 4px 8px rgba(180, 140, 100, 0.2);
$radius-md: 12px;
$radius-lg: 16px;
$font-handwritten: "Ma Shan Zheng", "Comic Sans MS", "Segoe Print", "Bradley Hand", cursive;
$font-rounded: "Nunito", "Quicksand", "PingFang SC", "Microsoft YaHei", sans-serif;

.notify-container {
  min-height: calc(100vh - 130px);
  background: $bg-page;
  font-family: $font-rounded;
  color: $text-primary;
  padding: 24px 16px 80px;
  max-width: 1000px;
  margin: 0 auto;
}

.page-header {
  text-align: center;
  margin-bottom: 20px;
  .header-tape {
    width: 120px; height: 10px;
    background: linear-gradient(90deg, transparent, rgba(232,146,124,0.5), transparent);
    border-radius: 5px; margin: 0 auto 14px;
  }
  .page-title { font-family: $font-handwritten; font-size: 30px; font-weight: 600; margin: 0 0 8px; }
  .page-subtitle { font-size: 14px; color: $text-secondary; margin: 0; }
}

.notify-card {
  background: $bg-card;
  border: 1.5px solid $border-color;
  border-radius: $radius-lg;
  box-shadow: $shadow-card;
  padding: 18px;
  min-height: 480px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  padding-bottom: 14px;
  margin-bottom: 14px;
  border-bottom: 2px dashed $border-color;
}

.tabs {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.tab-btn {
  font-family: $font-rounded;
  font-size: 14px;
  color: $text-regular;
  background: transparent;
  border: 1.5px dashed $border-color;
  border-radius: 999px;
  padding: 6px 14px;
  cursor: pointer;
  transition: all 0.25s ease;
  &:hover { color: $primary-dark; border-color: $primary; background: $primary-light; }
  &.active {
    color: #fff;
    background: linear-gradient(135deg, $primary, $primary-dark);
    border-style: solid;
    border-color: $primary-dark;
    font-weight: 600;
    box-shadow: 0 2px 0 $primary-darker;
  }
}

.toolbar-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.soft-btn {
  display: inline-flex; align-items: center;
  border-radius: $radius-md; padding: 7px 14px; font-size: 13px;
  color: $primary-dark; background: $bg-card;
  border: 1.5px dashed $primary; text-decoration: none;
  transition: all 0.25s ease;
  &:hover { background: $primary-light; border-style: solid; }
}

:deep(.read-all-btn) {
  font-family: $font-rounded;
  font-size: 13px;
  border-radius: $radius-md;
  color: $primary-dark;
  background: $primary-light;
  border: 1px solid $primary;
  padding: 7px 14px;
  &:hover { background: rgba(232, 146, 124, 0.25); color: $primary-darker; }
}

.loading-tip, .empty-tip {
  text-align: center;
  color: $text-secondary;
  padding: 48px 0;
  font-size: 14px;
  .empty-emoji { font-size: 42px; margin-bottom: 10px; }
}

.notify-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.notify-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px 16px;
  border-radius: $radius-md;
  border: 1.5px solid transparent;
  background: #fff;
  cursor: pointer;
  transition: all 0.3s ease;

  &:hover {
    border-color: $primary;
    background: rgba(232, 146, 124, 0.06);
    transform: translateX(2px);
  }

  &.unread {
    background: linear-gradient(135deg, #FFF9F4, #FFFDF8);
    border-color: rgba(232, 146, 124, 0.45);
  }

  &.fading {
    opacity: 0.35;
    transform: translateX(12px);
  }
}

.unread-dot {
  flex: none;
  width: 9px;
  height: 9px;
  margin-top: 7px;
  border-radius: 50%;
  background: $primary;
  box-shadow: 0 0 0 3px rgba(232, 146, 124, 0.18);
}

.type-emoji {
  flex: none;
  font-size: 20px;
  line-height: 1.4;
}

.item-body {
  flex: 1;
  min-width: 0;
}

.item-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
  .item-title {
    font-size: 15px;
    font-weight: 600;
    color: $text-primary;
  }
  .item-type {
    flex: none;
    font-size: 11px;
    color: $primary-darker;
    background: $primary-light;
    border-radius: 6px;
    padding: 1px 8px;
  }
}

.item-content {
  margin: 0 0 6px;
  font-size: 13.5px;
  color: $text-regular;
  line-height: 1.6;
  word-break: break-all;

  /* 公告内容为富文本，重置内部标签默认边距，避免列表预览留白过大 */
  :deep(p) {
    margin: 0;
  }
}

.item-time {
  font-size: 12px;
  color: $text-secondary;
}

.item-arrow {
  flex: none;
  font-size: 20px;
  color: $primary;
  line-height: 1.2;
}

.pager {
  display: flex;
  justify-content: center;
  margin-top: 18px;
  :deep(.el-pager li.is-active) {
    background: $primary !important;
    color: #fff;
  }
}

.announcement-content {
  font-family: $font-rounded;
  color: $text-regular;
  font-size: 14px;
  line-height: 1.8;
  max-height: 50vh;
  overflow-y: auto;
  :deep(img) { max-width: 100%; border-radius: 8px; }
  :deep(a) { color: $primary; }
}

.dialog-time {
  font-size: 12px;
  color: $text-secondary;
  margin-right: 16px;
}

:deep(.announcement-dialog) {
  border-radius: $radius-lg;
  .el-dialog__header {
    margin-right: 0;
    padding-bottom: 12px;
    border-bottom: 2px dashed $border-color;
  }
  .el-dialog__title {
    font-family: $font-handwritten;
    font-size: 20px;
    color: $text-primary;
  }
  .el-button--primary {
    background: linear-gradient(135deg, $primary, $primary-dark);
    border: none;
    border-radius: $radius-md;
    box-shadow: $shadow-button;
  }
}
</style>
