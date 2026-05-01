<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useOvertimeStore } from '@/stores/overtime'

const router = useRouter()
const store = useOvertimeStore()

const now = new Date()
const dateYear = ref(now.getFullYear())
const dateMonth = ref(now.getMonth() + 1)
const dateDay = ref(now.getDate())
const startHour = ref(18)
const startMinute = ref(0)
const endHour = ref(21)
const endMinute = ref(0)

const form = ref({
  type: 'PRE_APPLY' as 'PRE_APPLY' | 'POST_REPORT',
  hours: 3,
  reason: ''
})

const submitting = ref(false)
const message = ref<{ type: 'success' | 'error'; text: string } | null>(null)

const years = Array.from({ length: 3 }, (_, i) => now.getFullYear() - 1 + i)
const months = Array.from({ length: 12 }, (_, i) => i + 1)
const hours = Array.from({ length: 24 }, (_, i) => i)
const minutes = [0, 15, 30, 45]

function daysInMonth(y: number, m: number): number {
  return new Date(y, m, 0).getDate()
}

const days = computed(() => {
  return Array.from({ length: daysInMonth(dateYear.value, dateMonth.value) }, (_, i) => i + 1)
})

const overtimeDate = computed(() => {
  const m = String(dateMonth.value).padStart(2, '0')
  const d = String(dateDay.value).padStart(2, '0')
  return `${dateYear.value}-${m}-${d}`
})

const startTime = computed(() => {
  return `${String(startHour.value).padStart(2, '0')}:${String(startMinute.value).padStart(2, '0')}`
})

const endTime = computed(() => {
  return `${String(endHour.value).padStart(2, '0')}:${String(endMinute.value).padStart(2, '0')}`
})

const calculatedHours = computed(() => {
  const startMinutes = startHour.value * 60 + startMinute.value
  const endMinutes = endHour.value * 60 + endMinute.value
  const diff = endMinutes - startMinutes
  if (diff <= 0) return 0
  return Math.round((diff / 60) * 10) / 10
})

watch(calculatedHours, (val) => {
  if (val > 0) form.value.hours = val
}, { immediate: true })

watch([dateYear, dateMonth], () => {
  const max = daysInMonth(dateYear.value, dateMonth.value)
  if (dateDay.value > max) dateDay.value = max
})

function validate(): boolean {
  if (!form.value.type) {
    message.value = { type: 'error', text: '請選擇加班類型' }
    return false
  }
  if (form.value.hours <= 0) {
    message.value = { type: 'error', text: '結束時間必須晚於開始時間' }
    return false
  }
  if (!form.value.reason.trim()) {
    message.value = { type: 'error', text: '請填寫加班事由' }
    return false
  }
  return true
}

