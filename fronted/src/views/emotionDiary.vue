<template>
    <div class="emotionDiary-container">
        <!-- ===== 手账风：纸张纹理背景层 ===== -->
        <div class="paper-texture"></div>

        <!-- ===== 手账风：漂浮装饰 emoji ===== -->
        <div class="floating-decorations">
            <span class="float-emoji float-1">🌸</span>
            <span class="float-emoji float-2">🍃</span>
            <span class="float-emoji float-3">✨</span>
            <span class="float-emoji float-4">📖</span>
            <span class="float-emoji float-5">🎀</span>
        </div>

        <!-- ===== 页面主体 ===== -->
        <div class="diary-wrapper">
            <!-- ===== 手账风：标题区（便签纸 + 胶带装饰） ===== -->
            <div class="header-section">
                <div class="header-tape"></div>
                <div class="header-content">
                    <el-image :src="iconUrl" style="width: 56px; height: 56px" class="header-icon"></el-image>
                    <div class="header-text">
                        <h1>📖 情绪日志</h1>
                        <p class="header-subtitle">每一天的心情都值得被温柔记录 🌿</p>
                    </div>
                    <div class="header-date-badge">
                        <span class="date-label">今天</span>
                        <span class="date-value">{{ diaryForm.diaryDate }}</span>
                    </div>
                </div>
            </div>

            <!-- ===== 上半区：情绪评分 + 主要情绪（左右并排，填满宽度） ===== -->
            <div class="upper-grid">
                <!-- 情绪评分卡片 -->
                <div class="diary-card score-card">
                    <div class="card-tape"></div>
                    <div class="card-title-row">
                        <span class="title">✨ 今日情绪评分</span>
                    </div>
                    <div class="section">
                        <p class="section-desc">您今天的整体情绪状态如何？(1-10分)</p>
                        <div class="rate-wrapper">
                            <el-rate 
                                v-model="diaryForm.moodScore"
                                :texts="emotionStatus"
                                show-texts
                                :max="10"
                                size="large"
                            />
                        </div>
                    </div>
                    <!-- 装饰性情绪光谱条 -->
                    <div class="mood-spectrum">
                        <div class="spectrum-bar"></div>
                        <div class="spectrum-labels">
                            <span>😢</span>
                            <span>😐</span>
                            <span>😊</span>
                            <span>🤩</span>
                        </div>
                    </div>
                </div>

                <!-- 主要情绪卡片 -->
                <div class="diary-card emotion-select-card">
                    <div class="card-tape tape-pink"></div>
                    <div class="card-title-row">
                        <span class="title">🌻 主要情绪</span>
                    </div>
                    <div class="emotion-grid">
                        <div v-for="emotion in emotionOptions" :key="emotion.name" class="emotion-item" :class="{'selected': emotion.name === diaryForm.dominantEmotion}" @click="selectEmotion(emotion.name)">
                            <el-image :src="emotion.url" style="width: 44px; height: 44px"></el-image>
                            <div class="emotion-name">{{emotion.name}}</div>
                        </div>
                    </div>
                </div>
            </div>

            <!-- ===== 详细记录卡片（全宽，内部双列布局填满空间） ===== -->
            <div class="diary-card detail-card">
                <div class="card-tape tape-green"></div>
                <div class="card-title-row">
                    <span class="title">🖊️ 详细记录</span>
                </div>
                <div class="detail-form">
                    <!-- 双列：触发因素 + 今日感想 并排 -->
                    <div class="form-row two-col">
                        <div class="form-group">
                            <div class="form-label">📌 情绪触发因素</div>
                            <el-input v-model="diaryForm.emotionTriggers" placeholder="今天什么事情影响了您的情绪？" type="textarea" :rows="3" maxLength="1000" show-word-limit></el-input>
                        </div>
                         <div class="form-group">
                            <div class="form-label">💭 今日感想</div>
                            <el-input v-model="diaryForm.diaryContent" placeholder="写下您今天的想法、感受或发生的有趣事情..." type="textarea" :rows="5" maxLength="2000" show-word-limit></el-input>
                        </div>
                    </div>
                    <!-- 双列：生活指标 并排 -->
                    <div class="life-indicators">
                        <div class="indicator-group">
                            <div class="form-label">🌙 睡眠质量</div>
                           <el-select v-model="diaryForm.sleepQuality" placeholder="请选择">
                                <el-option label="很差" :value="1"></el-option>
                                <el-option label="较差" :value="2"></el-option>
                                <el-option label="一般" :value="3"></el-option>
                                <el-option label="良好" :value="4"></el-option>
                                <el-option label="优秀" :value="5"></el-option>
                            </el-select>
                        </div>
                        <div class="indicator-group">
                            <div class="form-label">📊 压力水平</div>
                            <el-select v-model="diaryForm.stressLevel" placeholder="请选择">
                                <el-option label="很低" :value="1"></el-option>
                                <el-option label="较低" :value="2"></el-option>
                                <el-option label="中等" :value="3"></el-option>
                                <el-option label="较高" :value="4"></el-option>
                                <el-option label="很高" :value="5"></el-option>
                            </el-select>
                        </div>
                    </div>
                    <!-- 虚线分隔 -->
                    <div class="form-divider"></div>
                    <!-- 操作按钮 -->
                    <div class="action-buttons">
                        <el-button  @click="resetForm">↺ 重置</el-button>
                        <el-button type="primary" @click="submit">✅ 提交记录</el-button>
                    </div>
                </div>
            </div>

            <!-- ===== 底部：治愈贴士卡片群（4列填满横向空间） ===== -->
            <div class="tips-row">
                <div class="tip-card tip-warm">
                    <span class="tip-emoji">💡</span>
                    <div class="tip-body">
                        <div class="tip-title">记录小贴士</div>
                        <div class="tip-desc">坚持每天记录情绪，你会发现内心的变化规律</div>
                    </div>
                </div>
                <div class="tip-card tip-pink">
                    <span class="tip-emoji">🌸</span>
                    <div class="tip-body">
                        <div class="tip-title">温柔对待自己</div>
                        <div class="tip-desc">每一种情绪都是合理的，接纳它们是治愈的开始</div>
                    </div>
                </div>
                <div class="tip-card tip-green">
                    <span class="tip-emoji">🌿</span>
                    <div class="tip-body">
                        <div class="tip-title">呼吸练习</div>
                        <div class="tip-desc">感到焦虑时，试试深呼吸三次，让心慢慢静下来</div>
                    </div>
                </div>
                <div class="tip-card tip-yellow">
                    <span class="tip-emoji">⭐</span>
                    <div class="tip-body">
                        <div class="tip-title">每日小确幸</div>
                        <div class="tip-desc">记录今天一件让你微笑的小事，积攒幸福感</div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>
