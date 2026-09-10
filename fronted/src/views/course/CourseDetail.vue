<template>
    <div class="detail-container" v-if="course">
        <!-- 返回 -->
        <div class="back-bar">
            <router-link to="/course" class="back-link">← 返回课程广场</router-link>
        </div>

        <!-- 课程介绍便签卡 -->
        <div class="course-card">
            <div class="card-tape"></div>
            <div class="cover-area">
                <img v-if="course.coverUrl" :src="course.coverUrl" alt="封面" class="cover-img" />
                <span v-else class="cover-emoji">{{ categoryEmoji }}</span>
            </div>
            <div class="course-info">
                <div class="cat-tag">{{ course.categoryLabel }}</div>
                <h1 class="course-title">{{ course.title }}</h1>
                <div class="course-meta">
                    <span>⏱️ {{ course.totalDurationText }}</span>
                    <span class="meta-dot">·</span>
                    <span>👥 {{ course.learnCount }} 人在学</span>
                    <span class="meta-dot">·</span>
                    <span>💬 {{ course.commentCount }} 条评论</span>
                </div>
                <p class="course-desc">{{ course.description }}</p>

                <!-- 完成度进度条 -->
                <div class="completion-box">
                    <div class="completion-head">
                        <span class="completion-label">📊 我的学习进度</span>
                        <span class="completion-num">{{ course.completion }}%</span>
                    </div>
                    <div class="progress-track">
                        <div class="progress-fill" :style="{ width: (course.completion || 0) + '%' }"></div>
                    </div>
                    <div class="action-row">
                        <button class="primary-btn" @click="continueLearn">{{ continueLabel }}</button>
                        <button class="fav-btn" :class="{ favorited: course.isFavorite }" @click="onToggleFav">
                            <span>{{ course.isFavorite ? '💛 已收藏' : '🤍 收藏' }}</span>
                        </button>
                    </div>
                </div>
            </div>
        </div>

        <!-- 章节列表 -->
        <div class="section-block">
            <h2 class="section-title">🎵 课程章节</h2>
            <div class="chapter-list">
                <div v-for="(ch, idx) in course.chapters" :key="ch.id"
                     class="chapter-item" :class="{ finished: ch.isFinished, current: isCurrentPlaying(ch.id) }"
                     @click="playChapter(idx)">
                    <div class="ch-index">{{ idx + 1 }}</div>
                    <div class="ch-main">
                        <div class="ch-title">{{ ch.title }}</div>
                        <div class="ch-sub">
                            <span>⏱️ {{ ch.durationText }}</span>
                            <span v-if="ch.isFinished" class="status-tag done">✓ 已完成</span>
                            <span v-else-if="(ch.progressSeconds || 0) > 0" class="status-tag resume">继续学习</span>
                            <span v-else class="status-tag new">未学</span>
                        </div>
                    </div>
                    <div class="ch-play">▶</div>
                </div>
            </div>
        </div>

        <!-- 评论区 -->
        <div class="section-block">
            <h2 class="section-title">💬 课程评论</h2>
            <div class="comment-input">
                <textarea v-model="commentText" class="comment-textarea"
                          placeholder="留下你的学习感受吧～（≤300字）" maxlength="300"
                          rows="3"></textarea>
                <div class="comment-submit-row">
                    <span class="char-count">{{ commentText.length }}/300</span>
                    <button class="primary-btn sm" :disabled="!commentText.trim() || submitting" @click="submitComment">
                        {{ submitting ? '发送中...' : '发表评论' }}
                    </button>
                </div>
            </div>

            <div v-if="comments.length === 0" class="empty-comment">还没有评论，来当第一个吧 🌱</div>
            <div v-else class="comment-list">
                <div v-for="c in comments" :key="c.id" class="comment-item">
                    <div class="c-avatar">
                        <img v-if="c.avatar" :src="c.avatar" alt="头像" />
                        <span v-else class="avatar-emoji">🌿</span>
                    </div>
                    <div class="c-body">
                        <div class="c-head">
                            <span class="c-name">{{ c.nickname || '匿名同学' }}</span>
                            <span v-if="c.isMine" class="c-mine-tag">我</span>
                            <span class="c-time">{{ formatTime(c.createdAt) }}</span>
                        </div>
                        <p class="c-content">{{ c.content }}</p>
                    </div>
                    <button v-if="c.isMine" class="c-del" @click="onDelete(c.id)">删除</button>
                </div>
            </div>

            <div v-if="commentTotal > commentPageSize" class="pager">
                <el-pagination background layout="prev, pager, next"
                    :current-page="commentPage" :page-size="commentPageSize" :total="commentTotal"
                    @current-change="onCommentPage" />
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCourseDetail, toggleFavorite, getCourseComments, createComment, deleteComment } from '@/api/course'
import { usePlayerStore } from '@/stores/player'

