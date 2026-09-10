<template>
    <div class="knowledge-container">
        <!-- ===== 手账风：纸张纹理背景层 ===== -->
        <div class="paper-texture"></div>

        <!-- ===== 手账风：漂浮装饰 emoji ===== -->
        <div class="floating-decorations">
            <span class="float-emoji float-1">📖</span>
            <span class="float-emoji float-2">🌸</span>
            <span class="float-emoji float-3">✨</span>
            <span class="float-emoji float-4">🍃</span>
            <span class="float-emoji float-5">🎀</span>
        </div>

        <!-- ===== 页面主体 ===== -->
        <div class="page-wrapper">
            <!-- ===== 顶部标题区 ===== -->
            <div class="header-section">
                <div class="header-tape"></div>
                <div class="header-inner">
                    <div class="header-left">
                        <div class="header-sticker">
                            <el-image :src="iconUrl" style="width: 50px; height: 50px"></el-image>
                        </div>
                        <div class="header-text">
                            <h1>📚 知识库</h1>
                            <p>探索心理健康知识，用温暖守护每一次成长</p>
                        </div>
                    </div>
                    <div class="header-badge">
                        <span class="badge-icon">🌿</span>
                        <span class="badge-text">每日一篇 · 温暖前行</span>
                    </div>
                </div>
            </div>

            <!-- ===== 主体两列布局 ===== -->
            <div class="main-body">
                <!-- ===== 左侧：推荐阅读 + 装饰卡片 ===== -->
                <aside class="sidebar">
                    <!-- 推荐阅读 -->
                    <div class="recommend-section">
                        <div class="section-tape"></div>
                        <div class="section-title">📌 推荐阅读</div>
                        <div class="section-divider"></div>
                        <div class="recommend-list">
                            <div v-for="item in recommendList" :key="item.id" class="recommend-item" @click="goToArticle(item.id)">
                                <h4>{{item.title}}</h4>
                                <p class="read-count">
                                    <el-icon><Histogram /></el-icon>
                                    阅读量 {{ item.readCount }}
                                </p>
                            </div>
                        </div>
                    </div>

                    <!-- 每日一句（装饰性静态卡片） -->
                    <div class="quote-card">
                        <div class="quote-tape"></div>
                        <div class="quote-icon">💝</div>
                        <p class="quote-text">「每一个小小的进步，都值得被温柔对待。」</p>
                        <span class="quote-deco">🌸</span>
                    </div>

                    <!-- 阅读小贴士（装饰性静态卡片） -->
                    <div class="tips-card">
                        <div class="tips-tape"></div>
                        <div class="tips-title">💡 阅读小贴士</div>
                        <div class="tips-divider"></div>
                        <ul class="tips-list">
                            <li>每天花 10 分钟阅读一篇</li>
                            <li>坚持 30 天感受变化 🌱</li>
                            <li>学到的知识试着写下来</li>
                            <li>和身边人分享你的感悟</li>
                        </ul>
                    </div>
                </aside>

                <!-- ===== 右侧：文章列表（双列网格） ===== -->
                <main class="article-list">
                    <div v-for="item in articleList" :key="item.id" class="article-item" @click="goToArticle(item.id)">
                        <div class="article-cover">
                            <el-image style="width: 100%; height: 180px" :src="getImage(item.coverImage)" fit="cover"></el-image>
                        </div>
                        <div class="article-info">
                            <div class="article-title-row">
                                <h3 class="article-title">{{ item.title }}</h3>
                                <el-tag Plain type="primary">{{ item.categoryName }}</el-tag>
                            </div>
                            <div class="article-meta">
                                <div class="meta-item">
                                    <el-icon><Avatar /></el-icon>
                                    <span>{{ item.authorName }}</span>
                                </div>
                                <div class="meta-item">
                                    <el-icon><List /></el-icon>
                                    <span>{{ dayjs(item.updatedAt).format('YYYY-MM-DD') }}</span>
                                </div>
                                <div class="meta-item">
                                    <el-icon><Platform /></el-icon>
                                    <span>观看次数 {{ item.readCount }}</span>
                                </div>
                            </div>
                        </div>
                    </div>
                </main>
            </div>

            <!-- ===== 分页 ===== -->
            <div class="pagination-wrapper">
                <el-pagination
                    style="margin-top: 25px"
                    :page-size="pagination.size"
                    layout="prev, pager, next"
                    :total="pagination.total"
                    @change="handleChange" />
            </div>
        </div>
    </div>
