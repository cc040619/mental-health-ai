<template>
    <div class="articleDetail-container">
        <!-- ===== 手账风：纸张纹理背景层 ===== -->
        <div class="paper-texture"></div>

        <!-- ===== 手账风：顶部标题区（暖橙手写感） ===== -->
        <div class="header-section">
            <div class="header-content">
                <!-- 返回知识库按钮 -->
                <router-link to="/knowledge" class="back-btn">← 返回知识库</router-link>
                <el-image :src="iconUrl" style="width: 60px;height: 60px"></el-image>
                <h1>📖 知识文章详情</h1>
            </div>
            <div class="header-tape"></div>
        </div>

        <div class="content">
            <!-- ===== 手账风：便签卡片 1 - 文章信息 ===== -->
            <div class="diary-card">
                <div class="tape tape-orange"></div>
                <p class="title">📌 文章信息</p>
                <div class="sub-title">
                    <el-tag size="large" class="category-tag">{{ articleDetail.categoryName }}</el-tag>
                    <div class="flex-box">
                        <el-icon><List /></el-icon>
                        <span>{{ dayjs(articleDetail.updatedAt).format('YYYY-MM-DD') }}</span>
                    </div>
                </div>
                <h1 class="article-title">{{ articleDetail.title }}</h1>
                <div class="summary-content" v-if="articleDetail.summary ">
                    <p>{{ articleDetail.summary }}</p>
                </div>
                <div :style="{marginTop: '20px'}" class="flex-box stats-box">
                   <div class="item flex-box">
                        <el-icon><Avatar /></el-icon>
                        <span>{{ articleDetail.authorName }}</span>
                    </div>
                    <div class="item flex-box">
                        <el-icon><Platform /></el-icon>
                        <span>{{ articleDetail.readCount }} 次阅读</span>
                    </div>
                    <div class="item flex-box">
                        <el-icon><Pointer /></el-icon>
                        <span>{{ likeCount }} 点赞</span>
                    </div>
                    <div class="item flex-box">
                        <el-icon><ChatDotSquare /></el-icon>
                        <span>{{ commentCount }} 评论</span>
                    </div>
                    <div class="item flex-box">
                        <el-icon><Star /></el-icon>
                        <span>{{ favoriteCount }} 收藏</span>
                    </div>
                    <div class="action-btns">
                        <!-- 点赞按钮（登录后显示） -->
                        <el-button
                            v-if="isLoggedIn"
                            class="like-btn"
                            :class="{ liked: isLiked }"
                            :loading="likeLoading"
                            @click="toggleLike"
                        >
                            {{ isLiked ? '❤️ 已赞' : '♡ 点赞' }}
                        </el-button>
                        <!-- 收藏按钮（登录后显示） -->
                        <el-button
                            v-if="isLoggedIn"
                            class="favorite-btn"
                            :class="{ favorited: isFavorited }"
                            :loading="favoriteLoading"
                            @click="toggleFavorite"
                        >
                            {{ isFavorited ? '⭐ 已收藏' : '☆ 收藏' }}
                        </el-button>
                    </div>
                </div>
            </div>

            <!-- ===== 手账风：便签卡片 2 - 正文内容 ===== -->
            <div class="diary-card">
                <div class="tape tape-yellow"></div>
                <div class="title">🖊️ 正文内容</div>
                <div class="content-wrapper" v-html="formatContent(articleDetail.content)"></div>
                <div class="tags-content" v-if="articleDetail.tagArray && articleDetail.tagArray.length">
                    <h4 class="tags-title"> 🎀 相关标签 </h4>
                    <div class="tags-list">
                        <el-tag v-for="tag in articleDetail.tagArray" :key="tag" type="info" effect="light" class="tag-item">{{ tag }}</el-tag>
                    </div>
                </div>
            </div>

            <!-- ===== 手账风：便签卡片 3 - 评论区 ===== -->
            <div class="diary-card comment-card">
                <div class="tape tape-green"></div>
                <p class="title">💬 文章评论</p>

                <!-- 发表评论（登录后显示） -->
                <div class="comment-form" v-if="isLoggedIn">
                    <el-input
                        v-model="commentForm.content"
                        type="textarea"
                        :rows="3"
                        maxlength="500"
                        show-word-limit
                        placeholder="写下你的想法～（评论发表后需审核通过才会展示）"
                        class="comment-input"
                    />
                    <div class="comment-form-footer">
                        <span class="comment-tip">📝 评论审核通过后会对所有人可见</span>
                        <el-button
                            type="primary"
                            class="submit-btn"
                            :loading="commentSubmitting"
                            :disabled="!commentForm.content.trim()"
                            @click="submitComment"
                        >
                            发表评论
                        </el-button>
                    </div>
                </div>
                <!-- 未登录提示 -->
                <div class="login-tip" v-else>
                    <el-icon><InfoFilled /></el-icon>
                    <span>请先登录后再发表评论</span>
                </div>

                <!-- 评论列表（仅展示已通过评论） -->
                <div class="comment-list">
                    <div v-if="commentList.length === 0" class="empty-comment">
                        🌱 暂无评论，期待你的声音～
                    </div>
                    <div v-else>
                        <div v-for="c in commentList" :key="c.id" class="comment-item">
                            <el-avatar :size="40" :src="c.userAvatar">{{ c.userName?.[0] || '匿' }}</el-avatar>
                            <div class="comment-body">
                                <div class="comment-head">
                                    <span class="comment-nickname">{{ c.userName || '匿名用户' }}</span>
                                    <span class="comment-time">{{ dayjs(c.createdAt).format('YYYY-MM-DD HH:mm') }}</span>
                                </div>
                                <div class="comment-content">{{ c.content }}</div>
                            </div>
                        </div>
                        <!-- 分页 -->
                        <el-pagination
                            v-if="commentTotal > commentPage.size"
                            class="comment-pagination"
                            background
                            layout="prev, pager, next"
                            :current-page="commentPage.page"
                            :page-size="commentPage.size"
                            :total="commentTotal"
                            @current-change="onCommentPageChange"
                        />
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>
<script setup>
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import { getKnowledgeDetail, toggleArticleLike, getArticleComments, createArticleComment, reportReadDuration } from '@/api/frontend'
import { checkFavorite, addFavorite, removeFavorite, recordBrowseHistory } from '@/api/user'
import { ElMessage } from 'element-plus'
import { dayjs } from 'element-plus'
import { Avatar, Pointer, ChatDotSquare, Star, InfoFilled } from '@element-plus/icons-vue'

