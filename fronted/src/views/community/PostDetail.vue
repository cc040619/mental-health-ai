<template>
  <div class="detail-container" v-loading="loading">
    <router-link to="/community" class="back-link">← 返回社区</router-link>

    <template v-if="post">
      <!-- 危机干预提示卡片：语气克制不评判，仅给陪伴与热线 -->
      <div v-if="post.isCrisis === 1 && post.crisisTip" class="crisis-card">
        <div class="crisis-head">
          <span class="crisis-icon">🤍</span>
          <strong>{{ post.crisisTip.title }}</strong>
        </div>
        <p class="crisis-content">{{ post.crisisTip.content }}</p>
        <div v-if="post.crisisTip.hotline" class="crisis-hotline">
          心理援助热线：<span>{{ post.crisisTip.hotline }}</span>（24h）
        </div>
      </div>

      <!-- 帖子正文卡片 -->
      <div class="post-card">
        <div class="post-header">
          <div class="post-tags">
            <span class="board-tag">{{ boardEmoji(post.board) }} {{ post.boardLabel }}</span>
            <span v-if="post.isAnonymous === 1" class="anon-tag">🎭 匿名</span>
            <span v-if="post.status === 2" class="warn-tag">举报待审核</span>
            <span v-if="post.status === 3" class="down-tag">已下架（仅本人/管理员可见）</span>
          </div>
          <h1 class="post-title">{{ post.title }}</h1>
          <div class="author-row">
            <el-avatar :size="36" :src="resolveFileUrl(post.avatar)">{{ (post.authorName || '匿')[0] }}</el-avatar>
            <div class="author-meta">
              <span class="author-name">{{ post.isAnonymous === 1 ? '匿名用户' : (post.authorName || '匿名用户') }}</span>
              <span class="post-time">{{ formatRelative(post.createdAt) }} · 👁 {{ post.viewCount || 0 }}</span>
            </div>
          </div>
        </div>

        <!-- 正文：后端已做 HTML 白名单清洗 -->
        <div class="post-content" v-html="post.content"></div>

        <!-- 配图九宫格 -->
        <div v-if="post.images && post.images.length" class="post-images" :class="'img-col-' + Math.min(post.images.length, 3)">
          <el-image
            v-for="(img, i) in post.images" :key="i"
            :src="resolveFileUrl(img)" :preview-src-list="resolvedImages"
            :initial-index="i" fit="cover" class="post-image"
          />
        </div>

        <!-- 操作条 -->
        <div class="action-bar">
          <button class="action-btn" :class="{ active: post.isLiked }" @click="onLike">
            {{ post.isLiked ? '❤️' : '🤍' }} {{ post.likeCount || 0 }} 暖心
          </button>
          <button class="action-btn" :class="{ active: post.isFavorited }" @click="onFavorite">
            {{ post.isFavorited ? '⭐' : '☆' }} {{ post.favoriteCount || 0 }} 收藏
          </button>
          <span class="action-spacer"></span>
          <button class="ghost-btn" @click="openReport(1, post.id)">⚠ 举报</button>
          <button v-if="canDelete(post.authorId)" class="ghost-btn danger" @click="onDeletePost">删除帖子</button>
        </div>
      </div>

      <!-- 评论区 -->
      <div class="comment-section">
        <h2 class="section-title">💬 评论区 <span class="comment-count">{{ total }}</span></h2>

        <!-- 评论输入 -->
        <div class="comment-input-card">
          <el-input
            v-model="newComment" type="textarea" :rows="3"
            maxlength="1000" show-word-limit resize="none"
            placeholder="说点温暖的话… 请记得：你正在和一个真实的人说话 🌿"
          />
          <div class="input-foot">
            <label class="anon-check">
              <el-switch v-model="commentAnonymous" :active-value="1" :inactive-value="0" size="small" />
              <span>🎭 匿名评论</span>
            </label>
            <el-button type="primary" class="send-btn" :loading="commentSubmitting" @click="submitComment(null)">
              发送
            </el-button>
          </div>
        </div>

        <!-- 评论列表 -->
        <div v-if="commentsLoading" class="loading-tip">评论载入中…</div>
        <div v-else-if="comments.length === 0" class="empty-tip">
          <div class="empty-emoji">🕊</div>
          <p>还没有评论，来留下第一份温柔吧</p>
        </div>
        <div v-else class="comment-list">
          <div v-for="c in comments" :key="c.id" class="comment-item">
            <el-avatar :size="34" :src="resolveFileUrl(c.avatar)">{{ (c.authorName || '匿')[0] }}</el-avatar>
            <div class="comment-body">
              <div class="comment-head">
                <span class="comment-name">{{ c.isAnonymous === 1 ? '匿名用户' : (c.authorName || '匿名用户') }}</span>
                <span class="comment-time">{{ formatRelative(c.createdAt) }}</span>
              </div>
              <p class="comment-text">{{ c.content }}</p>
              <div class="comment-ops">
                <button class="op-btn" @click="toggleReplyBox(c.id)">↩ 回复</button>
                <button class="op-btn" @click="openReport(2, c.id)">⚠ 举报</button>
                <button v-if="canDelete(c.authorId)" class="op-btn danger" @click="onDeleteComment(c.id)">删除</button>
              </div>

              <!-- 楼中楼回复 -->
              <div v-if="c.replies && c.replies.length" class="reply-list">
                <div v-for="r in c.replies" :key="r.id" class="reply-item">
                  <el-avatar :size="26" :src="resolveFileUrl(r.avatar)">{{ (r.authorName || '匿')[0] }}</el-avatar>
                  <div class="reply-body">
                    <div class="comment-head">
                      <span class="comment-name">{{ r.isAnonymous === 1 ? '匿名用户' : (r.authorName || '匿名用户') }}</span>
                      <span class="comment-time">{{ formatRelative(r.createdAt) }}</span>
                    </div>
                    <p class="comment-text">{{ r.content }}</p>
                    <div class="comment-ops">
                      <button class="op-btn" @click="toggleReplyBox(c.id)">↩ 回复</button>
                      <button class="op-btn" @click="openReport(2, r.id)">⚠ 举报</button>
                      <button v-if="canDelete(r.authorId)" class="op-btn danger" @click="onDeleteComment(r.id)">删除</button>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 行内回复输入框 -->
              <div v-if="replyParent === c.id" class="reply-input">
                <el-input
                  v-model="replyContent" type="textarea" :rows="2"
                  maxlength="1000" resize="none"
                  placeholder="友善回复，温暖彼此…"
                />
                <div class="reply-input-foot">
                  <el-button size="small" @click="replyParent = null">取消</el-button>
                  <el-button size="small" type="primary" :loading="commentSubmitting"
                             @click="submitComment(c.id)">回复</el-button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div v-if="total > commentSize" class="pager">
          <el-pagination background layout="prev, pager, next"
            :current-page="commentPage" :page-size="commentSize" :total="total"
            @current-change="onCommentPageChange" />
        </div>
      </div>
    </template>

    <!-- 举报弹窗 -->
    <el-dialog v-model="reportVisible" title="举报内容" width="440px" class="report-dialog">
      <p class="report-tip">如果这条内容让你感到不适或违反社区规则，请告诉我们，管理员会尽快处理。</p>
      <el-input v-model="reportReason" type="textarea" :rows="4" maxlength="300" show-word-limit
                placeholder="请描述举报原因（必填）…" />
      <template #footer>
        <el-button @click="reportVisible = false">取消</el-button>
        <el-button type="primary" class="send-btn" :loading="reportSubmitting" @click="submitReport">提交举报</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getPostDetail, getComments, createComment, deleteCommentApi,
  deletePost, togglePostLike, togglePostFavorite, reportTarget
} from '@/api/community'
import { resolveFileUrl } from '@/config/index.js'
import { formatRelative } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const postId = computed(() => route.params.id)

