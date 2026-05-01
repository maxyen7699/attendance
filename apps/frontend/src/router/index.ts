import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { public: true }
    },
    {
      path: '/',
      redirect: '/attendance'
    },
    {
      path: '/attendance',
      name: 'attendance',
      component: () => import('@/views/AttendanceView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/attendance/my',
      name: 'attendance-my',
      component: () => import('@/views/AttendanceMyView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/profile',
      name: 'profile',
      component: () => import('@/views/ProfileView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/admin/users',
      name: 'admin-users',
      component: () => import('@/views/AdminUsersView.vue'),
      meta: { requiresAuth: true, requiresAdmin: true }
    },
    {
      path: '/admin/attendance',
      name: 'admin-attendance',
      component: () => import('@/views/AdminAttendanceView.vue'),
      meta: { requiresAuth: true, requiresAdmin: true }
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: () => import('@/views/NotFoundView.vue'),
      meta: { public: true }
    }
  ]
})

router.beforeEach(async (to, _from, next) => {
  const auth = useAuthStore()

  // Try to fetch user if we have a token but no user object
  if (auth.accessToken && !auth.user) {
    try {
      await auth.fetchUser()
    } catch {
      auth.logout()
    }
  }

  const loggedIn = !!auth.user

  // Public route — allow access
  if (to.meta.public) {
    if (loggedIn && to.name === 'login') {
      return next({ path: '/' })
    }
    return next()
  }

  // Not logged in — redirect to login
  if (!loggedIn) {
    return next({ name: 'login', query: { redirect: to.fullPath } })
  }

  // Admin-only route
  if (to.meta.requiresAdmin && auth.user?.role !== 'ADMIN') {
    return next({ path: '/' })
  }

  next()
})

export default router
