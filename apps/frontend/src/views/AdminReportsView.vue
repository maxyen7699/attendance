<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getDepartmentStats, getLeaveStats, getOvertimeStats } from '@/api/reports'

interface DepartmentStat {
  department: string
  employeeCount: number
  attendanceRate: number
  avgLeaveDays: number
  avgOvertimeHours: number
  lateCount: number
}

interface LeaveStat {
  leaveType: string
  totalCount: number
  totalDays: number
  approvedCount: number
  rejectedCount: number
  pendingCount: number
}

interface OvertimeStat {
  totalHours: number
  totalCount: number
  approvedCount: number
  rejectedCount: number
  pendingCount: number
}

const now = new Date()
const year = ref(now.getFullYear())
const month = ref(now.getMonth() + 1)

const activeTab = ref<'department' | 'leave' | 'overtime'>('department')
const loading = ref(false)

const departmentStats = ref<DepartmentStat[]>([])
const leaveStats = ref<LeaveStat[]>([])
const overtimeStat = ref<OvertimeStat | null>(null)

const yearOptions = Array.from({ length: 5 }, (_, i) => now.getFullYear() - i)
const monthOptions = Array.from({ length: 12 }, (_, i) => i + 1)

onMounted(() => {
  fetchCurrentTab()
})

async function fetchCurrentTab() {
  loading.value = true
  try {
    if (activeTab.value === 'department') {
      const res = await getDepartmentStats(year.value, month.value)
      departmentStats.value = res.data.data
    } else if (activeTab.value === 'leave') {
      const res = await getLeaveStats(year.value, month.value)
      leaveStats.value = res.data.data
    } else {
      const res = await getOvertimeStats(year.value, month.value)
      overtimeStat.value = res.data.data
    }
  } catch {
    if (activeTab.value === 'department') departmentStats.value = []
    else if (activeTab.value === 'leave') leaveStats.value = []
    else overtimeStat.value = null
  } finally {
    loading.value = false
  }
}

function switchTab(tab: 'department' | 'leave' | 'overtime') {
  activeTab.value = tab
  fetchCurrentTab()
}

function formatRate(rate: number): string {
  return (rate * 100).toFixed(1) + '%'
}

