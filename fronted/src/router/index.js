import { createRouter, createWebHistory } from 'vue-router'
import BackendLayout from '@/components/BackendLayout.vue'
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
                path: 'knowledge',
                component: () => import('@/views/knowledge.vue'),
                meta: {
                    title: '知识文章',
                    icon: 'ChatLineSquare'
                }
            },
            {
                path: 'consultations',
                component: () => import('@/views/consultations.vue'),
                meta: {
                    title: '咨询记录',
                    icon: 'Message'
                }
            },
            {
                path: 'emotional',
                component: () => import('@/views/emotional.vue'),
                meta: {
                    title: '情绪日志',
                    icon: 'User'
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
            }
        ]
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes: [ ...backendRoutes, ...frontendRoutes]
})

// 路由前置守卫
router.beforeEach((to, from, next) => {
    const token = localStorage.getItem('token')
    // 需要登录才能访问的页面（如个人中心），未登录则跳转登录页
    if (to.meta?.requiresAuth && !token) {
        next('/auth/login')
        return
    }
    // 当前用户是否登录
    if (token) {
        const userInfo = JSON.parse(localStorage.getItem('userInfo'))
        // 如果是后台用户
        if (userInfo.userType == 2) {
            if (to.path.startsWith('/back')) {
                next()
            } else {
                next('/back/dashboard')
            }
        } else if (userInfo.userType == 1){
            // 用户端账号只能访问前台路由
            if (to.path.startsWith('/back') || to.path.startsWith('/auth')) {
                next('/')
            } else {
                next()
            }
        }
    } else {
        if (to.path.startsWith('/back')) {
            // 如果是访问后台页面，那么跳转岛登录页
            next('/auth/login')
        } else {
            next()
        }
    }
})

export default router
