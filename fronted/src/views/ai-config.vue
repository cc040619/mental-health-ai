<template>
  <div class="ai-config-page">
    <PageHead title="模型参数">
      <template #buttons>
        <el-button type="primary" :loading="saving" @click="onSave">保存配置</el-button>
      </template>
    </PageHead>

    <el-row :gutter="16">
      <!-- 模型参数 -->
      <el-col :span="14">
        <el-card shadow="never" class="panel">
          <template #header><span class="panel-title">模型参数（保存后下一条对话即生效，无需重启）</span></template>
          <el-form label-width="110px" class="config-form">
            <el-form-item label="模型名称">
              <el-input v-model="form.MODEL" placeholder="如 deepseek-v3" style="width: 260px" />
            </el-form-item>
            <el-form-item label="温度">
              <el-slider v-model="form.TEMPERATURE" :min="0" :max="2" :step="0.1" style="width: 260px" show-input :show-input-controls="false" />
            </el-form-item>
            <el-form-item label="最大 Token">
              <el-input-number v-model="form.MAX_TOKENS" :min="1" :max="8192" :step="128" />
            </el-form-item>
            <el-form-item label="top_p">
              <el-slider v-model="form.TOP_P" :min="0" :max="1" :step="0.05" style="width: 260px" show-input :show-input-controls="false" />
            </el-form-item>
            <el-form-item label="输入单价">
              <el-input-number v-model="form.INPUT_PRICE" :min="0" :step="0.1" :precision="2" />
              <span class="unit">元 / 百万 token</span>
            </el-form-item>
            <el-form-item label="输出单价">
              <el-input-number v-model="form.OUTPUT_PRICE" :min="0" :step="0.1" :precision="2" />
              <span class="unit">元 / 百万 token</span>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <!-- 过滤规则 -->
      <el-col :span="10">
        <el-card shadow="never" class="panel">
          <template #header><span class="panel-title">AI 回复敏感词过滤</span></template>
          <el-form label-width="110px" class="config-form">
            <el-form-item label="过滤开关">
              <el-switch v-model="filterOn" active-text="开启" inactive-text="关闭" />
            </el-form-item>
            <el-form-item label="处理方式">
              <el-radio-group v-model="form.FILTER_ACTION" :disabled="!filterOn">
                <el-radio value="BLOCK">整条屏蔽</el-radio>
                <el-radio value="REPLACE">打码替换</el-radio>
                <el-radio value="LOG_ONLY">仅记录</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-form>
          <el-alert
            type="info"
            :closable="false"
            show-icon
            title="使用社区敏感词词库；危机词不受此开关影响，始终走危机干预流程"
          />
        </el-card>
      </el-col>
    </el-row>

    <!-- 实时测试 -->
    <el-card shadow="never" class="panel test-panel">
      <template #header><span class="panel-title">实时测试（真实消耗 token，同一账号 10 秒限一次）</span></template>
      <div class="test-row">
        <el-input
          v-model="testMessage"
          placeholder="默认：你好，介绍一下你自己"
          maxlength="1000"
          style="flex: 1"
          @keyup.enter="onTest"
        />
        <el-button type="primary" :loading="testing" @click="onTest">测试</el-button>
      </div>
      <div v-if="testResult" class="test-result">
        <div class="meta">
          <el-tag size="small">{{ testResult.model }}</el-tag>
          <span class="duration">耗时 {{ testResult.durationMs }} ms</span>
          <span v-if="testResult.inputTokens != null" class="duration">
            token：输入 {{ testResult.inputTokens }} / 输出 {{ testResult.outputTokens }}
          </span>
        </div>
        <div class="reply">{{ testResult.reply }}</div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import PageHead from '@/components/PageHead.vue'
import { getAiConfig, saveAiConfig, testAi } from '@/api/admin'

const form = reactive({
  MODEL: '',
  TEMPERATURE: 0.7,
  MAX_TOKENS: 2048,
  TOP_P: 0.9,
  INPUT_PRICE: 0,
  OUTPUT_PRICE: 0,
  FILTER_ACTION: 'LOG_ONLY'
})
const filterOn = ref(false)
const saving = ref(false)
const testMessage = ref('')
const testing = ref(false)
const testResult = ref(null)

const loadConfig = async () => {
  const res = await getAiConfig()
  const items = res.data.items || {}
  form.MODEL = items.MODEL || ''
  form.TEMPERATURE = Number(items.TEMPERATURE ?? 0.7)
  form.MAX_TOKENS = Number(items.MAX_TOKENS ?? 2048)
  form.TOP_P = Number(items.TOP_P ?? 0.9)
  form.INPUT_PRICE = Number(items.INPUT_PRICE ?? 0)
  form.OUTPUT_PRICE = Number(items.OUTPUT_PRICE ?? 0)
  form.FILTER_ACTION = items.FILTER_ACTION || 'LOG_ONLY'
  filterOn.value = items.FILTER_AI_REPLY === 'ON'
}

const onSave = async () => {
  saving.value = true
  try {
    const items = [
      { configKey: 'MODEL', configValue: form.MODEL?.trim() },
      { configKey: 'TEMPERATURE', configValue: String(form.TEMPERATURE) },
      { configKey: 'MAX_TOKENS', configValue: String(form.MAX_TOKENS) },
      { configKey: 'TOP_P', configValue: String(form.TOP_P) },
      { configKey: 'INPUT_PRICE', configValue: String(form.INPUT_PRICE) },
      { configKey: 'OUTPUT_PRICE', configValue: String(form.OUTPUT_PRICE) },
      { configKey: 'FILTER_AI_REPLY', configValue: filterOn.value ? 'ON' : 'OFF' },
      { configKey: 'FILTER_ACTION', configValue: form.FILTER_ACTION }
    ]
    await saveAiConfig({ items })
    ElMessage.success('已保存，下一条对话即生效')
  } finally {
    saving.value = false
  }
}

const onTest = async () => {
  testing.value = true
  try {
    const res = await testAi({ message: testMessage.value || undefined })
    testResult.value = res.data
  } finally {
    testing.value = false
  }
}

onMounted(loadConfig)
</script>

<style lang="scss" scoped>
.ai-config-page {
  .panel {
    margin-bottom: 16px;
    border-radius: 8px;
  }
  .panel-title {
    font-size: 15px;
    font-weight: 600;
    color: #1f2937;
  }
  .config-form {
    max-width: 560px;
  }
  .unit {
    margin-left: 10px;
    font-size: 13px;
    color: #6b7280;
  }
  .test-row {
    display: flex;
    gap: 12px;
  }
  .test-result {
    margin-top: 14px;
    padding: 12px 16px;
    background: #f7f8fa;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
    .meta {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 8px;
      .duration {
        font-size: 13px;
        color: #6b7280;
      }
    }
    .reply {
      font-size: 14px;
      line-height: 1.7;
      color: #1f2937;
      white-space: pre-wrap;
    }
  }
}
</style>
