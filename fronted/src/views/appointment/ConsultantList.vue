<template>
    <div class="consultant-container">
        <!-- 便签标题 -->
        <div class="page-header">
            <!-- 我的预约入口：左上角（与标题垂直居中对齐，不挤压居中标题） -->
            <div class="mine-entry">
                <router-link to="/appointments" class="mine-link">
                    📒 查看我的预约
                </router-link>
            </div>
            <div class="header-tape"></div>
            <h1 class="page-title">👩‍⚕️ 预约咨询师</h1>
            <p class="page-subtitle">和专业的人聊聊，把心里的重量轻轻放下 🕊️</p>
        </div>

        <!-- 擅长领域筛选 Tab -->
        <div class="category-tabs">
            <button v-for="item in specialties" :key="item.code"
                    class="tab-btn" :class="{ active: activeSpecialty === item.code }"
                    @click="switchSpecialty(item.code)">
                <span class="tab-emoji">{{ item.emoji }}</span>
                <span>{{ item.label }}</span>
            </button>
        </div>

        <!-- 咨询师卡片 -->
        <div v-if="loading" class="loading-tip">载入中... 🌿</div>
        <div v-else-if="consultantList.length === 0" class="empty-tip">
            <div class="empty-emoji">📭</div>
            <p>该领域暂时没有可预约的咨询师</p>
        </div>
        <div v-else class="consultant-grid">
            <div v-for="c in consultantList" :key="c.id" class="consultant-card" @click="goDetail(c.id)">
                <div class="card-tape"></div>
                <div class="card-top">
                    <el-avatar :size="72" :src="c.avatar" class="avatar">{{ (c.name || '?').charAt(0) }}</el-avatar>
                    <div class="name-box">
                        <h3 class="c-name">{{ c.name }}</h3>
                        <span class="c-title">{{ c.title || '心理咨询师' }}</span>
                    </div>
                </div>
                <div class="specialty-tags">
                    <span v-for="s in (c.specialties || [])" :key="s" class="s-tag">{{ s }}</span>
                </div>
                <div class="c-meta">
                    <span class="meta-item">⭐ {{ Number(c.rating || 0).toFixed(1) }}</span>
                    <span class="meta-divider">|</span>
                    <span class="meta-item">🌿 从业 {{ c.yearsOfPractice || 0 }} 年</span>
                    <span class="meta-divider">|</span>
                    <span class="meta-item">💬 {{ c.consultCount || 0 }} 次咨询</span>
                </div>
                <div class="book-btn">查看可约时段 →</div>
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
import { getConsultantList } from '@/api/consultant'

const router = useRouter()

const specialties = [
    { code: '', label: '全部', emoji: '🌈' },
    { code: '情绪管理', label: '情绪管理', emoji: '💙' },
    { code: '人际关系', label: '人际关系', emoji: '🤝' },
    { code: '学业压力', label: '学业压力', emoji: '📚' },
    { code: '睡眠问题', label: '睡眠问题', emoji: '🌙' }
]

const activeSpecialty = ref('')
const consultantList = ref([])
const loading = ref(false)
const page = ref(1)
const pageSize = 9
const total = ref(0)

const switchSpecialty = (code) => {
    activeSpecialty.value = code
    page.value = 1
    loadConsultants()
}

const onPageChange = (p) => {
    page.value = p
    loadConsultants()
}

const loadConsultants = () => {
    loading.value = true
    const params = { page: page.value, size: pageSize }
    if (activeSpecialty.value) params.specialty = activeSpecialty.value
    getConsultantList(params).then(data => {
        consultantList.value = data.records || []
        total.value = data.total || 0
    }).finally(() => {
        loading.value = false
    })
}

const goDetail = (id) => {
    router.push(`/consultants/${id}`)
}

onMounted(() => {
    loadConsultants()
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

.consultant-container {
    min-height: calc(100vh - 130px);
    background: $bg-page;
    font-family: $font-rounded;
    color: $text-primary;
    padding: 24px 16px 80px;
    max-width: 1200px;
    margin: 0 auto;
}

.page-header {
    position: relative;
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

.consultant-grid {
    display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 22px;
}

.consultant-card {
    position: relative; background: $bg-card; border-radius: $radius-lg;
    border: 1.5px solid $border-color; box-shadow: $shadow-card;
    padding: 22px 20px 18px; cursor: pointer;
    transition: transform 0.25s ease, box-shadow 0.25s ease;
    &:hover { transform: translateY(-3px); box-shadow: 0 8px 24px rgba(180,140,100,0.18); }
    .card-tape {
        position: absolute; top: -8px; left: 50%; transform: translateX(-50%) rotate(-2deg);
        width: 70px; height: 16px;
        background: linear-gradient(135deg, rgba(232,146,124,0.45), rgba(247,232,166,0.55));
        border-radius: 2px;
    }
    .card-top {
        display: flex; align-items: center; gap: 14px; margin-bottom: 14px;
        .avatar {
            flex-shrink: 0; border: 2px solid #fff;
            box-shadow: 0 2px 8px rgba(180,140,100,0.25);
            background: $primary-light; color: $primary;
        }
        .name-box { flex: 1; min-width: 0; }
        .c-name {
            font-family: $font-handwritten; font-size: 21px; color: $text-primary;
            margin: 0 0 4px;
        }
        .c-title {
            font-size: 12px; color: $primary; background: $primary-light;
            border: 1px dashed $primary; border-radius: 10px; padding: 2px 10px;
        }
    }
    .specialty-tags {
        display: flex; flex-wrap: wrap; gap: 6px; margin-bottom: 12px;
        .s-tag {
            font-size: 12px; color: #7a6a58; background: #f7efe0;
            border: 1px dashed $border-color; border-radius: 10px; padding: 2px 9px;
        }
    }
    .c-meta {
        display: flex; align-items: center; gap: 8px; flex-wrap: wrap;
        font-size: 13px; color: $text-regular; padding: 10px 0;
        border-top: 1.5px dashed $border-color;
        .meta-divider { color: $border-color; }
    }
    .book-btn {
        margin-top: 6px; text-align: center; font-size: 14px;
        color: #fff; background: linear-gradient(135deg, $primary, $primary-dark);
        border-radius: $radius-md; padding: 9px 0; box-shadow: $shadow-button;
        transition: transform 0.2s ease;
    }
    &:hover .book-btn { transform: translateY(-1px); }
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
    position: absolute; left: 0; top: 50%;
    transform: translateY(-50%);
    .mine-link {
        display: inline-block; font-family: $font-rounded; color: $primary;
        background: $primary-light; border: 1.5px dashed $primary;
        border-radius: $radius-md; padding: 10px 24px; text-decoration: none; font-size: 14px;
        white-space: nowrap;
        transition: all 0.25s ease;
        &:hover { background: $primary; color: #fff; border-style: solid; }
    }
}

// 窄屏：绝对定位会与居中标题重叠，回退为标题上方左对齐的普通流元素
@media (max-width: 768px) {
    .mine-entry {
        position: static; transform: none;
        text-align: left; margin-bottom: 12px;
    }
}
</style>
