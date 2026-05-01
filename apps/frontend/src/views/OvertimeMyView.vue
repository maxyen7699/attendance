<script setup lang="ts">
import { onMounted } from 'vue'
import { useOvertimeStore } from '@/stores/overtime'

const store = useOvertimeStore()

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

function statusBadge(status: string): string {
  const map: Record<string, string> = {
    PENDING: 'bg-yellow-100 text-yellow-700',
    APPROVED: 'bg-green-100 text-green-700',
    REJECTED: 'bg-red-100 text-red-700'
  }
  return map[status] || 'bg-gray-100 text-gray-600'
}

function statusLabel(status: string): string {
  const map: Record<string, string> = {
    PENDING: '審核中',
    APPROVED: '已核准',
    REJECTED: '已駁回'
  }
  return map[status] || status
}

onMounted(() => {
  store.fetchMyOvertime()
})
</script>

<template>
  <div class="mx-auto max-w-4xl px-4 py-8">
    <div class="mb-6 flex items-center justify-between">
      <h1 class="text-2xl font-bold text-gray-800">我的加班紀錄</h1>
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
      v-else-if="store.myOvertime.length === 0"
      class="rounded-lg border border-gray-200 bg-white py-12 text-center text-gray-500"
    >
      尚無加班紀錄
    </div>

    <!-- Table -->
    <div v-else class="overflow-hidden rounded-lg border border-gray-200 bg-white shadow-sm">
      <table class="w-full text-sm">
        <thead class="border-b border-gray-200 bg-gray-50">
          <tr>
            <th class="px-4 py-3 text-left font-medium text-gray-600">日期</th>
            <th class="px-4 py-3 text-center font-medium text-gray-600">類型</th>
            <th class="px-4 py-3 text-center font-medium text-gray-600">開始時間</th>
            <th class="px-4 py-3 text-center font-medium text-gray-600">結束時間</th>
            <th class="px-4 py-3 text-center font-medium text-gray-600">時數</th>
            <th class="px-4 py-3 text-center font-medium text-gray-600">狀態</th>
            <th class="px-4 py-3 text-left font-medium text-gray-600">事由</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="record in store.myOvertime"
            :key="record.id"
            class="border-b border-gray-100 last:border-b-0"
          >
            <td class="px-4 py-3 text-gray-800">{{ record.overtimeDate }}</td>
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
            <td class="px-4 py-3 text-center text-gray-600">{{ record.startTime }}</td>
            <td class="px-4 py-3 text-center text-gray-600">{{ record.endTime }}</td>
            <td class="px-4 py-3 text-center text-gray-800">{{ record.hours }}</td>
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
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>
