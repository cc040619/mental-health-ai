<template>
    <div class="publish-article">
        <PageHead title="发布文章" />

        <el-card shadow="never" class="form-card" v-loading="loading">
            <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
                <el-form-item label="文章标题" prop="title">
                    <el-input v-model="form.title" maxlength="200" show-word-limit
                              placeholder="请输入文章标题" />
                </el-form-item>

                <el-form-item label="文章分类" prop="categoryId">
                    <el-tree-select v-model="form.categoryId" :data="categoryTree"
                                    node-key="value" :props="{ label: 'label', children: 'children' }"
                                    check-strictly default-expand-all placeholder="请选择分类"
                                    style="width: 100%" />
                </el-form-item>

                <el-form-item label="封面图片">
                    <el-upload :show-file-list="false" :http-request="handleUploadCover" accept="image/*">
                        <img v-if="coverPreview" :src="coverPreview" class="cover-preview" />
                        <div v-else class="cover-uploader">
                            <el-icon class="cover-icon"><Plus /></el-icon>
                            <span>上传封面</span>
                        </div>
                    </el-upload>
                </el-form-item>

                <el-form-item label="文章摘要" prop="summary">
                    <el-input v-model="form.summary" type="textarea" :rows="3" maxlength="1000"
                              show-word-limit placeholder="一句话概括文章内容（选填，1000 字以内）" />
                </el-form-item>

                <el-form-item label="文章内容" prop="content">
                    <RichTextEditor v-model="form.content" placeholder="请输入文章正文"
                                    :max-char-count="20000" />
                </el-form-item>

                <el-form-item label="标签" prop="tags">
                    <el-input v-model="form.tags" maxlength="500" show-word-limit
                              placeholder="多个标签用英文逗号分隔，如：焦虑,情绪管理（选填）" />
                </el-form-item>

                <el-form-item>
                    <el-button type="primary" :loading="submitting" @click="submit">提交审核</el-button>
                    <el-button @click="resetForm">重置</el-button>
                    <span class="tip-text">提交后默认保存为草稿，由管理员审核上架后在前台展示</span>
                </el-form-item>
            </el-form>
        </el-card>
    </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import PageHead from '@/components/PageHead.vue'
import RichTextEditor from '@/components/RichTextEditor.vue'
import { resolveFileUrl } from '@/config/index.js'
import { getArticleCategories, publishArticle, uploadArticleCover } from '@/api/consultant'

const loading = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const categoryList = ref([])

const defaultForm = () => ({
    title: '',
    categoryId: null,
    coverImage: '',
    summary: '',
    content: '',
    tags: ''
})
const form = reactive(defaultForm())

const coverPreview = computed(() => (form.coverImage ? resolveFileUrl(form.coverImage) : ''))

// 扁平分类（parentId=0 为一级）转成树供 el-tree-select 使用
const categoryTree = computed(() => {
    const nodes = new Map(categoryList.value.map(c => [c.id, {
        value: c.id,
        label: c.categoryName,
        children: []
    }]))
    const roots = []
    categoryList.value.forEach(c => {
        const node = nodes.get(c.id)
        if (c.parentId && nodes.has(c.parentId)) {
            nodes.get(c.parentId).children.push(node)
        } else {
            roots.push(node)
        }
    })
    // 清空没有子节点的 children，避免空展开箭头
    const clean = (list) => list.forEach(n => {
        if (!n.children.length) {
            delete n.children
        } else {
            clean(n.children)
        }
    })
    clean(roots)
    return roots
})

const rules = {
    title: [{ required: true, message: '请输入文章标题', trigger: 'blur' }],
    categoryId: [{ required: true, message: '请选择文章分类', trigger: 'change' }],
    content: [{
        validator: (_rule, _value, callback) => {
            const text = (form.content || '').replace(/<[^>]*>/g, '').replace(/&nbsp;/g, '').trim()
            if (!text) callback(new Error('请输入文章内容'))
            else callback()
        },
        trigger: 'blur'
    }]
}

const loadCategories = () => {
    loading.value = true
    getArticleCategories().then(data => {
        categoryList.value = data || []
    }).finally(() => {
        loading.value = false
    })
}

const handleUploadCover = async (option) => {
    try {
        const res = await uploadArticleCover(option.file)
        form.coverImage = res.filePath
        ElMessage.success('封面上传成功')
    } catch (e) {
        // request 拦截器已统一提示
    }
}

const submit = () => {
    formRef.value.validate((valid) => {
        if (!valid) return
        submitting.value = true
        publishArticle({
            title: form.title,
            categoryId: form.categoryId,
            coverImage: form.coverImage || null,
            summary: form.summary || null,
            content: form.content,
            tags: form.tags || null
        }).then(() => {
            ElMessage.success('已提交为草稿，审核通过后展示')
            resetForm()
        }).finally(() => {
            submitting.value = false
        })
    })
}

const resetForm = () => {
    Object.assign(form, defaultForm())
    formRef.value?.clearValidate()
}

onMounted(loadCategories)
</script>

<style lang="scss" scoped>
.publish-article {
    .form-card {
        border-color: #e5e7eb;
        max-width: 960px;
    }
    .cover-preview {
        width: 160px;
        height: 90px;
        object-fit: cover;
        border-radius: 8px;
        border: 1px solid #e5e7eb;
    }
    .cover-uploader {
        width: 160px;
        height: 90px;
        border: 1px dashed #dcdfe6;
        border-radius: 8px;
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        gap: 6px;
        color: #909399;
        font-size: 13px;
        cursor: pointer;
        &:hover {
            border-color: #2563eb;
            color: #2563eb;
        }
        .cover-icon {
            font-size: 22px;
        }
    }
    .tip-text {
        margin-left: 12px;
        font-size: 13px;
        color: #9ca3af;
    }
}
</style>