const loading = ref(true)
const post = ref(null)

const comments = ref([])
const commentsLoading = ref(false)
const commentPage = ref(1)
const commentSize = 10
const total = ref(0)

const newComment = ref('')
const commentAnonymous = ref(0)
const commentSubmitting = ref(false)

const replyParent = ref(null)
const replyContent = ref('')

// 举报弹窗
const reportVisible = ref(false)
const reportTargetType = ref(1)
const reportTargetId = ref(null)
const reportReason = ref('')
const reportSubmitting = ref(false)

const currentUser = JSON.parse(localStorage.getItem('userInfo') || 'null')
const isAdmin = currentUser?.userType === 2
// 删除权限：作者本人或管理员可见删除按钮；后端接口会再次校验
const canDelete = (authorId) => isAdmin || currentUser?.id === authorId

const resolvedImages = computed(() =>
  (post.value?.images || []).map(img => resolveFileUrl(img))
)

const boardEmojiMap = {
  TREES: '🌳', ACADEMIC: '📖', RELATIONSHIP: '🫖',
  SLEEP: '🌙', RECOVERY: '🌿', HELP: '🙋'
}
const boardEmoji = (code) => boardEmojiMap[code] || '🌱'

const loadPost = () => {
  loading.value = true
  getPostDetail(postId.value).then(data => {
    post.value = data
  }).catch(() => {
    post.value = null
  }).finally(() => {
    loading.value = false
  })
}

