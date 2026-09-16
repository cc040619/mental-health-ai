<template>
  <div class="community-container">
    <!-- 便签标题 -->
    <div class="page-header">
      <div class="header-tape"></div>
      <h1 class="page-title">🌱 暖心社区</h1>
      <p class="page-subtitle">把心事轻轻放在这里，我们彼此陪伴 ✨</p>
    </div>

    <!-- 危机干预引导位（本人发过危机帖时展示，可关闭） -->
    <div v-if="crisisNotice.hasCrisis && showCrisisBanner" class="crisis-banner">
      <div class="crisis-icon">🤍</div>
      <div class="crisis-text">
        <strong>{{ crisisNotice.tip.title }}</strong>
        <p>{{ crisisNotice.tip.content }}</p>
      </div>
      <el-button link class="crisis-close" @click="showCrisisBanner = false">×</el-button>
    </div>

    <!-- 搜索 + 发布操作行 -->
    <div class="action-row">
      <div class="search-box">
        <el-input
          v-model="keyword"
          placeholder="搜索大家的故事…"
          clearable
          class="search-input"
          @keyup.enter="onSearch"
          @clear="onClearSearch"
        >
          <template #append>
            <el-button @click="onSearch">🔍 搜索</el-button>
          </template>
        </el-input>
      </div>
      <div class="action-btns">
        <router-link to="/community/mine" class="soft-btn">📒 我的社区</router-link>
        <router-link to="/community/publish" class="primary-btn">✏️ 发布心情</router-link>
      </div>
    </div>

    <!-- 板块筛选 -->
    <div class="board-tabs">
      <button class="chip-btn" :class="{ active: activeBoard === '' }" @click="switchBoard('')">
        <span>🌈</span><span>全部</span>
      </button>
      <button v-for="b in boards" :key="b.code"
              class="chip-btn" :class="{ active: activeBoard === b.code }"
              @click="switchBoard(b.code)">
        <span>{{ boardEmoji(b.code) }}</span><span>{{ b.label }}</span>
      </button>
    </div>

    <!-- 排序 Tab -->
    <div class="sort-tabs">
      <button v-for="t in tabs" :key="t.code"
              class="sort-btn" :class="{ active: activeTab === t.code }"
              @click="switchTab(t.code)" :disabled="searching">
        {{ t.label }}
      </button>
      <span v-if="searching" class="search-hint">
        「{{ keyword }}」的搜索结果
        <el-button link type="primary" @click="onClearSearch">返回信息流</el-button>
      </span>
    </div>

    <!-- 帖子卡片流 -->
    <div v-if="loading" class="loading-tip">载入中... 🌿</div>
    <div v-else-if="postList.length === 0" class="empty-tip">
      <div class="empty-emoji">📭</div>
      <p>这里还安安静静的，来写下第一份心情吧</p>
      <router-link to="/community/publish" class="primary-btn inline">✏️ 立即发布</router-link>
    </div>
    <div v-else class="post-list">
      <div v-for="post in postList" :key="post.id" class="post-card" @click="goDetail(post.id)">
        <div class="card-topline"></div>
        <div class="post-main">
          <div class="post-text">
            <div class="post-tags">
              <span v-if="post.isTop === 1" class="top-tag">📌 置顶</span>
              <span v-if="post.isFeatured === 1" class="featured-tag">⭐ 精选</span>
              <span class="board-tag">{{ boardEmoji(post.board) }} {{ post.boardLabel }}</span>
              <span v-if="post.isCrisis === 1" class="warm-tag">🤍 温暖陪伴中</span>
            </div>
            <h3 class="post-title">{{ post.title }}</h3>
            <p class="post-summary">{{ post.summary || '（图片分享）' }}</p>
            <div class="post-foot">
              <div class="author">
                <el-avatar :size="24" :src="resolveFileUrl(post.avatar)">{{ (post.authorName || '匿')[0] }}</el-avatar>
                <span class="author-name">{{ post.isAnonymous === 1 ? '匿名用户' : (post.authorName || '匿名用户') }}</span>
                <span class="dot">·</span>
                <span class="time">{{ formatRelative(post.createdAt) }}</span>
              </div>
              <div class="stats">
                <span>👁 {{ post.viewCount || 0 }}</span>
                <span>💬 {{ post.commentCount || 0 }}</span>
                <span :class="{ liked: post.isLiked }">❤ {{ post.likeCount || 0 }}</span>
              </div>
            </div>
          </div>
          <div v-if="post.images && post.images.length" class="post-thumb">
            <img :src="resolveFileUrl(post.images[0])" alt="配图" />
            <span v-if="post.images.length > 1" class="img-count">🖼 ×{{ post.images.length }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 分页 -->
    <div v-if="total > pageSize" class="pager">
      <el-pagination background layout="prev, pager, next"
        :current-page="page" :page-size="pageSize" :total="total"
        @current-change="onPageChange" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getPostList, getBoards, searchPosts, getCrisisNotice } from '@/api/community'
