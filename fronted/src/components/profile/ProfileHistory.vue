<template>
    <div class="profile-history">
        <div class="diary-card">
            <div class="tape tape-yellow"></div>
            <p class="card-title">📜 浏览历史</p>
            <p class="card-subtitle">最近看过的文章都在这里，随时回顾 📖</p>

            <div v-if="total > 0" class="header-actions">
                <el-button class="clear-btn" @click="handleClear">🧹 清空历史</el-button>
            </div>

            <!-- 加载中 -->
            <div v-if="loading" class="loading-box">
                <el-skeleton :rows="3" animated />
            </div>

            <!-- 空状态 -->
            <div v-else-if="history.length === 0" class="empty-box">
                <p class="empty-emoji">📭</p>
                <p class="empty-text">还没有浏览记录哦</p>
                <router-link to="/knowledge" class="empty-link">去知识库逛逛 →</router-link>
            </div>

            <!-- 历史列表 -->
            <div v-else class="history-list">
                <div v-for="item in history" :key="item.historyId" class="history-card">
                    <div class="history-cover" @click="goDetail(item.articleId)">
                        <el-image v-if="item.coverImage" :src="item.coverImage" fit="cover" class="cover-img">
                            <template #error><div class="cover-placeholder">📖</div></template>
                        </el-image>
                        <div v-else class="cover-placeholder">📖</div>
                    </div>
                    <div class="history-info">
                        <h3 class="history-title" @click="goDetail(item.articleId)">{{ item.title }}</h3>
                        <p class="history-summary">{{ item.summary || '暂无摘要' }}</p>
                        <div class="history-meta">
                            <el-tag size="small" effect="light" class="category-tag">{{ item.categoryName || '未分类' }}</el-tag>
                            <span class="meta-text">🕐 {{ formatTime(item.browseTime) }} 浏览</span>
                            <span class="meta-text">👁️ {{ item.readCount || 0 }} 阅读</span>
                        </div>
                    </div>
                    <el-button class="remove-btn" :loading="removingId === item.historyId" @click="handleDelete(item)">
                        删除
                    </el-button>
                </div>
            </div>

            <!-- 分页 -->
            <div v-if="total > 0" class="pagination-box">
                <el-pagination
                    v-model:current-page="page"
                    v-model:page-size="size"
                    :total="total"
                    :page-sizes="[10, 20, 30]"
                    layout="total, prev, pager, next"
                    @current-change="loadHistory"
                    @size-change="loadHistory"
                    background
                />
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getBrowseHistory, deleteBrowseHistory, clearBrowseHistory } from '@/api/user'
import { dayjs } from 'element-plus'

const router = useRouter()

const history = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)
const removingId = ref(null)

const formatTime = (t) => t ? dayjs(t).format('YYYY-MM-DD HH:mm') : ''

const loadHistory = async () => {
    loading.value = true
    try {
        const res = await getBrowseHistory({ page: page.value, size: size.value })
        history.value = res.records || []
        total.value = res.total || 0
    } catch (e) {
        // 错误已由拦截器统一提示
    } finally {
        loading.value = false
    }
}

const goDetail = (articleId) => {
    router.push(`/knowledge/article/${articleId}`)
}