const iconUrl = new URL('@/assets/images/book.png', import.meta.url).href

const props = defineProps({
    id: String
})

const articleDetail = ref({})
const isLoggedIn = ref(false)
const isFavorited = ref(false)
const favoriteLoading = ref(false)

// 点赞与计数（来自详情接口；点赞/收藏 toggle 后由前端即时更新）
const isLiked = ref(false)
const likeLoading = ref(false)
const likeCount = ref(0)
const commentCount = ref(0)
const favoriteCount = ref(0)

// 评论区
const commentList = ref([])
const commentTotal = ref(0)
const commentPage = reactive({ page: 1, size: 10 })
const commentForm = reactive({ content: '' })
const commentSubmitting = ref(false)

// 阅读时长上报（前台累加，离开/隐藏时上报，30 秒内重复忽略；单次上限 7200 秒与后端 @Max 对齐）
let visibleSince = Date.now()
let lastReportAt = Date.now()

const formatContent = (content) => {
  if (!content) return ''
  let formatted = content
      .replace(/\n/g, '<br>')
      .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
      .replace(/\*(.*?)\*/g, '<em>$1</em>')
  return formatted
}

// 检查收藏状态（登录用户）
const checkFavoriteStatus = async () => {
    try {
        const res = await checkFavorite(props.id)
        isFavorited.value = res.favorited
    } catch (e) {
        // 未登录或失败时忽略，按钮已通过 isLoggedIn 控制显示
    }
}