import { resolveFileUrl } from '@/config/index.js'
import { formatRelative } from '@/utils/format'

const router = useRouter()

const boards = ref([])
const postList = ref([])
const loading = ref(false)
const page = ref(1)
const pageSize = 10
const total = ref(0)

const activeBoard = ref('')
const activeTab = ref('latest')
const tabs = [
  { code: 'recommend', label: '💡 推荐' },
  { code: 'latest', label: '🕘 最新' },
  { code: 'hot', label: '🔥 热门' }
]

// 搜索态
const keyword = ref('')
const searching = ref(false)

// 危机引导位
const crisisNotice = ref({ hasCrisis: false, tip: {} })
const showCrisisBanner = ref(true)

const boardEmojiMap = {
  TREES: '🌳', ACADEMIC: '📖', RELATIONSHIP: '🫖',
  SLEEP: '🌙', RECOVERY: '🌿', HELP: '🙋'
}
const boardEmoji = (code) => boardEmojiMap[code] || '🌱'

const loadPosts = () => {
  loading.value = true
  const params = { page: page.value, size: pageSize }
  const request = searching.value
    ? searchPosts({ ...params, keyword: keyword.value })
    : getPostList({ ...params, board: activeBoard.value || undefined, tab: activeTab.value })
  request.then(data => {
    postList.value = data.records || []
    total.value = data.total || 0
  }).finally(() => {
    loading.value = false
  })
}

const switchBoard = (code) => {
  activeBoard.value = code
  page.value = 1
  loadPosts()
}

const switchTab = (code) => {
  activeTab.value = code
  page.value = 1
  loadPosts()
}

const onSearch = () => {
  const kw = keyword.value.trim()
  if (!kw) {
    onClearSearch()
    return
  }
  searching.value = true
  page.value = 1
  loadPosts()
}

const onClearSearch = () => {
  keyword.value = ''
  searching.value = false
  page.value = 1
  loadPosts()
}

const onPageChange = (p) => {
  page.value = p
  loadPosts()
}

const goDetail = (id) => {
  router.push(`/community/post/${id}`)
}

