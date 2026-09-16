<template>
  <div class="mine-container" v-loading="loading">
    <router-link to="/community" class="back-link">← 返回社区</router-link>

    <div class="page-header">
      <div class="header-tape"></div>
      <h1 class="page-title">📒 我的社区足迹</h1>
      <p class="page-subtitle">记录过的心情、说过的话、收藏的温暖 ✨</p>
    </div>

    <!-- Tab 切换 -->
    <div class="mine-tabs">
      <button v-for="t in tabs" :key="t.code" class="tab-btn"
              :class="{ active: activeTab === t.code }" @click="activeTab = t.code">
        {{ t.label }}
        <span class="tab-count">{{ countOf(t.code) }}</span>
      </button>
    </div>

    <!-- 我的帖子 -->
    <div v-if="activeTab === 'posts'" class="tab-panel">
      <div v-if="mine.posts.length === 0" class="empty-tip">
        <div class="empty-emoji">🌱</div>
        <p>还没有发布过帖子</p>
        <router-link to="/community/publish" class="primary-btn inline">✏️ 去发布第一篇</router-link>
      </div>
      <div v-for="p in mine.posts" :key="p.id" class="mine-card" @click="goDetail(p.id)">
        <div class="card-main">
          <div class="card-tags">
            <span class="board-tag">{{ boardEmoji(p.board) }} {{ p.boardLabel }}</span>
            <el-tooltip v-if="p.status === 5 && p.rejectReason" :content="`驳回原因：${p.rejectReason}`" placement="top">
              <span :class="['status-tag', statusClass(p.status)]">{{ statusText(p.status) }} ⓘ</span>
            </el-tooltip>
            <span v-else :class="['status-tag', statusClass(p.status)]">{{ statusText(p.status) }}</span>
          </div>
          <h3 class="card-title">{{ p.title }}</h3>
          <p class="card-summary">{{ p.summary || '（图片分享）' }}</p>
          <div class="card-foot">
            <span>{{ formatRelative(p.createdAt) }}</span>
            <span>👁 {{ p.viewCount || 0 }}</span>
            <span>💬 {{ p.commentCount || 0 }}</span>
            <span>❤ {{ p.likeCount || 0 }}</span>
          </div>
        </div>
        <button class="delete-btn" @click.stop="onDeletePost(p.id)">删除</button>
      </div>
    </div>

    <!-- 我的评论 -->
    <div v-if="activeTab === 'comments'" class="tab-panel">
      <div v-if="mine.comments.length === 0" class="empty-tip">
        <div class="empty-emoji">💬</div>
        <p>还没有发表过评论</p>
      </div>
      <div v-for="c in mine.comments" :key="c.id" class="mine-card comment-card" @click="goDetail(c.postId)">
        <div class="card-main">
          <p class="comment-content">“{{ c.content }}”</p>
          <div class="card-foot">
            <span class="post-ref">发表在《{{ c.postTitle || '帖子' }}》</span>
            <span>{{ formatRelative(c.createdAt) }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 我的收藏 -->
    <div v-if="activeTab === 'favorites'" class="tab-panel">
      <div v-if="mine.favorites.length === 0" class="empty-tip">
        <div class="empty-emoji">⭐</div>
        <p>还没有收藏帖子</p>
      </div>
      <div v-for="p in mine.favorites" :key="p.id" class="mine-card" @click="goDetail(p.id)">
        <div class="card-main">
          <div class="card-tags">
            <span class="board-tag">{{ boardEmoji(p.board) }} {{ p.boardLabel }}</span>
            <span v-if="p.isCrisis === 1" class="warm-tag">🤍 温暖陪伴中</span>
          </div>
          <h3 class="card-title">{{ p.title }}</h3>
          <p class="card-summary">{{ p.summary || '（图片分享）' }}</p>
          <div class="card-foot">
            <span>{{ p.isAnonymous === 1 ? '匿名用户' : (p.authorName || '匿名用户') }}</span>
            <span>{{ formatRelative(p.createdAt) }}</span>
            <span>❤ {{ p.likeCount || 0 }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMine, deletePost } from '@/api/community'
import { formatRelative } from '@/utils/format'

const router = useRouter()

const loading = ref(false)
const activeTab = ref('posts')
const tabs = [
  { code: 'posts', label: '📝 我的帖子' },
  { code: 'comments', label: '💬 我的评论' },
  { code: 'favorites', label: '⭐ 我的收藏' }
]

const mine = reactive({ posts: [], comments: [], favorites: [] })

const boardEmojiMap = {
  TREES: '🌳', ACADEMIC: '📖', RELATIONSHIP: '🫖',
  SLEEP: '🌙', RECOVERY: '🌿', HELP: '🙋'
}
const boardEmoji = (code) => boardEmojiMap[code] || '🌱'

const countOf = (code) => mine[code]?.length || 0

const statusMap = {
  0: { text: '正常', cls: 'st-normal' },
  1: { text: '已删除', cls: 'st-deleted' },
  2: { text: '举报待审核', cls: 'st-pending' },
  3: { text: '已下架', cls: 'st-down' },
  4: { text: '发布审核中', cls: 'st-pending' },
  5: { text: '审核未通过', cls: 'st-down' }
}
const statusText = (s) => statusMap[s]?.text || '正常'
const statusClass = (s) => statusMap[s]?.cls || 'st-normal'

const goDetail = (id) => {
  router.push(`/community/post/${id}`)
}

const onDeletePost = (id) => {
  ElMessageBox.confirm('确定删除这篇帖子吗？删除后不可恢复', '删除确认', {
    confirmButtonText: '确定删除', cancelButtonText: '再想想', type: 'warning'
  }).then(() => {
    deletePost(id).then(() => {
      ElMessage.success('帖子已删除')
      mine.posts = mine.posts.filter(p => p.id !== id)
    })
  }).catch(() => {})
}

const loadMine = () => {
  loading.value = true
  getMine().then(data => {
    mine.posts = data.posts || []
    mine.comments = data.comments || []
    mine.favorites = data.favorites || []
  }).finally(() => {
    loading.value = false
  })
}

onMounted(() => {
  loadMine()
})
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

.mine-container {
  min-height: calc(100vh - 130px);
  background: $bg-page;
  font-family: $font-rounded;
  color: $text-primary;
  padding: 24px 16px 80px;
  max-width: 820px;
  margin: 0 auto;
}

.back-link {
  display: inline-block; color: $primary-dark; text-decoration: none;
  font-size: 14px; margin-bottom: 14px;
  &:hover { text-decoration: underline; }
}

.page-header {
  text-align: center;
  margin-bottom: 20px;
  .header-tape {
    width: 120px; height: 10px;
    background: linear-gradient(90deg, transparent, rgba(232,146,124,0.5), transparent);
    border-radius: 5px; margin: 0 auto 14px;
  }
  .page-title { font-family: $font-handwritten; font-size: 28px; font-weight: 600; margin: 0 0 8px; }
  .page-subtitle { font-size: 14px; color: $text-secondary; margin: 0; }
}

.mine-tabs {
  display: flex; justify-content: center; gap: 12px; margin-bottom: 22px; flex-wrap: wrap;
  .tab-btn {
    font-family: $font-rounded; background: $bg-card; border: 1.5px dashed $border-color;
    color: $text-regular; border-radius: 22px; padding: 9px 20px; font-size: 14px;
    cursor: pointer; transition: all 0.25s ease; display: inline-flex; align-items: center; gap: 8px;
    .tab-count {
      background: rgba(0,0,0,0.06); border-radius: 10px; padding: 0 8px;
      font-size: 12px; line-height: 18px;
    }
    &:hover { border-color: $primary; color: $primary; }
    &.active {
      background: linear-gradient(135deg, $primary, $primary-dark);
      border-style: solid; color: #fff; box-shadow: $shadow-button;
      .tab-count { background: rgba(255,255,255,0.25); color: #fff; }
    }
  }
}

.tab-panel { display: flex; flex-direction: column; gap: 14px; }

.mine-card {
  display: flex; align-items: stretch; gap: 12px;
  background: $bg-card; border: 1.5px solid $border-color; border-radius: $radius-lg;
  box-shadow: $shadow-card; padding: 16px 20px; cursor: pointer;
  transition: transform 0.25s ease, box-shadow 0.25s ease;
  &:hover { transform: translateY(-2px); box-shadow: 0 8px 20px rgba(180,140,100,0.16); }
  .card-main { flex: 1; min-width: 0; }
  .card-tags { display: flex; gap: 8px; margin-bottom: 8px; flex-wrap: wrap; }
  .board-tag {
    font-size: 12px; color: $primary; background: $primary-light;
    border: 1px dashed $primary; border-radius: 10px; padding: 2px 10px;
  }
  .warm-tag {
    font-size: 12px; color: #9b7a52; background: rgba(247,232,166,0.5);
    border: 1px dashed #d9bd7a; border-radius: 10px; padding: 2px 10px;
  }
  .status-tag {
    font-size: 12px; border-radius: 10px; padding: 2px 10px; border: 1px dashed;
    &.st-normal { color: #6b8e7f; background: rgba(184,216,190,0.3); border-color: #9bc4a3; }
    &.st-pending { color: #9b7a2e; background: rgba(247,232,166,0.5); border-color: #d9bd7a; }
    &.st-down, &.st-deleted { color: #b05a4d; background: rgba(232,146,124,0.15); border-color: $primary; }
  }
  .card-title {
    font-family: $font-handwritten; font-size: 19px; margin: 0 0 6px; color: $text-primary;
    overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
  }
  .card-summary {
    font-size: 13px; color: $text-regular; margin: 0 0 10px; line-height: 1.6;
    display: -webkit-box; -webkit-line-clamp: 1; -webkit-box-orient: vertical; overflow: hidden;
  }
  .card-foot {
    display: flex; gap: 16px; font-size: 12.5px; color: $text-secondary;
    border-top: 1.5px dashed $border-color; padding-top: 8px; flex-wrap: wrap;
    .post-ref { color: $primary-dark; }
  }
  .delete-btn {
    align-self: center; background: none; border: 1.5px dashed #d9a096; color: #c0564a;
    border-radius: $radius-md; padding: 6px 14px; font-size: 13px; cursor: pointer;
    font-family: $font-rounded; white-space: nowrap; transition: all 0.2s ease;
    &:hover { background: rgba(192,86,74,0.1); border-style: solid; }
  }
}

.comment-card {
  .comment-content {
    font-size: 14px; color: $text-regular; line-height: 1.7; margin: 0 0 10px;
    overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
  }
}

.loading-tip, .empty-tip {
  text-align: center; padding: 50px 0; color: $text-secondary; font-size: 14px;
  .empty-emoji { font-size: 44px; margin-bottom: 10px; }
}
.primary-btn {
  display: inline-flex; align-items: center; justify-content: center;
  border-radius: $radius-md; padding: 9px 20px; font-size: 14px;
  text-decoration: none; background: linear-gradient(135deg, $primary, $primary-dark);
  color: #fff; box-shadow: $shadow-button; font-weight: 600;
  &.inline { margin-top: 14px; }
}
</style>
