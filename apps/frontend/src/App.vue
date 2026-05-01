<script setup lang="ts">
import { RouterLink, RouterView, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const route = useRoute()

function handleLogout() {
  auth.logout()
  window.location.href = '/login'
}
</script>

<template>
  <div class="min-h-screen bg-gray-50">
    <!-- Top navigation bar — hidden on login page -->
    <nav
      v-if="route.name !== 'login' && route.name !== 'not-found'"
      class="bg-white shadow"
    >
      <div class="mx-auto flex max-w-7xl items-center justify-between px-4 py-3">
        <div class="flex items-center gap-6">
          <RouterLink to="/" class="text-lg font-bold text-gray-800"
            >考勤管理系統</RouterLink
          >
          <div class="flex gap-4 text-sm">
            <RouterLink
              to="/attendance"
              class="text-gray-600 hover:text-blue-600"
              active-class="text-blue-600 font-semibold"
            >
              打卡
            </RouterLink>
            <RouterLink
              to="/attendance/my"
              class="text-gray-600 hover:text-blue-600"
              active-class="text-blue-600 font-semibold"
            >
              出勤紀錄
            </RouterLink>
            <RouterLink
              to="/profile"
              class="text-gray-600 hover:text-blue-600"
              active-class="text-blue-600 font-semibold"
            >
              個人資料
            </RouterLink>
            <RouterLink
              v-if="auth.user?.role === 'ADMIN'"
              to="/admin/users"
              class="text-gray-600 hover:text-blue-600"
              active-class="text-blue-600 font-semibold"
            >
              使用者管理
            </RouterLink>
            <RouterLink
              v-if="auth.user?.role === 'ADMIN'"
              to="/admin/attendance"
              class="text-gray-600 hover:text-blue-600"
              active-class="text-blue-600 font-semibold"
            >
              出勤管理
            </RouterLink>
          </div>
        </div>

        <div class="flex items-center gap-4">
          <span class="text-sm text-gray-600">{{ auth.user?.name }}</span>
          <button
            class="rounded-md border border-gray-300 px-3 py-1 text-sm text-gray-600 hover:bg-gray-100"
            @click="handleLogout"
          >
            登出
          </button>
        </div>
      </div>
    </nav>

    <main>
      <RouterView />
    </main>
  </div>
</template>
