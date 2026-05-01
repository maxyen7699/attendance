<script setup lang="ts">
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const user = auth.user
</script>

<template>
  <div class="mx-auto max-w-2xl p-6">
    <h1 class="mb-6 text-2xl font-bold text-gray-800">個人資料</h1>

    <div v-if="user" class="rounded-lg bg-white p-6 shadow-md">
      <div class="space-y-4">
        <div class="flex border-b border-gray-200 pb-4">
          <span class="w-24 shrink-0 font-medium text-gray-500">姓名</span>
          <span class="text-gray-800">{{ user.name }}</span>
        </div>
        <div class="flex border-b border-gray-200 pb-4">
          <span class="w-24 shrink-0 font-medium text-gray-500">帳號</span>
          <span class="text-gray-800">{{ user.username }}</span>
        </div>
        <div class="flex border-b border-gray-200 pb-4">
          <span class="w-24 shrink-0 font-medium text-gray-500">Email</span>
          <span class="text-gray-800">{{ user.email }}</span>
        </div>
        <div class="flex border-b border-gray-200 pb-4">
          <span class="w-24 shrink-0 font-medium text-gray-500">部門</span>
          <span class="text-gray-800">{{ user.department }}</span>
        </div>
        <div class="flex border-b border-gray-200 pb-4">
          <span class="w-24 shrink-0 font-medium text-gray-500">職位</span>
          <span class="text-gray-800">{{ user.position }}</span>
        </div>
        <div class="flex">
          <span class="w-24 shrink-0 font-medium text-gray-500">角色</span>
          <span
            class="rounded-full px-2 py-0.5 text-xs font-semibold"
            :class="
              user.role === 'ADMIN'
                ? 'bg-purple-100 text-purple-800'
                : 'bg-blue-100 text-blue-800'
            "
          >
            {{ user.role === 'ADMIN' ? '管理員' : '使用者' }}
          </span>
        </div>
      </div>
    </div>

    <div v-else class="rounded-lg bg-white p-6 text-center text-gray-500 shadow-md">
      載入中...
    </div>
  </div>
</template>
