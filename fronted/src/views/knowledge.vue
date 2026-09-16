<template>
    <div class="knowledge-page">
        <el-tabs v-model="activeTab" class="content-tabs">
            <!-- ==================== 文章管理 ==================== -->
            <el-tab-pane label="文章管理" name="article">
                <div class="tab-header">
                    <TableSearch :formItem="formItem" @search="handleSearch" />
                    <div class="batch-ops" v-if="selectedIds.length > 0">
                        <el-button type="primary" size="small" @click="handleBatchStatus(1)">批量上架</el-button>
                        <el-button type="warning" size="small" @click="handleBatchStatus(2)">批量下架</el-button>
                        <el-button type="info" size="small" @click="openBatchCategoryDialog">批量移动分类</el-button>
                        <el-button type="danger" size="small" @click="handleBatchDelete">批量删除</el-button>
                        <span class="selected-count">已选 {{ selectedIds.length }} 条</span>
                    </div>
                </div>

                <el-table :data="tableData" style="width: 100%; margin-top: 16px"
                          @selection-change="handleSelectionChange">
                    <el-table-column type="selection" width="50" fixed="left" />
                    <el-table-column label="文章标题" width="320" fixed="left">
                        <template #default="scope">
                            <span class="article-title" @click="openStatsDrawer(scope.row)">{{ scope.row.title }}</span>
                        </template>
                    </el-table-column>
                    <el-table-column label="分类" width="120">
                        <template #default="scope">{{ categoryMap[scope.row.categoryId] }}</template>
                    </el-table-column>
                    <el-table-column prop="authorName" label="作者" width="100" />
                    <el-table-column prop="readCount" label="浏览量" width="90" sortable="custom"
                                      :sort-orders="['descending','ascending']" />
                    <el-table-column prop="likeCount" label="点赞" width="80" sortable="custom"
                                      :sort-orders="['descending','ascending']" />
                    <el-table-column prop="commentCount" label="评论" width="80" sortable="custom"
                                      :sort-orders="['descending','ascending']" />
                    <el-table-column prop="favoriteCount" label="收藏" width="80" sortable="custom"
                                      :sort-orders="['descending','ascending']" />
                    <el-table-column prop="createdAt" label="创建时间" width="150" sortable="custom"
                                      :sort-orders="['descending','ascending']" />
                    <el-table-column label="操作" width="280" fixed="right">
                        <template #default="scope">
                            <el-button @click="handleEdit(scope.row)" text type="primary" size="small">编辑</el-button>
                            <el-button @click="openStatsDrawer(scope.row)" text type="primary" size="small">数据</el-button>
                            <el-button @click="handlePublish(scope.row)"
                                       v-if="scope.row.status === 0 || scope.row.status === 2"
                                       text type="success" size="small">发布</el-button>
                            <el-button @click="handleUnpublish(scope.row)" v-if="scope.row.status === 1"
                                       text type="warning" size="small">下线</el-button>
                            <el-button @click="handleDelete(scope.row)" text type="danger" size="small">删除</el-button>
                        </template>
                    </el-table-column>
                </el-table>

                <el-pagination style="margin-top: 20px" :page-size="pagination.size"
                               layout="prev, pager, next" :total="pagination.total"
                               @change="handleChange" />
            </el-tab-pane>

            <!-- ==================== 评论管理 ==================== -->
            <el-tab-pane label="评论管理" name="comment">
                <div class="tab-header">
                    <el-select v-model="commentFilter.status" placeholder="评论状态" clearable style="width: 140px">
                        <el-option label="待审核" :value="0" />
                        <el-option label="已通过" :value="1" />
                        <el-option label="已拒绝" :value="2" />
                    </el-select>
                    <el-input v-model="commentFilter.articleId" placeholder="文章ID" clearable style="width: 300px; margin-left: 12px" />
                    <el-button type="primary" size="small" style="margin-left: 12px" @click="loadComments">查询</el-button>
                </div>

                <el-table :data="commentData" style="width: 100%; margin-top: 16px">
                    <el-table-column prop="articleTitle" label="文章标题" width="200" show-overflow-tooltip />
                    <el-table-column prop="userName" label="评论人" width="100" />
                    <el-table-column prop="content" label="评论内容" min-width="200" show-overflow-tooltip />
                    <el-table-column label="状态" width="100">
                        <template #default="scope">
                            <el-tag v-if="scope.row.status === 0" type="warning" size="small">待审核</el-tag>
                            <el-tag v-else-if="scope.row.status === 1" type="success" size="small">已通过</el-tag>
                            <el-tag v-else type="danger" size="small">已拒绝</el-tag>
                        </template>
                    </el-table-column>
                    <el-table-column prop="createdAt" label="评论时间" width="160" />
                    <el-table-column label="操作" width="200" fixed="right">
                        <template #default="scope">
                            <template v-if="scope.row.status === 0">
                                <el-button @click="handleReviewComment(scope.row.id, true)" text type="success" size="small">通过</el-button>
                                <el-button @click="handleReviewComment(scope.row.id, false)" text type="warning" size="small">拒绝</el-button>
                            </template>
                            <el-button @click="handleDeleteComment(scope.row.id)" text type="danger" size="small">删除</el-button>
                        </template>
                    </el-table-column>
                </el-table>

                <el-pagination style="margin-top: 20px" :page-size="commentPagination.size"
                               layout="prev, pager, next" :total="commentPagination.total"
                               @change="handleCommentPageChange" />
            </el-tab-pane>

            <!-- ==================== 分类管理 ==================== -->
            <el-tab-pane label="分类管理" name="category">
                <div class="tab-header">
                    <el-button @click="handleEditCategory(null)" type="primary" size="small">新增分类</el-button>
                </div>
                <el-table :data="categoryData" style="width: 100%; margin-top: 16px">
                    <el-table-column label="图标" width="60">
                        <template #default="scope">
                            <span style="font-size: 20px">{{ scope.row.icon || '📄' }}</span>
                        </template>
                    </el-table-column>
                    <el-table-column prop="categoryName" label="分类名称" width="200" />
                    <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
                    <el-table-column prop="sortOrder" label="排序" width="80" />
                    <el-table-column label="状态" width="80">
                        <template #default="scope">
                            <el-tag :type="scope.row.status === 1 ? 'success' : 'info'" size="small">
                                {{ scope.row.status === 1 ? '启用' : '禁用' }}
                            </el-tag>
                        </template>
                    </el-table-column>
                    <el-table-column prop="articleCount" label="文章数" width="80" />
                    <el-table-column label="操作" width="150" fixed="right">
                        <template #default="scope">
                            <el-button @click="handleEditCategory(scope.row)" text type="primary" size="small">编辑</el-button>
                            <el-button @click="handleDeleteCategory(scope.row)" text type="danger" size="small">删除</el-button>
                        </template>
                    </el-table-column>
                </el-table>
            </el-tab-pane>
        </el-tabs>

        <!-- ==================== 文章编辑弹窗 ==================== -->
        <ArticleDialog v-model:modelValue="dialogVisible" :article="currentArticle"
                       :categories="categories" @success="handleSuccess" />

        <!-- ==================== 文章数据统计抽屉 ==================== -->
        <el-drawer v-model="statsDrawerVisible" title="文章数据统计" size="600px" direction="rtl">
            <div v-if="currentStats" class="stats-content">
                <div class="stats-cards">
                    <div class="stat-card">
                        <span class="stat-label">浏览量</span>
                        <span class="stat-value">{{ currentStats.readCount }}</span>
                    </div>
                    <div class="stat-card">
                        <span class="stat-label">点赞</span>
                        <span class="stat-value">{{ currentStats.likeCount }}</span>
                    </div>
                    <div class="stat-card">
                        <span class="stat-label">评论</span>
                        <span class="stat-value">{{ currentStats.commentCount }}</span>
                    </div>
                    <div class="stat-card">
                        <span class="stat-label">收藏</span>
                        <span class="stat-value">{{ currentStats.favoriteCount }}</span>
                    </div>
                </div>
                <div class="stats-cards">
                    <div class="stat-card">
                        <span class="stat-label">平均阅读时长</span>
                        <span class="stat-value">{{ formatDuration(currentStats.avgDuration) }}</span>
                    </div>
                    <div class="stat-card">
                        <span class="stat-label">累计阅读时长</span>
                        <span class="stat-value">{{ formatDuration(currentStats.totalDuration) }}</span>
                    </div>
                    <div class="stat-card">
                        <span class="stat-label">总浏览人次</span>
                        <span class="stat-value">{{ currentStats.totalViews }}</span>
                    </div>
                </div>
                <div class="chart-section">
                    <span class="chart-title">近30天浏览趋势</span>
                    <div ref="statsChartRef" style="width: 100%; height: 280px; margin-top: 12px"></div>
                </div>
            </div>
        </el-drawer>

        <!-- ==================== 批量移动分类弹窗 ==================== -->
        <el-dialog v-model="batchCategoryDialog" title="批量移动分类" width="400px">
            <el-select v-model="batchTargetCategoryId" placeholder="选择目标分类" style="width: 100%">
                <el-option v-for="c in categories" :key="c.value" :label="c.label" :value="c.value" />
            </el-select>
            <template #footer>
                <el-button @click="batchCategoryDialog = false">取消</el-button>
                <el-button type="primary" @click="handleBatchMoveCategory">确认</el-button>
            </template>
        </el-dialog>

        <!-- ==================== 分类编辑弹窗 ==================== -->
        <el-dialog v-model="categoryDialogVisible" :title="editingCategory?.id ? '编辑分类' : '新增分类'" width="480px">
            <el-form :model="editingCategory" label-width="80px">
                <el-form-item label="分类名称">
                    <el-input v-model="editingCategory.categoryName" placeholder="请输入分类名称" />
                </el-form-item>
                <el-form-item label="分类图标">
                    <el-input v-model="editingCategory.icon" placeholder="输入 emoji 或图片URL（如 🧠）" />
                </el-form-item>
                <el-form-item label="分类描述">
                    <el-input v-model="editingCategory.description" type="textarea" :rows="2" placeholder="分类描述" />
                </el-form-item>
                <el-form-item label="排序">
                    <el-input-number v-model="editingCategory.sortOrder" :min="0" :max="999" />
                </el-form-item>
                <el-form-item label="状态">
                    <el-radio-group v-model="editingCategory.status">
                        <el-radio :value="1">启用</el-radio>
                        <el-radio :value="0">禁用</el-radio>
                    </el-radio-group>
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="categoryDialogVisible = false">取消</el-button>
                <el-button type="primary" @click="handleSaveCategory">保存</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup>
