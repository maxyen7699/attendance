<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useAttendanceStore } from '@/stores/attendance'

const store = useAttendanceStore()
const currentTime = ref(new Date())
const message = ref<{ type: 'success' | 'error'; text: string } | null>(null)
const actionLoading = ref(false)

let timer: ReturnType<typeof setInterval> | null = null

onMounted(async () => {
  timer = setInterval(() => {
    currentTime.value = new Date()
  }, 1000)
  await store.fetchTodayStatus()
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})

function formatTime(date: Date): string {
  const h = date.getHours().toString().padStart(2, '0')
  const m = date.getMinutes().toString().padStart(2, '0')
  const s = date.getSeconds().toString().padStart(2, '0')
  return `${h}:${m}:${s}`
}

function formatDate(date: Date): string {
  const y = date.getFullYear()
  const m = (date.getMonth() + 1).toString().padStart(2, '0')
  const d = date.getDate().toString().padStart(2, '0')
  const weekdays = ['日', '一', '二', '三', '四', '五', '六']
  return `${y}/${m}/${d} (星期${weekdays[date.getDay()]})`
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

function statusLabel(status: string | null): string {
  if (!status) return '--'
  const map: Record<string, string> = {
    NORMAL: '正常',
    LATE: '遲到',
    EARLY_LEAVE: '早退',
    ABSENT: '缺勤',
    FORGOT: '忘記打卡'
  }
  return map[status] || status
}

function statusClass(status: string | null): string {
  if (!status) return ''
  const map: Record<string, string> = {
    NORMAL: 'bg-green-100 text-green-700',
    LATE: 'bg-red-100 text-red-700',
    EARLY_LEAVE: 'bg-orange-100 text-orange-700',
    ABSENT: 'bg-gray-100 text-gray-600',
    FORGOT: 'bg-yellow-100 text-yellow-700'
  }
  return map[status] || 'bg-gray-100 text-gray-600'
}

async function handleClockIn() {
  actionLoading.value = true
  message.value = null
  try {
    const res = await store.doClockIn()
    message.value = { type: 'success', text: res.message || '上班打卡成功' }
  } catch (err: any) {
    message.value = { type: 'error', text: err.response?.data?.message || '打卡失敗，請稍後再試' }
  } finally {
    actionLoading.value = false
  }
}

async function handleClockOut() {
  actionLoading.value = true
  message.value = null
  try {
    const res = await store.doClockOut()
    message.value = { type: 'success', text: res.message || '下班打卡成功' }
  } catch (err: any) {
    message.value = { type: 'error', text: err.response?.data?.message || '打卡失敗，請稍後再試' }
  } finally {
    actionLoading.value = false
  }
}
</script>

<template>
  <div class="mx-auto max-w-xl px-4 py-8">
    <!-- Date display -->
    <div class="mb-2 text-center text-gray-500">{{ formatDate(currentTime) }}</div>

    <!-- Live clock -->
    <div class="mb-8 text-center text-5xl font-bold tracking-widest text-gray-800">
      {{ formatTime(currentTime) }}
    </div>

    <!-- Message -->
    <div
      v-if="message"
      :class="[
        'mb-6 rounded-lg px-4 py-3 text-center text-sm font-medium',
        message.type === 'success'
          ? 'bg-green-50 text-green-700'
          : 'bg-red-50 text-red-700'
      ]"
    >
      {{ message.text }}
    </div>

    <!-- Clock-in / Clock-out buttons -->
    <div v-if="store.loading" class="flex justify-center py-12">
      <svg class="h-8 w-8 animate-spin text-blue-500" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z" />
      </svg>
    </div>

    <div v-else class="flex flex-col items-center gap-6">
      <!-- Not clocked in yet -->
      <button
        v-if="!store.todayStatus?.hasClockedIn"
        :disabled="actionLoading"
        class="flex h-44 w-44 flex-col items-center justify-center rounded-full bg-blue-600 text-white shadow-lg transition-all hover:bg-blue-700 hover:shadow-xl disabled:cursor-not-allowed disabled:opacity-60"
        @click="handleClockIn"
      >
        <svg v-if="actionLoading" class="mb-2 h-8 w-8 animate-spin" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z" />
        </svg>
        <span class="text-lg font-bold">{{ actionLoading ? '打卡中...' : '上班打卡' }}</span>
      </button>

      <!-- Clocked in, not out -->
      <template v-else-if="store.todayStatus?.hasClockedIn && !store.todayStatus?.hasClockedOut">
        <div class="text-sm text-gray-500">
          上班時間：{{ formatDateTime(store.todayStatus.clockIn) }}
        </div>
        <button
          :disabled="actionLoading"
          class="flex h-44 w-44 flex-col items-center justify-center rounded-full bg-green-600 text-white shadow-lg transition-all hover:bg-green-700 hover:shadow-xl disabled:cursor-not-allowed disabled:opacity-60"
          @click="handleClockOut"
        >
          <svg v-if="actionLoading" class="mb-2 h-8 w-8 animate-spin" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z" />
          </svg>
          <span class="text-lg font-bold">{{ actionLoading ? '打卡中...' : '下班打卡' }}</span>
        </button>
      </template>

      <!-- Already clocked out -->
      <div
        v-else-if="store.todayStatus?.hasClockedOut"
        class="flex flex-col items-center gap-3"
      >
        <div class="flex h-44 w-44 flex-col items-center justify-center rounded-full bg-gray-100 text-gray-500">
          <svg class="mb-2 h-10 w-10 text-green-500" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" d="M9 12.75L11.25 15 15 9.75M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <span class="text-lg font-bold">今日已完成</span>
        </div>
        <div class="text-sm text-gray-500">
          上班：{{ formatDateTime(store.todayStatus.clockIn) }}
        </div>
        <div class="text-sm text-gray-500">
          下班：{{ formatDateTime(store.todayStatus.clockOut) }}
        </div>
      </div>
    </div>

    <!-- Today status card -->
    <div
      v-if="store.todayStatus && !store.loading"
      class="mt-10 rounded-lg border border-gray-200 bg-white p-5 shadow-sm"
    >
      <h3 class="mb-3 text-sm font-semibold text-gray-500">今日狀態</h3>
      <div class="grid grid-cols-2 gap-4">
        <div>
          <div class="text-xs text-gray-400">上班打卡</div>
          <div class="text-sm font-medium text-gray-700">
            {{ formatDateTime(store.todayStatus.clockIn) }}
          </div>
        </div>
        <div>
          <div class="text-xs text-gray-400">下班打卡</div>
          <div class="text-sm font-medium text-gray-700">
            {{ formatDateTime(store.todayStatus.clockOut) }}
          </div>
        </div>
        <div>
          <div class="text-xs text-gray-400">出勤狀態</div>
          <span
            v-if="store.todayStatus.status"
            :class="[
              'mt-1 inline-block rounded-full px-2.5 py-0.5 text-xs font-medium',
              statusClass(store.todayStatus.status)
            ]"
          >
            {{ statusLabel(store.todayStatus.status) }}
          </span>
          <span v-else class="text-sm text-gray-400">--</span>
        </div>
      </div>
    </div>
  </div>
</template>
