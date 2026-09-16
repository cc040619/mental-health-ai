import { createRouter, createWebHistory } from 'vue-router'
import BackendLayout from '@/components/BackendLayout.vue'
import ConsultantLayout from '@/components/ConsultantLayout.vue'
import AuthLayout from '@/components/AuthLayout.vue'
import FrontendLayout from '@/components/FrontendLayout.vue'


// 路由配置
const backendRoutes = [
    {
        path: '/back',
        redirect: '/back/dashboard',
        component: BackendLayout,
        children: [
            {
                path: 'dashboard',
                component: () => import('@/views/dashboard.vue'),
                meta: {
                    title: '数据分析',
                    icon: 'PieChart'
                }
            },
            {
                path: 'users',
                component: () => import('@/views/users.vue'),
                meta: {
                    title: '用户管理',
                    icon: 'User'
                }
            },
            {
                path: 'consultations',
                component: () => import('@/views/consultations.vue'),
                meta: {
                    title: '咨询记录',
                    icon: 'Message',
                    group: '心理咨询'
                }
            },
            {
                path: 'emotional',
                component: () => import('@/views/emotional.vue'),
                meta: {
                    title: '情绪日志',
                    icon: 'Sunny',
                    group: '心理咨询'
                }
            },
            {
                path: 'consultants',
                component: () => import('@/views/back/ConsultantManage.vue'),
                meta: {
                    title: '咨询师管理',
                    icon: 'Avatar',
                    group: '预约咨询'
                }
            },
            {
                path: 'appointments',
                component: () => import('@/views/back/AppointmentManage.vue'),
                meta: {
                    title: '预约管理',
                    icon: 'Calendar',
                    group: '预约咨询'
                }
            },
            {
                path: 'assessment-scales',
                component: () => import('@/views/assessment-scales.vue'),
                meta: { title: '量表管理', icon: 'Document', group: '测评管理' }
            },
            {
                path: 'question-bank',
                component: () => import('@/views/question-bank.vue'),
                meta: { title: '题目库', icon: 'Collection', group: '测评管理' }
            },
            {
                path: 'assessment-records',
                component: () => import('@/views/assessment-records.vue'),
                meta: { title: '测评记录', icon: 'Tickets', group: '测评管理' }
            },
            {
                path: 'assessment-alerts',
                component: () => import('@/views/assessment-alerts.vue'),
                meta: { title: '高危预警', icon: 'Warning', group: '测评管理' }
            },
            {
                path: 'knowledge',
                component: () => import('@/views/knowledge.vue'),
                meta: {
                    title: '知识文章',
                    icon: 'ChatLineSquare',
                    group: '内容管理'
                }
            },
            {
                path: 'courses',
                component: () => import('@/views/courses.vue'),
                meta: {
                    title: '课程管理',
                    icon: 'VideoCamera',
                    group: '内容管理'
                }
            },
            {
                path: 'banners',
                component: () => import('@/views/banners.vue'),
                meta: {
                    title: '轮播图管理',
                    icon: 'Picture',
                    group: '内容管理'
                }
            },
            {
                path: 'community-posts',
                component: () => import('@/views/community-posts.vue'),
                meta: {
                    title: '帖子管理',
                    icon: 'Notebook',
                    group: '社区管理'
                }
            },
            {
                path: 'community-comments',
                component: () => import('@/views/community-comments.vue'),
                meta: {
                    title: '评论管理',
                    icon: 'ChatDotSquare',
                    group: '社区管理'
                }
            },
            {
                path: 'community-boards',
                component: () => import('@/views/community-boards.vue'),
                meta: {
                    title: '板块管理',
                    icon: 'Grid',
                    group: '社区管理'
                }
            },
            {
                path: 'community-reports',
                component: () => import('@/views/community/ReportManage.vue'),
                meta: {
                    title: '举报管理',
                    icon: 'AlarmClock',
                    group: '社区管理'
                }
            },
            {
                path: 'announcements',
                component: () => import('@/views/AnnouncementManage.vue'),
                meta: {
                    title: '公告管理',
                    icon: 'Bell',
                    group: '消息通知'
                }
            },
            {
                path: 'notifications',
                component: () => import('@/views/notifications.vue'),
                meta: {
                    title: '消息推送',
                    icon: 'Promotion',
                    group: '消息通知'
                }
            },
            {
                path: 'ai-config',
                component: () => import('@/views/ai-config.vue'),
                meta: { title: '模型参数', icon: 'Setting', group: 'AI 配置' }
            },
            {
                path: 'ai-prompts',
                component: () => import('@/views/ai-prompts.vue'),
                meta: { title: '提示词管理', icon: 'EditPen', group: 'AI 配置' }
            },
            {
                path: 'ai-blocks',
                component: () => import('@/views/ai-blocks.vue'),
                meta: { title: '拦截记录', icon: 'CircleClose', group: 'AI 配置' }
            },
            {
                path: 'ai-samples',
                component: () => import('@/views/ai-samples.vue'),
                meta: { title: '质量抽检', icon: 'Finished', group: 'AI 配置' }
            },
            {
                path: 'ai-stats',
                component: () => import('@/views/ai-stats.vue'),
                meta: { title: '调用统计', icon: 'TrendCharts', group: 'AI 配置' }
            },
            {
                path: 'settings-basic',
                component: () => import('@/views/settings-basic.vue'),
                meta: { title: '基本设置', icon: 'Tools', group: '系统设置' }
            },
            {
                path: 'settings-register',
                component: () => import('@/views/settings-register.vue'),
                meta: { title: '注册设置', icon: 'Key', group: '系统设置' }
            },
            {
                path: 'settings-content',
                component: () => import('@/views/settings-content.vue'),
                meta: { title: '内容设置', icon: 'DocumentChecked', group: '系统设置' }
            },
            {
                path: 'settings-crisis',
                component: () => import('@/views/settings-crisis.vue'),
                meta: { title: '危机干预', icon: 'WarningFilled', group: '系统设置' }
            },
            {
                path: 'operation-logs',
                component: () => import('@/views/operation-logs.vue'),
                meta: { title: '操作日志', icon: 'Document', group: '系统设置' }
            },
            {
                path: 'sensitive-words',
                component: () => import('@/views/sensitive-words.vue'),
                meta: {
                    title: '敏感词管理',
                    icon: 'Lock'
                }
            },
            {
                // 管理员模拟咨询师回复留言（复用用户端留言室组件），菜单隐藏
                path: 'appointment-chat/:id',
                component: () => import('@/views/appointment/AppointmentDetail.vue'),
                props: true,
                meta: {
                    title: '预约留言',
                    icon: 'ChatDotRound',
                    hidden: true
                }
            }
        ]
    },
    {
        path: '/auth',
        component: AuthLayout,
        children: [
            {
                path: 'login',
                component: () => import('@/views/login.vue'),
                meta: {
                    title: '登录'
                }
            },
            {
                path: 'register',
                component: () => import('@/views/register.vue'),
                meta: {
                    title: '注册'
                }
            }
        ]
    }
]