import { onMounted, ref, reactive, watch, nextTick, onBeforeUnmount } from 'vue'
import PageHead from '@/components/PageHead.vue'
import TableSearch from '@/components/TableSearch.vue'
import ArticleDialog from '@/components/ArticleDialog.vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import {
    categoryTree, articlePage, getArticleDetail, changeArticleStatus, deleteArticle,
    batchUpdateArticleStatus, batchDeleteArticles, batchMoveArticleCategory, getArticleStats,
    getCommentPage, reviewComment, deleteComment,
    getCategoryList, createCategory, updateCategory, deleteCategory
} from '@/api/admin'
import { lineChart } from '@/utils/echartsOptions'

const activeTab = ref('article')

// ==================== 文章管理 ====================
const formItem = [
    { comp: 'input', prop: 'title', label: '文章标题', placeholder: '请输入文章标题',
      col: { xs: 24, sm: 24, md: 10, lg: 10, xl: 10 } },
    { comp: 'select', prop: 'categoryId', label: '分类', placeholder: '请选择分类',
      col: { xs: 24, sm: 12, md: 7, lg: 7, xl: 7 } },
    { comp: 'select', prop: 'status', label: '状态', placeholder: '请选择',
      col: { xs: 24, sm: 12, md: 7, lg: 7, xl: 7 },
      options: [
          { label: '草稿', value: '0' },
          { label: '已发布', value: '1' },
          { label: '已下线', value: '2' }
      ]}
]

