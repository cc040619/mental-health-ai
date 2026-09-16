<template>
    <!-- ===== 个人安全计划编辑（三步引导：可信赖的人 / 应对策略 / 紧急联系方式）===== -->
    <div class="safety-plan">
        <!-- 顶部进度 -->
        <el-steps :active="activeStep" finish-status="success" align-center class="plan-steps">
            <el-step title="可信赖的人" icon="👤"/>
            <el-step title="应对策略" icon="🌿"/>
            <el-step title="紧急联系" icon="📞"/>
        </el-steps>

        <!-- 步骤 1：可信赖的人 -->
        <div v-show="activeStep === 0" class="step-section">
            <p class="step-hint">写下你信任、能在难熬时联系的人。哪怕只是一个人，也是一份支撑。</p>
            <div v-for="(person, idx) in form.lovedOnes" :key="idx" class="person-row">
                <el-input v-model="person.name" placeholder="姓名 ✏️" maxlength="30" class="cell-name"/>
                <el-input v-model="person.relation" placeholder="关系（如朋友/家人）" maxlength="20" class="cell-relation"/>
                <el-input v-model="person.phone" placeholder="电话 📞" maxlength="20" class="cell-phone"/>
                <el-button v-if="form.lovedOnes.length > 1" text type="warning" @click="removePerson(idx)">✕</el-button>
            </div>
            <el-button text class="add-btn" @click="addPerson">+ 添加一位</el-button>
        </div>

        <!-- 步骤 2：应对策略 -->
        <div v-show="activeStep === 1" class="step-section">
            <p class="step-hint">写下难熬时能让你稍微平静一点的事。比如散步、听歌、深呼吸、找人说说话。</p>
            <div v-for="(strategy, idx) in form.copingStrategies" :key="idx" class="strategy-row">
                <span class="strategy-bullet">🌿</span>
                <el-input v-model="form.copingStrategies[idx]" placeholder="例如：出去走十分钟" maxlength="50"/>
                <el-button v-if="form.copingStrategies.length > 1" text type="warning" @click="removeStrategy(idx)">✕</el-button>
            </div>
            <el-button text class="add-btn" @click="addStrategy">+ 添加一条</el-button>
        </div>

        <!-- 步骤 3：紧急联系方式 -->
        <div v-show="activeStep === 2" class="step-section">
            <p class="step-hint">留下一个随时能联系到的方式，比如最亲近的人的电话、或你常去的援助机构。</p>
            <el-input v-model="form.emergencyContact" placeholder="紧急联系方式 📞" maxlength="50" class="emergency-input"/>
            <div class="review-card">
                <div class="review-title">📒 我的安心清单</div>
                <div class="review-row">
                    <span class="review-label">可信赖的人</span>
                    <span class="review-value">{{ lovedOnesSummary }}</span>
                </div>
                <div class="review-row">
                    <span class="review-label">应对策略</span>
                    <span class="review-value">{{ strategiesSummary }}</span>
                </div>
                <div class="review-row">
                    <span class="review-label">紧急联系</span>
                    <span class="review-value">{{ form.emergencyContact || '尚未填写' }}</span>
                </div>
            </div>
        </div>

        <!-- 底部操作 -->
        <div class="plan-actions">
            <el-button v-if="activeStep > 0" @click="activeStep--">上一步</el-button>
            <el-button v-if="activeStep < 2" type="primary" @click="nextStep">下一步</el-button>
            <el-button v-if="activeStep === 2" type="primary" :loading="saving" @click="onSave">保存我的安全计划</el-button>
        </div>
    </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getSafetyPlan, saveSafetyPlan } from '@/api/crisis'

// 安全计划编辑组件：加载 → 三步填写 → 保存
// 可嵌入 crisisSafety 路由页或 dialog 中；保存后 emit('saved')
const emit = defineEmits(['saved'])

const activeStep = ref(0)
const saving = ref(false)
const form = ref({
    lovedOnes: [{name: '', relation: '', phone: ''}],
    copingStrategies: [''],
    emergencyContact: ''
})

// 预览摘要
const lovedOnesSummary = computed(() => {
    const names = form.value.lovedOnes.filter(p => p.name).map(p => p.name)
    return names.length ? names.join('、') : '尚未填写'
})
const strategiesSummary = computed(() => {
    const items = form.value.copingStrategies.filter(s => s)
    return items.length ? items.join('；') : '尚未填写'
})

const addPerson = () => {
    form.value.lovedOnes.push({name: '', relation: '', phone: ''})
}
const removePerson = (idx) => {
    form.value.lovedOnes.splice(idx, 1)
}
const addStrategy = () => {
    form.value.copingStrategies.push('')
}
const removeStrategy = (idx) => {
    form.value.copingStrategies.splice(idx, 1)
}

const nextStep = () => {
    if (activeStep.value < 2) activeStep.value++
}

