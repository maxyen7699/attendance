<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useLeaveStore } from '@/stores/leave'

const store = useLeaveStore()
const actionLoading = ref<number | null>(null)
const message = ref<{ type: 'success' | 'error'; text: string } | null>(null)

function statusBadge(status: string): string {
  const map: Record<string, string> = {
    PENDING: 'bg-yellow-100 text-yellow-700',
    APPROVED: 'bg-green-100 text-green-700',
    REJECTED: 'bg-red-100 text-red-700',
    CANCELLED: 'bg-gray-100 text-gray-500'
  }
  return map[status] || 'bg-gray-100 text-gray-600'
}

function statusLabel(status: string): string {
  const map: Record<string, string> = {
    PENDING: '審核中',
    APPROVED: '已核准',
    REJECTED: '已駁回',
    CANCELLED: '已取消'
  }
  return map[status] || status
}

function formatDate(dateStr: string): string {
  return dateStr
}

async function handleCancel(id: number) {
  message.value = null
  actionLoading.value = id
  try {
    await store.doCancel(id)
    message.value = { type: 'success', text: '已取消請假申請' }
  } catch (err: any) {
    message.value = { type: 'error', text: err.response?.data?.message || '取消失敗' }
  } finally {
    actionLoading.value = null
  }
}

onMounted(() => {
  store.fetchMyLeaves()
})
</script>

<template>
  <div class="mx-auto max-w-4xl px-4 py-8">
    <div class="mb-6 flex items-center justify-between">
      <h1 class="text-2xl font-bold text-gray-800">我的請假紀錄</h1>
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
      v-else-if="store.myLeaves.length === 0"
      class="rounded-lg border border-gray-200 bg-white py-12 text-center text-gray-500"
    >
      尚無請假紀錄
    </div>

    <!-- Table -->
    <div v-else class="overflow-hidden rounded-lg border border-gray-200 bg-white shadow-sm">
      <table class="w-full text-sm">
        <thead class="border-b border-gray-200 bg-gray-50">
          <tr>
            <th class="px-4 py-3 text-left font-medium text-gray-600">假別</th>
            <th class="px-4 py-3 text-left font-medium text-gray-600">起始日期</th>
            <th class="px-4 py-3 text-left font-medium text-gray-600">結束日期</th>
            <th class="px-4 py-3 text-center font-medium text-gray-600">天數</th>
            <th class="px-4 py-3 text-center font-medium text-gray-600">狀態</th>
            <th class="px-4 py-3 text-left font-medium text-gray-600">事由</th>
            <th class="px-4 py-3 text-center font-medium text-gray-600">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="record in store.myLeaves"
            :key="record.id"
            class="border-b border-gray-100 last:border-b-0"
          >
            <td class="px-4 py-3 text-gray-800">{{ record.leaveTypeName }}</td>
            <td class="px-4 py-3 text-gray-600">{{ formatDate(record.startDate) }}</td>
            <td class="px-4 py-3 text-gray-600">{{ formatDate(record.endDate) }}</td>
            <td class="px-4 py-3 text-center text-gray-800">{{ record.totalDays }}</td>
            <td class="px-4 py-3 text-center">
              <span
                :class="[
                  'inline-block rounded-full px-2.5 py-0.5 text-xs font-medium',
                  statusBadge(record.status)
                ]"
              >
                {{ statusLabel(record.status) }}
              </span>
            </td>
            <td class="max-w-[200px] truncate px-4 py-3 text-gray-600">{{ record.reason }}</td>
            <td class="px-4 py-3 text-center">
              <button
                v-if="record.status === 'PENDING'"
                :disabled="actionLoading === record.id"
                class="inline-flex items-center rounded-md border border-red-300 px-3 py-1 text-xs text-red-600 hover:bg-red-50 disabled:cursor-not-allowed disabled:opacity-60"
                @click="handleCancel(record.id)"
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
                {{ actionLoading === record.id ? '處理中...' : '取消' }}
              </button>
              <span v-else class="text-xs text-gray-400">--</span>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>
