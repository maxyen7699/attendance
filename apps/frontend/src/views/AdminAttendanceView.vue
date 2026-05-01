<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getAllRecords } from '@/api/attendance'

interface AdminRecord {
  id: number
  userId: number
  userName: string
  department: string
  date: string
  clockIn: string | null
  clockOut: string | null
  status: string
  remark: string | null
}

const records = ref<AdminRecord[]>([])
const loading = ref(false)

const now = new Date()
const startDate = ref(
  `${now.getFullYear()}-${((now.getMonth() + 1).toString().padStart(2, '0'))}-01`
)
const endDate = ref(
  `${now.getFullYear()}-${((now.getMonth() + 1).toString().padStart(2, '0'))}-${now.getDate().toString().padStart(2, '0')}`
)

onMounted(() => {
  fetchRecords()
})

async function fetchRecords() {
  loading.value = true
  try {
    const res = await getAllRecords(startDate.value, endDate.value)
    records.value = res.data.data
  } catch {
    records.value = []
  } finally {
    loading.value = false
  }
}

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
  <div class="mx-auto max-w-6xl px-4 py-8">
    <h1 class="mb-6 text-2xl font-bold text-gray-800">出勤管理 — 全員查詢</h1>

    <!-- Filter bar -->
    <div class="mb-6 flex flex-wrap items-end gap-4 rounded-lg border border-gray-200 bg-white p-4 shadow-sm">
      <div>
        <label class="mb-1 block text-xs font-medium text-gray-500">開始日期</label>
        <input
          v-model="startDate"
          type="date"
          class="rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
        />
      </div>
      <div>
        <label class="mb-1 block text-xs font-medium text-gray-500">結束日期</label>
        <input
          v-model="endDate"
          type="date"
          class="rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
        />
      </div>
      <button
        :disabled="loading"
        class="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-60"
        @click="fetchRecords"
      >
        {{ loading ? '查詢中...' : '查詢' }}
      </button>
    </div>

    <!-- Table -->
    <div class="overflow-hidden rounded-lg border border-gray-200 bg-white shadow-sm">
      <table class="w-full text-left text-sm">
        <thead class="border-b border-gray-200 bg-gray-50">
          <tr>
            <th class="px-4 py-3 font-medium text-gray-600">姓名</th>
            <th class="px-4 py-3 font-medium text-gray-600">部門</th>
            <th class="px-4 py-3 font-medium text-gray-600">日期</th>
            <th class="px-4 py-3 font-medium text-gray-600">上班時間</th>
            <th class="px-4 py-3 font-medium text-gray-600">下班時間</th>
            <th class="px-4 py-3 font-medium text-gray-600">狀態</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="record in records"
            :key="record.id"
            class="border-b border-gray-100 transition-colors hover:bg-gray-50"
          >
            <td class="px-4 py-3 text-gray-700">{{ record.userName }}</td>
            <td class="px-4 py-3 text-gray-700">{{ record.department }}</td>
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
          </tr>
          <tr v-if="records.length === 0 && !loading">
            <td colspan="6" class="px-4 py-12 text-center text-gray-400">尚無出勤紀錄</td>
          </tr>
          <tr v-if="loading">
            <td colspan="6" class="px-4 py-8 text-center text-gray-400">載入中...</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>
