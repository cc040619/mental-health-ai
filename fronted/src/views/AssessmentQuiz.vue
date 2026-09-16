<template>
    <div class="quiz-container">
        <!-- 退出确认按钮 -->
        <div class="top-bar">
            <button class="exit-btn" @click="handleExit">← 退出并保存</button>
            <div class="scale-title" v-if="scaleDetail">{{ scaleDetail.name }}</div>
        </div>

        <!-- 进度条 -->
        <div class="progress-wrapper">
            <div class="progress-info">
                第 {{ currentIndex + 1 }} / {{ totalQuestions }} 题
            </div>
            <div class="progress-bar">
                <div class="progress-fill" :style="{ width: progressPercent + '%' }"></div>
            </div>
        </div>

        <div v-if="loading" class="loading-tip">載入中... 🌿</div>

        <div v-else-if="currentQuestion" class="question-card">
            <div class="card-pin"></div>
            <div class="q-index">第 {{ currentIndex + 1 }} 题</div>
            <p class="q-text">{{ currentQuestion.text }}</p>

            <div class="options">
                <label v-for="(opt, idx) in currentQuestion.options" :key="idx"
                       class="option-item"
                       :class="{ selected: selectedScore === opt.score }">
                    <input type="radio" :value="opt.score" v-model="selectedScore"
                           @change="selectOption(opt.score)" />
                    <span class="option-radio"></span>
                    <span class="option-label">{{ opt.label }}</span>
                </label>
            </div>

            <div class="nav-btns">
                <button class="nav-btn prev" v-if="currentIndex > 0" @click="prevQuestion">上一题</button>
                <button class="nav-btn next" v-if="currentIndex < totalQuestions - 1"
                        :disabled="selectedScore === null" @click="nextQuestion">下一题 →</button>
                <button class="nav-btn submit" v-else
                        :disabled="selectedScore === null" @click="handleSubmit">提交并查看报告 📊</button>
            </div>
        </div>

        <!-- 学术声明 -->
        <div class="disclaimer" v-if="scaleDetail">
            {{ scaleDetail.description }}
        </div>
    </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, ElLoading } from 'element-plus'
import { getScaleDetail, getRecordDetail, saveProgress, submitAssessment } from '@/api/assessment'

const route = useRoute()
const router = useRouter()
const recordId = route.params.recordId

const scaleDetail = ref(null)
const questions = ref([])
const answers = ref({})      // questionIndex -> score
const currentIndex = ref(0)
const selectedScore = ref(null)
const loading = ref(true)

const totalQuestions = computed(() => questions.value.length)
const currentQuestion = computed(() => questions.value[currentIndex.value])
const progressPercent = computed(() => {
    if (totalQuestions.value === 0) return 0
    return Math.round(((currentIndex.value + 1) / totalQuestions.value) * 100)
})

// 加载量表题目 + 已存进度
const loadData = async () => {
    loading.value = true
    try {
        // 获取记录详情（含 scaleId 与已存进度）
        const record = await getRecordDetail(recordId)
        const scaleId = record.scaleId
        // 获取量表题目
        const detail = await getScaleDetail(scaleId)
        scaleDetail.value = detail
        questions.value = detail.questions || []

        // 恢复已存答案
        const savedAnswers = record.answers || []
        const savedMap = {}
        savedAnswers.forEach(a => {
            const idx = Number(a.questionIndex)
            savedMap[idx] = Number(a.score)
        })
        answers.value = savedMap

        // 续答定位：定位到第一个未作答的题
        const resumed = Object.keys(savedMap).length > 0
        if (resumed) {
            let idx = 0
            while (idx < questions.value.length && savedMap[idx] !== undefined) {
                idx++
            }
            currentIndex.value = Math.min(idx, questions.value.length - 1)
        }

        selectedScore.value = answers.value[currentIndex.value] ?? null

        if (resumed) {
            ElMessage.info('已恢复上次进度，继续作答吧～')
        }
    } catch (e) {
        // 错误由拦截器提示
    } finally {
        loading.value = false
    }
}

const selectOption = (score) => {
    selectedScore.value = score
    answers.value[currentIndex.value] = score
}

const prevQuestion = () => {
    if (currentIndex.value > 0) {
        currentIndex.value--
        selectedScore.value = answers.value[currentIndex.value] ?? null
    }
}

const nextQuestion = () => {
    if (selectedScore.value === null) {
        ElMessage.warning('请先选择一个选项')
        return
    }
    // 保存当前题进度
    persistProgress()
    if (currentIndex.value < totalQuestions.value - 1) {
        currentIndex.value++
        selectedScore.value = answers.value[currentIndex.value] ?? null
    }
}

const buildAnswerList = () => Object.keys(answers.value).map(k => ({
    questionIndex: Number(k),
    score: answers.value[Number(k)]
}))

// 返回 Promise，提交前需 await：最后一题没有“下一题”，其答案只能靠这里同步到 Redis
const persistProgress = () => {
    return saveProgress(recordId, {
        currentIndex: currentIndex.value,
        answers: buildAnswerList()
    }).catch(() => { /* 静默，拦截器已提示 */ })
}

const handleExit = () => {
    ElMessageBox.confirm('退出前会保存当前进度，下次可继续作答。确定退出吗？', '保存并退出', {
        confirmButtonText: '保存并退出',
        cancelButtonText: '继续作答',
        type: 'warning'
    }).then(() => {
        saveProgress(recordId, {
            currentIndex: currentIndex.value,
            answers: buildAnswerList()
        }).finally(() => {
            router.push('/assessment')
        })
    }).catch(() => { /* 继续作答 */ })
}