<script setup>
    import { dayjs, ElMessage } from 'element-plus'
    import { ref, reactive } from 'vue'
    import { addEmotionDiary } from '@/api/frontend'

    // 情绪评分
    const emotionStatus = ['绝望崩溃', '消沉抑郁', '焦虑烦躁', '低落不悦', '平静淡然', '轻松惬意', '愉悦舒心', '欢欣满足', '兴奋欣喜', '极致幸福']

    // 情绪选项
    const emotionOptions = [
        { name: '开心', url: new URL('@/assets/images/开心.png', import.meta.url).href },
        { name: '平静', url: new URL('@/assets/images/平静.png', import.meta.url).href },
        { name: '焦虑', url: new URL('@/assets/images/焦虑.png', import.meta.url).href },
        { name: '悲伤', url: new URL('@/assets/images/悲伤.png', import.meta.url).href },
        { name: '兴奋', url: new URL('@/assets/images/兴奋.png', import.meta.url).href },
        { name: '疲惫', url: new URL('@/assets/images/疲惫.png', import.meta.url).href },
        { name: '惊讶', url: new URL('@/assets/images/惊讶.png', import.meta.url).href },
        { name: '困惑', url: new URL('@/assets/images/困惑.png', import.meta.url).href },
    ]

    const selectEmotion = (emotion) => {
        diaryForm.dominantEmotion = emotion
    }

    const diaryForm = reactive({    
        diaryDate: dayjs().format('YYYY-MM-DD'),
        moodScore: null,
        dominantEmotion: '',
        emotionTriggers: '',
        diaryContent: '',
        sleepQuality: null,
        stressLevel: null
    })

    const resetForm = () => {
        Object.assign(diaryForm, {
            diaryDate: dayjs().format('YYYY-MM-DD'),
            moodScore: null,
            dominantEmotion: '',
            emotionTriggers: '',
            diaryContent: '',
            sleepQuality: null,
            stressLevel: null
        })
    }

    const submit = () => {
        console.log(diaryForm)
        if (!diaryForm.moodScore) {
            ElMessage.error('请选择情绪评分')
            return
        }
        addEmotionDiary(diaryForm).then(() => {
            ElMessage.success('提交成功')
            resetForm()
        })
    }

    const iconUrl = new URL('@/assets/images/like.png', import.meta.url).href
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