</template>
<script setup>
    import { dayjs, ElMessage } from 'element-plus'
    import { ref, reactive, onMounted } from 'vue'
    import { getKnowledgeList } from '@/api/frontend'
    import { resolveFileUrl } from '@/config/index.js'
    import { useRouter } from 'vue-router'

    const router = useRouter()
    
    import iconUrl from '@/assets/images/book.png'
    import { Platform } from '@element-plus/icons-vue'

    // 推荐阅读列表
    const recommendList = ref([])

    // 右侧列表数据
    const pagination = reactive({
        currentPage: 1,
        size: 10,
        total: 0
    })

    const articleList = ref([])
    // 获取列表数据
    const getPageList = () => {
        const params = {
            sortField: 'publishedAt',
            sortDirection: 'desc',
            ...pagination
        }
        getKnowledgeList(params).then(res => {
            articleList.value = res.records
            pagination.total = res.total
        })
    }
    // 获取封面图片
    const getImage = (url) => {
        return url ? resolveFileUrl(url) : '\n' +
            'https://mental-health-ai.oss-cn-beijing.aliyuncs.com/bussiness/ARTICLE/202609/%E5%BE%AE%E4%BF%A1%E5%9B%BE%E7%89%87_20260907150713_126_2.jpg'
    }

    const handleChange = (page) => {
        pagination.currentPage = page
        getPageList()
    }

    // 跳转到详情
    const goToArticle = (id) => {
        router.push(`/knowledge/article/${id}`)
    }

    onMounted(() => {
        // 获取推荐阅读列表
        const params = {
            sortField: 'readCount',
            sortDirection: 'desc',
            currentPage: 1,
            size: 5
        }
        getPageList()
        getKnowledgeList(params).then(res => {
            // console.log(res)
            recommendList.value = res.records
        })
    })
</script>
<style lang="scss" scoped>
// ========== 手绘手账治愈风 - 局部主题变量 ==========
$bg-page: #FFF8E7;
$bg-card: #FFFEF9;
$bg-input: #FFFEF9;
$primary: #E8927C;
$primary-dark: #D9826B;
$primary-darker: #C4705C;
$primary-light: rgba(232, 146, 124, 0.15);
$accent-pink: #F5C6C6;
$accent-green: #B8D8BE;
$accent-yellow: #F7E8A6;
$text-primary: #5C4A3A;
$text-regular: #6B5B4B;
$text-secondary: #8B7D6B;
$text-placeholder: #B8A898;
$border-color: #E8DCC8;
$border-light: #EFE6D5;
$divider: #E0D5C0;
$shadow-card: 0 4px 16px rgba(180, 140, 100, 0.12);
$shadow-button: 0 3px 0 $primary-darker, 0 4px 8px rgba(180, 140, 100, 0.2);
$shadow-input-focus: 0 0 0 3px rgba(232, 146, 124, 0.2);
$radius-sm: 8px;
$radius-md: 12px;
$radius-lg: 16px;
$radius-xl: 24px;
$font-handwritten: "Ma Shan Zheng", "Comic Sans MS", "Segoe Print", "Bradley Hand", cursive;
$font-rounded: "Nunito", "Quicksand", "PingFang SC", "Microsoft YaHei", sans-serif;

// ===== 手账风：整体页面背景 =====
.knowledge-container {
    min-height: 100vh;
    background: $bg-page;
    position: relative;
    overflow-x: hidden;
    font-family: $font-rounded;
    color: $text-primary;
}

