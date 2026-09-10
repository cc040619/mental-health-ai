import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

/**
 * 全局音频播放器状态
 * 播放器组件挂载在 FrontendLayout 底部，切路由不销毁（后台播放）
 * 仅用原生 <audio> + playbackRate 实现倍速、setInterval 实现定时关闭
 */
export const usePlayerStore = defineStore('player', () => {
    // 是否展示播放器
    const visible = ref(false)
    // 当前课程信息
    const courseTitle = ref('')
    const courseCover = ref('')
    const category = ref('')
    // 章节列表（每个章节含 id/title/audioUrl/duration/progressSeconds/isFinished）
    const chapters = ref([])
    const currentIndex = ref(0)
    // 播放状态
    const isPlaying = ref(false)
    const currentTime = ref(0)
    const duration = ref(0)
    const playbackRate = ref(1)
    // 定时关闭剩余秒数（null 表示未开启）
    const sleepRemaining = ref(null)
    // 待应用的跳转位置（续播/重播），组件在 loadedmetadata 后应用一次
    const seekTo = ref(null)

    let sleepTimerId = null

    const currentChapter = computed(() => chapters.value[currentIndex.value] || null)

    // 进度上报需要：当前章节 id + duration
    const reportPayload = computed(() => {
        const ch = currentChapter.value
        if (!ch) return null
        return {
            chapterId: ch.id,
            progressSeconds: Math.floor(currentTime.value),
            duration: ch.duration || duration.value || 0
        }
    })

    /**
     * 计算某章节的续播位置：已完成则从头播，否则续播到上次进度
     */
    const seekPositionFor = (chapter) => {
        if (!chapter) return 0
        if (chapter.isFinished) return 0
        return chapter.progressSeconds || 0
    }

    const applyChapter = (index, autoPlay = true) => {
        currentIndex.value = index
        const ch = chapters.value[index]
        if (!ch) return
        seekTo.value = seekPositionFor(ch)
        currentTime.value = seekTo.value || 0
        duration.value = ch.duration || 0
        if (autoPlay) isPlaying.value = true
    }

    /**
     * 开始播放某课程的某章节
     * @param {Object} payload { course, chapters, startIndex }
     */
    const play = ({ course, chapters: list, startIndex = 0 }) => {
        courseTitle.value = course?.title || ''
        courseCover.value = course?.coverUrl || ''
        category.value = course?.category || ''
        chapters.value = list || []
        stopSleep()
        applyChapter(startIndex, true)
        visible.value = true
    }

    const togglePlay = () => {
        if (!visible.value) return
        isPlaying.value = !isPlaying.value
    }

    const next = () => {
        if (currentIndex.value < chapters.value.length - 1) {
            applyChapter(currentIndex.value + 1, true)
        }
    }

    const prev = () => {
        if (currentIndex.value > 0) {
            applyChapter(currentIndex.value - 1, true)
        }
    }

    const seek = (time) => {
        seekTo.value = time
        currentTime.value = time
    }

    // ===== 以下由 AudioPlayer 组件在原生 audio 事件中回调 =====
    const onTimeUpdate = (t) => { currentTime.value = t }
    const onDurationChange = (d) => { duration.value = d }
    const onPlay = () => { isPlaying.value = true }
    const onPause = () => { isPlaying.value = false }
    const onEnded = () => {
        isPlaying.value = false
        // 自动播放下一章
        if (currentIndex.value < chapters.value.length - 1) {
            applyChapter(currentIndex.value + 1, true)
        }
    }
    const clearSeek = () => { seekTo.value = null }

    const setRate = (rate) => { playbackRate.value = rate }

    // ===== 定时关闭 =====
    const startSleep = (minutes) => {
        stopSleep()
        sleepRemaining.value = minutes * 60
        sleepTimerId = setInterval(() => {
            if (sleepRemaining.value === null) return
            sleepRemaining.value -= 1
            if (sleepRemaining.value <= 0) {
                isPlaying.value = false
                stopSleep()
            }
        }, 1000)
    }
    const stopSleep = () => {
        if (sleepTimerId) {
            clearInterval(sleepTimerId)
            sleepTimerId = null
        }
        sleepRemaining.value = null
    }

    const close = () => {
        isPlaying.value = false
        stopSleep()
        visible.value = false
    }

    return {
        visible, courseTitle, courseCover, category, chapters, currentIndex,
        isPlaying, currentTime, duration, playbackRate, sleepRemaining, seekTo,
        currentChapter, reportPayload,
        play, togglePlay, next, prev, seek, applyChapter,
        onTimeUpdate, onDurationChange, onPlay, onPause, onEnded, clearSeek,
        setRate, startSleep, stopSleep, close
    }
})