// 上报浏览历史（登录用户）
const recordHistory = async () => {
    try {
        await recordBrowseHistory(props.id)
    } catch (e) {
        // 上报失败不影响阅读体验，静默忽略
    }
}

// 切换收藏状态（同步本地 favoriteCount 即时展示）
const toggleFavorite = async () => {
    favoriteLoading.value = true
    try {
        if (isFavorited.value) {
            await removeFavorite(props.id)
            isFavorited.value = false
            favoriteCount.value = Math.max(0, favoriteCount.value - 1)
            ElMessage.success('已取消收藏')
        } else {
            await addFavorite(props.id)
            isFavorited.value = true
            favoriteCount.value += 1
            ElMessage.success('收藏成功 ⭐')
        }
    } catch (e) {
        // 错误已由拦截器统一提示
    } finally {
        favoriteLoading.value = false
    }
}

// 点赞 toggle（同步本地 likeCount 即时展示）
const toggleLike = async () => {
    likeLoading.value = true
    try {
        const res = await toggleArticleLike(props.id)
        isLiked.value = res.liked
        likeCount.value = res.likeCount
    } catch (e) {
        // 错误已由拦截器统一提示
    } finally {
        likeLoading.value = false
    }
}

// 加载评论列表（仅 status=1，对所有访客可见）
const loadComments = async () => {
    try {
        const res = await getArticleComments(props.id, {
            page: commentPage.page,
            size: commentPage.size
        })
        commentList.value = res.records || []
        commentTotal.value = res.total || 0
    } catch (e) {
        // 加载失败静默，列表为空
    }
}

// 翻页
const onCommentPageChange = (p) => {
    commentPage.page = p
    loadComments()
}

// 发表评论（过敏感词过滤，默认待审核；命中敏感词由后端 BusinessException 抛出）
const submitComment = async () => {
    const content = commentForm.content.trim()
    if (!content) return
    commentSubmitting.value = true
    try {
        await createArticleComment(props.id, { content })
        ElMessage.success('评论已提交，审核通过后展示 🌱')
        commentForm.content = ''
    } catch (e) {
        // 错误已由拦截器统一提示
    } finally {
        commentSubmitting.value = false
    }
}

// 上报阅读时长（30 秒内重复忽略，单次上限 2 小时）
const reportReadDurationOnce = async () => {
    if (!isLoggedIn.value) return
    const now = Date.now()
    // ponytail: 30 秒内重复忽略——前端节流，后端按 created_at 距上次更新再兜底
    if (now - lastReportAt < 30000) return
    let seconds = Math.floor((now - visibleSince) / 1000)
    if (seconds < 1) return
    if (seconds > 7200) seconds = 7200  // 与后端 ReadDurationDTO @Max(7200) 对齐
    lastReportAt = now
    visibleSince = now  // 重置累加基准
    try {
        await reportReadDuration(props.id, { seconds })
    } catch (e) {
        // 上报失败不影响阅读体验
    }
}

// 页面可见性变化（隐藏时上报、可见时重置累加基准，不计隐藏时段）
const onVisibilityChange = () => {
    if (document.hidden) {
        reportReadDurationOnce()
    } else {
        visibleSince = Date.now()
    }
}

// 页面即将卸载（刷新/关闭/路由跳转）时尽力上报
const onBeforeUnload = () => {
    reportReadDurationOnce()
}

onMounted(() => {
    getKnowledgeDetail(props.id).then(res => {
        articleDetail.value = res
        // 同步计数到本地（点赞/评论/收藏）
        likeCount.value = res.likeCount || 0
        commentCount.value = res.commentCount || 0
        favoriteCount.value = res.favoriteCount || 0
    })
    // 登录用户：检查收藏状态 + 上报浏览历史
    isLoggedIn.value = localStorage.getItem('token') !== null
    if (isLoggedIn.value) {
        checkFavoriteStatus()
        recordHistory()
    }
    // 评论列表对所有访客可见
    loadComments()
    // 初始化阅读时长计时
    visibleSince = Date.now()
    lastReportAt = Date.now()
    document.addEventListener('visibilitychange', onVisibilityChange)
    window.addEventListener('beforeunload', onBeforeUnload)
})

