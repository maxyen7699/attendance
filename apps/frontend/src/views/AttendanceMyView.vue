<script setup lang="ts">
import { onMounted } from 'vue'
import { useAttendanceStore } from '@/stores/attendance'

const store = useAttendanceStore()

onMounted(() => {
  store.fetchMyRecords()
})

function formatDateTime(iso: string | null): string {
  if (!iso) return '--'
  const d = new Date(iso)
  const y = d.getFullYear()
  const mo = (d.getMonth() + 1).toString().padStart(2, '0')
  const da = d.getDate().toString().padStart(2, '0')
  const h = d.getHours().toString().padStart(2, '0')
  const mi = d.getMinutes().toString().padStart(2, '0')
  return `${y}-${mo}-${da} ${h}:${mi}`
}

function statusLabel(status: string): string {
  const map: Record<string, string> = {
    NORMAL: '正常',
    LATE: '遲到',
    EARLY_LEAVE: '早退',
    ABSENT: '缺勤',
    FORGOT: '忘記打卡'
  }
  return map[status] || status
}

function statusClass(status: string): string {
  const map: Record<string, string> = {
    NORMAL: 'bg-green-100 text-green-700',
    LATE: 'bg-red-100 text-red-700',
    EARLY_LEAVE: 'bg-orange-100 text-orange-700',
    ABSENT: 'bg-gray-100 text-gray-600',
    FORGOT: 'bg-yellow-100 text-yellow-700'
  }
  return map[status] || 'bg-gray-100 text-gray-600'
}
</script>

<template>
  <div class="mx-auto max-w-5xl px-4 py-8">
    <h1 class="mb-6 text-2xl font-bold text-gray-800">個人出勤紀錄</h1>

    <div class="overflow-hidden rounded-lg border border-gray-200 bg-white shadow-sm">
      <table class="w-full text-left text-sm">
        <thead class="border-b border-gray-200 bg-gray-50">
          <tr>
            <th class="px-4 py-3 font-medium text-gray-600">日期</th>
            <th class="px-4 py-3 font-medium text-gray-600">上班時間</th>
            <th class="px-4 py-3 font-medium text-gray-600">下班時間</th>
            <th class="px-4 py-3 font-medium text-gray-600">狀態</th>
            <th class="px-4 py-3 font-medium text-gray-600">備註</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="record in store.myRecords"
            :key="record.id"
            class="border-b border-gray-100 transition-colors hover:bg-gray-50"
          >
            <td class="px-4 py-3 text-gray-700">{{ record.date }}</td>
            <td class="px-4 py-3 text-gray-700">{{ formatDateTime(record.clockIn) }}</td>
            <td class="px-4 py-3 text-gray-700">{{ formatDateTime(record.clockOut) }}</td>
            <td class="px-4 py-3">
              <span
                :class="[
                  'inline-block rounded-full px-2.5 py-0.5 text-xs font-medium',
                  statusClass(record.status)
                ]"
              >
                {{ statusLabel(record.status) }}
              </span>
            </td>
            <td class="px-4 py-3 text-gray-500">{{ record.remark || '--' }}</td>
          </tr>
          <tr v-if="store.myRecords.length === 0">
            <td colspan="5" class="px-4 py-12 text-center text-gray-400">尚無出勤紀錄</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>
