<template>
  <div class="settings-page">
    <PageHead title="内容设置" />

    <el-card shadow="never" class="settings-card" v-loading="loading">
      <template #header>
        <span class="card-title">社区内容审核</span>
        <span class="card-subtitle">开启后新内容进入「待审核」，与帖子/评论管理联动</span>
      </template>

      <el-form label-width="120px" class="settings-form">
        <el-form-item label="发帖需审核">
          <div class="switch-line">
            <el-switch v-model="form.postReview" />
            <span class="switch-tip">开启后用户新发帖先进待审核，管理员在「帖子管理」审核通过后展示</span>
          </div>
        </el-form-item>
        <el-form-item label="评论需审核">
          <div class="switch-line">
            <el-switch v-model="form.commentReview" />
            <span class="switch-tip">开启后新评论先进待审核，管理员在「评论管理」审核通过后展示</span>
          </div>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="onSave">保存设置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import PageHead from '@/components/PageHead.vue'
import { getAdminSettings, saveAdminSettings } from '@/api/admin'

const loading = ref(false)
const saving = ref(false)

const form = reactive({
  postReview: false,
  commentReview: false
})

const KEY_FIELDS = {
  COMMUNITY_POST_REVIEW: 'postReview',
  COMMUNITY_COMMENT_REVIEW: 'commentReview'
}

async function load() {
  loading.value = true
  try {
    const list = (await getAdminSettings('CONTENT')) || []
    list.forEach(item => {
      const field = KEY_FIELDS[item.configKey]
      if (field) form[field] = item.configValue === 'true'
    })
  } catch (e) {
    ElMessage.error('配置加载失败')
  } finally {
    loading.value = false
  }
}

async function onSave() {
  saving.value = true
  try {
    const items = Object.entries(KEY_FIELDS).map(([key, field]) => ({
      configKey: key,
      configValue: String(form[field])
    }))
    await saveAdminSettings({ items })
    ElMessage.success('保存成功，对新发布的内容立即生效')
  } catch (e) {
    // 拦截器已提示
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style lang="scss" scoped>
.settings-page {
  padding: 16px;
  background-color: #F7F8FA;
  min-height: 100%;
}

.settings-card {
  border: 1px solid #E5E7EB;
  border-radius: 4px;
  max-width: 860px;

  :deep(.el-card__header) {
    padding: 14px 16px;
    border-bottom: 1px solid #E5E7EB;
  }

  :deep(.el-card__body) {
    padding: 24px 16px 16px;
  }
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #1F2937;
}

.card-subtitle {
  margin-left: 12px;
  font-size: 12px;
  color: #6B7280;
}

.switch-line {
  display: flex;
  align-items: center;
  gap: 12px;

  .switch-tip {
    font-size: 12px;
    color: #6B7280;
  }
}
</style>
