<template>
    <div class="mine-container">
        <div class="page-header">
            <div class="header-tape"></div>
            <h1 class="page-title">📚 我的课程</h1>
            <p class="page-subtitle">记录每一段安顿身心的练习 ✨</p>
        </div>

        <div class="tabs">
            <button v-for="t in tabs" :key="t.key"
                    class="tab-btn" :class="{ active: activeTab === t.key }"
                    @click="activeTab = t.key">
                <span class="tab-emoji">{{ t.emoji }}</span>
                <span>{{ t.label }}</span>
                <span class="tab-count">{{ (listByKey[t.key] || []).length }}</span>
            </button>
        </div>

        <div v-if="loading" class="loading-tip">载入中... 🌿</div>
        <div v-else-if="(currentList || []).length === 0" class="empty-tip">
            <div class="empty-emoji">{{ emptyEmoji }}</div>
            <p>{{ emptyText }}</p>
            <router-link to="/course" class="go-link">去课程广场逛逛 →</router-link>
        </div>
        <div v-else class="course-grid">
            <div v-for="course in currentList" :key="course.id" class="course-card" @click="goDetail(course.id)">
                <div class="card-pin"></div>
                <div class="cover-area">
                    <img v-if="course.coverUrl" :src="course.coverUrl" alt="封面" class="cover-img" />
                    <span v-else class="cover-emoji">{{ categoryEmoji(course.category) }}</span>
                    <span v-if="course.isFavorite" class="fav-mark">💛</span>
                </div>
                <div class="card-body">
                    <h3 class="course-name">{{ course.title }}</h3>
                    <div class="course-meta">
                        <span>{{ course.categoryLabel }}</span>
                        <span class="meta-dot">·</span>
                        <span>⏱️ {{ course.totalDurationText }}</span>
                    </div>
                    <div class="completion-row">
                        <div class="progress-track">
                            <div class="progress-fill" :style="{ width: (course.completion || 0) + '%' }"></div>
                        </div>
                        <span class="completion-num">{{ course.completion || 0 }}%</span>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getMyCourses } from '@/api/course'

const router = useRouter()

const tabs = [
    { key: 'learned', label: '已学习', emoji: '✅' },
    { key: 'learning', label: '学习中', emoji: '📖' },
    { key: 'favorites', label: '已收藏', emoji: '💛' }
]

const emojiMap = { MEDITATION: '🧘', SLEEP: '🌙', EMOTION: '💙', CBT: '🧠', INTERPERSONAL: '🤝' }
const categoryEmoji = (code) => emojiMap[code] || '🎧'

const activeTab = ref('learning')
const listByKey = ref({})
const loading = ref(false)

const currentList = computed(() => listByKey.value[activeTab.value] || [])

const emptyEmoji = computed(() => {
    const map = { learned: '🎯', learning: '🌱', favorites: '🤍' }
    return map[activeTab.value] || '📭'
})
const emptyText = computed(() => {
    const map = {
        learned: '还没有完整学完一门课程',
        learning: '还没有开始学习的课程',
        favorites: '还没有收藏课程'
    }
    return map[activeTab.value] || '暂无数据'
})

const loadMine = () => {
    loading.value = true
    getMyCourses().then(data => {
        listByKey.value = {
            learned: data.learned || [],
            learning: data.learning || [],
            favorites: data.favorites || []
        }
        // 默认选中第一个有数据的 tab，否则保持 learning
        if (!listByKey.value[activeTab.value] || listByKey.value[activeTab.value].length === 0) {
            const order = ['learning', 'learned', 'favorites']
            for (const k of order) {
                if ((listByKey.value[k] || []).length > 0) { activeTab.value = k; break }
            }
        }
    }).finally(() => {
        loading.value = false
    })
}

const goDetail = (id) => {
    router.push(`/course/${id}`)
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
    background: $bg-page; font-family: $font-rounded; color: $text-primary;
    padding: 24px 16px 80px; max-width: 1200px; margin: 0 auto;
}

