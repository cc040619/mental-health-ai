<template>
  <div class="settings-page">
    <PageHead title="基本设置" />

    <el-card shadow="never" class="settings-card" v-loading="loading">
      <template #header>
        <span class="card-title">站点信息</span>
        <span class="card-subtitle">保存后用户端登录页与布局即时展示新值</span>
      </template>

      <el-form :model="form" label-width="100px" class="settings-form">
        <el-form-item label="系统名称">
          <el-input v-model="form.systemName" maxlength="50" show-word-limit placeholder="如：大学生心理健康AI助手" style="max-width: 420px" />
        </el-form-item>
        <el-form-item label="系统 Logo">
          <div class="logo-upload">
            <el-image v-if="form.systemLogo" :src="form.systemLogo" fit="cover" class="logo-preview" />
            <div v-else class="logo-placeholder">暂无 Logo</div>
            <div class="logo-actions">
              <el-upload :show-file-list="false" accept="image/*" :before-upload="beforeLogoUpload" :http-request="doUploadLogo">
                <el-button type="primary" plain>上传 Logo</el-button>
              </el-upload>
              <el-button v-if="form.systemLogo" text type="danger" @click="form.systemLogo = ''">清除</el-button>
              <div class="upload-tip">支持 JPG/PNG，不超过 5MB，建议正方形</div>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="备案号">
          <el-input v-model="form.icpNumber" maxlength="50" placeholder="如：京ICP备XXXXXXXX号" style="max-width: 420px" />
        </el-form-item>
        <el-form-item label="客服联系方式">
          <el-input v-model="form.customerService" maxlength="100" placeholder="邮箱 / 电话 / 工作时间" style="max-width: 420px" />
        </el-form-item>
        <el-form-item label="系统简介">
          <el-input v-model="form.systemIntro" type="textarea" :rows="4" maxlength="500" show-word-limit placeholder="展示在用户端关于/登录页的介绍文案" style="max-width: 620px" />
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
import { getAdminSettings, saveAdminSettings, uploadSystemLogo } from '@/api/admin'

const loading = ref(false)
const saving = ref(false)

const form = reactive({
  systemName: '',
  systemLogo: '',
  icpNumber: '',
  customerService: '',
  systemIntro: ''
})

// KV 列表项 → 表单字段
const KEY_FIELDS = {
  SYSTEM_NAME: 'systemName',
  SYSTEM_LOGO: 'systemLogo',
  ICP_NUMBER: 'icpNumber',
  CUSTOMER_SERVICE: 'customerService',
  SYSTEM_INTRO: 'systemIntro'
}

async function load() {
  loading.value = true
  try {
    const list = (await getAdminSettings('BASIC')) || []
    list.forEach(item => {
      const field = KEY_FIELDS[item.configKey]
      if (field) form[field] = item.configValue || ''
    })
  } catch (e) {
    ElMessage.error('配置加载失败')
  } finally {
    loading.value = false
  }
}

// Logo 上传前校验（白名单类型 + 5MB 上限）
function beforeLogoUpload(file) {
  const okType = ['image/jpeg', 'image/png', 'image/webp'].includes(file.type)
  if (!okType) {
    ElMessage.error('仅支持 JPG/PNG/WebP 图片')
    return false
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.error('图片不能超过 5MB')
    return false
  }
  return true
}

async function doUploadLogo({ file }) {
  try {
    const info = await uploadSystemLogo(file)
    form.systemLogo = info?.filePath || ''
    ElMessage.success('Logo 已上传，记得保存设置')
  } catch (e) {
    // 拦截器已提示
  }
}

async function onSave() {
  if (!form.systemName.trim()) {
    ElMessage.warning('系统名称不能为空')
    return
  }
  saving.value = true
  try {
    const items = Object.entries(KEY_FIELDS).map(([key, field]) => ({
      configKey: key,
      configValue: (form[field] || '').trim()
    }))
    await saveAdminSettings({ items })
    ElMessage.success('保存成功')
    load()
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

.logo-upload {
  display: flex;
  align-items: center;
  gap: 16px;

  .logo-preview {
    width: 72px;
    height: 72px;
    border-radius: 8px;
    border: 1px solid #E5E7EB;
  }

  .logo-placeholder {
    width: 72px;
    height: 72px;
    border-radius: 8px;
    border: 1px dashed #D1D5DB;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 12px;
    color: #9CA3AF;
  }

  .upload-tip {
    margin-top: 6px;
    font-size: 12px;
    color: #9CA3AF;
  }
}
</style>
