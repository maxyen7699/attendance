<script setup lang="ts">
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

const username = ref('')
const password = ref('')
const error = ref('')
const loading = ref(false)

async function handleLogin() {
  error.value = ''
  loading.value = true
  try {
    await auth.login(username.value, password.value)
    const redirect = (route.query.redirect as string) || '/'
    router.push(redirect)
  } catch (err: unknown) {
    const message =
      (err as { response?: { data?: { message?: string } } })?.response?.data
        ?.message ?? '登入失敗，請檢查帳號密碼'
    error.value = message
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="flex min-h-screen items-center justify-center bg-gray-100">
    <div class="w-full max-w-md rounded-lg bg-white p-8 shadow-md">
      <h1 class="mb-6 text-center text-2xl font-bold text-gray-800">考勤管理系統</h1>

      <div
        v-if="error"
        class="mb-4 rounded border border-red-400 bg-red-100 px-4 py-3 text-red-700"
      >
        {{ error }}
      </div>

      <form @submit.prevent="handleLogin">
        <div class="mb-4">
          <label class="mb-2 block text-sm font-medium text-gray-700" for="username"
            >帳號</label
          >
          <input
            id="username"
            v-model="username"
            type="text"
            required
            class="w-full rounded-md border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
            placeholder="請輸入帳號"
          />
        </div>

        <div class="mb-6">
          <label class="mb-2 block text-sm font-medium text-gray-700" for="password"
            >密碼</label
          >
          <input
            id="password"
            v-model="password"
            type="password"
            required
            class="w-full rounded-md border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
            placeholder="請輸入密碼"
          />
        </div>

        <button
          type="submit"
          :disabled="loading"
          class="w-full rounded-md bg-blue-600 px-4 py-2 text-white hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
        >
          {{ loading ? '登入中...' : '登入' }}
        </button>
      </form>
    </div>
  </div>
</template>