.page-header {
    text-align: center; margin-bottom: 24px;
    .header-tape {
        width: 120px; height: 10px;
        background: linear-gradient(90deg, transparent, rgba(232,146,124,0.5), transparent);
        border-radius: 5px; margin: 0 auto 14px;
    }
    .page-title { font-family: $font-handwritten; font-size: 30px; font-weight: 600; color: $text-primary; margin: 0 0 8px; }
    .page-subtitle { font-size: 14px; color: $text-secondary; margin: 0; }
}

.tabs {
    display: flex; flex-wrap: wrap; justify-content: center; gap: 10px; margin-bottom: 28px;
    .tab-btn {
        font-family: $font-rounded; background: $bg-card; border: 1.5px dashed $border-color;
        color: $text-regular; border-radius: 20px; padding: 8px 18px; font-size: 14px;
        cursor: pointer; transition: all 0.25s ease; display: inline-flex; align-items: center; gap: 6px;
        .tab-emoji { font-size: 16px; }
        .tab-count {
            font-size: 12px; background: $border-color; color: $text-regular;
            border-radius: 8px; padding: 1px 8px; min-width: 18px; text-align: center;
        }
        &:hover { border-color: $primary; color: $primary; transform: translateY(-1px); }
        &.active {
            background: linear-gradient(135deg, $primary, $primary-dark);
            border-style: solid; color: #fff; box-shadow: $shadow-button;
            .tab-count { background: rgba(255,255,255,0.25); color: #fff; }
        }
    }
}

.course-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 22px; }

.course-card {
    position: relative; background: $bg-card; border-radius: $radius-lg;
    border: 1.5px solid $border-color; box-shadow: $shadow-card; overflow: hidden; cursor: pointer;
    transition: transform 0.25s ease, box-shadow 0.25s ease;
    &:hover { transform: translateY(-3px); box-shadow: 0 8px 24px rgba(180,140,100,0.18); }
    .card-pin {
        position: absolute; top: -8px; left: 50%; transform: translateX(-50%);
        width: 14px; height: 14px; background: $primary; border-radius: 50%;
        box-shadow: 0 2px 4px rgba(180,140,100,0.4); z-index: 2;
    }
    .cover-area {
        position: relative; height: 120px;
        background: linear-gradient(135deg, rgba(232,146,124,0.18), rgba(247,232,166,0.25));
        display: flex; align-items: center; justify-content: center;
        .cover-img { width: 100%; height: 100%; object-fit: cover; }
        .cover-emoji { font-size: 48px; }
        .fav-mark { position: absolute; top: 10px; right: 12px; font-size: 20px; }
    }
    .card-body { padding: 14px 18px 18px; }
    .course-name { font-family: $font-handwritten; font-size: 19px; color: $text-primary; margin: 0 0 10px; text-align: center; }
    .course-meta { display: flex; align-items: center; justify-content: center; gap: 8px; font-size: 12px; color: $text-secondary; margin-bottom: 12px;
        .meta-dot { color: $border-color; }
    }
    .completion-row { display: flex; align-items: center; gap: 10px;
        .progress-track { flex: 1; height: 6px; background: $border-color; border-radius: 3px; overflow: hidden;
            .progress-fill { height: 100%; border-radius: 3px; background: linear-gradient(90deg, $primary, $primary-dark); transition: width 0.4s ease; }
        }
        .completion-num { font-family: $font-handwritten; font-size: 15px; color: $primary; min-width: 36px; text-align: right; }
    }
}

.loading-tip, .empty-tip {
    text-align: center; padding: 50px 0; color: $text-secondary; font-size: 15px;
    .empty-emoji { font-size: 48px; margin-bottom: 12px; }
    .go-link {
        display: inline-block; margin-top: 14px; color: $primary; text-decoration: none;
        border-bottom: 1px dashed $primary; padding-bottom: 2px;
    }
}
</style>
