<template>
    <div class="course-container">
        <!-- 便签标题 -->
        <div class="page-header">
            <div class="header-tape"></div>
            <h1 class="page-title">🧘 心理课程广场</h1>
            <p class="page-subtitle">在声音里安顿身心，跟着音频慢慢来 ✨</p>
        </div>

        <!-- 类别筛选 Tab -->
        <div class="category-tabs">
            <button v-for="cat in categories" :key="cat.code"
                    class="tab-btn" :class="{ active: activeCategory === cat.code }"
                    @click="switchCategory(cat.code)">
                <span class="tab-emoji">{{ cat.emoji }}</span>
                <span>{{ cat.label }}</span>
            </button>
        </div>

        <!-- 课程卡片 -->
        <div v-if="loading" class="loading-tip">载入中... 🌿</div>
        <div v-else-if="courseList.length === 0" class="empty-tip">
            <div class="empty-emoji">📭</div>
            <p>该类别暂时没有课程</p>
        </div>
        <div v-else class="course-grid">
            <div v-for="course in courseList" :key="course.id" class="course-card" @click="goDetail(course.id)">
                <div class="card-pin"></div>
                <div class="cover-area">
                    <img v-if="course.coverUrl" :src="course.coverUrl" alt="封面" class="cover-img" />
                    <span v-else class="cover-emoji">{{ categoryEmoji(course.category) }}</span>
                    <span v-if="course.isFavorite" class="fav-mark">💛</span>
                </div>
                <div class="card-body">
                    <h3 class="course-name">{{ course.title }}</h3>
                    <div class="course-meta">
                        <span class="meta-item">⏱️ {{ course.totalDurationText }}</span>
                        <span class="meta-divider">|</span>
                        <span class="meta-item">👥 {{ course.learnCount }} 人在学</span>
                    </div>
                    <div class="course-tag">
                        <span class="cat-tag">{{ course.categoryLabel }}</span>
                        <span v-if="course.isLearning" class="learning-tag">学习中</span>
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

        <!-- 我的课程入口 -->
        <div class="mine-entry">
            <router-link to="/my-courses" class="mine-link">
                📚 查看我的课程
            </router-link>
        </div>
    </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getCourseList } from '@/api/course'

const router = useRouter()

const categories = [
    { code: '', label: '全部', emoji: '🌈' },
    { code: 'MEDITATION', label: '正念冥想', emoji: '🧘' },
    { code: 'SLEEP', label: '睡眠助眠', emoji: '🌙' },
    { code: 'EMOTION', label: '情绪管理', emoji: '💙' },
    { code: 'CBT', label: '认知行为', emoji: '🧠' },
    { code: 'INTERPERSONAL', label: '人际关系', emoji: '🤝' }
]

const emojiMap = {
    MEDITATION: '🧘', SLEEP: '🌙', EMOTION: '💙', CBT: '🧠', INTERPERSONAL: '🤝'
}

const activeCategory = ref('')
const courseList = ref([])
const loading = ref(false)
const page = ref(1)
const pageSize = 12
const total = ref(0)

const categoryEmoji = (code) => emojiMap[code] || '🎧'

const switchCategory = (code) => {
    activeCategory.value = code
    page.value = 1
    loadCourses()
}

const onPageChange = (p) => {
    page.value = p
    loadCourses()
}

const loadCourses = () => {
    loading.value = true
    const params = { page: page.value, size: pageSize }
    if (activeCategory.value) params.category = activeCategory.value
    getCourseList(params).then(data => {
        courseList.value = data.records || []
        total.value = data.total || 0
    }).finally(() => {
        loading.value = false
    })
}

const goDetail = (id) => {
    router.push(`/course/${id}`)
}

