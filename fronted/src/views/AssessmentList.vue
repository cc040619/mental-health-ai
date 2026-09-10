<template>
    <div class="assessment-container">
        <!-- 便签标题 -->
        <div class="page-header">
            <div class="header-tape"></div>
            <h1 class="page-title">🧭 心理测评广场</h1>
            <p class="page-subtitle">标准化量表，科学了解自己的心理状态 ✨</p>
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

        <!-- 量表卡片 -->
        <div v-if="loading" class="loading-tip">載入中... 🌿</div>
        <div v-else-if="scaleList.length === 0" class="empty-tip">
            <div class="empty-emoji">📭</div>
            <p>该类别暂时没有量表</p>
        </div>
        <div v-else class="scale-grid">
            <div v-for="scale in scaleList" :key="scale.id" class="scale-card">
                <div class="card-pin"></div>
                <div class="card-emoji">{{ categoryEmoji(scale.category) }}</div>
                <div class="card-body">
                    <h3 class="scale-name">{{ scale.name }}</h3>
                    <p class="scale-desc">{{ scale.description }}</p>
                    <div class="scale-meta">
                        <span class="meta-item">📝 {{ scale.questionCount }} 题</span>
                        <span class="meta-divider">|</span>
                        <span class="meta-item">⏱️ 约 {{ scale.estimatedMinutes }} 分钟</span>
                    </div>
                </div>
                <button class="start-btn" @click="handleStart(scale)">
                    开始测评 →
                </button>
            </div>
        </div>

        <!-- 历史入口 -->
        <div class="history-entry">
            <router-link to="/assessment/history" class="history-link">
                📊 查看我的测评历史
            </router-link>
        </div>
    </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getScaleList, startAssessment } from '@/api/assessment'

const router = useRouter()

const categories = [
    { code: '', label: '全部', emoji: '🌈' },
    { code: 'EMOTION', label: '情绪', emoji: '💙' },
    { code: 'PERSONALITY', label: '人格', emoji: '🌟' },
    { code: 'STRESS', label: '压力', emoji: '🎈' },
    { code: 'SLEEP', label: '睡眠', emoji: '🌙' },
    { code: 'INTERPERSONAL', label: '人际关系', emoji: '🤝' }
]

const emojiMap = {
    EMOTION: '💙',
    PERSONALITY: '🌟',
    STRESS: '🎈',
    SLEEP: '🌙',
    INTERPERSONAL: '🤝'
}

const activeCategory = ref('')
const scaleList = ref([])
const loading = ref(false)

const categoryEmoji = (code) => emojiMap[code] || '🧭'

const switchCategory = (code) => {
    activeCategory.value = code
    loadScales()
}

const loadScales = () => {
    loading.value = true
    const params = { page: 1, size: 20 }
    if (activeCategory.value) {
        params.category = activeCategory.value
    }
    getScaleList(params).then(data => {
        scaleList.value = data.records || []
    }).finally(() => {
        loading.value = false
    })
}

const handleStart = (scale) => {
    startAssessment({ scaleId: scale.id }).then(data => {
        // 进入作答页，携带续答信息
        router.push({
            path: `/assessment/quiz/${data.recordId}`,
            state: {
                scaleId: data.scaleId,
                resumed: data.resumed,
                currentIndex: data.currentIndex,
                answers: data.answers
            }
        })
    }).catch(() => {
        ElMessage.error('开始测评失败，请重试')
    })
}

onMounted(() => {
    loadScales()
})
</script>

<style lang="scss" scoped>
// ========== 手绘手账治愈风 ==========
$bg-page: #FFF8E7;
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
$shadow-card: 0 4px 16px rgba(180, 140, 100, 0.12);
$shadow-button: 0 3px 0 $primary-darker, 0 4px 8px rgba(180, 140, 100, 0.2);
$radius-md: 12px;
$radius-lg: 16px;
$font-handwritten: "Ma Shan Zheng", "Comic Sans MS", "Segoe Print", "Bradley Hand", cursive;
$font-rounded: "Nunito", "Quicksand", "PingFang SC", "Microsoft YaHei", sans-serif;

.assessment-container {
    min-height: calc(100vh - 130px);
    background: $bg-page;
    font-family: $font-rounded;
    color: $text-primary;
    padding: 24px 16px 48px;
    max-width: 1200px;
    margin: 0 auto;
}

