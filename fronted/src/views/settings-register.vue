<template>
  <div class="settings-page">
    <PageHead title="注册设置" />

    <el-card shadow="never" class="settings-card" v-loading="loading">
      <template #header>
        <span class="card-title">注册行为</span>
        <span class="card-subtitle">保存后立即生效（缓存 5 分钟内自动刷新）</span>
      </template>

      <el-form label-width="100px" class="settings-form">
        <el-form-item label="开放注册">
          <div class="switch-line">
            <el-switch v-model="form.registerEnabled" />
            <span class="switch-tip">关闭后注册接口直接拒绝，提示「暂未开放注册」</span>
          </div>
        </el-form-item>
        <el-form-item label="注册需审核">
          <div class="switch-line">
            <el-switch v-model="form.registerReview" :disabled="!form.registerEnabled" />
            <span class="switch-tip">开启后新用户需在用户管理中审核，审核通过前无法登录（状态为「待审核」）</span>
          </div>
        </el-form-item>
        <el-form-item label="默认角色">
          <el-radio-group v-model="form.defaultUserRole" :disabled="!form.registerEnabled">
            <el-radio :value="'1'">普通用户</el-radio>
            <el-radio :value="'2'">管理员</el-radio>
          </el-radio-group>
          <div class="radio-tip">注册成功后自动赋予的角色；设为管理员意味着任何人都可注册成管理员，请谨慎选择</div>
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

// 配置值均为字符串，开关在此做 'true'/'false' 转换
const form = reactive({
  registerEnabled: true,
  registerReview: false,
  defaultUserRole: '1'
})

const KEY_FIELDS = {
  REGISTER_ENABLED: 'registerEnabled',
  REGISTER_REVIEW: 'registerReview',
  DEFAULT_USER_ROLE: 'defaultUserRole'
}

async function load() {
  loading.value = true
  try {
    const list = (await getAdminSettings('REGISTER')) || []
    list.forEach(item => {
      const field = KEY_FIELDS[item.configKey]
      if (!field) return
      if (field === 'defaultUserRole') {
        // 白名单仅 1/2，异常值回退普通用户
        form.defaultUserRole = ['1', '2'].includes(item.configValue) ? item.configValue : '1'
      } else {
        form[field] = item.configValue === 'true'
      }
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
    const items = [
      { configKey: 'REGISTER_ENABLED', configValue: String(form.registerEnabled) },
      { configKey: 'REGISTER_REVIEW', configValue: String(form.registerReview) },
      { configKey: 'DEFAULT_USER_ROLE', configValue: form.defaultUserRole }
    ]
    await saveAdminSettings({ items })
    ElMessage.success('保存成功')
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

.radio-tip {
  width: 100%;
  margin-top: 4px;
  font-size: 12px;
  color: #9CA3AF;
  line-height: 1.6;
}
</style>
