<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useLeaveStore } from '@/stores/leave'

const router = useRouter()
const store = useLeaveStore()

const form = ref({
  leaveTypeId: 0,
  startDate: '',
  endDate: '',
  totalDays: 0,
  reason: '',
  agentId: undefined as number | undefined
})

const submitting = ref(false)
const message = ref<{ type: 'success' | 'error'; text: string } | null>(null)

const selectedBalance = computed(() => {
  if (!form.value.leaveTypeId) return null
  return store.leaveBalances.find(b => b.leaveTypeId === form.value.leaveTypeId) ?? null
})

const totalDays = computed(() => {
  if (!form.value.startDate || !form.value.endDate) return 0
  const start = new Date(form.value.startDate)
  const end = new Date(form.value.endDate)
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

function onDateChange() {
  form.value.totalDays = totalDays.value
}

function validate(): boolean {
  if (!form.value.leaveTypeId) {
    message.value = { type: 'error', text: '請選擇假別' }
    return false
  }
  if (!form.value.startDate) {
    message.value = { type: 'error', text: '請選擇起始日期' }
    return false
  }
  if (!form.value.endDate) {
    message.value = { type: 'error', text: '請選擇結束日期' }
    return false
  }
  if (form.value.totalDays <= 0) {
    message.value = { type: 'error', text: '請假天數需大於 0' }
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
      startDate: form.value.startDate,
      endDate: form.value.endDate,
      totalDays: form.value.totalDays,
      reason: form.value.reason,
      agentId: form.value.agentId
    })
    message.value = { type: 'success', text: '請假申請已送出' }
    setTimeout(() => {
      router.push({ name: 'leave-my' })
    }, 1000)
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

    <form class="space-y-6 rounded-lg border border-gray-200 bg-white p-6 shadow-sm" @submit.prevent="handleSubmit">
      <!-- Leave Type -->
      <div>
        <label class="mb-1 block text-sm font-medium text-gray-700">假別</label>
        <select
          v-model="form.leaveTypeId"
          class="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
        >
          <option :value="0" disabled>請選擇假別</option>
          <option
            v-for="balance in store.leaveBalances"
            :key="balance.leaveTypeId"
            :value="balance.leaveTypeId"
          >
            {{ balance.leaveTypeName }}（剩餘 {{ balance.remainingDays }} 天）
          </option>
        </select>
        <div v-if="selectedBalance" class="mt-1 text-xs text-gray-500">
          總額度 {{ selectedBalance.totalDays }} 天 / 已用 {{ selectedBalance.usedDays }} 天 / 剩餘 {{ selectedBalance.remainingDays }} 天
        </div>
      </div>

      <!-- Date Range -->
      <div class="grid grid-cols-2 gap-4">
        <div>
          <label class="mb-1 block text-sm font-medium text-gray-700">起始日期</label>
          <input
            v-model="form.startDate"
            type="date"
            class="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
            @change="onDateChange"
          />
        </div>
        <div>
          <label class="mb-1 block text-sm font-medium text-gray-700">結束日期</label>
          <input
            v-model="form.endDate"
            type="date"
            class="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
            @change="onDateChange"
          />
        </div>
      </div>

      <!-- Calculated Days -->
      <div>
        <label class="mb-1 block text-sm font-medium text-gray-700">請假天數（工作日）</label>
        <div class="rounded-md border border-gray-200 bg-gray-50 px-3 py-2 text-sm text-gray-800">
          {{ form.totalDays > 0 ? `${form.totalDays} 天` : '請選擇日期範圍' }}
        </div>
      </div>

      <!-- Reason -->
      <div>
        <label class="mb-1 block text-sm font-medium text-gray-700">事由</label>
        <textarea
          v-model="form.reason"
          rows="3"
          class="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
          placeholder="請輸入請假事由"
        ></textarea>
      </div>

      <!-- Submit -->
      <div class="flex justify-end gap-3">
        <button
          type="button"
          class="rounded-md border border-gray-300 px-4 py-2 text-sm text-gray-600 hover:bg-gray-50"
          @click="router.back()"
        >
          取消
        </button>
        <button
          type="submit"
          :disabled="submitting"
          class="inline-flex items-center rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-60"
        >
          <svg
            v-if="submitting"
            class="-ml-1 mr-2 h-4 w-4 animate-spin"
            xmlns="http://www.w3.org/2000/svg"
            fill="none"
            viewBox="0 0 24 24"
          >
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z" />
          </svg>
          {{ submitting ? '送出中...' : '送出申請' }}
        </button>
      </div>
    </form>
  </div>
</template>