onMounted(() => {
  getBoards().then(data => { boards.value = data || [] })
  getCrisisNotice().then(data => {
    crisisNotice.value = data || { hasCrisis: false, tip: {} }
  }).catch(() => {})
  loadPosts()
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

.community-container {
  min-height: calc(100vh - 130px);
  background: $bg-page;
  font-family: $font-rounded;
  color: $text-primary;
  padding: 24px 16px 80px;
  max-width: 900px;
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

// 危机引导条
.crisis-banner {
  display: flex; align-items: center; gap: 14px;
  background: linear-gradient(135deg, #FDF0E8, #FBEBD9);
  border: 1.5px solid $primary;
  border-radius: $radius-md;
  padding: 14px 18px;
  margin-bottom: 18px;
  box-shadow: $shadow-card;
  .crisis-icon { font-size: 28px; }
  .crisis-text {
    flex: 1;
    strong { color: $primary-darker; font-size: 15px; }
    p { margin: 4px 0 0; font-size: 13px; color: $text-regular; line-height: 1.6; }
  }
  .crisis-close { font-size: 20px; color: $text-secondary; }
}

// 操作行
.action-row {
  display: flex; gap: 14px; align-items: center; margin-bottom: 18px; flex-wrap: wrap;
  .search-box { flex: 1; min-width: 240px; }
  .action-btns { display: flex; gap: 10px; }
  :deep(.el-input__wrapper) {
    border-radius: $radius-md; box-shadow: 0 0 0 1.5px $border-color inset; background: $bg-card;
  }
  :deep(.el-input-group__append) {
    background: $primary-light; border-radius: 0 $radius-md $radius-md 0;
    .el-button { color: $primary-dark; font-weight: 600; }
  }
}

.soft-btn, .primary-btn {
  display: inline-flex; align-items: center; justify-content: center;
  border-radius: $radius-md; padding: 9px 18px; font-size: 14px;
  text-decoration: none; white-space: nowrap; transition: all 0.25s ease;
}
.soft-btn {
  background: $bg-card; border: 1.5px dashed $primary; color: $primary-dark;
  &:hover { background: $primary-light; border-style: solid; }
}
.primary-btn {
  background: linear-gradient(135deg, $primary, $primary-dark);
  color: #fff; border: none; box-shadow: $shadow-button; font-weight: 600;
  &:hover { transform: translateY(-1px); }
  &.inline { margin-top: 14px; }
}

// 板块 chips
.board-tabs {
  display: flex; flex-wrap: wrap; gap: 10px; margin-bottom: 14px;
  .chip-btn {
    font-family: $font-rounded; background: $bg-card; border: 1.5px dashed $border-color;
    color: $text-regular; border-radius: 20px; padding: 7px 16px; font-size: 13.5px;
    cursor: pointer; transition: all 0.25s ease; display: inline-flex; align-items: center; gap: 5px;
    &:hover { border-color: $primary; color: $primary; transform: translateY(-1px); }
    &.active {
      background: linear-gradient(135deg, $primary, $primary-dark);
      border-style: solid; color: #fff; box-shadow: $shadow-button;
    }
  }
}

// 排序 tab
.sort-tabs {
  display: flex; align-items: center; gap: 16px; margin-bottom: 18px;
  border-bottom: 2px dashed $border-color; padding-bottom: 10px;
  .sort-btn {
    background: none; border: none; font-family: $font-rounded; font-size: 15px;
    color: $text-secondary; cursor: pointer; padding: 4px 6px; position: relative;
    transition: color 0.2s ease;
    &.active { color: $primary-dark; font-weight: 700; }
    &.active::after {
      content: ''; position: absolute; bottom: -12px; left: 20%; width: 60%;
      height: 3px; background: $primary; border-radius: 2px;
    }
    &:disabled { opacity: 0.5; cursor: not-allowed; }
  }
  .search-hint { font-size: 13px; color: $primary-dark; }
}

// 帖子卡片
.post-list { display: flex; flex-direction: column; gap: 16px; }
.post-card {
  position: relative; background: $bg-card; border-radius: $radius-lg;
  border: 1.5px solid $border-color; box-shadow: $shadow-card;
  padding: 18px 20px; cursor: pointer; overflow: hidden;
  transition: transform 0.25s ease, box-shadow 0.25s ease;
  &:hover { transform: translateY(-2px); box-shadow: 0 8px 24px rgba(180,140,100,0.18); }
  .card-topline {
    position: absolute; top: 0; left: 0; width: 6px; height: 100%;
    background: linear-gradient(180deg, $primary, #F7E8A6); opacity: 0.65;
  }
  .post-main { display: flex; gap: 18px; }
  .post-text { flex: 1; min-width: 0; }
  .post-tags { display: flex; gap: 8px; margin-bottom: 8px; }
  .board-tag {
    font-size: 12px; color: $primary; background: $primary-light;
    border: 1px dashed $primary; border-radius: 10px; padding: 2px 10px;
  }
  .top-tag {
    font-size: 12px; color: #fff; background: linear-gradient(135deg, $primary, $primary-dark);
    border-radius: 10px; padding: 2px 10px; font-weight: 600;
  }
  .featured-tag {
    font-size: 12px; color: #9b7a52; background: rgba(247,232,166,0.7);
    border: 1px solid #d9bd7a; border-radius: 10px; padding: 2px 10px;
  }
  .warm-tag {
    font-size: 12px; color: #9b7a52; background: rgba(247,232,166,0.5);
    border: 1px dashed #d9bd7a; border-radius: 10px; padding: 2px 10px;
  }
  .post-title {
    font-family: $font-handwritten; font-size: 20px; color: $text-primary;
    margin: 0 0 8px; line-height: 1.4;
    overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
  }
  .post-summary {
    font-size: 13.5px; color: $text-regular; line-height: 1.7; margin: 0 0 14px;
    display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
  }
  .post-foot {
    display: flex; align-items: center; justify-content: space-between;
    border-top: 1.5px dashed $border-color; padding-top: 10px;
    .author { display: flex; align-items: center; gap: 6px; font-size: 12.5px; color: $text-secondary;
      .dot { color: $border-color; }
    }
    .stats { display: flex; gap: 14px; font-size: 12.5px; color: $text-secondary;
      .liked { color: $primary; font-weight: 700; }
    }
  }
  .post-thumb {
    position: relative; width: 110px; height: 110px; flex-shrink: 0; border-radius: $radius-md;
    overflow: hidden; background: rgba(232,146,124,0.1);
    img { width: 100%; height: 100%; object-fit: cover; }
    .img-count {
      position: absolute; right: 6px; bottom: 6px; font-size: 11px;
      background: rgba(0,0,0,0.45); color: #fff; border-radius: 8px; padding: 1px 7px;
    }
  }
}

.loading-tip, .empty-tip {
  text-align: center; padding: 60px 0; color: $text-secondary; font-size: 15px;
  .empty-emoji { font-size: 48px; margin-bottom: 12px; }
}

.pager {
  display: flex; justify-content: center; margin-top: 28px;
  :deep(.el-pagination.is-background .el-pager li.is-active) { background: $primary; color: #fff; }
}
</style>