const handleSubmit = () => {
    // 完整性校验
    for (let i = 0; i < totalQuestions.value; i++) {
        if (answers.value[i] === undefined || answers.value[i] === null) {
            ElMessage.warning(`第 ${i + 1} 题尚未作答，请完成全部题目`)
            currentIndex.value = i
            selectedScore.value = answers.value[i] ?? null
            return
        }
    }
    ElMessageBox.confirm('提交后将生成测评报告，确认提交吗？', '提交测评', {
        confirmButtonText: '确认提交',
        cancelButtonText: '再检查下',
        type: 'info'
    }).then(async () => {
        const loading = ElLoading.service({
            lock: true,
            text: '正在生成测评报告，请稍候...',
            background: 'rgba(255, 248, 231, 0.85)',
            spinner: 'el-icon-loading',
            customClass: 'submit-loading'
        })
        try {
            // 后端提交时以 Redis 进度为准，须先把最后一题答案同步进去，否则误报“还有未作答”
            await persistProgress()
            await submitAssessment(recordId, { answers: buildAnswerList() })
            ElMessage.success('测评完成！正在跳转到报告页...')
            router.replace(`/assessment/report/${recordId}`)
        } catch (e) {
            // 错误由拦截器提示
        } finally {
            loading.close()
        }
    }).catch(() => { /* 取消 */ })
}

onMounted(() => {
    loadData()
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

.quiz-container {
    min-height: calc(100vh - 130px);
    background: $bg-page;
    font-family: $font-rounded;
    color: $text-primary;
    padding: 20px 16px 48px;
    max-width: 720px;
    margin: 0 auto;
}

.top-bar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 18px;

    .exit-btn {
        font-family: $font-rounded;
        background: $bg-card;
        border: 1.5px dashed $primary;
        color: $primary;
        border-radius: $radius-md;
        padding: 6px 14px;
        font-size: 13px;
        cursor: pointer;
        transition: all 0.2s ease;

        &:hover {
            background: $primary-light;
            border-style: solid;
        }
    }

    .scale-title {
        font-family: $font-handwritten;
        font-size: 18px;
        color: $text-primary;
    }
}

.progress-wrapper {
    margin-bottom: 24px;

    .progress-info {
        text-align: center;
        font-size: 14px;
        color: $text-secondary;
        margin-bottom: 8px;
    }

    .progress-bar {
        height: 10px;
        background: $bg-card;
        border: 1.5px solid $border-color;
        border-radius: 6px;
        overflow: hidden;
    }

    .progress-fill {
        height: 100%;
        background: linear-gradient(90deg, $primary, $primary-dark);
        border-radius: 6px;
        transition: width 0.4s ease;
    }
}

.loading-tip {
    text-align: center;
    padding: 60px 0;
    color: $text-secondary;
}

.question-card {
    position: relative;
    background: $bg-card;
    border-radius: $radius-lg;
    border: 1.5px solid $border-color;
    box-shadow: $shadow-card;
    padding: 28px 24px 22px;
    margin-bottom: 18px;

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

    .q-index {
        text-align: center;
        font-size: 13px;
        color: $primary;
        font-weight: 600;
        margin-bottom: 8px;
    }

    .q-text {
        font-size: 17px;
        line-height: 1.6;
        color: $text-primary;
        text-align: center;
        margin: 0 0 22px;
    }
}

.options {
    display: flex;
    flex-direction: column;
    gap: 10px;
    margin-bottom: 24px;

    .option-item {
        display: flex;
        align-items: center;
        gap: 12px;
        padding: 12px 16px;
        background: $bg-page;
        border: 1.5px solid $border-color;
        border-radius: $radius-md;
        cursor: pointer;
        transition: all 0.2s ease;

        input[type="radio"] {
            display: none;
        }

        .option-radio {
            width: 18px;
            height: 18px;
            border: 2px solid $border-color;
            border-radius: 50%;
            position: relative;
            flex-shrink: 0;
            transition: all 0.2s ease;

            &::after {
                content: '';
                position: absolute;
                top: 50%;
                left: 50%;
                transform: translate(-50%, -50%) scale(0);
                width: 10px;
                height: 10px;
                background: $primary;
                border-radius: 50%;
                transition: transform 0.2s ease;
            }
        }

        .option-label {
            font-size: 15px;
            color: $text-regular;
        }

        &:hover {
            border-color: $primary;
            background: $primary-light;
        }

        &.selected {
            border-color: $primary;
            background: $primary-light;

            .option-radio {
                border-color: $primary;

                &::after {
                    transform: translate(-50%, -50%) scale(1);
                }
            }

            .option-label {
                color: $primary-dark;
                font-weight: 600;
            }
        }
    }
}

.nav-btns {
    display: flex;
    justify-content: space-between;
    gap: 12px;

    .nav-btn {
        flex: 1;
        font-family: $font-rounded;
        border: none;
        border-radius: $radius-md;
        padding: 12px 16px;
        font-size: 14px;
        font-weight: 600;
        cursor: pointer;
        transition: all 0.2s ease;

        &.prev {
            background: $bg-page;
            border: 1.5px solid $border-color;
            color: $text-regular;
        }

        &.next, &.submit {
            background: linear-gradient(135deg, $primary, $primary-dark);
            color: #fff;
            box-shadow: $shadow-button;
        }

        &:disabled {
            opacity: 0.5;
            cursor: not-allowed;
            box-shadow: none;
        }

        &:not(:disabled):hover {
            transform: translateY(-1px);
        }
    }
}

.disclaimer {
    background: $bg-card;
    border: 1.5px dashed $border-color;
    border-radius: $radius-md;
    padding: 14px 16px;
    font-size: 12px;
    color: $text-secondary;
    line-height: 1.6;
    text-align: center;
}
</style>
