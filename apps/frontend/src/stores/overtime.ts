import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as overtimeApi from '@/api/overtime'
import type { OvertimeRecord, OvertimeApplication } from '@/api/overtime'

export const useOvertimeStore = defineStore('overtime', () => {
  const myOvertime = ref<OvertimeRecord[]>([])
  const pendingOvertime = ref<OvertimeRecord[]>([])
  const loading = ref(false)

  async function fetchMyOvertime() {
    loading.value = true
    try {
      const res = await overtimeApi.getMyOvertime()
      myOvertime.value = res.data.data
    } finally {
      loading.value = false
    }
  }

  async function fetchPendingOvertime() {
    loading.value = true
    try {
      const res = await overtimeApi.getPendingOvertime()
      pendingOvertime.value = res.data.data
    } finally {
      loading.value = false
    }
  }

  async function doApplyOvertime(data: OvertimeApplication) {
    const res = await overtimeApi.applyOvertime(data)
    return res.data
  }

  async function doApprove(id: number) {
    const res = await overtimeApi.approveOvertime(id)
    pendingOvertime.value = pendingOvertime.value.filter(item => item.id !== id)
    return res.data
  }

  async function doReject(id: number) {
    const res = await overtimeApi.rejectOvertime(id)
    pendingOvertime.value = pendingOvertime.value.filter(item => item.id !== id)
    return res.data
  }

  return {
    myOvertime,
    pendingOvertime,
    loading,
    fetchMyOvertime,
    fetchPendingOvertime,
    doApplyOvertime,
    doApprove,
    doReject
  }
})