async function handleSubmit() {
  message.value = null
  if (!validate()) return

  submitting.value = true
  try {
    await store.doApplyOvertime({
      type: form.value.type,
      overtimeDate: overtimeDate.value,
      startTime: startTime.value,
      endTime: endTime.value,
      hours: form.value.hours,
      reason: form.value.reason
    })
    message.value = { type: 'success', text: '加班申請已送出' }
    setTimeout(() => router.push({ name: 'overtime-my' }), 1000)
  } catch (err: any) {
    message.value = { type: 'error', text: err.response?.data?.message || '申請失敗，請稍後再試' }
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="mx-auto max-w-2xl px-4 py-8">
    <h1 class="mb-6 text-2xl font-bold text-gray-800">加班申請</h1>

    <div v-if="message" :class="['mb-6 rounded-lg px-4 py-3 text-sm font-medium', message.type === 'success' ? 'bg-green-50 text-green-700' : 'bg-red-50 text-red-700']">
      {{ message.text }}
    </div>

    <form class="space-y-6 rounded-lg border border-gray-200 bg-white p-6 shadow-sm" @submit.prevent="handleSubmit">
      <!-- Overtime Type -->
      <div>
        <label class="mb-1 block text-sm font-medium text-gray-700">加班類型</label>
        <select v-model="form.type" class="w-full rounded-md border border-gray-300 px-3 py-2 text-sm">
          <option value="PRE_APPLY">事前申請</option>
          <option value="POST_REPORT">事後補報</option>
        </select>
      </div>

      <!-- Overtime Date -->
      <div>
        <label class="mb-1 block text-sm font-medium text-gray-700">加班日期</label>
        <div class="flex gap-2">
          <select v-model="dateYear" class="rounded-md border border-gray-300 px-3 py-2 text-sm">
            <option v-for="y in years" :key="y" :value="y">{{ y }} 年</option>
          </select>
          <select v-model="dateMonth" class="rounded-md border border-gray-300 px-3 py-2 text-sm">
            <option v-for="m in months" :key="m" :value="m">{{ m }} 月</option>
          </select>
          <select v-model="dateDay" class="rounded-md border border-gray-300 px-3 py-2 text-sm">
            <option v-for="d in days" :key="d" :value="d">{{ d }} 日</option>
          </select>
        </div>
      </div>

      <!-- Time Range -->
      <div class="grid grid-cols-2 gap-4">
        <div>
          <label class="mb-1 block text-sm font-medium text-gray-700">開始時間</label>
          <div class="flex gap-2">
            <select v-model="startHour" class="rounded-md border border-gray-300 px-3 py-2 text-sm">
              <option v-for="h in hours" :key="h" :value="h">{{ String(h).padStart(2, '0') }}</option>
            </select>
            <span class="self-center text-gray-500">:</span>
            <select v-model="startMinute" class="rounded-md border border-gray-300 px-3 py-2 text-sm">
              <option v-for="m in minutes" :key="m" :value="m">{{ String(m).padStart(2, '0') }}</option>
            </select>
          </div>
        </div>
        <div>
          <label class="mb-1 block text-sm font-medium text-gray-700">結束時間</label>
          <div class="flex gap-2">
            <select v-model="endHour" class="rounded-md border border-gray-300 px-3 py-2 text-sm">
              <option v-for="h in hours" :key="h" :value="h">{{ String(h).padStart(2, '0') }}</option>
            </select>
            <span class="self-center text-gray-500">:</span>
            <select v-model="endMinute" class="rounded-md border border-gray-300 px-3 py-2 text-sm">
              <option v-for="m in minutes" :key="m" :value="m">{{ String(m).padStart(2, '0') }}</option>
            </select>
          </div>
        </div>
      </div>

      <!-- Hours -->
      <div>
        <label class="mb-1 block text-sm font-medium text-gray-700">加班時數</label>
        <input v-model.number="form.hours" type="number" min="0" step="0.5"
          class="w-full rounded-md border border-gray-300 px-3 py-2 text-sm" />
        <div class="mt-1 text-xs text-gray-500">
          自動計算：{{ calculatedHours > 0 ? `${calculatedHours} 小時` : '結束時間需晚於開始時間' }}
        </div>
      </div>

      <!-- Reason -->
      <div>
        <label class="mb-1 block text-sm font-medium text-gray-700">加班事由</label>
        <textarea v-model="form.reason" rows="3"
          class="w-full rounded-md border border-gray-300 px-3 py-2 text-sm" placeholder="請輸入加班事由" />
      </div>

      <!-- Submit -->
      <div class="flex justify-end gap-3">
        <button type="button" class="rounded-md border border-gray-300 px-4 py-2 text-sm text-gray-600 hover:bg-gray-50" @click="router.back()">
          取消
        </button>
        <button type="submit" :disabled="submitting"
          class="inline-flex items-center rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-60">
          <svg v-if="submitting" class="-ml-1 mr-2 h-4 w-4 animate-spin" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z" />
          </svg>
          {{ submitting ? '送出中...' : '送出申請' }}
        </button>
      </div>
    </form>
  </div>
</template>