const loadComments = () => {
  commentsLoading.value = true
  getComments(postId.value, { page: commentPage.value, size: commentSize }).then(data => {
    comments.value = data.records || []
    total.value = data.total || 0
  }).finally(() => {
    commentsLoading.value = false
  })
}

const onLike = () => {
  togglePostLike(postId.value).then(liked => {
    post.value.isLiked = liked
    post.value.likeCount = Math.max(0, (post.value.likeCount || 0) + (liked ? 1 : -1))
  })
}

const onFavorite = () => {
  togglePostFavorite(postId.value).then(favorited => {
    post.value.isFavorited = favorited
    post.value.favoriteCount = Math.max(0, (post.value.favoriteCount || 0) + (favorited ? 1 : -1))
    ElMessage.success(favorited ? '已收藏 ⭐' : '已取消收藏')
  })
}

const onDeletePost = () => {
  ElMessageBox.confirm('确定删除这篇帖子吗？删除后不可恢复', '删除确认', {
    confirmButtonText: '确定删除', cancelButtonText: '再想想', type: 'warning'
  }).then(() => {
    deletePost(postId.value).then(() => {
      ElMessage.success('帖子已删除')
      router.replace('/community')
    })
  }).catch(() => {})
}

const toggleReplyBox = (topId) => {
  replyParent.value = replyParent.value === topId ? null : topId
  replyContent.value = ''
}

const submitComment = (parentId) => {
  const content = (parentId ? replyContent.value : newComment.value).trim()
  if (!content) {
    ElMessage.warning('评论内容不能为空')
    return
  }
  commentSubmitting.value = true
  createComment(postId.value, { content, parentId, isAnonymous: commentAnonymous.value })
    .then(data => {
      ElMessage.success('评论已发送 🌿')
      if (parentId) {
        replyParent.value = null
        replyContent.value = ''
      } else {
        newComment.value = ''
      }
      // 命中危机词：返回干预提示，弹窗温柔展示
      if (data.crisisTip) {
        ElMessageBox.alert(data.crisisTip.content + (data.crisisTip.hotline ? `\n心理援助热线：${data.crisisTip.hotline}` : ''),
          data.crisisTip.title || '我们一直在这里 🤍', { confirmButtonText: '我知道了' })
      }
      commentPage.value = 1
      loadComments()
    }).finally(() => {
      commentSubmitting.value = false
    })
}

const onDeleteComment = (commentId) => {
  ElMessageBox.confirm('确定删除这条评论吗？', '删除确认', {
    confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning'
  }).then(() => {
    deleteCommentApi(commentId).then(() => {
      ElMessage.success('评论已删除')
      loadComments()
    })
  }).catch(() => {})
}

const onCommentPageChange = (p) => {
  commentPage.value = p
  loadComments()
}

const openReport = (type, id) => {
  reportTargetType.value = type
  reportTargetId.value = id
  reportReason.value = ''
  reportVisible.value = true
}