const route = useRoute()
const player = usePlayerStore()

const course = ref(null)
const commentText = ref('')
const submitting = ref(false)
const comments = ref([])
const commentPage = ref(1)
const commentPageSize = 10
const commentTotal = ref(0)

const emojiMap = { MEDITATION: '🧘', SLEEP: '🌙', EMOTION: '💙', CBT: '🧠', INTERPERSONAL: '🤝' }
const categoryEmoji = computed(() => emojiMap[course.value?.category] || '🎧')

const continueLabel = computed(() => {
    if (!course.value || !course.value.chapters) return '开始学习'
    const idx = course.value.chapters.findIndex(ch => !ch.isFinished)
    if (idx === -1) return '重新学习'
    if (idx === 0 && !course.value.chapters[0].progressSeconds) return '开始学习'
    return '继续学习'
})

const formatTime = (t) => {
    if (!t) return ''
    return t.replace('T', ' ').slice(0, 16)
}

const loadDetail = () => {
    getCourseDetail(route.params.id).then(data => {
        course.value = data
    })
}

const loadComments = () => {
    getCourseComments(route.params.id, { page: commentPage.value, size: commentPageSize }).then(data => {
        comments.value = data.records || []
        commentTotal.value = data.total || 0
    })
}

const onCommentPage = (p) => {
    commentPage.value = p
    loadComments()
}

// 播放某章 → 触发全局播放器（chapters 共享引用，便于回写 isFinished）
const playChapter = (index) => {
    if (!course.value) return
    player.play({
        course: course.value,
        chapters: course.value.chapters,
        startIndex: index
    })
}

const continueLearn = () => {
    if (!course.value) return
    const idx = course.value.chapters.findIndex(ch => !ch.isFinished)
    playChapter(idx === -1 ? 0 : idx)
}

const isCurrentPlaying = (chapterId) => {
    return player.visible && player.currentChapter?.id === chapterId
}

const onToggleFav = () => {
    toggleFavorite(route.params.id).then(isFav => {
        course.value.isFavorite = isFav
        ElMessage.success(isFav ? '已收藏' : '已取消收藏')
    })
}

const submitComment = () => {
    if (!commentText.value.trim()) return
    submitting.value = true
    createComment(route.params.id, { content: commentText.value.trim() }).then(c => {
        comments.value.unshift(c)
        commentTotal.value += 1
        commentText.value = ''
        if (course.value) course.value.commentCount = (course.value.commentCount || 0) + 1
        ElMessage.success('评论发表成功')
    }).finally(() => {
        submitting.value = false
    })
}

const onDelete = (id) => {
    ElMessageBox.confirm('确定删除这条评论吗？', '删除评论', {
        confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning'
    }).then(() => {
        deleteComment(id).then(() => {
            comments.value = comments.value.filter(c => c.id !== id)
            commentTotal.value = Math.max(0, commentTotal.value - 1)
            if (course.value) course.value.commentCount = Math.max(0, (course.value.commentCount || 0) - 1)
            ElMessage.success('已删除')
        })
    }).catch(() => {})
}

// 监听播放器进度上报事件 → 更新完成度（章节 isFinished 已通过共享引用回写）
const onProgressUpdated = (e) => {
    if (course.value && typeof e.detail?.completion === 'number') {
        course.value.completion = e.detail.completion
    }
}

onMounted(() => {
    loadDetail()
    loadComments()
    window.addEventListener('course-progress-updated', onProgressUpdated)
})