// ===== 纸张纹理层 =====
.paper-texture {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    pointer-events: none;
    z-index: 0;
    background:
        repeating-linear-gradient(0deg, transparent, transparent 31px, rgba(232, 220, 200, 0.1) 31px, rgba(232, 220, 200, 0.1) 32px),
        repeating-linear-gradient(90deg, transparent, transparent 31px, rgba(232, 220, 200, 0.1) 31px, rgba(232, 220, 200, 0.1) 32px),
        radial-gradient(ellipse at 30% 20%, rgba(232, 146, 124, 0.06) 0%, transparent 60%),
        radial-gradient(ellipse at 70% 80%, rgba(184, 216, 190, 0.06) 0%, transparent 60%);
}

// ===== 漂浮装饰 emoji =====
.floating-decorations {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    pointer-events: none;
    z-index: 1;
    overflow: hidden;

    .float-emoji {
        position: absolute;
        font-size: 24px;
        opacity: 0.22;
        animation: floatDrift 8s ease-in-out infinite;
    }
    .float-1 { top: 12%; left: 5%; animation-delay: 0s; }
    .float-2 { top: 25%; right: 8%; animation-delay: 2s; font-size: 20px; }
    .float-3 { top: 55%; left: 3%; animation-delay: 4s; font-size: 18px; }
    .float-4 { bottom: 20%; right: 5%; animation-delay: 6s; }
    .float-5 { bottom: 35%; left: 8%; animation-delay: 1s; font-size: 22px; }
}

@keyframes floatDrift {
    0%, 100% { transform: translateY(0) rotate(0deg); }
    50% { transform: translateY(-12px) rotate(5deg); }
}

// ===== 页面主体容器 =====
.page-wrapper {
    position: relative;
    z-index: 2;
    max-width: 1400px;
    width: 100%;
    margin: 0 auto;
    padding: 20px 24px 40px;
    box-sizing: border-box;
}

// ===== 顶部标题区 =====
.header-section {
    position: relative;
    background: $bg-card;
    border-radius: $radius-xl;
    padding: 28px 32px;
    box-shadow: $shadow-card;
    margin-bottom: 20px;
    border: 1.5px solid $border-light;

    .header-tape {
        position: absolute;
        top: -8px;
        left: 40px;
        width: 80px;
        height: 20px;
        background: linear-gradient(135deg, rgba(232, 146, 124, 0.55), rgba(247, 232, 166, 0.55));
        border-radius: 3px;
        transform: rotate(-2deg);
        box-shadow: 0 2px 4px rgba(180, 140, 100, 0.15);
    }

    .header-inner {
        display: flex;
        align-items: center;
        justify-content: space-between;
    }

    .header-left {
        display: flex;
        align-items: center;
        gap: 16px;
    }

    .header-sticker {
        width: 70px;
        height: 70px;
        background: linear-gradient(135deg, $accent-yellow, rgba(247, 232, 166, 0.5));
        border-radius: $radius-lg;
        display: flex;
        align-items: center;
        justify-content: center;
        border: 2px dashed $border-color;
        transform: rotate(-2deg);
        transition: transform 0.3s ease;
        flex-shrink: 0;

        &:hover { transform: rotate(0deg) scale(1.05); }
    }

    .header-text {
        h1 {
            font-family: $font-handwritten;
            font-size: 32px;
            color: $text-primary;
            margin: 0 0 4px 0;
        }
        p {
            font-size: 14px;
            color: $text-secondary;
            margin: 0;
        }
    }

    .header-badge {
        display: flex;
        align-items: center;
        gap: 8px;
        background: linear-gradient(135deg, $accent-green, rgba(184, 216, 190, 0.5));
        padding: 10px 18px;
        border-radius: $radius-md;
        border: 1.5px dashed $border-color;
        flex-shrink: 0;

        .badge-icon { font-size: 20px; }
        .badge-text {
            font-family: $font-handwritten;
            font-size: 15px;
            color: $text-regular;
        }
    }
}

// ===== 主体两列布局 =====
.main-body {
    display: grid;
    grid-template-columns: 300px 1fr;
    gap: 20px;
    align-items: start;
}

// ===== 左侧边栏 =====
.sidebar {
    display: flex;
    flex-direction: column;
    gap: 20px;
    position: sticky;
    top: 84px;
}

