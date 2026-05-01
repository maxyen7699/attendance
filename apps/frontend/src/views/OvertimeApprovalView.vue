<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useOvertimeStore } from '@/stores/overtime'

const store = useOvertimeStore()
const actionLoading = ref<number | null>(null)
const message = ref<{ type: 'success' | 'error'; text: string } | null>(null)

function typeBadge(type: string): string {
  const map: Record<string, string> = {
    PRE_APPLY: 'bg-blue-100 text-blue-700',
    POST_REPORT: 'bg-orange-100 text-orange-700'
  }
  return map[type] || 'bg-gray-100 text-gray-600'
}

function typeLabel(type: string): string {
  const map: Record<string, string> = {
    PRE_APPLY: '事前',
    POST_REPORT: '事後'
  }
  return map[type] || type
}

async function handleApprove(id: number) {
  message.value = null
  actionLoading.value = id
  try {
    await store.doApprove(id)
    message.value = { type: 'success', text: '已核准' }
  } catch (err: any) {
    message.value = { type: 'error', text: err.response?.data?.message || '核准失敗' }
  } finally {
    actionLoading.value = null
  }
}

async function handleReject(id: number) {
  message.value = null
  actionLoading.value = id
  try {
    await store.doReject(id)
    message.value = { type: 'success', text: '已駁回' }
  } catch (err: any) {
    message.value = { type: 'error', text: err.response?.data?.message || '駁回失敗' }
  } finally {
    actionLoading.value = null
  }
}

onMounted(() => {
  store.fetchPendingOvertime()
})
</script>

<template>
  <div class="mx-auto max-w-4xl px-4 py-8">
    <div class="mb-6 flex items-center justify-between">
      <h1 class="text-2xl font-bold text-gray-800">加班簽核</h1>
      <span class="text-sm text-gray-500">{{ store.pendingOvertime.length }} 筆待簽核</span>
    </div>

    <!-- Message -->
    <div
      v-if="message"
      :class="[
        'mb-6 rounded-lg px-4 py-3 text-sm font-medium',
        message.type === 'success'
          ? 'bg-green-50 text-green-700'
          : 'bg-red-50 text-red-700'
      ]"
    >
      {{ message.text }}
    </div>

    <!-- Loading -->
    <div v-if="store.loading" class="flex justify-center py-12">
      <svg class="h-8 w-8 animate-spin text-blue-500" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z" />
      </svg>
    </div>

    <!-- Empty State -->
    <div
      v-else-if="store.pendingOvertime.length === 0"
      class="rounded-lg border border-gray-200 bg-white py-12 text-center text-gray-500"
    >
      目前無待簽核項目
    </div>

    <!-- Table -->
    <div v-else class="overflow-hidden rounded-lg border border-gray-200 bg-white shadow-sm">
      <table class="w-full text-sm">
        <thead class="border-b border-gray-200 bg-gray-50">
          <tr>
            <th class="px-4 py-3 text-left font-medium text-gray-600">申請人</th>
            <th class="px-4 py-3 text-left font-medium text-gray-600">日期</th>
            <th class="px-4 py-3 text-center font-medium text-gray-600">類型</th>
            <th class="px-4 py-3 text-center font-medium text-gray-600">時數</th>
            <th class="px-4 py-3 text-left font-medium text-gray-600">事由</th>
            <th class="px-4 py-3 text-center font-medium text-gray-600">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="record in store.pendingOvertime"
            :key="record.id"
            class="border-b border-gray-100 last:border-b-0"
          >
            <td class="px-4 py-3 text-gray-800">{{ record.userName }}</td>
            <td class="px-4 py-3 text-gray-600">{{ record.overtimeDate }}</td>
            <td class="px-4 py-3 text-center">
              <span
                :class="[
                  'inline-block rounded-full px-2.5 py-0.5 text-xs font-medium',
                  typeBadge(record.type)
                ]"
              >
                {{ typeLabel(record.type) }}
              </span>
            </td>
            <td class="px-4 py-3 text-center text-gray-800">{{ record.hours }}</td>
            <td class="max-w-[200px] truncate px-4 py-3 text-gray-600">{{ record.reason }}</td>
            <td class="px-4 py-3">
              <div class="flex items-center justify-center gap-2">
                <button
                  :disabled="actionLoading === record.id"
                  class="inline-flex items-center rounded-md bg-green-600 px-3 py-1 text-xs font-medium text-white hover:bg-green-700 disabled:cursor-not-allowed disabled:opacity-60"
                  @click="handleApprove(record.id)"
                >
                  <svg
                    v-if="actionLoading === record.id"
                    class="-ml-1 mr-1 h-3 w-3 animate-spin"
                    xmlns="http://www.w3.org/2000/svg"
                    fill="none"
                    viewBox="0 0 24 24"
                  >
                    <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
                    <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z" />
                  </svg>
                  {{ actionLoading === record.id ? '處理中...' : '核准' }}
                </button>
                <button
                  :disabled="actionLoading === record.id"
                  class="inline-flex items-center rounded-md bg-red-600 px-3 py-1 text-xs font-medium text-white hover:bg-red-700 disabled:cursor-not-allowed disabled:opacity-60"
                  @click="handleReject(record.id)"
                >
                  駁回
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>