// ===== 手账风：页面容器 + 纸张纹理 =====
.emotionDiary-container {
    position: relative;
    min-height: calc(100vh - 130px);
    background: $bg-page;
    font-family: $font-rounded;
    color: $text-primary;
    overflow: hidden;
}

// 纸张纹理层：网格线 + 暖色光晕
.paper-texture {
    position: fixed;
    top: 0; left: 0; right: 0; bottom: 0;
    pointer-events: none;
    z-index: 0;
    background-image:
        linear-gradient(rgba(232, 220, 200, 0.25) 1px, transparent 1px),
        linear-gradient(90deg, rgba(232, 220, 200, 0.25) 1px, transparent 1px),
        radial-gradient(ellipse at 20% 10%, rgba(247, 232, 166, 0.18), transparent 50%),
        radial-gradient(ellipse at 80% 80%, rgba(232, 146, 124, 0.08), transparent 50%);
    background-size: 28px 28px, 28px 28px, 100% 100%, 100% 100%;
}

// ===== 手账风：漂浮装饰 emoji =====
.floating-decorations {
    position: fixed;
    top: 0; left: 0; right: 0; bottom: 0;
    pointer-events: none;
    z-index: 0;
    .float-emoji {
        position: absolute;
        font-size: 28px;
        opacity: 0.35;
        animation: floatDrift 8s ease-in-out infinite;
    }
    .float-1 { top: 12%; left: 5%; animation-delay: 0s; }
    .float-2 { top: 28%; right: 6%; animation-delay: 1.5s; }
    .float-3 { bottom: 22%; left: 8%; animation-delay: 3s; }
    .float-4 { bottom: 10%; right: 10%; animation-delay: 4.5s; }
    .float-5 { top: 50%; left: 3%; animation-delay: 2s; font-size: 20px; }
}
@keyframes floatDrift {
    0%, 100% { transform: translateY(0) rotate(0deg); }
    50% { transform: translateY(-18px) rotate(8deg); }
}

// ===== 手账风：主体内容区（加宽填满） =====
.diary-wrapper {
    position: relative;
    z-index: 1;
    max-width: 1320px;
    margin: 0 auto;
    padding: 28px 40px 48px;
}

