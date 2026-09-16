<template>
    <el-form ref="ruleFormRef" :model="formData" style="width: 100%">
        <el-row :gutter="24">
            <template v-for="item in formItemAttrs" :key="item.prop">
                <el-col v-bind="item.col">
                    <el-form-item :label="item.label" :prop="item.prop">
                        <component v-model="formData[item.prop]" :is="isComp(item.comp)" :placeholder="item.placeholder" style="width: 100%">
                            <template v-if="item.comp === 'select'">
                                <el-option label="全部" value="" />
                                <el-option
                                    v-for="opt in item.options"
                                    :key="opt.value"
                                    :label="opt.label"
                                    :value="opt.value"/>
                            </template>
                        </component>
                    </el-form-item>
                </el-col>
            </template>
        </el-row>
        <el-row>
            <el-button type="primary" @click="handleSearch">查询</el-button>
            <el-button @click="handleReset(ruleFormRef)">重置</el-button>
        </el-row>
    </el-form>
</template>
<script setup>
import { ref, reactive, computed } from 'vue'

const props = defineProps({
    formItem: {
        type: Array,
        default: () => []
    }
})
const emit = defineEmits(['search'])

const formItemAttrs = computed(() => {
    const { formItem } = props
    formItem.forEach(item => {
        // 外部传了 col 就用外部的（允许 knowledge.vue 等页面按需调宽）
        item.col = item.col || { xs: 24, sm: 12, md: 8, lg: 6, xl: 6 }
    })
    return formItem
})

// 表单数据
const ruleFormRef = ref()
const formData = reactive({})
const isComp = (comp) => {
    return {
        input: 'elInput',
        select: 'elSelect'
    }[comp]
}

const handleSearch = () => {
    emit('search', formData)
}
const handleReset = (formEl) => {
    // 先重置表单，然后在触发查询
    if (!formEl)  return
    formEl.resetFields()
    emit('search', formData)
}
</script>