onBeforeUnmount(() => {
    window.removeEventListener('course-progress-updated', onProgressUpdated)
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
    background: $bg-page; font-family: $font-rounded; color: $text-primary;
    padding: 16px 16px 100px; max-width: 960px; margin: 0 auto;
}

.back-bar { margin-bottom: 12px;
    .back-link {
        font-family: $font-rounded; color: $primary; text-decoration: none; font-size: 14px;
        border-bottom: 1px dashed $primary; padding-bottom: 2px;
        &:hover { color: $primary-dark; }
    }
}

.course-card {
    position: relative; background: $bg-card; border-radius: $radius-lg;
    border: 1.5px solid $border-color; box-shadow: $shadow-card;
    overflow: hidden; margin-bottom: 28px;
    .card-tape {
        position: absolute; top: -6px; left: 50%; transform: translateX(-50%) rotate(-1deg);
        width: 100px; height: 16px;
        background: linear-gradient(90deg, rgba(247,232,166,0.6), rgba(232,146,124,0.5), rgba(247,232,166,0.6));
        border-radius: 3px; z-index: 2;
    }
    display: flex; flex-wrap: wrap;
    .cover-area {
        flex: 0 0 200px; height: 200px;
        background: linear-gradient(135deg, rgba(232,146,124,0.18), rgba(247,232,166,0.25));
        display: flex; align-items: center; justify-content: center;
        .cover-img { width: 100%; height: 100%; object-fit: cover; }
        .cover-emoji { font-size: 72px; }
    }
    .course-info { flex: 1; min-width: 280px; padding: 24px 24px 20px; }
    .cat-tag {
        display: inline-block; font-size: 12px; color: $primary; background: $primary-light;
        border: 1px dashed $primary; border-radius: 10px; padding: 3px 12px; margin-bottom: 10px;
    }
    .course-title {
        font-family: $font-handwritten; font-size: 26px; color: $text-primary; margin: 0 0 10px;
    }
    .course-meta {
        font-size: 13px; color: $text-secondary; margin-bottom: 12px;
        .meta-dot { margin: 0 8px; color: $border-color; }
    }
    .course-desc {
        font-size: 14px; color: $text-regular; line-height: 1.7; margin: 0 0 16px;
    }
}

.completion-box {
    border-top: 1.5px dashed $border-color; padding-top: 14px;
    .completion-head {
        display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;
        .completion-label { font-size: 13px; color: $text-secondary; }
        .completion-num { font-family: $font-handwritten; font-size: 20px; color: $primary; }
    }
    .progress-track {
        height: 8px; background: $border-color; border-radius: 4px; overflow: hidden;
        .progress-fill {
            height: 100%; border-radius: 4px;
            background: linear-gradient(90deg, $primary, $primary-dark);
            transition: width 0.4s ease;
        }
    }
    .action-row { display: flex; gap: 12px; margin-top: 16px; }
}

.primary-btn {
    font-family: $font-rounded; background: linear-gradient(135deg, $primary, $primary-dark);
    color: #fff; border: none; border-radius: $radius-md; padding: 10px 22px; font-size: 14px;
    font-weight: 600; cursor: pointer; box-shadow: $shadow-button; transition: all 0.2s ease;
    &:hover { transform: translateY(-1px); }
    &:active { transform: translateY(1px); box-shadow: 0 1px 0 $primary-darker; }
    &.sm { padding: 8px 18px; font-size: 13px; }
    &:disabled { opacity: 0.5; cursor: not-allowed; transform: none; }
}
.fav-btn {
    font-family: $font-rounded; background: $bg-card; color: $text-regular;
    border: 1.5px dashed $border-color; border-radius: $radius-md; padding: 10px 18px;
    font-size: 14px; cursor: pointer; transition: all 0.2s ease;
    &:hover { border-color: $primary; color: $primary; }
    &.favorited { color: $primary; border-color: $primary; background: $primary-light; }
}

.section-block { margin-bottom: 32px; }
.section-title {
    font-family: $font-handwritten; font-size: 22px; color: $text-primary;
    margin: 0 0 16px; padding-bottom: 8px; border-bottom: 2px dashed $border-color;
}

.chapter-list { display: flex; flex-direction: column; gap: 10px; }
.chapter-item {
    display: flex; align-items: center; gap: 14px; background: $bg-card;
    border: 1.5px solid $border-color; border-radius: $radius-md; padding: 14px 16px;
    cursor: pointer; transition: all 0.2s ease;
    &:hover { border-color: $primary; box-shadow: 0 4px 12px rgba(180,140,100,0.12); transform: translateX(2px); }
    &.finished { background: rgba(184,216,190,0.12); }
    &.current { border-color: $primary; box-shadow: 0 0 0 3px rgba(232,146,124,0.18); }
    .ch-index {
        width: 32px; height: 32px; border-radius: 50%; flex-shrink: 0;
        background: $primary-light; color: $primary; border: 1.5px dashed $primary;
        display: flex; align-items: center; justify-content: center;
        font-family: $font-handwritten; font-size: 16px;
    }
    .ch-main { flex: 1; min-width: 0; }
    .ch-title { font-size: 15px; color: $text-primary; margin-bottom: 4px; }
    .ch-sub { font-size: 12px; color: $text-secondary; display: flex; align-items: center; gap: 10px;
        .status-tag { font-size: 11px; padding: 2px 8px; border-radius: 8px; border: 1px dashed;
            &.done { color: #6b8e7f; border-color: #9bc4a3; background: rgba(184,216,190,0.3); }
            &.resume { color: $primary; border-color: $primary; background: $primary-light; }
            &.new { color: $text-secondary; border-color: $border-color; }
        }
    }
    .ch-play {
        width: 36px; height: 36px; border-radius: 50%; flex-shrink: 0;
        background: linear-gradient(135deg, $primary, $primary-dark); color: #fff;
        display: flex; align-items: center; justify-content: center; font-size: 14px;
        box-shadow: 0 2px 4px rgba(180,140,100,0.3);
    }
}

.comment-input {
    background: $bg-card; border: 1.5px solid $border-color; border-radius: $radius-md;
    padding: 14px; margin-bottom: 20px;
    .comment-textarea {
        width: 100%; border: 1px solid $border-color; border-radius: $radius-md;
        padding: 10px 12px; font-family: $font-rounded; font-size: 14px; color: $text-primary;
        background: #FFFDF7; resize: vertical; min-height: 60px; outline: none;
        &:focus { border-color: $primary; box-shadow: 0 0 0 3px rgba(232,146,124,0.18); }
    }
    .comment-submit-row {
        display: flex; justify-content: space-between; align-items: center; margin-top: 10px;
        .char-count { font-size: 12px; color: $text-secondary; }
    }
}

.empty-comment {
    text-align: center; color: $text-secondary; font-size: 14px; padding: 30px 0;
}

.comment-list { display: flex; flex-direction: column; gap: 14px; }
.comment-item {
    display: flex; gap: 12px; background: $bg-card; border: 1.5px solid $border-color;
    border-radius: $radius-md; padding: 14px;
    .c-avatar {
        width: 40px; height: 40px; border-radius: 50%; flex-shrink: 0; overflow: hidden;
        background: $primary-light; border: 1px dashed $border-color;
        display: flex; align-items: center; justify-content: center;
        img { width: 100%; height: 100%; object-fit: cover; }
        .avatar-emoji { font-size: 20px; }
    }
    .c-body { flex: 1; min-width: 0; }
    .c-head { display: flex; align-items: center; gap: 8px; margin-bottom: 6px;
        .c-name { font-size: 13px; color: $text-primary; font-weight: 600; }
        .c-mine-tag { font-size: 10px; color: #fff; background: $primary; border-radius: 6px; padding: 1px 6px; }
        .c-time { font-size: 11px; color: $text-secondary; margin-left: auto; }
    }
    .c-content { font-size: 14px; color: $text-regular; line-height: 1.6; margin: 0; word-break: break-word; }
    .c-del {
        align-self: flex-start; font-size: 12px; color: #c47a6b; background: transparent;
        border: 1px dashed #e0a89c; border-radius: 8px; padding: 3px 10px; cursor: pointer;
        &:hover { background: rgba(196,122,107,0.1); }
    }
}

.pager { display: flex; justify-content: center; margin-top: 20px;
    :deep(.el-pagination.is-background .el-pager li.is-active) { background: $primary; color: #fff; }
}

@media (max-width: 600px) {
    .course-card {
        .cover-area { flex: 0 0 100%; height: 160px; }
    }
}
</style>
