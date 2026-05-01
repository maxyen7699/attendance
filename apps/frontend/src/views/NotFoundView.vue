<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <div class="flex min-h-screen flex-col items-center justify-center bg-gray-100">
    <h1 class="mb-4 text-6xl font-bold text-gray-800">404</h1>
    <p class="mb-8 text-lg text-gray-600">找不到您要的頁面</p>
    <button
      class="rounded-md bg-blue-600 px-6 py-2 text-white hover:bg-blue-700"
      @click="router.push('/')"
    >
      返回首頁
    </button>
  </div>
</template>
