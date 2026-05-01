<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getMonthlyReport } from '@/api/reports'

interface MonthlySummary {
  workDays: number
  actualDays: number
  lateCount: number
  earlyLeaveCount: number
  leaveDays: number
  overtimeHours: number
}

interface DailyRecord {
  date: string
  clockIn: string | null
  clockOut: string | null
  status: string
  leaveType: string | null
  leaveStatus: string | null
}

const now = new Date()
const year = ref(now.getFullYear())
const month = ref(now.getMonth() + 1)

const summary = ref<MonthlySummary>({
  workDays: 0,
  actualDays: 0,
  lateCount: 0,
  earlyLeaveCount: 0,
  leaveDays: 0,
  overtimeHours: 0
})
const records = ref<DailyRecord[]>([])
const loading = ref(false)

const yearOptions = Array.from({ length: 5 }, (_, i) => now.getFullYear() - i)
const monthOptions = Array.from({ length: 12 }, (_, i) => i + 1)

onMounted(() => {
  fetchData()
})

async function fetchData() {
  loading.value = true
  try {
    const res = await getMonthlyReport(year.value, month.value)
    const data = res.data.data
    summary.value = data.summary ?? data
    records.value = data.records ?? data.dailyRecords ?? []
  } catch {
    summary.value = { workDays: 0, actualDays: 0, lateCount: 0, earlyLeaveCount: 0, leaveDays: 0, overtimeHours: 0 }
    records.value = []
  } finally {
    loading.value = false
  }
}

function formatTime(iso: string | null): string {
  if (!iso) return '--'
  const d = new Date(iso)
  const h = d.getHours().toString().padStart(2, '0')
  const mi = d.getMinutes().toString().padStart(2, '0')
  return `${h}:${mi}`
}

function statusLabel(status: string): string {
  const map: Record<string, string> = {
    NORMAL: '正常',
    LATE: '遲到',
    EARLY_LEAVE: '早退',
    ABSENT: '缺勤',
    FORGOT: '忘記打卡',
    LEAVE: '請假',
    HOLIDAY: '假日'
  }
  return map[status] || status
}

function statusClass(status: string): string {
  const map: Record<string, string> = {
    NORMAL: 'bg-green-100 text-green-700',
    LATE: 'bg-red-100 text-red-700',
    EARLY_LEAVE: 'bg-orange-100 text-orange-700',
    ABSENT: 'bg-gray-100 text-gray-600',
    FORGOT: 'bg-yellow-100 text-yellow-700',
    LEAVE: 'bg-blue-100 text-blue-700',
    HOLIDAY: 'bg-purple-100 text-purple-700'
  }
  return map[status] || 'bg-gray-100 text-gray-600'
}

function leaveStatusLabel(status: string | null): string {
  if (!status) return '--'
  const map: Record<string, string> = {
    PENDING: '待審核',
    APPROVED: '已核准',
    REJECTED: '已駁回'
  }
  return map[status] || status
}
</script>

<template>
  <div class="mx-auto max-w-6xl px-4 py-8">
    <h1 class="mb-6 text-2xl font-bold text-gray-800">個人月報</h1>

    <!-- Year/Month selector -->
    <div class="mb-6 flex flex-wrap items-end gap-4 rounded-lg border border-gray-200 bg-white p-4 shadow-sm">
      <div>
        <label class="mb-1 block text-xs font-medium text-gray-500">年份</label>
        <select
          v-model="year"
          class="rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
        >
          <option v-for="y in yearOptions" :key="y" :value="y">{{ y }}</option>
        </select>
      </div>
      <div>
        <label class="mb-1 block text-xs font-medium text-gray-500">月份</label>
        <select
          v-model="month"
          class="rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
        >
          <option v-for="m in monthOptions" :key="m" :value="m">{{ m }} 月</option>
        </select>
      </div>
      <button
        :disabled="loading"
        class="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-60"
        @click="fetchData"
      >
        {{ loading ? '查詢中...' : '查詢' }}
      </button>
    </div>

    <!-- Summary cards -->
    <div class="mb-6 grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-6">
      <div class="rounded-lg border border-gray-200 bg-white p-4 shadow-sm">
        <p class="text-xs font-medium text-gray-500">應上班天數</p>
        <p class="mt-1 text-2xl font-bold text-gray-800">{{ summary.workDays }}</p>
      </div>
      <div class="rounded-lg border border-gray-200 bg-white p-4 shadow-sm">
        <p class="text-xs font-medium text-gray-500">實際上班天數</p>
        <p class="mt-1 text-2xl font-bold text-blue-600">{{ summary.actualDays }}</p>
      </div>
      <div class="rounded-lg border border-gray-200 bg-white p-4 shadow-sm">
        <p class="text-xs font-medium text-gray-500">遲到次數</p>
        <p class="mt-1 text-2xl font-bold text-red-600">{{ summary.lateCount }}</p>
      </div>
      <div class="rounded-lg border border-gray-200 bg-white p-4 shadow-sm">
        <p class="text-xs font-medium text-gray-500">早退次數</p>
        <p class="mt-1 text-2xl font-bold text-orange-600">{{ summary.earlyLeaveCount }}</p>
      </div>
      <div class="rounded-lg border border-gray-200 bg-white p-4 shadow-sm">
        <p class="text-xs font-medium text-gray-500">請假天數</p>
        <p class="mt-1 text-2xl font-bold text-yellow-600">{{ summary.leaveDays }}</p>
      </div>
      <div class="rounded-lg border border-gray-200 bg-white p-4 shadow-sm">
        <p class="text-xs font-medium text-gray-500">加班時數</p>
        <p class="mt-1 text-2xl font-bold text-purple-600">{{ summary.overtimeHours }}</p>
      </div>
    </div>

    <!-- Daily detail table -->
    <div class="overflow-hidden rounded-lg border border-gray-200 bg-white shadow-sm">
      <table class="w-full text-left text-sm">
        <thead class="border-b border-gray-200 bg-gray-50">
          <tr>
            <th class="px-4 py-3 font-medium text-gray-600">日期</th>
            <th class="px-4 py-3 font-medium text-gray-600">上班時間</th>
            <th class="px-4 py-3 font-medium text-gray-600">下班時間</th>
            <th class="px-4 py-3 font-medium text-gray-600">出勤狀態</th>
            <th class="px-4 py-3 font-medium text-gray-600">請假類型</th>
            <th class="px-4 py-3 font-medium text-gray-600">請假狀態</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="record in records"
            :key="record.date"
            class="border-b border-gray-100 transition-colors hover:bg-gray-50"
          >
            <td class="px-4 py-3 text-gray-700">{{ record.date }}</td>
            <td class="px-4 py-3 text-gray-700">{{ formatTime(record.clockIn) }}</td>
            <td class="px-4 py-3 text-gray-700">{{ formatTime(record.clockOut) }}</td>
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
            <td class="px-4 py-3 text-gray-500">{{ record.leaveType || '--' }}</td>
            <td class="px-4 py-3 text-gray-500">{{ leaveStatusLabel(record.leaveStatus) }}</td>
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
