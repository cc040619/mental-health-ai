<template>
    <div class="consultant-settings">
        <PageHead title="预约设置" />

        <el-card shadow="never" class="settings-card" v-loading="loading">
            <el-form :model="form" label-width="140px" class="settings-form" style="max-width: 640px">
                <el-form-item label="咨询价格（元/次）">
                    <el-input-number v-model="form.price" :min="0" :precision="2" :step="50"
                                     controls-position="right" placeholder="留空 = 不收费" />
                    <span class="field-hint">为空或 0 表示不收费，用户端不展示收费信息</span>
                </el-form-item>
                <el-form-item label="可预约天数">
                    <el-input-number v-model="form.bookingWindowDays" :min="1" :max="90"
                                     controls-position="right" />
                    <span class="field-hint">用户端只能选择今天起未来 N 天（含今天）内的时段</span>
                </el-form-item>
                <el-form-item label="用户取消时限（小时）">
                    <el-input-number v-model="form.cancelHoursBefore" :min="0" :max="48"
                                     controls-position="right" />
                    <span class="field-hint">咨询开始前 N 小时内，用户不可自助取消（0 = 不限制）；咨询师/管理员取消不受此限</span>
                </el-form-item>
                <el-form-item>
                    <el-button type="primary" :loading="saving" @click="save">保存设置</el-button>
                </el-form-item>
            </el-form>
        </el-card>
    </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import PageHead from '@/components/PageHead.vue'
import { getMySettings, updateMySettings } from '@/api/consultant'

const loading = ref(false)
const saving = ref(false)
const form = reactive({ price: null, bookingWindowDays: 14, cancelHoursBefore: 2 })

const load = () => {
    loading.value = true
    getMySettings().then(data => {
        form.price = data.price ?? null
        form.bookingWindowDays = data.bookingWindowDays ?? 14
        form.cancelHoursBefore = data.cancelHoursBefore ?? 2
    }).finally(() => {
        loading.value = false
    })
}

const save = () => {
    saving.value = true
    updateMySettings({ ...form }).then(data => {
        form.price = data.price ?? null
        form.bookingWindowDays = data.bookingWindowDays
        form.cancelHoursBefore = data.cancelHoursBefore
        ElMessage.success('预约设置已保存')
    }).finally(() => {
        saving.value = false
    })
}

onMounted(load)
</script>

<style lang="scss" scoped>
.consultant-settings {
    .settings-card {
        border-color: #e5e7eb;
    }
    .field-hint {
        display: block;
        font-size: 12px;
        color: #9ca3af;
        line-height: 1.6;
        margin-top: 4px;
    }
}
</style>