// 咨询师端路由（user_type=3，简约现代白，复用 BackendLayout 骨架，品牌/菜单独立）
const consultantRoutes = [
    {
        path: '/consultant',
        redirect: '/consultant/dashboard',
        component: ConsultantLayout,
        children: [
            {
                path: 'dashboard',
                component: () => import('@/views/consultant/dashboard.vue'),
                meta: { title: '工作台', icon: 'Odometer' }
            },
            {
                path: 'appointments',
                component: () => import('@/views/consultant/appointments.vue'),
                meta: { title: '我的预约', icon: 'Calendar' }
            },
            {
                path: 'schedule',
                component: () => import('@/views/consultant/schedule.vue'),
                meta: { title: '日程管理', icon: 'Clock' }
            },
            {
                path: 'settings',
                component: () => import('@/views/consultant/settings.vue'),
                meta: { title: '预约设置', icon: 'Setting' }
            },
            {
                path: 'reviews',
                component: () => import('@/views/consultant/reviews.vue'),
                meta: { title: '评价回复', icon: 'Star' }
            },
            {
                path: 'publish-article',
                component: () => import('@/views/consultant/publish-article.vue'),
                meta: { title: '发布文章', icon: 'EditPen' }
            },
            {
                path: 'sessions',
                component: () => import('@/views/consultant/sessions.vue'),
                meta: { title: '咨询会话', icon: 'ChatDotRound' }
            },
            {
                path: 'sessions/:appointmentId',
                component: () => import('@/views/consultant/session-detail.vue'),
                meta: { title: '会话详情' }
            }
        ]
    }
]