const onSave = async () => {
    saving.value = true
    try {
        await saveSafetyPlan({
            lovedOnes: form.value.lovedOnes,
            copingStrategies: form.value.copingStrategies,
            emergencyContact: form.value.emergencyContact
        })
        ElMessage.success('安全计划已保存，难熬时可以翻开看看')
        emit('saved')
    } catch (e) {
        // request 拦截器已统一提示错误
    } finally {
        saving.value = false
    }
}

// 加载已有计划
const loadPlan = async () => {
    try {
        const data = await getSafetyPlan()
        if (data) {
            if (data.lovedOnes && data.lovedOnes.length) {
                form.value.lovedOnes = data.lovedOnes
            }
            if (data.copingStrategies && data.copingStrategies.length) {
                form.value.copingStrategies = data.copingStrategies
            }
            if (data.emergencyContact) {
                form.value.emergencyContact = data.emergencyContact
            }
        }
    } catch (e) {
        // 加载失败静默（空模板即可填写）
    }
}

onMounted(loadPlan)
</script>

<style scoped lang="scss">
// ===== 手账风：安全计划编辑器 =====
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
$divider: #E0D5C0;
$shadow-card: 0 4px 16px rgba(180, 140, 100, 0.12);
$radius-md: 12px;
$radius-lg: 16px;
$font-handwritten: "Ma Shan Zheng", "Comic Sans MS", "Segoe Print", "Bradley Hand", cursive;
$font-rounded: "Nunito", "Quicksand", "PingFang SC", "Microsoft YaHei", sans-serif;

.safety-plan {
    font-family: $font-rounded;
    color: $text-primary;
}

.plan-steps {
    margin-bottom: 24px;

    :deep(.el-step__title.is-process),
    :deep(.el-step__title.is-wait) {
        color: $text-secondary;
        font-weight: 500;
    }

    :deep(.el-step__title.is-finish) {
        color: $primary-dark;
        font-weight: 600;
    }

    :deep(.el-step__icon.is-text) {
        background: $bg-card;
        border-color: $border-color;
    }

    :deep(.el-step__icon.is-text.is-finish) {
        background: $primary;
        border-color: $primary;
        color: #fff;
    }

    :deep(.el-step__icon.is-text.is-process) {
        background: $primary;
        border-color: $primary;
        color: #fff;
    }

    :deep(.el-step__line) {
        background-color: $divider;
    }
}

.step-section {
    background: $bg-card;
    border: 1px solid $border-color;
    border-radius: $radius-lg;
    padding: 20px;
    box-shadow: $shadow-card;
}

.step-hint {
    font-size: 14px;
    color: $text-regular;
    line-height: 1.7;
    margin: 0 0 16px;
    padding: 10px 12px;
    background: $accent-yellow;
    border-left: 3px solid $primary-dark;
    border-radius: 8px;
}

.person-row,
.strategy-row {
    display: flex;
    gap: 8px;
    align-items: center;
    margin-bottom: 10px;
}

.cell-name {
    width: 120px;
    flex-shrink: 0;
}

.cell-relation {
    flex: 1;
}

.cell-phone {
    width: 160px;
    flex-shrink: 0;
}

.strategy-bullet {
    font-size: 18px;
    flex-shrink: 0;
}

.add-btn {
    color: $primary-dark;
    font-weight: 600;
    margin-top: 4px;
    border-style: dashed;
}

.emergency-input {
    width: 100%;
    margin-bottom: 16px;
}

.review-card {
    background: $bg-page;
    border: 1px dashed $border-color;
    border-radius: $radius-md;
    padding: 14px 16px;
}

.review-title {
    font-family: $font-handwritten;
    font-size: 16px;
    color: $primary-dark;
    margin-bottom: 10px;
}

.review-row {
    display: flex;
    gap: 12px;
    padding: 6px 0;
    border-bottom: 1px dashed $divider;
    font-size: 13px;

    &:last-child {
        border-bottom: none;
    }
}

.review-label {
    color: $text-secondary;
    width: 90px;
    flex-shrink: 0;
}

.review-value {
    color: $text-primary;
    flex: 1;
    word-break: break-all;
}

.plan-actions {
    display: flex;
    justify-content: flex-end;
    gap: 10px;
    margin-top: 20px;

    :deep(.el-button--primary) {
        background: linear-gradient(135deg, $primary, $primary-dark);
        border: none;
        font-weight: 600;
        box-shadow: 0 2px 0 $primary-darker, 0 3px 8px rgba(180, 140, 100, 0.2);

        &:hover {
            transform: translateY(-1px);
        }
    }
}

:deep(.el-input__wrapper) {
    border-radius: $radius-md;
    background: $bg-card;

    &:hover,
    &.is-focus {
        box-shadow: 0 0 0 1px $primary inset, 0 0 0 3px rgba(232, 146, 124, 0.15);
    }
}

@media (max-width: 768px) {
    .person-row {
        flex-wrap: wrap;

        .cell-name,
        .cell-relation,
        .cell-phone {
            width: 100%;
            flex: 1 1 100%;
        }
    }
}
</style>