// ===== 推荐阅读卡片 =====
.recommend-section {
    position: relative;
    background: $bg-card;
    border-radius: $radius-xl;
    padding: 20px;
    box-shadow: $shadow-card;
    border: 1.5px solid $border-light;

    .section-tape {
        position: absolute;
        top: -8px;
        left: 30px;
        width: 56px;
        height: 16px;
        background: linear-gradient(135deg, rgba(232, 146, 124, 0.5), rgba(247, 232, 166, 0.5));
        border-radius: 3px;
        transform: rotate(-1.5deg);
        box-shadow: 0 2px 4px rgba(180, 140, 100, 0.1);
    }

    .section-title {
        font-family: $font-handwritten;
        font-size: 20px;
        color: $text-primary;
        margin-bottom: 12px;
    }

    .section-divider {
        height: 0;
        border: none;
        border-top: 2px dashed $border-color;
        margin-bottom: 14px;
    }

    .recommend-list {
        display: flex;
        flex-direction: column;
        gap: 14px;

        .recommend-item {
            padding: 10px 12px;
            border-left: 4px solid $primary;
            border-radius: 0 $radius-sm $radius-sm 0;
            background: rgba(232, 146, 124, 0.04);
            cursor: pointer;
            transition: all 0.25s ease;

            &:hover {
                background: $primary-light;
                border-left-color: $primary-dark;
                transform: translateX(4px);
            }

            h4 {
                font-size: 14px;
                font-weight: 600;
                color: $text-primary;
                margin: 0 0 8px 0;
                line-height: 1.5;
                // 两行截断
                display: -webkit-box;
                -webkit-line-clamp: 2;
                -webkit-box-orient: vertical;
                overflow: hidden;
            }

            .read-count {
                font-size: 12px;
                color: $text-secondary;
                display: flex;
                align-items: center;
                gap: 4px;
                margin: 0;
            }
        }
    }
}

// ===== 每日一句卡片 =====
.quote-card {
    position: relative;
    background: linear-gradient(135deg, $accent-pink, rgba(245, 198, 198, 0.5));
    border-radius: $radius-xl;
    padding: 20px;
    box-shadow: $shadow-card;
    border: 1.5px dashed $border-color;
    text-align: center;
    overflow: hidden;

    .quote-tape {
        position: absolute;
        top: -8px;
        right: 24px;
        width: 50px;
        height: 16px;
        background: linear-gradient(135deg, rgba(232, 146, 124, 0.5), rgba(247, 232, 166, 0.5));
        border-radius: 3px;
        transform: rotate(2deg);
    }

    .quote-icon {
        font-size: 28px;
        margin-bottom: 8px;
    }

    .quote-text {
        font-family: $font-handwritten;
        font-size: 15px;
        color: $text-regular;
        line-height: 1.7;
        margin: 0 0 8px 0;
    }

    .quote-deco {
        font-size: 18px;
        opacity: 0.6;
    }
}

// ===== 阅读小贴士卡片 =====
.tips-card {
    position: relative;
    background: linear-gradient(135deg, rgba(247, 232, 166, 0.6), $accent-yellow);
    border-radius: $radius-xl;
    padding: 20px;
    box-shadow: $shadow-card;
    border: 1.5px dashed $border-color;

    .tips-tape {
        position: absolute;
        top: -8px;
        left: 28px;
        width: 50px;
        height: 16px;
        background: linear-gradient(135deg, rgba(184, 216, 190, 0.55), rgba(232, 220, 200, 0.45));
        border-radius: 3px;
        transform: rotate(-1.5deg);
    }

    .tips-title {
        font-family: $font-handwritten;
        font-size: 18px;
        color: $text-primary;
        margin-bottom: 10px;
    }

    .tips-divider {
        height: 0;
        border: none;
        border-top: 2px dashed $border-color;
        margin-bottom: 12px;
    }

    .tips-list {
        list-style: none;
        padding: 0;
        margin: 0;
        display: flex;
        flex-direction: column;
        gap: 8px;

        li {
            font-size: 13px;
            color: $text-regular;
            padding-left: 18px;
            position: relative;
            line-height: 1.6;

            &::before {
                content: '✿';
                position: absolute;
                left: 0;
                top: 0;
                color: $primary;
                font-size: 12px;
            }
        }
    }
}