onBeforeUnmount(() => {
    // 离开页面上报剩余阅读时长
    reportReadDurationOnce()
    document.removeEventListener('visibilitychange', onVisibilityChange)
    window.removeEventListener('beforeunload', onBeforeUnload)
})
</script>

<style lang="scss" scoped>
// ========== 手绘手账治愈风 - 局部主题变量 ==========
// 背景色
$bg-page: #FFF8E7;          // 页面纸张底色
$bg-card: #FFFEF9;          // 卡片/便签底色
$bg-input: #FFFEF9;         // 输入框底色

// 主色
$primary: #E8927C;           // 主色（暖橙/珊瑚）
$primary-dark: #D9826B;      // 主色加深
$primary-darker: #C4705C;    // 主色最深（按钮阴影边）
$primary-light: rgba(232, 146, 124, 0.15);  // 主色淡色

// 辅助色
$accent-pink: #F5C6C6;
$accent-green: #B8D8BE;
$accent-yellow: #F7E8A6;

// 文字色
$text-primary: #5C4A3A;      // 主文字（深棕）
$text-regular: #6B5B4B;      // 常规文字
$text-secondary: #8B7D6B;    // 次要文字
$text-placeholder: #B8A898;  // placeholder

// 边框/分隔
$border-color: #E8DCC8;
$border-light: #EFE6D5;
$divider: #E0D5C0;

// 阴影（暖色调）
$shadow-card: 0 4px 16px rgba(180, 140, 100, 0.12);
$shadow-button: 0 3px 0 $primary-darker, 0 4px 8px rgba(180, 140, 100, 0.2);
$shadow-input-focus: 0 0 0 3px rgba(232, 146, 124, 0.2);

// 圆角
$radius-sm: 8px;
$radius-md: 12px;
$radius-lg: 16px;
$radius-xl: 24px;

// 字体
$font-handwritten: "Ma Shan Zheng", "Comic Sans MS", "Segoe Print", "Bradley Hand", cursive;
$font-rounded: "Nunito", "Quicksand", "PingFang SC", "Microsoft YaHei", sans-serif;