const submitReport = () => {
  if (!reportReason.value.trim()) {
    ElMessage.warning('请填写举报原因')
    return
  }
  reportSubmitting.value = true
  reportTarget({
    targetType: reportTargetType.value,
    targetId: reportTargetId.value,
    reason: reportReason.value.trim()
  }).then(() => {
    ElMessage.success('举报已提交，管理员会尽快处理')
    reportVisible.value = false
  }).finally(() => {
    reportSubmitting.value = false
  })
}

onMounted(() => {
  loadPost()
  loadComments()
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

.detail-container {
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

// 危机干预卡片
.crisis-card {
  background: linear-gradient(135deg, #FDF0E8, #FBEBD9);
  border: 1.5px solid $primary; border-radius: $radius-lg;
  padding: 18px 22px; margin-bottom: 18px; box-shadow: $shadow-card;
  .crisis-head { display: flex; align-items: center; gap: 8px;
    strong { color: $primary-darker; font-size: 16px; }
    .crisis-icon { font-size: 22px; }
  }
  .crisis-content { margin: 10px 0 8px; font-size: 14px; line-height: 1.8; color: $text-regular; }
  .crisis-hotline {
    font-size: 13.5px; color: $primary-darker; font-weight: 700;
    span { font-size: 16px; letter-spacing: 1px; }
  }
}

// 帖子卡片
.post-card {
  background: $bg-card; border: 1.5px solid $border-color; border-radius: $radius-lg;
  box-shadow: $shadow-card; padding: 26px 28px; margin-bottom: 20px;
}
.post-header { margin-bottom: 18px; }
.post-tags { display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 12px; }
.board-tag {
  font-size: 12px; color: $primary; background: $primary-light;
  border: 1px dashed $primary; border-radius: 10px; padding: 2px 10px;
}
.anon-tag, .warn-tag, .down-tag {
  font-size: 12px; border-radius: 10px; padding: 2px 10px; border: 1px dashed;
}
.anon-tag { color: #8b7d6b; background: rgba(232,220,200,0.4); border-color: $border-color; }
.warn-tag { color: #9b7a2e; background: rgba(247,232,166,0.5); border-color: #d9bd7a; }
.down-tag { color: #b05a4d; background: rgba(232,146,124,0.15); border-color: $primary; }

.post-title {
  font-family: $font-handwritten; font-size: 27px; line-height: 1.4;
  margin: 0 0 16px; color: $text-primary;
}
.author-row { display: flex; align-items: center; gap: 10px; }
.author-meta { display: flex; flex-direction: column; gap: 2px; }
.author-name { font-size: 14px; font-weight: 700; color: $text-primary; }
.post-time { font-size: 12.5px; color: $text-secondary; }

// 正文排版（v-html，后端白名单清洗）
.post-content {
  font-size: 15px; line-height: 1.9; color: $text-regular;
  :deep(p) { margin: 0 0 12px; }
  :deep(img) { max-width: 100%; border-radius: $radius-md; margin: 8px 0; }
  :deep(blockquote) {
    margin: 12px 0; padding: 10px 16px; border-left: 4px solid $primary;
    background: $primary-light; border-radius: 0 $radius-md $radius-md 0; color: $text-regular;
  }
  :deep(a) { color: $primary-dark; text-decoration: underline; }
  :deep(h1), :deep(h2), :deep(h3) { font-family: $font-handwritten; color: $text-primary; }
  :deep(ul), :deep(ol) { padding-left: 24px; margin: 10px 0; }
}

.post-images {
  display: grid; gap: 8px; margin: 16px 0 4px;
  &.img-col-1 { grid-template-columns: minmax(0, 60%); }
  &.img-col-2 { grid-template-columns: repeat(2, 1fr); }
  &.img-col-3 { grid-template-columns: repeat(3, 1fr); }
  .post-image {
    width: 100%; aspect-ratio: 1; border-radius: $radius-md; overflow: hidden;
    border: 1px solid $border-color; cursor: zoom-in;
  }
}

// 操作条
.action-bar {
  display: flex; align-items: center; gap: 12px;
  margin-top: 20px; padding-top: 16px; border-top: 2px dashed $border-color;
  .action-spacer { flex: 1; }
  .action-btn {
    font-family: $font-rounded; font-size: 14px; border-radius: 20px;
    padding: 8px 18px; border: 1.5px dashed $primary; background: $bg-card;
    color: $primary-dark; cursor: pointer; transition: all 0.25s ease;
    &:hover { background: $primary-light; }
    &.active { background: linear-gradient(135deg, $primary, $primary-dark); color: #fff; border-style: solid; }
  }
  .ghost-btn {
    background: none; border: none; font-size: 13px; color: $text-secondary;
    cursor: pointer; padding: 6px 10px; border-radius: 8px; font-family: $font-rounded;
    &:hover { background: rgba(0,0,0,0.04); color: $text-primary; }
    &.danger:hover { color: #c0564a; }
  }
}

// 评论区
.comment-section {
  background: $bg-card; border: 1.5px solid $border-color; border-radius: $radius-lg;
  box-shadow: $shadow-card; padding: 22px 24px;
}
.section-title {
  font-family: $font-handwritten; font-size: 21px; margin: 0 0 16px; color: $text-primary;
  .comment-count { font-size: 14px; color: $primary; font-family: $font-rounded; }
}
.comment-input-card {
  background: #fff; border: 1.5px solid $border-color; border-radius: $radius-md;
  padding: 12px; margin-bottom: 20px;
  :deep(.el-textarea__inner) { box-shadow: none; border: none; padding: 4px; font-family: $font-rounded; }
  .input-foot {
    display: flex; align-items: center; justify-content: space-between; margin-top: 10px;
    .anon-check { display: flex; align-items: center; gap: 8px; font-size: 13px; color: $text-secondary;
      :deep(.el-switch.is-checked .el-switch__core) { background-color: $primary; border-color: $primary; }
    }
  }
}
.send-btn {
  background: linear-gradient(135deg, $primary, $primary-dark); border: none;
  border-radius: $radius-md; font-weight: 700; box-shadow: $shadow-button;
}

.comment-list { display: flex; flex-direction: column; gap: 18px; }
.comment-item { display: flex; gap: 12px; }
.comment-body { flex: 1; min-width: 0; }
.comment-head { display: flex; align-items: baseline; gap: 10px; }
.comment-name { font-size: 14px; font-weight: 700; color: $text-primary; }
.comment-time { font-size: 12px; color: $text-secondary; }
.comment-text {
  margin: 6px 0; font-size: 14.5px; line-height: 1.75; color: $text-regular;
  white-space: pre-wrap; word-break: break-word;
}
.comment-ops { display: flex; gap: 12px; }
.op-btn {
  background: none; border: none; font-size: 12.5px; color: $text-secondary;
  cursor: pointer; padding: 2px 4px; font-family: $font-rounded;
  &:hover { color: $primary; }
  &.danger:hover { color: #c0564a; }
}

.reply-list {
  margin-top: 12px; background: rgba(232,220,200,0.25); border-radius: $radius-md;
  padding: 12px 14px; display: flex; flex-direction: column; gap: 14px;
  .reply-item { display: flex; gap: 10px; }
  .reply-body { flex: 1; min-width: 0; }
}
.reply-input {
  margin-top: 10px; background: #fff; border: 1.5px dashed $border-color;
  border-radius: $radius-md; padding: 10px;
  :deep(.el-textarea__inner) { box-shadow: none; border: none; padding: 4px; }
  .reply-input-foot { display: flex; justify-content: flex-end; gap: 8px; margin-top: 8px; }
}

.loading-tip, .empty-tip {
  text-align: center; padding: 40px 0; color: $text-secondary; font-size: 14px;
  .empty-emoji { font-size: 40px; margin-bottom: 10px; }
}
.pager {
  display: flex; justify-content: center; margin-top: 22px;
  :deep(.el-pagination.is-background .el-pager li.is-active) { background: $primary; color: #fff; }
}

.report-dialog {
  font-family: $font-rounded;
  .report-tip { font-size: 13px; color: $text-secondary; line-height: 1.7; margin: 0 0 14px; }
}
</style>