.page-header {
    text-align: center;
    margin-bottom: 24px;
    position: relative;
    padding: 12px 0;

    .header-tape {
        width: 120px;
        height: 10px;
        background: linear-gradient(90deg, transparent, rgba(232, 146, 124, 0.5), transparent);
        border-radius: 5px;
        margin: 0 auto 14px;
    }

    .page-title {
        font-family: $font-handwritten;
        font-size: 30px;
        font-weight: 600;
        color: $text-primary;
        margin: 0 0 8px;
    }

    .page-subtitle {
        font-size: 14px;
        color: $text-secondary;
        margin: 0;
    }
}

.category-tabs {
    display: flex;
    flex-wrap: wrap;
    justify-content: center;
    gap: 10px;
    margin-bottom: 28px;

    .tab-btn {
        font-family: $font-rounded;
        background: $bg-card;
        border: 1.5px dashed $border-color;
        color: $text-regular;
        border-radius: 20px;
        padding: 8px 18px;
        font-size: 14px;
        cursor: pointer;
        transition: all 0.25s ease;
        display: inline-flex;
        align-items: center;
        gap: 6px;

        .tab-emoji {
            font-size: 16px;
        }

        &:hover {
            border-color: $primary;
            color: $primary;
            transform: translateY(-1px);
        }

        &.active {
            background: linear-gradient(135deg, $primary, $primary-dark);
            border-style: solid;
            color: #fff;
            box-shadow: $shadow-button;
        }
    }
}

.scale-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
    gap: 22px;
}

.scale-card {
    position: relative;
    background: $bg-card;
    border-radius: $radius-lg;
    border: 1.5px solid $border-color;
    box-shadow: $shadow-card;
    padding: 22px 20px 18px;
    display: flex;
    flex-direction: column;
    transition: transform 0.25s ease, box-shadow 0.25s ease;

    &:hover {
        transform: translateY(-3px);
        box-shadow: 0 8px 24px rgba(180, 140, 100, 0.18);
    }

    .card-pin {
        position: absolute;
        top: -8px;
        left: 50%;
        transform: translateX(-50%);
        width: 14px;
        height: 14px;
        background: $primary;
        border-radius: 50%;
        box-shadow: 0 2px 4px rgba(180, 140, 100, 0.4);
    }

    .card-emoji {
        font-size: 36px;
        text-align: center;
        margin-bottom: 8px;
    }

    .card-body {
        flex: 1;
    }

    .scale-name {
        font-family: $font-handwritten;
        font-size: 20px;
        color: $text-primary;
        margin: 0 0 8px;
        text-align: center;
    }

    .scale-desc {
        font-size: 13px;
        color: $text-secondary;
        line-height: 1.6;
        margin: 0 0 14px;
        display: -webkit-box;
        -webkit-line-clamp: 4;
        -webkit-box-orient: vertical;
        overflow: hidden;
    }

    .scale-meta {
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 10px;
        font-size: 13px;
        color: $text-regular;
        padding: 8px 0;
        border-top: 1.5px dashed $border-color;
        margin-bottom: 14px;

        .meta-divider {
            color: $border-color;
        }
    }

    .start-btn {
        font-family: $font-rounded;
        background: linear-gradient(135deg, $primary, $primary-dark);
        color: #fff;
        border: none;
        border-radius: $radius-md;
        padding: 10px 16px;
        font-size: 14px;
        font-weight: 600;
        cursor: pointer;
        box-shadow: $shadow-button;
        transition: all 0.2s ease;

        &:hover {
            transform: translateY(-1px);
        }

        &:active {
            transform: translateY(1px);
            box-shadow: 0 1px 0 $primary-darker;
        }
    }
}

.loading-tip, .empty-tip {
    text-align: center;
    padding: 60px 0;
    color: $text-secondary;
    font-size: 15px;

    .empty-emoji {
        font-size: 48px;
        margin-bottom: 12px;
    }
}

.history-entry {
    text-align: center;
    margin-top: 36px;

    .history-link {
        display: inline-block;
        font-family: $font-rounded;
        color: $primary;
        background: $primary-light;
        border: 1.5px dashed $primary;
        border-radius: $radius-md;
        padding: 10px 24px;
        text-decoration: none;
        font-size: 14px;
        transition: all 0.25s ease;

        &:hover {
            background: $primary;
            color: #fff;
            border-style: solid;
        }
    }
}
</style>
