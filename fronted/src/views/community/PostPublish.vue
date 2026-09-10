<template>
  <div class="publish-container">
    <div class="page-header">
      <div class="header-tape"></div>
      <h1 class="page-title">✏️ 写下此刻的心情</h1>
      <p class="page-subtitle">这里是安全的树洞，你可以选择匿名 🍃</p>
    </div>

    <div class="publish-card">
      <!-- 板块选择 -->
      <div class="form-section">
        <label class="form-label">选择板块 <span class="required">*</span></label>
        <div class="board-chips">
          <button v-for="b in boards" :key="b.code" type="button"
                  class="chip-btn" :class="{ active: form.board === b.code }"
                  @click="form.board = b.code">
            <span>{{ boardEmoji(b.code) }}</span><span>{{ b.label }}</span>
          </button>
        </div>
      </div>

      <!-- 标题 -->
      <div class="form-section">
        <label class="form-label">标题 <span class="required">*</span></label>
        <el-input v-model="form.title" maxlength="80" show-word-limit
                  placeholder="一句话说说你想聊什么…" class="title-input" />
      </div>

      <!-- 正文富文本 -->
      <div class="form-section">
        <label class="form-label">正文 <span class="required">*</span></label>
        <RichTextEditor
          ref="editorRef"
          v-model="form.content"
          :max-char-count="10000"
          placeholder="慢慢写，没有人会催你…"
          :min-height="'280px'"
        />
      </div>

      <!-- 图片 -->
      <div class="form-section">
        <label class="form-label">配图（最多 9 张）</label>
        <el-upload
          list-type="picture-card"
          :file-list="fileList"
          :limit="9"
          accept="image/*"
          :before-upload="beforeUpload"
          :http-request="handleUpload"
          :on-remove="handleRemove"
          :on-exceed="() => ElMessage.warning('最多上传 9 张图片')"
        >
          <div class="upload-trigger">
            <span class="upload-icon">🖼</span>
            <span>添加图片</span>
          </div>
        </el-upload>
      </div>

      <!-- 匿名开关 -->
      <div class="form-section anonymous-row">
        <div class="anonymous-info">
          <span class="anonymous-label">🎭 匿名发布</span>
          <span class="anonymous-desc">开启后其他人将看到「匿名用户」，没人知道是你</span>
        </div>
        <el-switch v-model="form.isAnonymous" :active-value="1" :inactive-value="0" />
      </div>

      <!-- 操作 -->
      <div class="form-actions">
        <el-button @click="router.back()">返回</el-button>
        <el-button type="primary" class="submit-btn" :loading="submitting" @click="submit">
          🌱 发布
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import RichTextEditor from '@/components/RichTextEditor.vue'
import { getBoards, createPost, uploadCommunityImage } from '@/api/community'
import { resolveFileUrl } from '@/config/index.js'

const router = useRouter()

const boards = ref([])
const submitting = ref(false)
const fileList = ref([])
const editorRef = ref(null)

const form = reactive({
  board: '',
  title: '',
  content: '',
  images: [],
  isAnonymous: 0
})

const boardEmojiMap = {
  TREES: '🌳', ACADEMIC: '📖', RELATIONSHIP: '🫖',
  SLEEP: '🌙', RECOVERY: '🌿', HELP: '🙋'
}
const boardEmoji = (code) => boardEmojiMap[code] || '🌱'

const beforeUpload = (file) => {
  if (!file.type.startsWith('image/')) {
    ElMessage.error('只能上传图片文件')
    return false
  }
  if (file.size / 1024 / 1024 > 5) {
    ElMessage.error('图片大小不能超过 5MB')
    return false
  }
  return true
}

// 自定义上传：走社区图片接口，保存相对路径入库
const handleUpload = async ({ file, onSuccess, onError }) => {
  try {
    const res = await uploadCommunityImage(file)
    form.images.push(res.filePath)
    onSuccess?.(res)
  } catch (e) {
    onError?.(e)
  }
}

const handleRemove = (file) => {
  const path = file.response?.filePath || file.url
  form.images = form.images.filter(p => p !== path)
}

