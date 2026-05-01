<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useLeaveStore } from '@/stores/leave'

const router = useRouter()
const store = useLeaveStore()

const now = new Date()

// Start date selects
const startYear = ref(now.getFullYear())
const startMonth = ref(now.getMonth() + 1)
const startDay = ref(now.getDate())

// End date selects
const endYear = ref(now.getFullYear())
const endMonth = ref(now.getMonth() + 1)
const endDay = ref(now.getDate())

const form = ref({
  leaveTypeId: 0,
  totalDays: 0,
  reason: '',
  agentId: undefined as number | undefined
})

const submitting = ref(false)
const message = ref<{ type: 'success' | 'error'; text: string } | null>(null)

const years = Array.from({ length: 3 }, (_, i) => now.getFullYear() - 1 + i)
const months = Array.from({ length: 12 }, (_, i) => i + 1)

function daysInMonth(y: number, m: number): number {
  return new Date(y, m, 0).getDate()
}

const startDays = computed(() => Array.from({ length: daysInMonth(startYear.value, startMonth.value) }, (_, i) => i + 1))
const endDays = computed(() => Array.from({ length: daysInMonth(endYear.value, endMonth.value) }, (_, i) => i + 1))

const startDate = computed(() => {
  return `${startYear.value}-${String(startMonth.value).padStart(2, '0')}-${String(startDay.value).padStart(2, '0')}`
})

const endDate = computed(() => {
  return `${endYear.value}-${String(endMonth.value).padStart(2, '0')}-${String(endDay.value).padStart(2, '0')}`
})

watch([startYear, startMonth], () => {
  const max = daysInMonth(startYear.value, startMonth.value)
  if (startDay.value > max) startDay.value = max
})

watch([endYear, endMonth], () => {
  const max = daysInMonth(endYear.value, endMonth.value)
  if (endDay.value > max) endDay.value = max
})

const selectedBalance = computed(() => {
  if (!form.value.leaveTypeId) return null
  return store.leaveBalances.find(b => b.leaveTypeId === form.value.leaveTypeId) ?? null
})

const totalDays = computed(() => {
  const start = new Date(startDate.value)
  const end = new Date(endDate.value)
  if (end < start) return 0
  let count = 0
  const current = new Date(start)
  while (current <= end) {
    const day = current.getDay()
    if (day !== 0 && day !== 6) count++
    current.setDate(current.getDate() + 1)
  }
  return count
})

watch(totalDays, (val) => {
  form.value.totalDays = val
}, { immediate: true })

function validate(): boolean {
  if (!form.value.leaveTypeId) {
    message.value = { type: 'error', text: '請選擇假別' }
    return false
  }
  if (form.value.totalDays <= 0) {
    message.value = { type: 'error', text: '結束日期必須大於等於起始日期' }
    return false
  }
  if (selectedBalance.value && form.value.totalDays > selectedBalance.value.remainingDays) {
    message.value = { type: 'error', text: `剩餘額度不足（剩餘 ${selectedBalance.value.remainingDays} 天）` }
    return false
  }
  if (!form.value.reason.trim()) {
    message.value = { type: 'error', text: '請填寫事由' }
    return false
  }
  return true
}

async function handleSubmit() {
  message.value = null
  if (!validate()) return

  submitting.value = true
  try {
    await store.doApplyLeave({
      leaveTypeId: form.value.leaveTypeId,
      startDate: startDate.value,
      endDate: endDate.value,
      totalDays: form.value.totalDays,
      reason: form.value.reason,
      agentId: form.value.agentId
    })
    message.value = { type: 'success', text: '請假申請已送出' }
    setTimeout(() => router.push({ name: 'leave-my' }), 1000)
  } catch (err: any) {
    message.value = { type: 'error', text: err.response?.data?.message || '申請失敗，請稍後再試' }
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  store.fetchLeaveBalances()
})
</script>

<template>
  <div class="mx-auto max-w-2xl px-4 py-8">
    <h1 class="mb-6 text-2xl font-bold text-gray-800">請假申請</h1>

    <div v-if="message" :class="['mb-6 rounded-lg px-4 py-3 text-sm font-medium', message.type === 'success' ? 'bg-green-50 text-green-700' : 'bg-red-50 text-red-700']">
      {{ message.text }}
    </div>

    <form class="space-y-6 rounded-lg border border-gray-200 bg-white p-6 shadow-sm" @submit.prevent="handleSubmit">
      <!-- Leave Type -->
      <div>
        <label class="mb-1 block text-sm font-medium text-gray-700">假別</label>
        <select v-model="form.leaveTypeId" class="w-full rounded-md border border-gray-300 px-3 py-2 text-sm">
          <option :value="0" disabled>請選擇假別</option>
          <option v-for="balance in store.leaveBalances" :key="balance.leaveTypeId" :value="balance.leaveTypeId">
            {{ balance.leaveTypeName }}（剩餘 {{ balance.remainingDays }} 天）
          </option>
        </select>
        <div v-if="selectedBalance" class="mt-1 text-xs text-gray-500">
          總額度 {{ selectedBalance.totalDays }} 天 / 已用 {{ selectedBalance.usedDays }} 天 / 剩餘 {{ selectedBalance.remainingDays }} 天
        </div>
      </div>

      <!-- Start Date -->
      <div>
        <label class="mb-1 block text-sm font-medium text-gray-700">起始日期</label>
        <div class="flex gap-2">
          <select v-model="startYear" class="rounded-md border border-gray-300 px-3 py-2 text-sm">
            <option v-for="y in years" :key="y" :value="y">{{ y }} 年</option>
          </select>
          <select v-model="startMonth" class="rounded-md border border-gray-300 px-3 py-2 text-sm">
            <option v-for="m in months" :key="m" :value="m">{{ m }} 月</option>
          </select>
          <select v-model="startDay" class="rounded-md border border-gray-300 px-3 py-2 text-sm">
            <option v-for="d in startDays" :key="d" :value="d">{{ d }} 日</option>
          </select>
        </div>
      </div>

      <!-- End Date -->
      <div>
        <label class="mb-1 block text-sm font-medium text-gray-700">結束日期</label>
        <div class="flex gap-2">
          <select v-model="endYear" class="rounded-md border border-gray-300 px-3 py-2 text-sm">
            <option v-for="y in years" :key="y" :value="y">{{ y }} 年</option>
          </select>
          <select v-model="endMonth" class="rounded-md border border-gray-300 px-3 py-2 text-sm">
            <option v-for="m in months" :key="m" :value="m">{{ m }} 月</option>
          </select>
          <select v-model="endDay" class="rounded-md border border-gray-300 px-3 py-2 text-sm">
            <option v-for="d in endDays" :key="d" :value="d">{{ d }} 日</option>
          </select>
        </div>
      </div>

      <!-- Calculated Days -->
      <div>
        <label class="mb-1 block text-sm font-medium text-gray-700">請假天數（工作日）</label>
        <div class="rounded-md border border-gray-200 bg-gray-50 px-3 py-2 text-sm text-gray-800">
          {{ form.totalDays > 0 ? `${form.totalDays} 天` : '請確認日期範圍' }}
        </div>
      </div>

      <!-- Reason -->
      <div>
        <label class="mb-1 block text-sm font-medium text-gray-700">事由</label>
        <textarea v-model="form.reason" rows="3" class="w-full rounded-md border border-gray-300 px-3 py-2 text-sm" placeholder="請輸入請假事由" />
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
