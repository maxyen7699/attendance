<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink, RouterView, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const route = useRoute()
const leaveMenuOpen = ref(false)
const overtimeMenuOpen = ref(false)

function handleLogout() {
  auth.logout()
  window.location.href = '/login'
}

function toggleLeaveMenu() {
  leaveMenuOpen.value = !leaveMenuOpen.value
}

function closeLeaveMenu() {
  leaveMenuOpen.value = false
}

function toggleOvertimeMenu() {
  overtimeMenuOpen.value = !overtimeMenuOpen.value
}

function closeOvertimeMenu() {
  overtimeMenuOpen.value = false
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

            <!-- Leave Management Dropdown -->
            <div class="relative" @mouseleave="closeLeaveMenu">
              <button
                class="inline-flex items-center gap-1 text-gray-600 hover:text-blue-600"
                :class="{ 'text-blue-600 font-semibold': route.path.startsWith('/leaves') }"
                @mouseenter="leaveMenuOpen = true"
                @click="toggleLeaveMenu"
              >
                請假管理
                <svg class="h-4 w-4" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M19.5 8.25l-7.5 7.5-7.5-7.5" />
                </svg>
              </button>
              <div
                v-show="leaveMenuOpen"
                class="absolute left-0 z-10 mt-2 w-40 origin-top-left rounded-md border border-gray-200 bg-white py-1 shadow-lg"
                @mouseenter="leaveMenuOpen = true"
                @mouseleave="closeLeaveMenu"
              >
                <RouterLink
                  to="/leaves/apply"
                  class="block px-4 py-2 text-gray-700 hover:bg-gray-50"
                  active-class="bg-blue-50 text-blue-600"
                  @click="closeLeaveMenu"
                >
                  請假申請
                </RouterLink>
                <RouterLink
                  to="/leaves/my"
                  class="block px-4 py-2 text-gray-700 hover:bg-gray-50"
                  active-class="bg-blue-50 text-blue-600"
                  @click="closeLeaveMenu"
                >
                  我的請假
                </RouterLink>
                <RouterLink
                  to="/leaves/balance"
                  class="block px-4 py-2 text-gray-700 hover:bg-gray-50"
                  active-class="bg-blue-50 text-blue-600"
                  @click="closeLeaveMenu"
                >
                  請假餘額
                </RouterLink>
                <RouterLink
                  to="/leaves/approval"
                  class="block px-4 py-2 text-gray-700 hover:bg-gray-50"
                  active-class="bg-blue-50 text-blue-600"
                  @click="closeLeaveMenu"
                >
                  簽核管理
                </RouterLink>
              </div>
            </div>

            <!-- Overtime Management Dropdown -->
            <div class="relative" @mouseleave="closeOvertimeMenu">
              <button
                class="inline-flex items-center gap-1 text-gray-600 hover:text-blue-600"
                :class="{ 'text-blue-600 font-semibold': route.path.startsWith('/overtime') }"
                @mouseenter="overtimeMenuOpen = true"
                @click="toggleOvertimeMenu"
              >
                加班管理
                <svg class="h-4 w-4" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M19.5 8.25l-7.5 7.5-7.5-7.5" />
                </svg>
              </button>
              <div
                v-show="overtimeMenuOpen"
                class="absolute left-0 z-10 mt-2 w-40 origin-top-left rounded-md border border-gray-200 bg-white py-1 shadow-lg"
                @mouseenter="overtimeMenuOpen = true"
                @mouseleave="closeOvertimeMenu"
              >
                <RouterLink
                  to="/overtime/apply"
                  class="block px-4 py-2 text-gray-700 hover:bg-gray-50"
                  active-class="bg-blue-50 text-blue-600"
                  @click="closeOvertimeMenu"
                >
                  加班申請
                </RouterLink>
                <RouterLink
                  to="/overtime/my"
                  class="block px-4 py-2 text-gray-700 hover:bg-gray-50"
                  active-class="bg-blue-50 text-blue-600"
                  @click="closeOvertimeMenu"
                >
                  我的加班
                </RouterLink>
                <RouterLink
                  v-if="auth.user?.role === 'ADMIN'"
                  to="/overtime/approval"
                  class="block px-4 py-2 text-gray-700 hover:bg-gray-50"
                  active-class="bg-blue-50 text-blue-600"
                  @click="closeOvertimeMenu"
                >
                  加班簽核
                </RouterLink>
              </div>
            </div>

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
