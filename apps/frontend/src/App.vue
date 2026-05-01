<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink, RouterView, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const route = useRoute()
const openMenu = ref<string | null>(null)

function handleLogout() {
  auth.logout()
  window.location.href = '/login'
}

function toggleMenu(menu: string) {
  openMenu.value = openMenu.value === menu ? null : menu
}

function openMenuTo(menu: string) {
  openMenu.value = menu
}

function closeMenu() {
  openMenu.value = null
}
</script>

<template>
  <div class="min-h-screen bg-gray-50">
    <nav
      v-if="route.name !== 'login' && route.name !== 'not-found'"
      class="bg-white shadow"
    >
      <div class="mx-auto flex max-w-7xl items-center justify-between px-4 py-3">
        <div class="flex items-center gap-6">
          <RouterLink to="/" class="text-lg font-bold text-gray-800">考勤管理系統</RouterLink>
          <div class="flex gap-4 text-sm">
            <RouterLink to="/attendance" class="text-gray-600 hover:text-blue-600" active-class="text-blue-600 font-semibold">
              打卡
            </RouterLink>
            <RouterLink to="/attendance/my" class="text-gray-600 hover:text-blue-600" active-class="text-blue-600 font-semibold">
              出勤紀錄
            </RouterLink>

            <!-- Leave Dropdown -->
            <div class="relative" @mouseleave="closeMenu">
              <button
                class="inline-flex items-center gap-1 text-gray-600 hover:text-blue-600"
                :class="{ 'text-blue-600 font-semibold': route.path.startsWith('/leaves') }"
                @mouseenter="openMenuTo('leave')"
                @click="toggleMenu('leave')"
              >
                請假管理
                <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M19.5 8.25l-7.5 7.5-7.5-7.5" />
                </svg>
              </button>
              <div v-show="openMenu === 'leave'" class="absolute left-0 top-full z-10 pt-2">
                <div class="w-40 rounded-md border border-gray-200 bg-white py-1 shadow-lg">
                  <RouterLink to="/leaves/apply" class="block px-4 py-2 text-gray-700 hover:bg-gray-50" @click="closeMenu">請假申請</RouterLink>
                  <RouterLink to="/leaves/my" class="block px-4 py-2 text-gray-700 hover:bg-gray-50" @click="closeMenu">我的請假</RouterLink>
                  <RouterLink to="/leaves/balance" class="block px-4 py-2 text-gray-700 hover:bg-gray-50" @click="closeMenu">請假餘額</RouterLink>
                  <RouterLink to="/leaves/approval" class="block px-4 py-2 text-gray-700 hover:bg-gray-50" @click="closeMenu">簽核管理</RouterLink>
                </div>
              </div>
            </div>

            <!-- Overtime Dropdown -->
            <div class="relative" @mouseleave="closeMenu">
              <button
                class="inline-flex items-center gap-1 text-gray-600 hover:text-blue-600"
                :class="{ 'text-blue-600 font-semibold': route.path.startsWith('/overtime') }"
                @mouseenter="openMenuTo('overtime')"
                @click="toggleMenu('overtime')"
              >
                加班管理
                <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M19.5 8.25l-7.5 7.5-7.5-7.5" />
                </svg>
              </button>
              <div v-show="openMenu === 'overtime'" class="absolute left-0 top-full z-10 pt-2">
                <div class="w-40 rounded-md border border-gray-200 bg-white py-1 shadow-lg">
                  <RouterLink to="/overtime/apply" class="block px-4 py-2 text-gray-700 hover:bg-gray-50" @click="closeMenu">加班申請</RouterLink>
                  <RouterLink to="/overtime/my" class="block px-4 py-2 text-gray-700 hover:bg-gray-50" @click="closeMenu">我的加班</RouterLink>
                  <RouterLink v-if="auth.user?.role === 'ADMIN'" to="/overtime/approval" class="block px-4 py-2 text-gray-700 hover:bg-gray-50" @click="closeMenu">加班簽核</RouterLink>
                </div>
              </div>
            </div>

            <RouterLink to="/reports/monthly" class="text-gray-600 hover:text-blue-600" active-class="text-blue-600 font-semibold">
              個人月報
            </RouterLink>

            <!-- Report Dropdown (Admin only) -->
            <div v-if="auth.user?.role === 'ADMIN'" class="relative" @mouseleave="closeMenu">
              <button
                class="inline-flex items-center gap-1 text-gray-600 hover:text-blue-600"
                :class="{ 'text-blue-600 font-semibold': route.path.startsWith('/admin/dashboard') || route.path.startsWith('/admin/reports') }"
                @mouseenter="openMenuTo('report')"
                @click="toggleMenu('report')"
              >
                報表管理
                <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M19.5 8.25l-7.5 7.5-7.5-7.5" />
                </svg>
              </button>
              <div v-show="openMenu === 'report'" class="absolute left-0 top-full z-10 pt-2">
                <div class="w-40 rounded-md border border-gray-200 bg-white py-1 shadow-lg">
                  <RouterLink to="/admin/dashboard" class="block px-4 py-2 text-gray-700 hover:bg-gray-50" @click="closeMenu">管理者 Dashboard</RouterLink>
                  <RouterLink to="/admin/reports" class="block px-4 py-2 text-gray-700 hover:bg-gray-50" @click="closeMenu">統計報表</RouterLink>
                </div>
              </div>
            </div>

            <RouterLink to="/profile" class="text-gray-600 hover:text-blue-600" active-class="text-blue-600 font-semibold">
              個人資料
            </RouterLink>
            <RouterLink v-if="auth.user?.role === 'ADMIN'" to="/admin/users" class="text-gray-600 hover:text-blue-600" active-class="text-blue-600 font-semibold">
              使用者管理
            </RouterLink>
            <RouterLink v-if="auth.user?.role === 'ADMIN'" to="/admin/attendance" class="text-gray-600 hover:text-blue-600" active-class="text-blue-600 font-semibold">
              出勤管理
            </RouterLink>
          </div>
        </div>

        <div class="flex items-center gap-4">
          <span class="text-sm text-gray-600">{{ auth.user?.name }}</span>
          <button class="rounded-md border border-gray-300 px-3 py-1 text-sm text-gray-600 hover:bg-gray-100" @click="handleLogout">
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
