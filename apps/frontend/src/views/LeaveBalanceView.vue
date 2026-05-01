<script setup lang="ts">
import { onMounted } from 'vue'
import { useLeaveStore } from '@/stores/leave'

const store = useLeaveStore()

function balancePercentage(balance: { totalDays: number; remainingDays: number }): number {
  if (balance.totalDays === 0) return 0
  return Math.round((balance.remainingDays / balance.totalDays) * 100)
}

function barColor(percentage: number): string {
  if (percentage > 50) return 'bg-green-500'
  if (percentage > 20) return 'bg-yellow-500'
  return 'bg-red-500'
}

onMounted(() => {
  store.fetchLeaveBalances()
})
</script>

<template>
  <div class="mx-auto max-w-3xl px-4 py-8">
    <div class="mb-6 flex items-center justify-between">
      <h1 class="text-2xl font-bold text-gray-800">請假餘額總覽</h1>
    </div>

    <!-- Loading -->
    <div v-if="store.loading" class="flex justify-center py-12">
      <svg class="h-8 w-8 animate-spin text-blue-500" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z" />
      </svg>
    </div>

    <!-- Empty State -->
    <div
      v-else-if="store.leaveBalances.length === 0"
      class="rounded-lg border border-gray-200 bg-white py-12 text-center text-gray-500"
    >
      尚無請假額度資料
    </div>

    <!-- Balance Cards -->
    <div v-else class="space-y-4">
      <div
        v-for="balance in store.leaveBalances"
        :key="balance.leaveTypeId"
        class="rounded-lg border border-gray-200 bg-white p-5 shadow-sm"
      >
        <div class="mb-3 flex items-center justify-between">
          <h3 class="text-base font-semibold text-gray-800">{{ balance.leaveTypeName }}</h3>
          <span class="text-sm text-gray-500">
            剩餘 <span class="font-bold text-gray-800">{{ balance.remainingDays }}</span> / {{ balance.totalDays }} 天
          </span>
        </div>

        <!-- Progress bar -->
        <div class="mb-2 h-3 w-full overflow-hidden rounded-full bg-gray-200">
          <div
            :class="['h-full rounded-full transition-all duration-300', barColor(balancePercentage(balance))]"
            :style="{ width: `${balancePercentage(balance)}%` }"
          ></div>
        </div>

        <div class="flex justify-between text-xs text-gray-500">
          <span>已使用 {{ balance.usedDays }} 天</span>
          <span>{{ balancePercentage(balance) }}%</span>
        </div>
      </div>
    </div>
  </div>
</template>
