<template>
  <div class="settings-container">
    <div class="page-header">
      <div class="header-tape"></div>
      <h1 class="page-title">⚙️ 消息设置</h1>
      <p class="page-subtitle">按自己的节奏接收关心，不被打扰也不会错过 🍊</p>
    </div>

    <div class="settings-card">
      <div class="setting-row">
        <div class="setting-info">
          <span class="setting-icon">📢</span>
          <div>
            <div class="setting-title">系统公告</div>
            <div class="setting-desc">接收管理员发布的公告与温馨提示</div>
          </div>
        </div>
        <el-switch v-model="form.announcementOn" :active-value="1" :inactive-value="0" />
      </div>

      <div class="setting-row">
        <div class="setting-info">
          <span class="setting-icon">📅</span>
          <div>
            <div class="setting-title">预约通知</div>
            <div class="setting-desc">预约确认、取消与提前 1 小时提醒</div>
          </div>
        </div>
        <el-switch v-model="form.appointmentOn" :active-value="1" :inactive-value="0" />
      </div>

      <div class="setting-row">
        <div class="setting-info">
          <span class="setting-icon">💬</span>
          <div>
            <div class="setting-title">互动通知</div>
            <div class="setting-desc">帖子被点赞、收到评论或回复时通知我</div>
          </div>
        </div>
        <el-switch v-model="form.interactionOn" :active-value="1" :inactive-value="0" />
      </div>

      <div class="setting-row">
        <div class="setting-info">
          <span class="setting-icon">👩‍⚕️</span>
          <div>
            <div class="setting-title">咨询师回复</div>
            <div class="setting-desc">咨询师在咨询会话中给你留言时通知我</div>
          </div>
        </div>
        <el-switch v-model="form.consultantOn" :active-value="1" :inactive-value="0" />
      </div>

      <div class="setting-row assessment-row">
        <div class="setting-info">
          <span class="setting-icon">🧭</span>
          <div>
            <div class="setting-title">测评提醒</div>
            <div class="setting-desc">定期提醒做心理测评，关注情绪变化</div>
          </div>
        </div>
        <div class="assessment-control">
          <el-radio-group v-model="form.assessmentFrequency" size="default">
            <el-radio-button label="NONE">关闭</el-radio-button>
            <el-radio-button label="WEEKLY">每周</el-radio-button>
            <el-radio-button label="DAILY">每天</el-radio-button>
          </el-radio-group>
          <el-switch v-model="form.assessmentOn" :active-value="1" :inactive-value="0" class="assessment-switch" />
        </div>
      </div>

      <div class="save-bar">
        <el-button type="primary" class="save-btn" :loading="saving" @click="onSave">💾 保存设置</el-button>
        <router-link to="/notifications" class="back-link">‹ 返回消息中心</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getNotifySettings, updateNotifySettings } from '@/api/notify'

const form = reactive({
  announcementOn: 1,
  appointmentOn: 1,
  interactionOn: 1,
  consultantOn: 1,
  assessmentOn: 1,
  assessmentFrequency: 'WEEKLY'
})
const saving = ref(false)

onMounted(() => {
  getNotifySettings().then(data => {
    if (data) {
      Object.assign(form, {
        announcementOn: data.announcementOn ?? 1,
        appointmentOn: data.appointmentOn ?? 1,
        interactionOn: data.interactionOn ?? 1,
        consultantOn: data.consultantOn ?? 1,
        assessmentOn: data.assessmentOn ?? 1,
        assessmentFrequency: data.assessmentFrequency || 'WEEKLY'
      })
    }
  })
})

const onSave = () => {
  saving.value = true
  updateNotifySettings({ ...form }).then(() => {
    ElMessage.success('设置已保存，立即生效 🌿')
  }).finally(() => {
    saving.value = false
  })
}
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

.settings-container {
  min-height: calc(100vh - 130px);
  background: $bg-page;
  font-family: $font-rounded;
  color: $text-primary;
  padding: 24px 16px 80px;
  max-width: 720px;
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

.settings-card {
  background: $bg-card;
  border: 1.5px solid $border-color;
  border-radius: $radius-lg;
  box-shadow: $shadow-card;
  padding: 10px 22px 22px;
}

.setting-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 20px 4px;
  border-bottom: 2px dashed $border-color;
  &:last-of-type { border-bottom: none; }
}

.setting-info {
  display: flex;
  align-items: center;
  gap: 14px;
  .setting-icon {
    flex: none;
    width: 44px;
    height: 44px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 22px;
    background: $primary-light;
    border-radius: 50%;
  }
  .setting-title { font-size: 16px; font-weight: 600; color: $text-primary; }
  .setting-desc { font-size: 13px; color: $text-secondary; margin-top: 3px; }
}

.assessment-control {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

:deep(.el-switch.is-checked .el-switch__core) {
  background-color: $primary;
  border-color: $primary;
}

:deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background-color: $primary;
  border-color: $primary;
  box-shadow: -1px 0 0 0 $primary;
}
:deep(.el-radio-button__inner) {
  font-family: $font-rounded;
  border-radius: 0;
}
:deep(.el-radio-button:first-child .el-radio-button__inner) { border-radius: $radius-md 0 0 $radius-md; }
:deep(.el-radio-button:last-child .el-radio-button__inner) { border-radius: 0 $radius-md $radius-md 0; }

.save-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 22px;
}

:deep(.save-btn) {
  font-family: $font-rounded;
  background: linear-gradient(135deg, $primary, $primary-dark);
  border: none;
  border-radius: $radius-md;
  box-shadow: $shadow-button;
  font-weight: 600;
  padding: 10px 26px;
}

.back-link {
  font-size: 14px;
  color: $primary-dark;
  text-decoration: none;
  border-bottom: 1px dashed $primary;
  &:hover { color: $primary-darker; border-bottom-style: solid; }
}

@media (max-width: 600px) {
  .assessment-row { flex-direction: column; align-items: flex-start; }
  .assessment-control { justify-content: flex-start; }
}
</style>