.articleDetail-container {
    position: relative;
    min-height: 100vh;
    background: $bg-page;
    font-family: $font-rounded;
    color: $text-primary;
    padding-bottom: 40px;

    .flex-box {
        display: flex;
        align-items: center;
        .item {
            margin-right: 20px;
            span {
                margin-left: 5px;
                color: $text-secondary;
                font-size: 14px;
            }
        }

        // ===== 手账风：操作按钮容器（点赞/收藏，靠右） =====
        .action-btns {
            margin-left: auto;
            display: flex;
            gap: 10px;
        }

        // ===== 手账风：点赞按钮 =====
        :deep(.like-btn) {
            font-family: $font-rounded;
            background: transparent;
            border: 1.5px dashed $primary;
            color: $primary-dark;
            border-radius: $radius-md;
            padding: 6px 18px;
            font-size: 14px;
            font-weight: 600;
            transition: all 0.25s ease;
            &:hover {
                background: $primary-light;
                border-style: solid;
                transform: translateY(-1px);
            }
            &.liked {
                background: linear-gradient(135deg, $accent-pink, $primary-light);
                border-style: solid;
                color: $primary-darker;
            }
        }

        // ===== 手账风：收藏按钮 =====
        :deep(.favorite-btn) {
            font-family: $font-rounded;
            background: transparent;
            border: 1.5px dashed $primary;
            color: $primary-dark;
            border-radius: $radius-md;
            padding: 6px 18px;
            font-size: 14px;
            font-weight: 600;
            transition: all 0.25s ease;
            &:hover {
                background: $primary-light;
                border-style: solid;
                transform: translateY(-1px);
            }
            &.favorited {
                background: linear-gradient(135deg, $accent-yellow, $primary-light);
                border-style: solid;
                color: $primary-darker;
            }
        }
    }

    // ===== 手账风：纸张纹理背景层（网格线 + 噪点） =====
    .paper-texture {
        position: fixed;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        pointer-events: none;
        z-index: 0;
        background-image:
            linear-gradient(rgba(232, 220, 200, 0.25) 1px, transparent 1px),
            linear-gradient(90deg, rgba(232, 220, 200, 0.25) 1px, transparent 1px),
            radial-gradient(ellipse at center, rgba(180, 140, 100, 0.04) 0%, transparent 70%);
        background-size: 28px 28px, 28px 28px, 100% 100%;
    }

    // ===== 手账风：顶部标题区 =====
    .header-section {
        position: relative;
        background: linear-gradient(135deg, $accent-yellow 0%, $primary-light 100%);
        color: $text-primary;
        padding: 48px 40px;
        border-bottom: 2px dashed $divider;
        overflow: hidden;
        z-index: 1;

        .header-content {
            display: flex;
            align-items: center;
            gap: 14px;
            max-width: 980px;
            margin: 0 auto;
            position: relative;
            z-index: 2;

            // ===== 手账风：返回知识库按钮（便签贴纸风） =====
            .back-btn {
                display: inline-flex;
                align-items: center;
                padding: 8px 18px;
                margin-right: 6px;
                font-family: $font-handwritten;
                font-size: 15px;
                color: #fff;
                background: linear-gradient(135deg, $primary, $primary-dark);
                border-radius: $radius-md;
                text-decoration: none;
                box-shadow: $shadow-button;
                transition: transform 0.2s ease, box-shadow 0.2s ease;
                letter-spacing: 0.5px;
                &:hover {
                    color: #fff;
                    transform: translateY(-2px);
                    box-shadow: 0 5px 0 $primary-darker, 0 6px 12px rgba(180, 140, 100, 0.25);
                }
                &:active {
                    transform: translateY(1px);
                    box-shadow: 0 1px 0 $primary-darker, 0 2px 4px rgba(180, 140, 100, 0.15);
                }
            }

            h1 {
                font-family: $font-handwritten;
                font-size: 32px;
                color: $text-primary;
                margin: 0;
                letter-spacing: 1px;
                text-shadow: 1px 1px 0 rgba(255, 255, 255, 0.6);
            }
        }

        // 顶部装饰胶带
        .header-tape {
            position: absolute;
            top: 14px;
            right: -30px;
            width: 180px;
            height: 38px;
            background: rgba(232, 146, 124, 0.35);
            transform: rotate(8deg);
            border-radius: 2px;
            box-shadow: 0 2px 4px rgba(180, 140, 100, 0.15);
            &::before,
            &::after {
                content: '';
                position: absolute;
                top: 0;
                bottom: 0;
                width: 8px;
                background: rgba(255, 255, 255, 0.4);
            }
            &::before { left: 0; clip-path: polygon(0 0, 100% 0, 80% 50%, 100% 100%, 0 100%); }
            &::after { right: 0; clip-path: polygon(0 0, 100% 0, 100% 100%, 20% 100%, 0 50%); }
        }
    }

    .content {
        position: relative;
        z-index: 1;
        margin: 0 auto;
        width: 980px;
        padding: 28px 20px;

        // ===== 手账风：便签卡片 =====
        .diary-card {
            position: relative;
            margin-bottom: 24px;
            background: $bg-card;
            border-radius: $radius-xl;
            padding: 32px 28px 28px;
            box-shadow: $shadow-card;
            border: 1px solid $border-light;

            .title {
                margin-bottom: 16px;
                font-size: 20px;
                font-weight: 600;
                color: $text-primary;
                font-family: $font-handwritten;
                letter-spacing: 0.5px;
            }

            .sub-title {
                margin-top: 18px;
                display: flex;
                align-items: center;
                .category-tag {
                    margin-right: 20px;
                }
            }

            .article-title {
                font-family: $font-handwritten;
                font-size: 30px;
                font-weight: bold;
                color: $text-primary;
                margin-top: 28px;
                margin-bottom: 12px;
                line-height: 1.4;
                letter-spacing: 0.5px;
            }

            // ===== 手账风：摘要便签（浅黄便签感） =====
            .summary-content {
                background: rgba(247, 232, 166, 0.35);
                border-left: 4px solid $accent-yellow;
                padding: 12px 16px;
                border-radius: 0 $radius-md $radius-md 0;
                position: relative;
                color: $text-regular;
                font-size: 15px;
                line-height: 1.7;
                p {
                    margin: 0;
                }
            }

            // ===== 手账风：正文内容 =====
            .content-wrapper {
                font-size: 15px;
                color: $text-regular;
                line-height: 1.8;

                :deep(p) {
                    margin-bottom: 10px;
                }
                :deep(h1),
                :deep(h2),
                :deep(h3),
                :deep(h4),
                :deep(h5),
                :deep(h6) {
                    margin: 18px 0 10px;
                    color: $text-primary;
                    font-weight: 600;
                    font-family: $font-handwritten;
                }
                :deep(h2) {
                    font-size: 18px;
                    border-bottom: 2px dashed $primary;
                    padding-bottom: 6px;
                }
                :deep(h3) {
                    font-size: 15px;
                }
                :deep(strong) {
                    color: $primary-dark;
                }
                :deep(ul),
                :deep(ol) {
                    padding-left: 18px;
                    margin-bottom: 10px;
                    color: $text-regular;
                }
                :deep(li) {
                    margin-bottom: 6px;
                }
                :deep(a) {
                    color: $primary;
                    border-bottom: 1px dashed $primary;
                    text-decoration: none;
                }
            }

            // ===== 手账风：标签区（虚线分隔） =====
            .tags-content {
                margin-top: 22px;
                padding-top: 18px;
                border-top: 1px dashed $divider;

                .tags-title {
                    margin-bottom: 12px;
                    font-size: 15px;
                    font-weight: 600;
                    color: $text-primary;
                    font-family: $font-handwritten;
                }
                .tags-list {
                    display: flex;
                    flex-wrap: wrap;
                    gap: 10px;
                }
            }
        }
    }
}