onMounted(() => {
    loadCourses()
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

.course-container {
    min-height: calc(100vh - 130px);
    background: $bg-page;
    font-family: $font-rounded;
    color: $text-primary;
    padding: 24px 16px 80px;
    max-width: 1200px;
    margin: 0 auto;
}

.page-header {
    text-align: center;
    margin-bottom: 24px;
    .header-tape {
        width: 120px; height: 10px;
        background: linear-gradient(90deg, transparent, rgba(232,146,124,0.5), transparent);
        border-radius: 5px; margin: 0 auto 14px;
    }
    .page-title {
        font-family: $font-handwritten; font-size: 30px; font-weight: 600;
        color: $text-primary; margin: 0 0 8px;
    }
    .page-subtitle { font-size: 14px; color: $text-secondary; margin: 0; }
}

.category-tabs {
    display: flex; flex-wrap: wrap; justify-content: center; gap: 10px; margin-bottom: 28px;
    .tab-btn {
        font-family: $font-rounded; background: $bg-card; border: 1.5px dashed $border-color;
        color: $text-regular; border-radius: 20px; padding: 8px 18px; font-size: 14px;
        cursor: pointer; transition: all 0.25s ease; display: inline-flex; align-items: center; gap: 6px;
        .tab-emoji { font-size: 16px; }
        &:hover { border-color: $primary; color: $primary; transform: translateY(-1px); }
        &.active {
            background: linear-gradient(135deg, $primary, $primary-dark);
            border-style: solid; color: #fff; box-shadow: $shadow-button;
        }
    }
}

.course-grid {
    display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 22px;
}

.course-card {
    position: relative; background: $bg-card; border-radius: $radius-lg;
    border: 1.5px solid $border-color; box-shadow: $shadow-card;
    overflow: hidden; cursor: pointer;
    transition: transform 0.25s ease, box-shadow 0.25s ease;
    &:hover { transform: translateY(-3px); box-shadow: 0 8px 24px rgba(180,140,100,0.18); }
    .card-pin {
        position: absolute; top: -8px; left: 50%; transform: translateX(-50%);
        width: 14px; height: 14px; background: $primary; border-radius: 50%;
        box-shadow: 0 2px 4px rgba(180,140,100,0.4); z-index: 2;
    }
    .cover-area {
        position: relative; height: 140px;
        background: linear-gradient(135deg, rgba(232,146,124,0.18), rgba(247,232,166,0.25));
        display: flex; align-items: center; justify-content: center;
        .cover-img { width: 100%; height: 100%; object-fit: cover; }
        .cover-emoji { font-size: 56px; filter: drop-shadow(0 2px 4px rgba(180,140,100,0.2)); }
        .fav-mark {
            position: absolute; top: 10px; right: 12px; font-size: 20px;
            filter: drop-shadow(0 1px 2px rgba(0,0,0,0.15));
        }
    }
    .card-body { padding: 16px 18px 18px; }
    .course-name {
        font-family: $font-handwritten; font-size: 20px; color: $text-primary;
        margin: 0 0 12px; text-align: center;
    }
    .course-meta {
        display: flex; align-items: center; justify-content: center; gap: 10px;
        font-size: 13px; color: $text-regular; padding: 8px 0;
        border-top: 1.5px dashed $border-color; margin-bottom: 12px;
        .meta-divider { color: $border-color; }
    }
    .course-tag {
        display: flex; align-items: center; justify-content: center; gap: 8px;
        .cat-tag {
            font-size: 12px; color: $primary; background: $primary-light;
            border: 1px dashed $primary; border-radius: 10px; padding: 3px 10px;
        }
        .learning-tag {
            font-size: 12px; color: #6b8e7f; background: rgba(184,216,190,0.35);
            border: 1px dashed #9bc4a3; border-radius: 10px; padding: 3px 10px;
        }
    }
}

.loading-tip, .empty-tip {
    text-align: center; padding: 60px 0; color: $text-secondary; font-size: 15px;
    .empty-emoji { font-size: 48px; margin-bottom: 12px; }
}

.pager {
    display: flex; justify-content: center; margin-top: 28px;
    :deep(.el-pagination.is-background .el-pager li.is-active) {
        background: $primary; color: #fff;
    }
}

.mine-entry {
    text-align: center; margin-top: 36px;
    .mine-link {
        display: inline-block; font-family: $font-rounded; color: $primary;
        background: $primary-light; border: 1.5px dashed $primary;
        border-radius: $radius-md; padding: 10px 24px; text-decoration: none; font-size: 14px;
        transition: all 0.25s ease;
        &:hover { background: $primary; color: #fff; border-style: solid; }
    }
}
</style>
