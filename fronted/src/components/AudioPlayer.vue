<template>
    <!-- 全局沉浸式音频播放器：挂载在 FrontendLayout 底部，切路由不销毁 -->
    <transition name="player-slide">
        <div v-if="player.visible" class="audio-player">
            <div class="player-inner">
                <!-- 左：封面 + 标题 -->
                <div class="player-info">
                    <div class="cover-box">
                        <img v-if="player.courseCover" :src="player.courseCover" alt="封面" class="cover-img" />
                        <span v-else class="cover-emoji">{{ categoryEmoji }}</span>
                        <div class="cover-rotor" :class="{ spinning: player.isPlaying }"></div>
                    </div>
                    <div class="title-box">
                        <div class="course-name">{{ player.courseTitle }}</div>
                        <div class="chapter-name">{{ player.currentChapter?.title }}</div>
                    </div>
                </div>

                <!-- 中：播放控制 + 进度条 -->
                <div class="player-center">
                    <div class="controls">
                        <button class="ctrl-btn" :disabled="player.currentIndex === 0" @click="player.prev()" title="上一章">⏮</button>
                        <button class="ctrl-btn play-btn" @click="player.togglePlay()" :title="player.isPlaying ? '暂停' : '播放'">
                            <span v-if="player.isPlaying">⏸</span>
                            <span v-else>▶</span>
                        </button>
                        <button class="ctrl-btn" :disabled="player.currentIndex >= player.chapters.length - 1" @click="player.next()" title="下一章">⏭</button>
                    </div>
                    <div class="progress-row">
                        <span class="time-text">{{ formatTime(player.currentTime) }}</span>
                        <input type="range" class="progress-bar"
                               min="0" :max="player.duration || 0" step="0.1"
                               :value="player.currentTime"
                               @input="onSeek($event)" />
                        <span class="time-text">{{ formatTime(player.duration) }}</span>
                    </div>
                </div>

                <!-- 右：倍速 + 定时 + 关闭 -->
                <div class="player-right">
                    <el-dropdown trigger="click" @command="onRate" class="rate-dropdown">
                        <span class="rate-btn">{{ player.playbackRate }}x</span>
                        <template #dropdown>
                            <el-dropdown-menu>
                                <el-dropdown-item v-for="r in rates" :key="r" :command="r">{{ r }}x</el-dropdown-item>
                            </el-dropdown-menu>
                        </template>
                    </el-dropdown>
                    <el-dropdown trigger="click" @command="onSleep" class="rate-dropdown">
                        <span class="rate-btn">
                            <span v-if="player.sleepRemaining === null">🌙</span>
                            <span v-else>{{ formatSleep(player.sleepRemaining) }}</span>
                        </span>
                        <template #dropdown>
                            <el-dropdown-menu>
                                <el-dropdown-item v-for="m in sleepOptions" :key="m" :command="m">
                                    {{ m === 0 ? '关闭定时' : m + ' 分钟' }}
                                </el-dropdown-item>
                            </el-dropdown-menu>
                        </template>
                    </el-dropdown>
                    <button class="ctrl-btn close-btn" @click="player.close()" title="关闭播放器">✕</button>
                </div>
            </div>
            <!-- 视频遮罩：浅黑底板挡住页面其他区域，关闭播放器后才可点击 -->
            <transition name="mask-fade">
                <div v-if="isVideoSrc" class="video-mask"></div>
            </transition>
            <!-- 视频画面：默认占主内容区大小，单击暂停/播放，双击全屏切换 -->
            <div v-show="isVideoSrc" class="video-panel">
                <video ref="audioRef" :src="audioSrc" preload="metadata" playsinline
                       @timeupdate="onTimeUpdate" @loadedmetadata="onLoadedMeta"
                       @durationchange="onDurationChange" @play="player.onPlay()"
                       @pause="player.onPause()" @ended="player.onEnded()" @error="onError"
                       @click="onVideoClick" @dblclick="onVideoDblclick"></video>
            </div>
        </div>
    </transition>
</template>

<script setup>
import { ref, watch, onMounted, onBeforeUnmount, computed } from 'vue'
import { storeToRefs } from 'pinia'
import { ElMessage } from 'element-plus'
import { usePlayerStore } from '@/stores/player'
import { updateProgress } from '@/api/course'

const player = usePlayerStore()
const { isPlaying, playbackRate, seekTo, currentIndex, currentChapter, visible } = storeToRefs(player)