const submit = () => {
  if (!form.board) {
    ElMessage.warning('请选择一个板块')
    return
  }
  if (!form.title.trim()) {
    ElMessage.warning('请填写标题')
    return
  }
  // 富文本去标签判空
  const plainText = (editorRef.value?.getText() || '').replace(/\s+/g, '').trim()
  if (!plainText) {
    ElMessage.warning('正文内容不能为空')
    return
  }
  submitting.value = true
  createPost({
    board: form.board,
    title: form.title.trim(),
    content: form.content,
    images: form.images,
    isAnonymous: form.isAnonymous
  }).then(data => {
    ElMessage.success('发布成功 🌱')
    router.replace(`/community/post/${data.id}`)
  }).catch(() => {
    ElMessage.error('发布失败，请稍后重试')
  }).finally(() => {
    submitting.value = false
  })
}

onMounted(() => {
  getBoards().then(data => { boards.value = data || [] })
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

.publish-container {
  min-height: calc(100vh - 130px);
  background: $bg-page;
  font-family: $font-rounded;
  color: $text-primary;
  padding: 24px 16px 80px;
  max-width: 820px;
  margin: 0 auto;
}

.page-header {
  text-align: center;
  margin-bottom: 20px;
  .header-tape {
    width: 120px; height: 10px;
    background: linear-gradient(90deg, transparent, rgba(232,146,124,0.5), transparent);
    border-radius: 5px; margin: 0 auto 14px;
  }
  .page-title { font-family: $font-handwritten; font-size: 30px; font-weight: 600; margin: 0 0 8px; }
  .page-subtitle { font-size: 14px; color: $text-secondary; margin: 0; }
}

.publish-card {
  background: $bg-card; border: 1.5px solid $border-color; border-radius: $radius-lg;
  box-shadow: $shadow-card; padding: 26px 28px;
}

.form-section { margin-bottom: 22px; }
.form-label {
  display: block; font-size: 14.5px; font-weight: 700; color: $text-primary; margin-bottom: 10px;
  .required { color: $primary; }
}

.board-chips {
  display: flex; flex-wrap: wrap; gap: 10px;
  .chip-btn {
    font-family: $font-rounded; background: $bg-card; border: 1.5px dashed $border-color;
    color: $text-regular; border-radius: 20px; padding: 8px 18px; font-size: 14px;
    cursor: pointer; transition: all 0.25s ease; display: inline-flex; align-items: center; gap: 6px;
    &:hover { border-color: $primary; color: $primary; }
    &.active {
      background: linear-gradient(135deg, $primary, $primary-dark);
      border-style: solid; color: #fff; box-shadow: $shadow-button;
    }
  }
}

.title-input {
  :deep(.el-input__wrapper) {
    border-radius: $radius-md; box-shadow: 0 0 0 1.5px $border-color inset; background: #fff;
    &.is-focused { box-shadow: 0 0 0 2px rgba(232,146,124,0.4) inset; }
  }
}

.anonymous-row {
  display: flex; align-items: center; justify-content: space-between;
  background: rgba(247,232,166,0.35); border: 1.5px dashed #d9bd7a;
  border-radius: $radius-md; padding: 14px 18px;
  .anonymous-label { font-size: 14.5px; font-weight: 700; color: $text-primary; }
  .anonymous-desc { display: block; font-size: 12.5px; color: $text-secondary; margin-top: 3px; }
  :deep(.el-switch.is-checked .el-switch__core) {
    background-color: $primary; border-color: $primary;
  }
}

.form-actions {
  display: flex; justify-content: center; gap: 14px; margin-top: 28px;
  .submit-btn {
    background: linear-gradient(135deg, $primary, $primary-dark); border: none;
    border-radius: $radius-md; padding: 10px 34px; font-size: 15px; font-weight: 700;
    box-shadow: $shadow-button;
    &:hover { transform: translateY(-1px); }
  }
}

// 上传卡片手账化
:deep(.el-upload-list--picture-card .el-upload-list__item),
:deep(.el-upload--picture-card) {
  width: 92px; height: 92px; border-radius: $radius-md;
  border: 1.5px dashed $border-color; background: #fff;
}
:deep(.el-upload--picture-card:hover) { border-color: $primary; }
.upload-trigger {
  display: flex; flex-direction: column; align-items: center; gap: 4px;
  color: $text-secondary; font-size: 12px;
  .upload-icon { font-size: 22px; }
}
</style>
