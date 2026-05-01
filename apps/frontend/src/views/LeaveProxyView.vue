<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useLeaveStore } from '@/stores/leave'

const store = useLeaveStore()
const actionLoading = ref<number | null>(null)
const message = ref<{ type: 'success' | 'error'; text: string } | null>(null)

async function handleProxyApprove(leaveId: number) {
  message.value = null
  actionLoading.value = leaveId
  try {
    await store.doProxyApprove(leaveId)
    message.value = { type: 'success', text: '已代為核准' }
  } catch (err: any) {
    message.value = { type: 'error', text: err.response?.data?.message || '核准失敗' }
  } finally {
    actionLoading.value = null
  }
}

async function handleProxyReject(leaveId: number) {
  message.value = null
  actionLoading.value = leaveId
  try {
    await store.doProxyReject(leaveId)
    message.value = { type: 'success', text: '已代為駁回' }
  } catch (err: any) {
    message.value = { type: 'error', text: err.response?.data?.message || '駁回失敗' }
  } finally {
    actionLoading.value = null
  }
}

function formatDate(dateStr: string): string {
  return dateStr
}

onMounted(() => {
  store.fetchProxyTasks()
})
</script>

<template>
  <div class="mx-auto max-w-4xl px-4 py-8">
    <div class="mb-6 flex items-center justify-between">
      <h1 class="text-2xl font-bold text-gray-800">代理簽核</h1>
      <span class="text-sm text-gray-500">{{ store.proxyTasks.length }} 筆待代理簽核</span>
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
      v-else-if="store.proxyTasks.length === 0"
      class="rounded-lg border border-gray-200 bg-white py-12 text-center text-gray-500"
    >
      目前無代理簽核項目
    </div>

    <!-- Table -->
    <div v-else class="overflow-hidden rounded-lg border border-gray-200 bg-white shadow-sm">
      <table class="w-full text-sm">
        <thead class="border-b border-gray-200 bg-gray-50">
          <tr>
            <th class="px-4 py-3 text-left font-medium text-gray-600">原簽核人</th>
            <th class="px-4 py-3 text-left font-medium text-gray-600">申請人</th>
            <th class="px-4 py-3 text-left font-medium text-gray-600">假別</th>
            <th class="px-4 py-3 text-left font-medium text-gray-600">起始日期</th>
            <th class="px-4 py-3 text-left font-medium text-gray-600">結束日期</th>
            <th class="px-4 py-3 text-center font-medium text-gray-600">天數</th>
            <th class="px-4 py-3 text-left font-medium text-gray-600">事由</th>
            <th class="px-4 py-3 text-center font-medium text-gray-600">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="task in store.proxyTasks"
            :key="task.id"
            class="border-b border-gray-100 last:border-b-0"
          >
            <td class="px-4 py-3">
              <span class="inline-block rounded-full bg-blue-50 px-2.5 py-0.5 text-xs font-medium text-blue-700">
                {{ task.originalApproverName }}
              </span>
            </td>
            <td class="px-4 py-3 text-gray-800">{{ task.applicantName }}</td>
            <td class="px-4 py-3 text-gray-800">{{ task.leaveTypeName }}</td>
            <td class="px-4 py-3 text-gray-600">{{ formatDate(task.startDate) }}</td>
            <td class="px-4 py-3 text-gray-600">{{ formatDate(task.endDate) }}</td>
            <td class="px-4 py-3 text-center text-gray-800">{{ task.totalDays }}</td>
            <td class="max-w-[200px] truncate px-4 py-3 text-gray-600">{{ task.reason }}</td>
            <td class="px-4 py-3">
              <div class="flex items-center justify-center gap-2">
                <button
                  :disabled="actionLoading === task.leaveId"
                  class="inline-flex items-center rounded-md bg-green-600 px-3 py-1 text-xs font-medium text-white hover:bg-green-700 disabled:cursor-not-allowed disabled:opacity-60"
                  @click="handleProxyApprove(task.leaveId)"
                >
                  <svg
                    v-if="actionLoading === task.leaveId"
                    class="-ml-1 mr-1 h-3 w-3 animate-spin"
                    xmlns="http://www.w3.org/2000/svg"
                    fill="none"
                    viewBox="0 0 24 24"
                  >
                    <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
                    <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z" />
                  </svg>
                  {{ actionLoading === task.leaveId ? '處理中...' : '核准' }}
                </button>
                <button
                  :disabled="actionLoading === task.leaveId"
                  class="inline-flex items-center rounded-md bg-red-600 px-3 py-1 text-xs font-medium text-white hover:bg-red-700 disabled:cursor-not-allowed disabled:opacity-60"
                  @click="handleProxyReject(task.leaveId)"
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