const handleDelete = (item) => {
    ElMessageBox.confirm(`确定删除这条浏览记录吗？`, '删除记录', {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(async () => {
        removingId.value = item.historyId
        try {
            await deleteBrowseHistory(item.historyId)
            ElMessage.success('已删除浏览记录')
            if (history.value.length === 1 && page.value > 1) {
                page.value--
            }
            loadHistory()
        } catch (e) {
            // 错误已由拦截器统一提示
        } finally {
            removingId.value = null
        }
    }).catch(() => {})
}

const handleClear = () => {
    ElMessageBox.confirm('确定清空全部浏览历史吗？此操作不可恢复', '清空历史', {
        confirmButtonText: '确定清空',
        cancelButtonText: '再想想',
        type: 'warning'
    }).then(async () => {
        try {
            await clearBrowseHistory()
            ElMessage.success('已清空全部浏览历史 🧹')
            page.value = 1
            loadHistory()
        } catch (e) {
            // 错误已由拦截器统一提示
        }
    }).catch(() => {})
}

onMounted(() => {
    loadHistory()
})
</script>

<style lang="scss" scoped>
// ========== 手绘手账治愈风 - 局部主题变量 ==========
$bg-card: #FFFEF9;
$primary: #E8927C;
$primary-dark: #D9826B;
$primary-darker: #C4705C;
$primary-light: rgba(232, 146, 124, 0.15);
$accent-yellow: #F7E8A6;
$text-primary: #5C4A3A;
$text-regular: #6B5B4B;
$text-secondary: #8B7D6B;
$border-color: #E8DCC8;
$border-light: #EFE6D5;
$divider: #E0D5C0;
$shadow-card: 0 4px 16px rgba(180, 140, 100, 0.12);
$radius-md: 12px;
$radius-xl: 24px;
$font-handwritten: "Ma Shan Zheng", "Comic Sans MS", "Segoe Print", "Bradley Hand", cursive;
$font-rounded: "Nunito", "Quicksand", "PingFang SC", "Microsoft YaHei", sans-serif;

.profile-history {
    font-family: $font-rounded;
    color: $text-primary;
}

.diary-card {
    position: relative;
    background: $bg-card;
    border-radius: $radius-xl;
    padding: 32px 28px 28px;
    box-shadow: $shadow-card;
    border: 1px solid $border-light;

    .card-title {
        margin: 0 0 6px;
        font-size: 22px;
        font-weight: 600;
        color: $text-primary;
        font-family: $font-handwritten;
        letter-spacing: 0.5px;
    }
    .card-subtitle {
        margin: 0 0 24px;
        font-size: 13px;
        color: $text-secondary;
    }
}

.tape {
    position: absolute;
    top: -10px;
    right: 36px;
    width: 110px;
    height: 30px;
    border-radius: 2px;
    background: rgba(247, 232, 166, 0.55);
    transform: rotate(3deg);
    box-shadow: 0 2px 4px rgba(180, 140, 100, 0.15);
    z-index: 2;
    &::before, &::after {
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

.header-actions {
    display: flex;
    justify-content: flex-end;
    margin-bottom: 16px;
    .clear-btn {
        font-family: $font-rounded;
        background: transparent;
        border: 1px dashed $border-color;
        color: $text-secondary;
        border-radius: $radius-md;
        font-size: 13px;
        &:hover {
            color: $primary-dark;
            border-color: $primary;
            background: $primary-light;
        }
    }
}

.loading-box { padding: 20px 0; }

.empty-box {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 48px 0;
    .empty-emoji { font-size: 48px; margin: 0 0 8px; }
    .empty-text { color: $text-secondary; font-size: 15px; margin: 0 0 12px; }
    .empty-link {
        color: $primary;
        text-decoration: none;
        border-bottom: 1px dashed $primary;
        font-size: 14px;
        &:hover { color: $primary-dark; }
    }
}

.history-list {
    display: flex;
    flex-direction: column;
    gap: 16px;
}

.history-card {
    display: flex;
    align-items: center;
    gap: 16px;
    padding: 16px;
    background: #FFFDF6;
    border: 1px dashed $border-color;
    border-radius: $radius-md;
    transition: all 0.25s ease;
    &:hover {
        border-color: $primary;
        box-shadow: 0 2px 10px rgba(180, 140, 100, 0.1);
        transform: translateY(-1px);
    }

    .history-cover {
        width: 96px;
        height: 72px;
        border-radius: $radius-md;
        overflow: hidden;
        flex-shrink: 0;
        background: #f5efe4;
        cursor: pointer;
        .cover-img { width: 100%; height: 100%; }
        .cover-placeholder {
            width: 100%;
            height: 100%;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 28px;
        }
    }

    .history-info {
        flex: 1;
        min-width: 0;
        .history-title {
            margin: 0 0 6px;
            font-size: 16px;
            font-weight: 600;
            color: $text-primary;
            cursor: pointer;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
            &:hover { color: $primary-dark; }
        }
        .history-summary {
            margin: 0 0 8px;
            font-size: 13px;
            color: $text-secondary;
            line-height: 1.6;
            display: -webkit-box;
            -webkit-line-clamp: 2;
            -webkit-box-orient: vertical;
            overflow: hidden;
        }
        .history-meta {
            display: flex;
            align-items: center;
            gap: 14px;
            flex-wrap: wrap;
            .meta-text {
                font-size: 12px;
                color: $text-secondary;
            }
            .category-tag {
                background: $primary-light !important;
                color: $primary-dark !important;
                border: 1px dashed $primary !important;
                border-radius: 6px;
            }
        }
    }

    .remove-btn {
        flex-shrink: 0;
        font-family: $font-rounded;
        background: transparent;
        border: 1px dashed $border-color;
        color: $text-secondary;
        border-radius: $radius-md;
        font-size: 13px;
        &:hover {
            color: $primary-dark;
            border-color: $primary;
            background: $primary-light;
        }
    }
}

.pagination-box {
    margin-top: 24px;
    display: flex;
    justify-content: center;
    :deep(.el-pagination) {
        --el-pagination-hover-color: #{$primary};
        font-family: $font-rounded;
    }
    :deep(.el-pagination.is-background .el-pager li.is-active) {
        background: $primary;
        color: #fff;
    }
}
</style>