// ===== 手账风：卡片顶部胶带装饰 =====
.tape {
    position: absolute;
    top: -10px;
    width: 110px;
    height: 30px;
    border-radius: 2px;
    box-shadow: 0 2px 4px rgba(180, 140, 100, 0.15);
    z-index: 2;

    &::before,
    &::after {
        content: '';
        position: absolute;
        top: 0;
        bottom: 0;
        width: 6px;
        background: rgba(255, 255, 255, 0.4);
    }
    &::before { left: 0; clip-path: polygon(0 0, 100% 0, 75% 50%, 100% 100%, 0 100%); }
    &::after { right: 0; clip-path: polygon(0 0, 100% 0, 100% 100%, 25% 100%, 0 50%); }
}
.tape-orange {
    left: 36px;
    background: rgba(232, 146, 124, 0.4);
    transform: rotate(-4deg);
}
.tape-yellow {
    right: 36px;
    background: rgba(247, 232, 166, 0.55);
    transform: rotate(3deg);
}
.tape-green {
    left: 36px;
    background: rgba(184, 216, 190, 0.55);
    transform: rotate(-3deg);
}

// ===== 手账风：评论区 =====
.comment-card {
    .comment-form {
        margin-bottom: 24px;

        :deep(.comment-input) {
            .el-textarea__inner {
                background: $bg-input;
                border: 1.5px dashed $border-color;
                border-radius: $radius-md;
                font-family: $font-rounded;
                color: $text-primary;
                font-size: 14px;
                line-height: 1.7;
                padding: 12px 14px;
                &:focus {
                    border-color: $primary;
                    box-shadow: $shadow-input-focus;
                }
            }
        }

        .comment-form-footer {
            margin-top: 10px;
            display: flex;
            justify-content: space-between;
            align-items: center;
            .comment-tip {
                font-size: 12px;
                color: $text-secondary;
                font-family: $font-rounded;
            }
            :deep(.submit-btn) {
                font-family: $font-rounded;
                background: linear-gradient(135deg, $primary, $primary-dark);
                border: none;
                color: #fff;
                border-radius: $radius-md;
                padding: 8px 22px;
                font-size: 14px;
                font-weight: 600;
                box-shadow: $shadow-button;
                transition: transform 0.2s ease, box-shadow 0.2s ease;
                &:hover:not(:disabled) {
                    transform: translateY(-2px);
                    box-shadow: 0 5px 0 $primary-darker, 0 6px 12px rgba(180, 140, 100, 0.25);
                }
                &:active:not(:disabled) {
                    transform: translateY(1px);
                    box-shadow: 0 1px 0 $primary-darker, 0 2px 4px rgba(180, 140, 100, 0.15);
                }
                &:disabled {
                    opacity: 0.5;
                    cursor: not-allowed;
                }
            }
        }
    }

    .login-tip {
        display: flex;
        align-items: center;
        gap: 8px;
        padding: 16px;
        margin-bottom: 20px;
        background: rgba(247, 232, 166, 0.25);
        border: 1px dashed $divider;
        border-radius: $radius-md;
        color: $text-secondary;
        font-size: 14px;
    }

    .comment-list {
        margin-top: 8px;
        .empty-comment {
            padding: 36px 16px;
            text-align: center;
            color: $text-placeholder;
            font-family: $font-handwritten;
            font-size: 16px;
        }

        .comment-item {
            display: flex;
            gap: 14px;
            padding: 16px 0;
            border-bottom: 1px dashed $divider;
            &:last-child {
                border-bottom: none;
            }

            .comment-body {
                flex: 1;
                min-width: 0;
                .comment-head {
                    display: flex;
                    align-items: center;
                    gap: 12px;
                    margin-bottom: 6px;
                    .comment-nickname {
                        font-weight: 600;
                        color: $text-primary;
                        font-size: 14px;
                    }
                    .comment-time {
                        color: $text-placeholder;
                        font-size: 12px;
                    }
                }
                .comment-content {
                    color: $text-regular;
                    font-size: 14px;
                    line-height: 1.7;
                    word-break: break-word;
                }
            }
        }

        .comment-pagination {
            margin-top: 20px;
            justify-content: center;
        }
    }
}