function exportCSV(data: Record<string, unknown>[], filename: string) {
  if (data.length === 0) return
  const first = data[0]!
  const headers = Object.keys(first)
  const csv = [
    headers.join(','),
    ...data.map(row => headers.map(h => `"${row[h] ?? ''}"`).join(','))
  ].join('\n')
  const blob = new Blob(['﻿' + csv], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = `${filename}.csv`
  link.click()
}

function handleExport() {
  if (activeTab.value === 'department') {
    exportCSV(departmentStats.value as unknown as Record<string, unknown>[], `部門統計_${year.value}_${month.value}月`)
  } else if (activeTab.value === 'leave') {
    exportCSV(leaveStats.value as unknown as Record<string, unknown>[], `請假統計_${year.value}_${month.value}月`)
  } else {
    if (overtimeStat.value) {
      exportCSV([overtimeStat.value as unknown as Record<string, unknown>], `加班統計_${year.value}_${month.value}月`)
    }
  }
}

const tabs = [
  { key: 'department' as const, label: '部門統計' },
  { key: 'leave' as const, label: '請假統計' },
  { key: 'overtime' as const, label: '加班統計' }
]
</script>

<template>
  <div class="mx-auto max-w-6xl px-4 py-8">
    <h1 class="mb-6 text-2xl font-bold text-gray-800">統計報表</h1>

    <!-- Filter bar -->
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
        @click="fetchCurrentTab"
      >
        {{ loading ? '查詢中...' : '查詢' }}
      </button>
    </div>

    <!-- Tabs -->
    <div class="mb-6 flex items-center gap-1 border-b border-gray-200">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        :class="[
          'px-4 py-2.5 text-sm font-medium transition-colors',
          activeTab === tab.key
            ? 'border-b-2 border-blue-600 text-blue-600'
            : 'text-gray-500 hover:text-gray-700'
        ]"
        @click="switchTab(tab.key)"
      >
        {{ tab.label }}
      </button>
      <div class="flex-1"></div>
      <button
        class="rounded-md border border-gray-300 px-3 py-1.5 text-sm text-gray-600 transition-colors hover:bg-gray-50"
        @click="handleExport"
      >
        匯出 CSV
      </button>
    </div>

    <!-- Department tab -->
    <div v-if="activeTab === 'department'">
      <div class="overflow-hidden rounded-lg border border-gray-200 bg-white shadow-sm">
        <table class="w-full text-left text-sm">
          <thead class="border-b border-gray-200 bg-gray-50">
            <tr>
              <th class="px-4 py-3 font-medium text-gray-600">部門</th>
              <th class="px-4 py-3 font-medium text-gray-600">人數</th>
              <th class="px-4 py-3 font-medium text-gray-600">出勤率</th>
              <th class="px-4 py-3 font-medium text-gray-600">平均請假天數</th>
              <th class="px-4 py-3 font-medium text-gray-600">平均加班時數</th>
              <th class="px-4 py-3 font-medium text-gray-600">遲到次數</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="stat in departmentStats"
              :key="stat.department"
              class="border-b border-gray-100 transition-colors hover:bg-gray-50"
            >
              <td class="px-4 py-3 text-gray-700">{{ stat.department }}</td>
              <td class="px-4 py-3 text-gray-700">{{ stat.employeeCount }}</td>
              <td class="px-4 py-3 text-gray-700">{{ formatRate(stat.attendanceRate) }}</td>
              <td class="px-4 py-3 text-gray-700">{{ stat.avgLeaveDays }}</td>
              <td class="px-4 py-3 text-gray-700">{{ stat.avgOvertimeHours }}</td>
              <td class="px-4 py-3">
                <span v-if="stat.lateCount > 0" class="inline-block rounded-full bg-red-100 px-2.5 py-0.5 text-xs font-medium text-red-700">
                  {{ stat.lateCount }}
                </span>
                <span v-else class="text-gray-400">0</span>
              </td>
            </tr>
            <tr v-if="departmentStats.length === 0 && !loading">
              <td colspan="6" class="px-4 py-12 text-center text-gray-400">尚無部門統計資料</td>
            </tr>
            <tr v-if="loading">
              <td colspan="6" class="px-4 py-8 text-center text-gray-400">載入中...</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Leave tab -->
    <div v-if="activeTab === 'leave'">
      <div class="overflow-hidden rounded-lg border border-gray-200 bg-white shadow-sm">
        <table class="w-full text-left text-sm">
          <thead class="border-b border-gray-200 bg-gray-50">
            <tr>
              <th class="px-4 py-3 font-medium text-gray-600">假別</th>
              <th class="px-4 py-3 font-medium text-gray-600">申請數</th>
              <th class="px-4 py-3 font-medium text-gray-600">總天數</th>
              <th class="px-4 py-3 font-medium text-gray-600">核准數</th>
              <th class="px-4 py-3 font-medium text-gray-600">駁回數</th>
              <th class="px-4 py-3 font-medium text-gray-600">待審數</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="stat in leaveStats"
              :key="stat.leaveType"
              class="border-b border-gray-100 transition-colors hover:bg-gray-50"
            >
              <td class="px-4 py-3 text-gray-700">{{ stat.leaveType }}</td>
              <td class="px-4 py-3 text-gray-700">{{ stat.totalCount }}</td>
              <td class="px-4 py-3 text-gray-700">{{ stat.totalDays }}</td>
              <td class="px-4 py-3">
                <span class="inline-block rounded-full bg-green-100 px-2.5 py-0.5 text-xs font-medium text-green-700">
                  {{ stat.approvedCount }}
                </span>
              </td>
              <td class="px-4 py-3">
                <span class="inline-block rounded-full bg-red-100 px-2.5 py-0.5 text-xs font-medium text-red-700">
                  {{ stat.rejectedCount }}
                </span>
              </td>
              <td class="px-4 py-3">
                <span class="inline-block rounded-full bg-yellow-100 px-2.5 py-0.5 text-xs font-medium text-yellow-700">
                  {{ stat.pendingCount }}
                </span>
              </td>
            </tr>
            <tr v-if="leaveStats.length === 0 && !loading">
              <td colspan="6" class="px-4 py-12 text-center text-gray-400">尚無請假統計資料</td>
            </tr>
            <tr v-if="loading">
              <td colspan="6" class="px-4 py-8 text-center text-gray-400">載入中...</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Overtime tab -->
    <div v-if="activeTab === 'overtime'">
      <div v-if="overtimeStat" class="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-5">
        <div class="rounded-lg border border-gray-200 bg-white p-5 shadow-sm">
          <p class="text-sm font-medium text-gray-500">總時數</p>
          <p class="mt-2 text-2xl font-bold text-purple-600">{{ overtimeStat.totalHours }}<span class="ml-1 text-sm font-normal text-gray-400">小時</span></p>
        </div>
        <div class="rounded-lg border border-gray-200 bg-white p-5 shadow-sm">
          <p class="text-sm font-medium text-gray-500">總申請數</p>
          <p class="mt-2 text-2xl font-bold text-gray-800">{{ overtimeStat.totalCount }}</p>
        </div>
        <div class="rounded-lg border border-gray-200 bg-white p-5 shadow-sm">
          <p class="text-sm font-medium text-gray-500">核准數</p>
          <p class="mt-2 text-2xl font-bold text-green-600">{{ overtimeStat.approvedCount }}</p>
        </div>
        <div class="rounded-lg border border-gray-200 bg-white p-5 shadow-sm">
          <p class="text-sm font-medium text-gray-500">駁回數</p>
          <p class="mt-2 text-2xl font-bold text-red-600">{{ overtimeStat.rejectedCount }}</p>
        </div>
        <div class="rounded-lg border border-gray-200 bg-white p-5 shadow-sm">
          <p class="text-sm font-medium text-gray-500">待審數</p>
          <p class="mt-2 text-2xl font-bold text-yellow-600">{{ overtimeStat.pendingCount }}</p>
        </div>
      </div>
      <div v-else-if="!loading" class="rounded-lg border border-gray-200 bg-white py-12 text-center text-gray-400 shadow-sm">
        尚無加班統計資料
      </div>
      <div v-if="loading" class="rounded-lg border border-gray-200 bg-white py-8 text-center text-gray-400 shadow-sm">
        載入中...
      </div>
    </div>
  </div>
</template>
