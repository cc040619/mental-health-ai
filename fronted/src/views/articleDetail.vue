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
                <div :style="{marginTop: '20px'}" class="flex-box">
                   <div class="item flex-box">
                        <el-icon><Avatar /></el-icon>
                        <span>{{ articleDetail.authorName }}</span>
                    </div>
                    <div class="item flex-box">
                        <el-icon><Platform /></el-icon>
                        <span>{{ articleDetail.readCount }} 次阅读</span>
                    </div>
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
        </div>
    </div>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import { getKnowledgeDetail } from '@/api/frontend'
import { checkFavorite, addFavorite, removeFavorite, recordBrowseHistory } from '@/api/user'
import { ElMessage } from 'element-plus'
import { dayjs } from 'element-plus'
import { Avatar } from '@element-plus/icons-vue'

const iconUrl = new URL('@/assets/images/book.png', import.meta.url).href

const props = defineProps({
    id: String
})

const articleDetail = ref({})
const isLoggedIn = ref(false)
const isFavorited = ref(false)
const favoriteLoading = ref(false)

const formatContent = (content) => {
  if (!content) return ''

  // 基本的HTML清理和格式化
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

// 切换收藏状态
const toggleFavorite = async () => {
    favoriteLoading.value = true
    try {
        if (isFavorited.value) {
            await removeFavorite(props.id)
            isFavorited.value = false
            ElMessage.success('已取消收藏')
        } else {
            await addFavorite(props.id)
            isFavorited.value = true
            ElMessage.success('收藏成功 ⭐')
        }
    } catch (e) {
        // 错误已由拦截器统一提示
    } finally {
        favoriteLoading.value = false
    }
}

onMounted(() => {
    getKnowledgeDetail(props.id).then(res => {
        articleDetail.value = res
    })
    // 登录用户：检查收藏状态 + 上报浏览历史
    isLoggedIn.value = localStorage.getItem('token') !== null
    if (isLoggedIn.value) {
        checkFavoriteStatus()
        recordHistory()
    }
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

        // ===== 手账风：收藏按钮 =====
        :deep(.favorite-btn) {
            margin-left: auto;
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

    // el-icon 颜色
    :deep(.el-icon) {
        color: $primary;
    }
}
</style>