// ===== 手账风：标题区 =====
.header-section {
    position: relative;
    background: $bg-card;
    border-radius: $radius-xl;
    padding: 28px 36px;
    box-shadow: $shadow-card;
    margin-bottom: 24px;
    overflow: hidden;
    // 顶部和纸胶带
    .header-tape {
        position: absolute;
        top: -8px;
        left: 50%;
        transform: translateX(-50%) rotate(-1deg);
        width: 180px;
        height: 28px;
        background: linear-gradient(90deg,
            rgba(232, 146, 124, 0.15),
            rgba(232, 146, 124, 0.45) 30%,
            rgba(247, 232, 166, 0.45) 70%,
            rgba(247, 232, 166, 0.15));
        border-radius: 2px;
    }
    .header-content {
        display: flex;
        align-items: center;
        gap: 20px;
        .header-icon {
            flex-shrink: 0;
            filter: drop-shadow(0 2px 6px rgba(232, 146, 124, 0.25));
        }
        .header-text {
            flex: 1;
            h1 {
                font-family: $font-handwritten;
                font-size: 30px;
                color: $text-primary;
                margin: 0;
                line-height: 1.3;
            }
            .header-subtitle {
                font-size: 14px;
                color: $text-secondary;
                margin: 4px 0 0;
            }
        }
        // 日期便签
        .header-date-badge {
            flex-shrink: 0;
            display: flex;
            flex-direction: column;
            align-items: center;
            padding: 10px 20px;
            background: linear-gradient(135deg, $accent-yellow, rgba(247, 232, 166, 0.5));
            border-radius: $radius-md;
            border: 1px dashed $primary;
            .date-label {
                font-size: 12px;
                color: $text-secondary;
                margin-bottom: 2px;
            }
            .date-value {
                font-family: $font-handwritten;
                font-size: 18px;
                color: $primary-dark;
                font-weight: bold;
            }
        }
    }
}

// ===== 手账风：上半区双列布局 =====
.upper-grid {
    display: flex;
    gap: 24px;
    margin-bottom: 24px;
}

// ===== 手账风：通用卡片样式 =====
.diary-card {
    position: relative;
    background: $bg-card;
    border-radius: $radius-xl;
    padding: 28px 32px;
    box-shadow: $shadow-card;
    border: 1px solid $border-light;
    // 顶部胶带装饰
    .card-tape {
        position: absolute;
        top: -6px;
        left: 32px;
        width: 80px;
        height: 18px;
        background: linear-gradient(90deg,
            rgba(232, 146, 124, 0.1),
            rgba(232, 146, 124, 0.4),
            rgba(232, 146, 124, 0.1));
        border-radius: 2px;
        transform: rotate(-2deg);
        &.tape-pink {
            background: linear-gradient(90deg,
                rgba(245, 198, 198, 0.1),
                rgba(245, 198, 198, 0.5),
                rgba(245, 198, 198, 0.1));
        }
        &.tape-green {
            background: linear-gradient(90deg,
                rgba(184, 216, 190, 0.1),
                rgba(184, 216, 190, 0.5),
                rgba(184, 216, 190, 0.1));
        }
    }
    .card-title-row {
        margin-bottom: 20px;
        padding-bottom: 14px;
        border-bottom: 2px dashed $divider;
        .title {
            font-family: $font-handwritten;
            font-size: 24px;
            color: $text-primary;
        }
    }
}

// ===== 情绪评分卡片 =====
.score-card {
    flex: 0 0 420px;
    .section {
        .section-desc {
            font-size: 15px;
            color: $text-secondary;
            margin-bottom: 18px;
        }
        .rate-wrapper {
            display: flex;
            justify-content: center;
            padding: 12px 0;
        }
    }
    // 装饰性情绪光谱条
    .mood-spectrum {
        margin-top: 20px;
        padding-top: 16px;
        border-top: 1px dashed $divider;
        .spectrum-bar {
            height: 10px;
            border-radius: 5px;
            background: linear-gradient(90deg,
                #8B7D6B 0%,
                #B8A898 20%,
                $accent-yellow 40%,
                $accent-green 60%,
                $primary 80%,
                $primary-dark 100%);
        }
        .spectrum-labels {
            display: flex;
            justify-content: space-between;
            margin-top: 6px;
            font-size: 18px;
        }
    }
}