const frontendRoutes = [
    {
        path: '/',
        component: FrontendLayout,
        children: [
            {
                path: '',
                component: () => import('@/views/home.vue')
            },
            {
                path: 'consultation',
                component: () => import('@/views/consultation.vue')
            },
            {
                path: 'emotion-diary',
                component: () => import('@/views/emotionDiary.vue')
            },
            {
                path: 'assessment',
                component: () => import('@/views/AssessmentList.vue'),
                meta: { title: '心理测评', requiresAuth: true }
            },
            {
                path: 'assessment/quiz/:recordId',
                component: () => import('@/views/AssessmentQuiz.vue'),
                props: true,
                meta: { requiresAuth: true }
            },
            {
                path: 'assessment/report/:recordId',
                component: () => import('@/views/AssessmentReport.vue'),
                props: true,
                meta: { requiresAuth: true }
            },
            {
                path: 'assessment/history',
                component: () => import('@/views/AssessmentHistory.vue'),
                meta: { title: '测评历史', requiresAuth: true }
            },
            {
                path: 'course',
                component: () => import('@/views/course/CourseList.vue'),
                meta: { title: '心理课程', requiresAuth: true }
            },
            {
                path: 'course/:id',
                component: () => import('@/views/course/CourseDetail.vue'),
                props: true,
                meta: { title: '课程详情', requiresAuth: true }
            },
            {
                path: 'my-courses',
                component: () => import('@/views/course/MyCourses.vue'),
                meta: { title: '我的课程', requiresAuth: true }
            },
            {
                path: 'daily-practice',
                component: () => import('@/views/course/DailyPractice.vue'),
                meta: { title: '每日一练', requiresAuth: true }
            },
            {
                path: 'community',
                component: () => import('@/views/community/CommunityHome.vue'),
                meta: { title: '暖心社区', requiresAuth: true }
            },
            {
                path: 'community/publish',
                component: () => import('@/views/community/PostPublish.vue'),
                meta: { title: '发布帖子', requiresAuth: true }
            },
            {
                path: 'community/post/:id',
                component: () => import('@/views/community/PostDetail.vue'),
                props: true,
                meta: { title: '帖子详情', requiresAuth: true }
            },
            {
                path: 'community/mine',
                component: () => import('@/views/community/CommunityMine.vue'),
                meta: { title: '我的社区', requiresAuth: true }
            },
            {
                path: 'knowledge',
                component: () => import('@/views/frontendKnowledge.vue')
            },
            {
                path: 'knowledge/article/:id',
                component: () => import('@/views/articleDetail.vue'),
                props: true
            },
            {
                path: 'profile',
                component: () => import('@/views/Profile.vue'),
                meta: { title: '个人中心', requiresAuth: true }
            },
            {
                path: 'notifications',
                component: () => import('@/views/notify/NotificationCenter.vue'),
                meta: { title: '消息通知', requiresAuth: true }
            },
            {
                path: 'notifications/settings',
                component: () => import('@/views/notify/NotifySettings.vue'),
                meta: { title: '消息设置', requiresAuth: true }
            },
            {
                path: 'crisis-safety',
                component: () => import('@/views/crisisSafety.vue'),
                meta: { title: '危机安全计划', requiresAuth: true }
            },
            {
                path: 'consultants',
                component: () => import('@/views/appointment/ConsultantList.vue'),
                meta: { title: '预约咨询师', requiresAuth: true }
            },
            {
                path: 'consultants/:id',
                component: () => import('@/views/appointment/ConsultantDetail.vue'),
                props: true,
                meta: { title: '咨询师详情', requiresAuth: true }
            },
            {
                path: 'appointments',
                component: () => import('@/views/appointment/MyAppointments.vue'),
                meta: { title: '我的预约', requiresAuth: true }
            },
            {
                path: 'appointments/:id',
                component: () => import('@/views/appointment/AppointmentDetail.vue'),
                props: true,
                meta: { title: '预约详情', requiresAuth: true }
            }
        ]
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes: [...backendRoutes, ...consultantRoutes, ...frontendRoutes]
})

// 各角色登录后的首页
const HOME_BY_ROLE = {
    1: '/',
    2: '/back/dashboard',
    3: '/consultant/dashboard'
}

// 路由前置守卫：按 userType 三角色分流，未登录访问受保护区域一律登录页
router.beforeEach((to) => {
    const token = localStorage.getItem('token')
    let userType = null
    if (token) {
        try {
            userType = JSON.parse(localStorage.getItem('userInfo') || 'null')?.userType
        } catch (e) {
            userType = null
        }
    }

    // 未登录（或本地身份损坏）：后台/咨询师端与 requiresAuth 页面回登录页，公开前台放行
    // 注意：咨询师端前缀用 '/consultant/' 带尾斜杠精确匹配，避免误伤前台 '/consultants'（预约咨询师列表）
    if (!token || !userType) {
        if (to.path.startsWith('/back') || to.path.startsWith('/consultant/') || to.meta?.requiresAuth) {
            return '/auth/login'
        }
        return true
    }

    const home = HOME_BY_ROLE[userType] || '/'

    // 已登录访问登录/注册页：按角色跳各自首页
    if (to.path.startsWith('/auth')) {
        return home
    }
    // 管理端：仅管理员
    if (to.path.startsWith('/back')) {
        return userType === 2 ? true : home
    }
    // 咨询师端：仅咨询师（带尾斜杠匹配，不误伤前台 /consultants）
    if (to.path.startsWith('/consultant/')) {
        return userType === 3 ? true : home
    }
    // 前台：仅普通用户；管理员/咨询师访问前台回各自首页
    return userType === 1 ? true : home
})

export default router
