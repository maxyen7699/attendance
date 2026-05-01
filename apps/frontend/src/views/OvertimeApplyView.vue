<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useOvertimeStore } from '@/stores/overtime'

const router = useRouter()
const store = useOvertimeStore()

const form = ref({
  type: 'PRE_APPLY' as 'PRE_APPLY' | 'POST_REPORT',
  overtimeDate: '',
  startTime: '',
  endTime: '',
  hours: 0,
  reason: ''
})

const submitting = ref(false)
const message = ref<{ type: 'success' | 'error'; text: string } | null>(null)

const calculatedHours = computed(() => {
  if (!form.value.startTime || !form.value.endTime) return 0
  const [startH = 0, startM = 0] = form.value.startTime.split(':').map(Number)
  const [endH = 0, endM = 0] = form.value.endTime.split(':').map(Number)
  const startMinutes = startH * 60 + startM
  const endMinutes = endH * 60 + endM
  const diff = endMinutes - startMinutes
  if (diff <= 0) return 0
  return Math.round((diff / 60) * 10) / 10
})

watch([() => form.value.startTime, () => form.value.endTime], () => {
  if (calculatedHours.value > 0) {
    form.value.hours = calculatedHours.value
  }
})

function validate(): boolean {
  if (!form.value.type) {
    message.value = { type: 'error', text: '請選擇加班類型' }
    return false
  }
  if (!form.value.overtimeDate) {
    message.value = { type: 'error', text: '請選擇加班日期' }
    return false
  }
  if (!form.value.startTime) {
    message.value = { type: 'error', text: '請選擇開始時間' }
    return false
  }
  if (!form.value.endTime) {
    message.value = { type: 'error', text: '請選擇結束時間' }
    return false
  }
  if (form.value.hours <= 0) {
    message.value = { type: 'error', text: '加班時數需大於 0' }
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
      overtimeDate: form.value.overtimeDate,
      startTime: form.value.startTime,
      endTime: form.value.endTime,
      hours: form.value.hours,
      reason: form.value.reason
    })
    message.value = { type: 'success', text: '加班申請已送出' }
    setTimeout(() => {
      router.push({ name: 'overtime-my' })
    }, 1000)
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
      <!-- Overtime Type -->
      <div>
        <label class="mb-1 block text-sm font-medium text-gray-700">加班類型</label>
        <select
          v-model="form.type"
          class="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
        >
          <option value="PRE_APPLY">事前申請</option>
          <option value="POST_REPORT">事後補報</option>
        </select>
      </div>

      <!-- Overtime Date -->
      <div>
        <label class="mb-1 block text-sm font-medium text-gray-700">加班日期</label>
        <input
          v-model="form.overtimeDate"
          type="date"
          class="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
        />
      </div>

      <!-- Time Range -->
      <div class="grid grid-cols-2 gap-4">
        <div>
          <label class="mb-1 block text-sm font-medium text-gray-700">開始時間</label>
          <input
            v-model="form.startTime"
            type="time"
            class="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
          />
        </div>
        <div>
          <label class="mb-1 block text-sm font-medium text-gray-700">結束時間</label>
          <input
            v-model="form.endTime"
            type="time"
            class="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
          />
        </div>
      </div>

      <!-- Hours -->
      <div>
        <label class="mb-1 block text-sm font-medium text-gray-700">加班時數</label>
        <input
          v-model.number="form.hours"
          type="number"
          min="0"
          step="0.5"
          class="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
          placeholder="自動計算或手動輸入"
        />
        <div class="mt-1 text-xs text-gray-500">
          自動計算：{{ calculatedHours > 0 ? `${calculatedHours} 小時` : '請選擇開始與結束時間' }}
        </div>
      </div>

      <!-- Reason -->
      <div>
        <label class="mb-1 block text-sm font-medium text-gray-700">加班事由</label>
        <textarea
          v-model="form.reason"
          rows="3"
          class="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
          placeholder="請輸入加班事由"
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