// ===== 主要情绪卡片 =====
.emotion-select-card {
    flex: 1;
    .emotion-grid {
        display: grid;
        grid-template-columns: repeat(4, 1fr);
        gap: 14px;
        .emotion-item {
            display: flex;
            flex-direction: column;
            align-items: center;
            gap: 8px;
            padding: 16px 8px;
            border: 2px solid $border-color;
            border-radius: $radius-lg;
            cursor: pointer;
            background: $bg-page;
            transition: all 0.25s ease;
            &:hover {
                border-color: $primary;
                transform: translateY(-3px);
                box-shadow: 0 4px 12px rgba(232, 146, 124, 0.15);
            }
            &.selected {
                border-color: $primary;
                background: rgba(232, 146, 124, 0.08);
                transform: translateY(-3px);
                box-shadow: 0 4px 12px rgba(232, 146, 124, 0.2);
                .emotion-name {
                    color: $primary-dark;
                    font-weight: bold;
                }
            }
            .emotion-name {
                font-size: 14px;
                color: $text-regular;
                transition: color 0.25s ease;
            }
        }
    }
}

// ===== 详细记录卡片（全宽） =====
.detail-card {
    margin-bottom: 24px;
    .detail-form {
        .form-row.two-col {
            display: flex;
            gap: 24px;
            margin-bottom: 20px;
            .form-group {
                flex: 1;
                display: flex;
                flex-direction: column;
            }
        }
        .form-group {
            .form-label {
                margin-bottom: 8px;
                font-size: 15px;
                color: $text-primary;
                font-weight: 600;
            }
        }
        // 生活指标双列
        .life-indicators {
            display: flex;
            gap: 24px;
            margin-bottom: 20px;
            .indicator-group {
                flex: 1;
                .form-label {
                    margin-bottom: 8px;
                    font-size: 15px;
                    color: $text-primary;
                    font-weight: 600;
                }
            }
        }
        // 虚线分隔
        .form-divider {
            border-top: 2px dashed $divider;
            margin: 8px 0 24px;
        }
        // 操作按钮
        .action-buttons {
            display: flex;
            justify-content: center;
            gap: 20px;
        }
    }
}

// ===== 手账风：底部治愈贴士卡片群 =====
.tips-row {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 20px;
    .tip-card {
        display: flex;
        align-items: flex-start;
        gap: 14px;
        padding: 20px;
        border-radius: $radius-lg;
        border: 1px solid $border-light;
        box-shadow: $shadow-card;
        transition: transform 0.25s ease, box-shadow 0.25s ease;
        &:hover {
            transform: translateY(-4px);
            box-shadow: 0 8px 24px rgba(180, 140, 100, 0.18);
        }
        .tip-emoji {
            font-size: 32px;
            flex-shrink: 0;
            line-height: 1;
        }
        .tip-body {
            .tip-title {
                font-family: $font-handwritten;
                font-size: 17px;
                color: $text-primary;
                margin-bottom: 6px;
            }
            .tip-desc {
                font-size: 13px;
                color: $text-secondary;
                line-height: 1.6;
            }
        }
        // 不同色调的贴士卡
        &.tip-warm {
            background: linear-gradient(135deg, $bg-card, rgba(232, 146, 124, 0.06));
            border-top: 3px solid $primary;
        }
        &.tip-pink {
            background: linear-gradient(135deg, $bg-card, rgba(245, 198, 198, 0.1));
            border-top: 3px solid $accent-pink;
        }
        &.tip-green {
            background: linear-gradient(135deg, $bg-card, rgba(184, 216, 190, 0.1));
            border-top: 3px solid $accent-green;
        }
        &.tip-yellow {
            background: linear-gradient(135deg, $bg-card, rgba(247, 232, 166, 0.12));
            border-top: 3px solid $accent-yellow;
        }
    }
}

