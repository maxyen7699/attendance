<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getDashboard } from '@/api/reports'

interface DashboardData {
  totalEmployees: number
  todayAttendanceRate: number
  pendingLeaveCount: number
  pendingOvertimeCount: number
  monthlyOvertimeHours: number
  monthlyLeaveDays: number
  recentAnomalies: AnomalyRecord[]
}

interface AnomalyRecord {
  id: number
  userName: string
  date: string
  type: string
  detail: string
}

const dashboard = ref<DashboardData>({
  totalEmployees: 0,
  todayAttendanceRate: 0,
  pendingLeaveCount: 0,
  pendingOvertimeCount: 0,
  monthlyOvertimeHours: 0,
  monthlyLeaveDays: 0,
  recentAnomalies: []
})
const loading = ref(false)

onMounted(() => {
  fetchData()
})

async function fetchData() {
  loading.value = true
  try {
    const res = await getDashboard()
    dashboard.value = res.data.data
  } catch {
    // keep default empty state
  } finally {
    loading.value = false
  }
}

function anomalyTypeLabel(type: string): string {
  const map: Record<string, string> = {
    LATE: '遲到',
    EARLY_LEAVE: '早退',
    ABSENT: '缺勤',
    FORGOT: '忘記打卡'
  }
  return map[type] || type
}

function anomalyBadgeClass(type: string): string {
  const map: Record<string, string> = {
    LATE: 'bg-red-100 text-red-700',
    EARLY_LEAVE: 'bg-orange-100 text-orange-700',
    ABSENT: 'bg-gray-100 text-gray-600',
    FORGOT: 'bg-yellow-100 text-yellow-700'
  }
  return map[type] || 'bg-gray-100 text-gray-600'
}

const attendancePercent = () => {
  return Math.round(dashboard.value.todayAttendanceRate * 100)
}
</script>

<template>
  <div class="mx-auto max-w-6xl px-4 py-8">
    <h1 class="mb-6 text-2xl font-bold text-gray-800">管理者 Dashboard</h1>

    <div v-if="loading" class="py-12 text-center text-gray-400">載入中...</div>

    <template v-else>
      <!-- Top stat cards -->
      <div class="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <!-- Total employees -->
        <div class="rounded-lg border border-gray-200 bg-white p-5 shadow-sm">
          <p class="text-sm font-medium text-gray-500">員工總數</p>
          <p class="mt-2 text-3xl font-bold text-gray-800">{{ dashboard.totalEmployees }}</p>
          <p class="mt-1 text-xs text-gray-400">位在職員工</p>
        </div>

        <!-- Today attendance rate -->
        <div class="rounded-lg border border-gray-200 bg-white p-5 shadow-sm">
          <p class="text-sm font-medium text-gray-500">今日出勤率</p>
          <p class="mt-2 text-3xl font-bold text-blue-600">{{ attendancePercent() }}%</p>
          <div class="mt-2 h-2 w-full rounded-full bg-gray-200">
            <div
              class="h-2 rounded-full bg-blue-500 transition-all"
              :style="{ width: `${attendancePercent()}%` }"
            ></div>
          </div>
        </div>

        <!-- Pending leaves -->
        <div class="rounded-lg border border-gray-200 bg-white p-5 shadow-sm">
          <p class="text-sm font-medium text-gray-500">待簽核請假</p>
          <p class="mt-2 text-3xl font-bold text-yellow-600">{{ dashboard.pendingLeaveCount }}</p>
          <p class="mt-1 text-xs text-gray-400">筆待處理</p>
        </div>

        <!-- Pending overtime -->
        <div class="rounded-lg border border-gray-200 bg-white p-5 shadow-sm">
          <p class="text-sm font-medium text-gray-500">待簽核加班</p>
          <p class="mt-2 text-3xl font-bold text-purple-600">{{ dashboard.pendingOvertimeCount }}</p>
          <p class="mt-1 text-xs text-gray-400">筆待處理</p>
        </div>
      </div>

      <!-- Middle row -->
      <div class="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-2">
        <div class="rounded-lg border border-gray-200 bg-white p-5 shadow-sm">
          <p class="text-sm font-medium text-gray-500">本月加班總時數</p>
          <p class="mt-2 text-3xl font-bold text-purple-600">{{ dashboard.monthlyOvertimeHours }}<span class="ml-1 text-base font-normal text-gray-400">小時</span></p>
        </div>
        <div class="rounded-lg border border-gray-200 bg-white p-5 shadow-sm">
          <p class="text-sm font-medium text-gray-500">本月請假總天數</p>
          <p class="mt-2 text-3xl font-bold text-yellow-600">{{ dashboard.monthlyLeaveDays }}<span class="ml-1 text-base font-normal text-gray-400">天</span></p>
        </div>
      </div>

      <!-- Recent anomalies table -->
      <div class="rounded-lg border border-gray-200 bg-white shadow-sm">
        <div class="border-b border-gray-200 px-5 py-4">
          <h2 class="text-base font-semibold text-gray-800">近期異常紀錄</h2>
        </div>
        <table class="w-full text-left text-sm">
          <thead class="border-b border-gray-200 bg-gray-50">
            <tr>
              <th class="px-5 py-3 font-medium text-gray-600">姓名</th>
              <th class="px-5 py-3 font-medium text-gray-600">日期</th>
              <th class="px-5 py-3 font-medium text-gray-600">類型</th>
              <th class="px-5 py-3 font-medium text-gray-600">詳情</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="item in dashboard.recentAnomalies"
              :key="item.id"
              class="border-b border-gray-100 transition-colors hover:bg-gray-50"
            >
              <td class="px-5 py-3 text-gray-700">{{ item.userName }}</td>
              <td class="px-5 py-3 text-gray-700">{{ item.date }}</td>
              <td class="px-5 py-3">
                <span
                  :class="[
                    'inline-block rounded-full px-2.5 py-0.5 text-xs font-medium',
                    anomalyBadgeClass(item.type)
                  ]"
                >
                  {{ anomalyTypeLabel(item.type) }}
                </span>
              </td>
              <td class="px-5 py-3 text-gray-500">{{ item.detail || '--' }}</td>
            </tr>
            <tr v-if="dashboard.recentAnomalies.length === 0">
              <td colspan="4" class="px-5 py-12 text-center text-gray-400">尚無異常紀錄</td>
            </tr>
          </tbody>
        </table>
      </div>
    </template>
  </div>
</template>