// ===== 手账风：Element Plus 组件覆盖（穿透） =====
.articleDetail-container {
    // 分类标签 - 暖橙手账风
    :deep(.category-tag) {
        background: rgba(232, 146, 124, 0.15) !important;
        color: $primary-dark !important;
        border: 1px dashed $primary !important;
        border-radius: $radius-sm !important;
        font-weight: 600;
    }

    // 相关标签列表项 - 多色便签感
    :deep(.tag-item) {
        background: rgba(184, 216, 190, 0.25) !important;
        color: $text-regular !important;
        border: 1px dashed $divider !important;
        border-radius: $radius-sm !important;
        padding: 4px 12px;
        font-size: 13px;
        transition: all 0.2s ease;
        &:hover {
            background: rgba(232, 146, 124, 0.15) !important;
            color: $primary-dark !important;
            border-color: $primary !important;
            transform: translateY(-1px);
        }
    }

    // el-image 圆角与轻阴影
    :deep(.el-image) {
        border-radius: $radius-md;
        filter: drop-shadow(0 2px 4px rgba(180, 140, 100, 0.2));
    }

    // el-avatar 圆角与轻阴影（评论区头像）
    :deep(.el-avatar) {
        background: $accent-yellow;
        color: $primary-darker;
        font-weight: 600;
        font-family: $font-rounded;
        box-shadow: 0 2px 6px rgba(180, 140, 100, 0.15);
    }

    // el-icon 颜色
    :deep(.el-icon) {
        color: $primary;
    }
}
</style>