// ===== 手账风：Element Plus 组件穿透定制 =====
// 输入框 / 文本域
:deep(.el-input__wrapper),
:deep(.el-textarea__inner) {
    border-radius: $radius-md !important;
    border: 1px solid $border-color !important;
    background: $bg-input !important;
    box-shadow: none !important;
    font-family: $font-rounded !important;
    color: $text-primary !important;
    transition: all 0.25s ease !important;
    &:hover {
        border-color: $primary !important;
    }
    &:focus, &.is-focus {
        border-color: $primary !important;
        box-shadow: $shadow-input-focus !important;
    }
}
:deep(.el-textarea__inner) {
    padding: 12px 16px !important;
    &::placeholder {
        color: $text-placeholder !important;
    }
}
// 字数统计
:deep(.el-input__count) {
    background: transparent !important;
    color: $text-secondary !important;
    font-size: 12px;
}

// 评分组件
:deep(.el-rate) {
    display: flex;
    flex-wrap: wrap;
    gap: 2px;
    .el-rate__icon {
        font-size: 28px;
        transition: transform 0.15s ease;
        &.hover {
            transform: scale(1.25);
        }
    }
    .el-rate__text {
        font-family: $font-handwritten !important;
        font-size: 18px !important;
        color: $primary-dark !important;
        margin-left: 12px !important;
    }
}

// 下拉选择器
:deep(.el-select) {
    width: 100%;
    .el-select__wrapper {
        border-radius: $radius-md !important;
        border: 1px solid $border-color !important;
        background: $bg-input !important;
        box-shadow: none !important;
        min-height: 42px;
        transition: all 0.25s ease !important;
        &:hover {
            border-color: $primary !important;
        }
        &.is-focused {
            border-color: $primary !important;
            box-shadow: $shadow-input-focus !important;
        }
    }
}

// 按钮
:deep(.el-button) {
    border-radius: $radius-md !important;
    font-family: $font-rounded !important;
    font-size: 15px !important;
    padding: 10px 28px !important;
    transition: all 0.2s ease !important;
    // 默认按钮（重置）
    &:not(.el-button--primary) {
        border: 2px dashed $border-color !important;
        background: $bg-card !important;
        color: $text-secondary !important;
        &:hover {
            border-color: $primary !important;
            border-style: solid !important;
            color: $primary-dark !important;
            background: rgba(232, 146, 124, 0.06) !important;
        }
    }
    // 主按钮（提交）
    &.el-button--primary {
        background: linear-gradient(135deg, $primary, $primary-dark) !important;
        border: none !important;
        color: #fff !important;
        box-shadow: $shadow-button !important;
        &:hover {
            transform: translateY(-2px);
            box-shadow: 0 5px 0 $primary-darker, 0 6px 16px rgba(180, 140, 100, 0.3) !important;
        }
        &:active {
            transform: translateY(1px);
            box-shadow: 0 1px 0 $primary-darker, 0 2px 6px rgba(180, 140, 100, 0.15) !important;
        }
    }
}

// ===== 响应式适配 =====
@media (max-width: 1100px) {
    .diary-wrapper {
        max-width: 100%;
        padding: 20px 24px 40px;
    }
    .upper-grid {
        flex-direction: column;
        .score-card {
            flex: 1;
        }
    }
    .detail-card .detail-form .form-row.two-col {
        flex-direction: column;
    }
    .tips-row {
        grid-template-columns: repeat(2, 1fr);
    }
}
@media (max-width: 640px) {
    .diary-wrapper {
        padding: 16px 12px 32px;
    }
    .header-section {
        padding: 20px 16px;
        .header-content {
            flex-wrap: wrap;
            .header-date-badge {
                width: 100%;
                flex-direction: row;
                justify-content: center;
                gap: 8px;
            }
        }
    }
    .diary-card {
        padding: 20px 16px;
    }
    .emotion-select-card .emotion-grid {
        grid-template-columns: repeat(2, 1fr);
    }
    .life-indicators {
        flex-direction: column;
    }
    .tips-row {
        grid-template-columns: 1fr;
    }
}
</style>