const audioRef = ref(null)
const rates = [0.75, 1, 1.25, 1.5]
const sleepOptions = [0, 15, 30, 45, 60]
let lastReportAt = 0
let lastReportedPosition = 0

const audioSrc = computed(() => currentChapter.value?.audioUrl || '')

// 章节地址是视频文件时展示画面（OSS 带签名参数也能匹配）
const isVideoSrc = computed(() => /\.(mp4|m4v|webm|mov|ogv)(\?|#|$)/i.test(audioSrc.value))

const categoryEmoji = computed(() => {
    const map = { MEDITATION: '🧘', SLEEP: '🌙', EMOTION: '💙', CBT: '🧠', INTERPERSONAL: '🤝' }
    return map[player.category] || '🎧'
})

const formatTime = (sec) => {
    if (!sec || sec < 0) return '0:00'
    const m = Math.floor(sec / 60)
    const s = Math.floor(sec % 60)
    return `${m}:${s.toString().padStart(2, '0')}`
}

const formatSleep = (sec) => {
    const m = Math.floor(sec / 60)
    const s = sec % 60
    return `${m}:${s.toString().padStart(2, '0')}`
}

const onLoadedMeta = () => {
    const a = audioRef.value
    if (!a) return
    player.onDurationChange(a.duration || 0)
    // 续播/重播：应用 seekTo
    if (seekTo.value !== null && seekTo.value > 0 && a.duration && seekTo.value < a.duration) {
        a.currentTime = seekTo.value
    }
    player.clearSeek()
    if (isPlaying.value) {
        a.play().catch(() => { /* 自动播放策略拦截，忽略 */ })
    }
}

const onDurationChange = () => {
    if (audioRef.value) player.onDurationChange(audioRef.value.duration || 0)
}

const onTimeUpdate = () => {
    const a = audioRef.value
    if (!a) return
    player.onTimeUpdate(a.currentTime || 0)
    reportProgress(false)
}

const onSeek = (e) => {
    const v = parseFloat(e.target.value)
    const a = audioRef.value
    if (a) a.currentTime = v
    player.onTimeUpdate(v)
}

const onRate = (r) => {
    player.setRate(r)
}

const onSleep = (m) => {
    if (m === 0) {
        player.stopSleep()
        ElMessage.success('已关闭定时关闭')
    } else {
        player.startSleep(m)
        ElMessage.success(`将在 ${m} 分钟后暂停`)
    }
}

const onError = () => {
    ElMessage.error('音视频加载失败，请稍后重试')
    player.onPause()
}

// ===== 视频交互：单击暂停/播放，双击全屏切换 =====
// 单击延迟 250ms 执行，双击时取消，避免双击误触发两次暂停
let clickTimer = null
const onVideoClick = () => {
    if (clickTimer) return
    clickTimer = setTimeout(() => {
        clickTimer = null
        player.togglePlay()
    }, 250)
}
const onVideoDblclick = () => {
    if (clickTimer) { clearTimeout(clickTimer); clickTimer = null }
    toggleFullscreen()
}
// 全屏用浏览器原生 Fullscreen API，再次双击或按 Esc 即恢复默认大小
const toggleFullscreen = () => {
    const v = audioRef.value
    if (!v) return
    if (document.fullscreenElement) {
        document.exitFullscreen()
    } else if (v.requestFullscreen) {
        v.requestFullscreen().catch(() => { /* 被浏览器拒绝时忽略 */ })
    }
}

// ===== 进度上报：节流每 10 秒一次；切章/暂停/结束立即上报 =====
const reportProgress = async (force = false) => {
    const now = Date.now()
    const a = audioRef.value
    if (!a) return
    const payload = player.reportPayload
    if (!payload || !payload.chapterId) return
    // 节流：非强制时，距上次上报 <10s 且进度推进 <10s 则跳过
    if (!force && now - lastReportAt < 10000) return
    lastReportAt = now
    lastReportedPosition = payload.progressSeconds
    try {
        const res = await updateProgress(payload)
        // 回写章节完成状态，便于 UI 刷新
        if (res && res.isFinished && player.currentChapter) {
            player.currentChapter.isFinished = true
        }
        if (res && typeof res.completion === 'number') {
            // 触发上层详情页刷新（通过自定义事件总线）
            window.dispatchEvent(new CustomEvent('course-progress-updated', {
                detail: { completion: res.completion, isFinished: res.isFinished }
            }))
        }
    } catch (e) {
        // 静默失败，不打断播放
    }
}

// 切章时：换 src 后等 loadedmetadata 应用 seekTo；立即上报上一章进度
watch(() => currentIndex.value, () => {
    reportProgress(true)
    lastReportAt = 0
})

// 暂停/播放切换 → 暂停时立即上报
watch(isPlaying, (val) => {
    if (!val) reportProgress(true)
})

// 倍速变化同步到 audio
watch(playbackRate, (r) => {
    if (audioRef.value) audioRef.value.playbackRate = r
})

// 播放状态变化同步到 audio.play/pause
watch(isPlaying, (val) => {
    const a = audioRef.value
    if (!a) return
    if (val) {
        if (a.src) a.play().catch(() => {})
    } else {
        a.pause()
    }
})

// 播放器关闭时释放（全屏中则先退出全屏）
watch(visible, (v) => {
    if (!v) {
        reportProgress(true)
        if (document.fullscreenElement) document.exitFullscreen()
        const a = audioRef.value
        if (a) a.pause()
    }
})

onBeforeUnmount(() => {
    if (clickTimer) { clearTimeout(clickTimer); clickTimer = null }
    reportProgress(true)
    player.stopSleep()
    const a = audioRef.value
    if (a) a.pause()
})
</script>

<style scoped lang="scss">
$bg-card: #FFFEF9;
$primary: #E8927C;
$primary-dark: #D9826B;
$primary-darker: #C4705C;
$primary-light: rgba(232, 146, 124, 0.15);
$text-primary: #5C4A3A;
$text-regular: #6B5B4B;
$text-secondary: #8B7D6B;
$border-color: #E8DCC8;
$shadow-card: 0 -4px 20px rgba(180, 140, 100, 0.18);
$radius-md: 12px;
$radius-lg: 16px;
$font-handwritten: "Ma Shan Zheng", "Comic Sans MS", "Segoe Print", "Bradley Hand", cursive;
$font-rounded: "Nunito", "Quicksand", "PingFang SC", "Microsoft YaHei", sans-serif;

.audio-player {
    position: fixed;
    bottom: 0;
    left: 0;
    right: 0;
    z-index: 1100;
    background: linear-gradient(135deg, #FFFEF9 0%, #FFF3E6 100%);
    border-top: 2px dashed $border-color;
    box-shadow: $shadow-card;
    font-family: $font-rounded;
    color: $text-primary;

    // 浅黑遮罩：盖住页面其他区域（底栏与视频在其之上），关闭播放器才消失
    .video-mask {
        position: fixed;
        inset: 0;
        z-index: 1;
        background: rgba(0, 0, 0, 0.45);
    }

    // 视频画面：默认占主内容区大小（百分比，随屏幕自适应），居中悬在底栏上方
    .video-panel {
        position: fixed;
        left: 50%;
        bottom: 92px;
        transform: translateX(-50%);
        z-index: 2;
        // 宽度 74vw；若按 16:9 换算高度会超出可用空间，则以高度反推宽度兜底
        width: min(74vw, calc((100vh - 210px) * 1.7778));
        background: $bg-card;
        border: 1.5px dashed $border-color;
        border-radius: $radius-lg;
        padding: 8px;
        box-shadow: 0 8px 24px rgba(180, 140, 100, 0.25);

        video {
            display: block;
            width: 100%;
            max-height: calc(100vh - 226px);
            border-radius: $radius-md;
            background: #000;
            object-fit: contain;
            cursor: pointer;
        }
    }

    .player-inner {
        position: relative;
        z-index: 2; // 底栏（进度条/控制/关闭）保持在遮罩之上可点
        max-width: 1200px;
        margin: 0 auto;
        padding: 12px 20px;
        display: flex;
        align-items: center;
        gap: 20px;
    }

    .player-info {
        display: flex;
        align-items: center;
        gap: 12px;
        min-width: 200px;
        max-width: 260px;

        .cover-box {
            position: relative;
            width: 48px;
            height: 48px;
            border-radius: 50%;
            background: $primary-light;
            border: 1.5px dashed $border-color;
            display: flex;
            align-items: center;
            justify-content: center;
            overflow: hidden;
            flex-shrink: 0;

            .cover-img {
                width: 100%;
                height: 100%;
                object-fit: cover;
            }
            .cover-emoji {
                font-size: 24px;
            }
            // 旋转指针
            .cover-rotor {
                display: none;
            }
        }

        .title-box {
            overflow: hidden;
            .course-name {
                font-family: $font-handwritten;
                font-size: 15px;
                color: $text-primary;
                white-space: nowrap;
                overflow: hidden;
                text-overflow: ellipsis;
            }
            .chapter-name {
                font-size: 12px;
                color: $text-secondary;
                white-space: nowrap;
                overflow: hidden;
                text-overflow: ellipsis;
            }
        }
    }

    .player-center {
        flex: 1;
        display: flex;
        flex-direction: column;
        gap: 6px;
        min-width: 0;

        .controls {
            display: flex;
            justify-content: center;
            align-items: center;
            gap: 16px;

            .ctrl-btn {
                font-family: $font-rounded;
                background: $bg-card;
                border: 1.5px dashed $border-color;
                color: $text-regular;
                border-radius: 50%;
                width: 36px;
                height: 36px;
                font-size: 14px;
                cursor: pointer;
                transition: all 0.2s ease;
                display: flex;
                align-items: center;
                justify-content: center;

                &:hover:not(:disabled) {
                    border-color: $primary;
                    color: $primary;
                    transform: translateY(-1px);
                }
                &:disabled {
                    opacity: 0.4;
                    cursor: not-allowed;
                }

                &.play-btn {
                    width: 44px;
                    height: 44px;
                    background: linear-gradient(135deg, $primary, $primary-dark);
                    color: #fff;
                    border: none;
                    font-size: 18px;
                    box-shadow: 0 3px 0 $primary-darker, 0 4px 8px rgba(180, 140, 100, 0.2);
                    &:hover:not(:disabled) {
                        color: #fff;
                    }
                    &:active {
                        transform: translateY(1px);
                        box-shadow: 0 1px 0 $primary-darker;
                    }
                }

                &.close-btn {
                    width: 30px;
                    height: 30px;
                    font-size: 13px;
                }
            }
        }

        .progress-row {
            display: flex;
            align-items: center;
            gap: 10px;

            .time-text {
                font-size: 11px;
                color: $text-secondary;
                min-width: 32px;
                text-align: center;
                font-variant-numeric: tabular-nums;
            }

            .progress-bar {
                flex: 1;
                height: 4px;
                -webkit-appearance: none;
                appearance: none;
                background: $border-color;
                border-radius: 2px;
                outline: none;
                cursor: pointer;

                &::-webkit-slider-thumb {
                    -webkit-appearance: none;
                    appearance: none;
                    width: 14px;
                    height: 14px;
                    background: $primary;
                    border-radius: 50%;
                    border: 2px solid #fff;
                    box-shadow: 0 1px 3px rgba(180, 140, 100, 0.4);
                    cursor: pointer;
                }
                &::-moz-range-thumb {
                    width: 14px;
                    height: 14px;
                    background: $primary;
                    border-radius: 50%;
                    border: 2px solid #fff;
                    cursor: pointer;
                }
            }
        }
    }

    .player-right {
        display: flex;
        align-items: center;
        gap: 10px;
        flex-shrink: 0;

        .rate-dropdown {
            cursor: pointer;
        }
        .rate-btn {
            font-size: 12px;
            color: $text-regular;
            background: $bg-card;
            border: 1.5px dashed $border-color;
            border-radius: $radius-md;
            padding: 5px 10px;
            cursor: pointer;
            display: inline-block;
            min-width: 36px;
            text-align: center;
            transition: all 0.2s ease;
            &:hover {
                border-color: $primary;
                color: $primary;
            }
        }
    }
}

// 进入/离开动画
.player-slide-enter-active, .player-slide-leave-active {
    transition: transform 0.3s ease, opacity 0.3s ease;
}
.player-slide-enter-from, .player-slide-leave-to {
    transform: translateY(100%);
    opacity: 0;
}
.mask-fade-enter-active, .mask-fade-leave-active {
    transition: opacity 0.3s ease;
}
.mask-fade-enter-from, .mask-fade-leave-to {
    opacity: 0;
}

@media (max-width: 768px) {
    .audio-player .player-inner {
        flex-wrap: wrap;
        gap: 8px;
        padding: 10px 12px;

        .player-info {
            min-width: 0;
            max-width: 100%;
            flex: 1;
        }
        .player-right {
            .rate-btn { font-size: 11px; padding: 4px 8px; }
        }
    }
    .audio-player .video-panel {
        width: 94vw;
        bottom: 130px; // 移动端底栏换行变高，上移避让
    }
}
</style>