const pagination = reactive({ currentPage: 1, size: 10, total: 0 })
const tableData = ref([])
const categoryMap = reactive({})
const categories = ref([])
const selectedIds = ref([])

const handleSearch = async (formData) => {
    const params = { ...pagination, ...formData }
    const { records, total } = await articlePage(params)
    tableData.value = records
    pagination.total = total
}

const handleChange = (page) => {
    pagination.currentPage = page
    handleSearch({})
}

const handleSelectionChange = (rows) => {
    selectedIds.value = rows.map(r => r.id)
}

// 批量上架/下架
const handleBatchStatus = (status) => {
    const label = status === 1 ? '上架' : '下架'
    ElMessageBox.confirm(`确认批量${label}选中的 ${selectedIds.value.length} 篇文章吗？`, '确认', {
        confirmButtonText: `确认${label}`, cancelButtonText: '取消', type: 'info'
    }).then(() => {
        batchUpdateArticleStatus({ ids: selectedIds.value, status }).then(() => {
            ElMessage.success(`批量${label}成功`)
            handleSearch({})
        })
    })
}

// 批量删除
const handleBatchDelete = () => {
    ElMessageBox.confirm(
        `确认删除选中的 ${selectedIds.value.length} 篇文章吗？删除后将级联清除收藏、浏览、点赞、评论数据，不可恢复！`,
        '危险操作', { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning' }
    ).then(() => {
        batchDeleteArticles({ ids: selectedIds.value }).then(() => {
            ElMessage.success('批量删除成功')
            handleSearch({})
        })
    })
}

// 批量移动分类
const batchCategoryDialog = ref(false)
const batchTargetCategoryId = ref(null)
const openBatchCategoryDialog = () => { batchCategoryDialog.value = true }
const handleBatchMoveCategory = () => {
    if (!batchTargetCategoryId.value) { ElMessage.warning('请选择目标分类'); return }
    batchMoveArticleCategory({ ids: selectedIds.value, categoryId: batchTargetCategoryId.value }).then(() => {
        ElMessage.success('批量移动分类成功')
        batchCategoryDialog.value = false
        handleSearch({})
    })
}

// 文章编辑
const dialogVisible = ref(false)
const currentArticle = ref(null)
const handleEdit = (row) => {
    if (!row.id) {
        currentArticle.value = null
        dialogVisible.value = true
    } else {
        getArticleDetail(row.id).then(res => {
            currentArticle.value = res
            dialogVisible.value = true
        })
    }
}
const handleSuccess = () => { dialogVisible.value = false; handleSearch({}) }

// 发布/下线
const handlePublish = (row) => {
    ElMessageBox.confirm(`确认发布文章"${row.title}"吗？`, '确认',
        { confirmButtonText: '确认发布', cancelButtonText: '取消', type: 'info' }
    ).then(() => {
        changeArticleStatus(row.id, { status: 1 }).then(() => { ElMessage.success('发布成功'); handleSearch({}) })
    })
}
const handleUnpublish = (row) => {
    ElMessageBox.confirm(`确认下线文章"${row.title}"吗？`, '确认',
        { confirmButtonText: '确认下线', cancelButtonText: '取消', type: 'warning' }
    ).then(() => {
        changeArticleStatus(row.id, { status: 2 }).then(() => { ElMessage.success('下线成功'); handleSearch({}) })
    })
}

// 单条删除
const handleDelete = (row) => {
    ElMessageBox.confirm(`确认删除文章"${row.title}"吗？`, '确认',
        { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'danger' }
    ).then(() => {
        deleteArticle(row.id).then(() => { ElMessage.success('删除成功'); handleSearch({}) })
    })
}

// ==================== 文章数据统计 ====================
const statsDrawerVisible = ref(false)
const currentStats = ref(null)
const statsChartRef = ref(null)
let statsChart = null

const openStatsDrawer = async (row) => {
    currentStats.value = await getArticleStats(row.id)
    statsDrawerVisible.value = true
    await nextTick()
    renderStatsChart()
}

const renderStatsChart = () => {
    if (!statsChartRef.value || !currentStats.value) return
    if (statsChart) statsChart.dispose()
    statsChart = echarts.init(statsChartRef.value)
    const dates = currentStats.value.dailyBrowses.map(d => d.name)
    const values = currentStats.value.dailyBrowses.map(d => d.value)
    statsChart.setOption(lineChart(dates, [{ name: '浏览量', data: values, area: true }], { legend: false, smooth: true }))
}

const formatDuration = (seconds) => {
    if (!seconds) return '0秒'
    const m = Math.floor(seconds / 60)
    const s = seconds % 60
    return m > 0 ? `${m}分${s}秒` : `${s}秒`
}

onBeforeUnmount(() => {
    if (statsChart) { statsChart.dispose(); statsChart = null }
})

// ==================== 评论管理 ====================
const commentFilter = reactive({ status: null, articleId: '' })
const commentPagination = reactive({ currentPage: 1, size: 10, total: 0 })
const commentData = ref([])

const loadComments = async () => {
    const params = {
        page: commentPagination.currentPage,
        size: commentPagination.size,
        status: commentFilter.status,
        articleId: commentFilter.articleId || undefined
    }
    const { records, total } = await getCommentPage(params)
    commentData.value = records
    commentPagination.total = total
}

const handleCommentPageChange = (page) => {
    commentPagination.currentPage = page
    loadComments()
}

const handleReviewComment = (id, approve) => {
    const label = approve ? '通过' : '拒绝'
    ElMessageBox.confirm(`确认${label}这条评论吗？`, '确认',
        { confirmButtonText: `确认${label}`, cancelButtonText: '取消', type: 'info' }
    ).then(() => {
        reviewComment(id, { approve }).then(() => {
            ElMessage.success(`已${label}`)
            loadComments()
        })
    })
}

const handleDeleteComment = (id) => {
    ElMessageBox.confirm('确认删除这条评论吗？', '确认',
        { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning' }
    ).then(() => {
        deleteComment(id).then(() => { ElMessage.success('删除成功'); loadComments() })
    })
}

// ==================== 分类管理 ====================
const categoryData = ref([])
const categoryDialogVisible = ref(false)
const editingCategory = reactive({})

const loadCategories = async () => {
    categoryData.value = await getCategoryList()
}

const handleEditCategory = (row) => {
    if (row) {
        Object.assign(editingCategory, row)
    } else {
        Object.keys(editingCategory).forEach(k => delete editingCategory[k])
        Object.assign(editingCategory, { sortOrder: 0, status: 1 })
    }
    categoryDialogVisible.value = true
}

const handleSaveCategory = () => {
    if (!editingCategory.categoryName) { ElMessage.warning('请输入分类名称'); return }
    const data = {
        parentId: editingCategory.parentId || 0,
        categoryName: editingCategory.categoryName,
        categoryCode: editingCategory.categoryCode,
        description: editingCategory.description,
        icon: editingCategory.icon,
        sortOrder: editingCategory.sortOrder ?? 0,
        status: editingCategory.status ?? 1
    }
    if (editingCategory.id) {
        updateCategory(editingCategory.id, data).then(() => {
            ElMessage.success('更新成功')
            categoryDialogVisible.value = false
            loadCategories()
        })
    } else {
        createCategory(data).then(() => {
            ElMessage.success('创建成功')
            categoryDialogVisible.value = false
            loadCategories()
        })
    }
}

const handleDeleteCategory = (row) => {
    ElMessageBox.confirm(`确认删除分类"${row.categoryName}"吗？有文章的分类不可删除。`, '确认',
        { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning' }
    ).then(() => {
        deleteCategory(row.id).then(() => { ElMessage.success('删除成功'); loadCategories() })
    })
}

// ==================== Tab 切换加载 ====================
watch(activeTab, (tab) => {
    if (tab === 'comment') loadComments()
    else if (tab === 'category') loadCategories()
})

onMounted(async () => {
    const data = await categoryTree()
    categories.value = data.map(item => {
        categoryMap[item.id] = item.categoryName
        return { label: item.categoryName, value: item.id }
    })
    formItem[1].options = categories.value
    handleSearch({})
})
</script>

<style lang="scss" scoped>
.knowledge-page {
    background: #fff;
    padding: 20px;
    border-radius: 8px;
}

.content-tabs {
    :deep(.el-tabs__header) {
        margin-bottom: 16px;
    }
}

.tab-header {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 12px;
}

.batch-ops {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px 12px;
    background: #F7F8FA;
    border-radius: 6px;
    border: 1px solid #E5E7EB;
    margin-left: auto;

    .selected-count {
        color: #6B7280;
        font-size: 13px;
        margin-left: 4px;
    }
}

.article-title {
    color: #1F2937;
    cursor: pointer;
    &:hover { color: #409EFF; }
}

// 统计抽屉
.stats-content {
    padding: 0 20px;

    .stats-cards {
        display: flex;
        gap: 12px;
        margin-bottom: 16px;
    }

    .stat-card {
        flex: 1;
        display: flex;
        flex-direction: column;
        align-items: center;
        padding: 12px 8px;
        background: #F7F8FA;
        border-radius: 6px;
        border: 1px solid #E5E7EB;

        .stat-label {
            font-size: 12px;
            color: #6B7280;
            margin-bottom: 4px;
        }
        .stat-value {
            font-size: 20px;
            font-weight: 600;
            color: #1F2937;
        }
    }

    .chart-section {
        margin-top: 20px;
        .chart-title {
            font-size: 14px;
            font-weight: 600;
            color: #1F2937;
        }
    }
}
</style>