// ===== 右侧文章列表（双列网格） =====
.article-list {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 20px;

    .article-item {
        position: relative;
        background: $bg-card;
        border-radius: $radius-xl;
        box-shadow: $shadow-card;
        border: 1.5px solid $border-light;
        overflow: hidden;
        cursor: pointer;
        transition: all 0.3s ease;

        &:hover {
            transform: translateY(-4px);
            box-shadow: 0 8px 24px rgba(180, 140, 100, 0.2);
            border-color: rgba(232, 146, 124, 0.3);

            .article-cover {
                :deep(.el-image__inner) {
                    transform: scale(1.05);
                }
            }
        }

        .article-cover {
            position: relative;
            overflow: hidden;
            border-bottom: 2px dashed $border-color;

            :deep(.el-image) {
                display: block;
            }

            :deep(.el-image__inner) {
                transition: transform 0.4s ease;
                width: 100%;
                height: 100%;
                object-fit: cover;
            }
        }

        .article-info {
            padding: 16px 18px 18px;

            .article-title-row {
                display: flex;
                align-items: flex-start;
                justify-content: space-between;
                gap: 10px;
                margin-bottom: 12px;

                .article-title {
                    font-size: 16px;
                    font-weight: 600;
                    color: $text-primary;
                    margin: 0;
                    line-height: 1.5;
                    // 两行截断
                    display: -webkit-box;
                    -webkit-line-clamp: 2;
                    -webkit-box-orient: vertical;
                    overflow: hidden;
                    flex: 1;
                }

                :deep(.el-tag) {
                    background: rgba(232, 146, 124, 0.1);
                    border-color: rgba(232, 146, 124, 0.3);
                    color: $primary-dark;
                    border-radius: $radius-sm;
                    font-size: 12px;
                    flex-shrink: 0;
                }
            }

            .article-meta {
                display: flex;
                flex-wrap: wrap;
                gap: 14px;

                .meta-item {
                    display: flex;
                    align-items: center;
                    gap: 5px;
                    font-size: 12px;
                    color: $text-secondary;

                    :deep(.el-icon) {
                        color: $primary;
                        font-size: 14px;
                    }

                    span {
                        color: $text-secondary;
                    }
                }
            }
        }
    }
}

// ===== 分页 =====
.pagination-wrapper {
    display: flex;
    justify-content: center;
    padding: 10px 0 20px;

    :deep(.el-pagination) {
        .el-pager li {
            border-radius: $radius-sm;
            margin: 0 3px;
            font-family: $font-rounded;
            color: $text-regular;
            background: $bg-card;
            border: 1px solid $border-light;

            &.is-active {
                background: linear-gradient(135deg, $primary, $primary-dark);
                color: #fff;
                border-color: $primary-dark;
            }

            &:hover:not(.is-active) {
                color: $primary;
            }
        }

        .btn-prev, .btn-next {
            border-radius: $radius-sm;
            background: $bg-card;
            border: 1px solid $border-light;
            color: $text-regular;

            &:hover {
                color: $primary;
            }
        }
    }
}

// ===== 响应式 =====
@media (max-width: 1024px) {
    .main-body {
        grid-template-columns: 1fr;
    }

    .sidebar {
        position: static;
        flex-direction: row;
        flex-wrap: wrap;

        .recommend-section,
        .quote-card,
        .tips-card {
            flex: 1 1 280px;
        }
    }

    .article-list {
        grid-template-columns: 1fr;
    }
}

@media (max-width: 640px) {
    .page-wrapper {
        padding: 12px 12px 30px;
    }

    .header-section {
        padding: 20px;
        flex-direction: column;

        .header-inner {
            flex-direction: column;
            gap: 14px;
            text-align: center;
        }

        .header-left {
            flex-direction: column;
        }
    }

    .sidebar {
        flex-direction: column;
    }

    .article-list {
        grid-template-columns: 1fr;
    }
}
</style>
