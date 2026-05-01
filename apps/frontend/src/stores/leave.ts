import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as leaveApi from '@/api/leaves'
import type { LeaveRecord, LeaveBalance, ProxyTask, LeaveApplication } from '@/api/leaves'

export const useLeaveStore = defineStore('leave', () => {
  const myLeaves = ref<LeaveRecord[]>([])
  const pendingApprovals = ref<LeaveRecord[]>([])
  const leaveBalances = ref<LeaveBalance[]>([])
  const proxyTasks = ref<ProxyTask[]>([])
  const loading = ref(false)

  async function fetchMyLeaves() {
    loading.value = true
    try {
      const res = await leaveApi.getMyLeaves()
      myLeaves.value = res.data.data
    } finally {
      loading.value = false
    }
  }

  async function fetchPendingApprovals() {
    loading.value = true
    try {
      const res = await leaveApi.getPendingApprovals()
      pendingApprovals.value = res.data.data
    } finally {
      loading.value = false
    }
  }

  async function fetchLeaveBalances(year?: number) {
    loading.value = true
    try {
      const res = await leaveApi.getLeaveBalances(year)
      leaveBalances.value = res.data.data
    } finally {
      loading.value = false
    }
  }

  async function fetchProxyTasks() {
    loading.value = true
    try {
      const res = await leaveApi.getProxyTasks()
      proxyTasks.value = res.data.data
    } finally {
      loading.value = false
    }
  }

  async function doApplyLeave(data: LeaveApplication) {
    const res = await leaveApi.applyLeave(data)
    return res.data
  }

  async function doApprove(id: number) {
    const res = await leaveApi.approveLeave(id)
    pendingApprovals.value = pendingApprovals.value.filter(item => item.id !== id)
    return res.data
  }

  async function doReject(id: number) {
    const res = await leaveApi.rejectLeave(id)
    pendingApprovals.value = pendingApprovals.value.filter(item => item.id !== id)
    return res.data
  }

  async function doCancel(id: number) {
    const res = await leaveApi.cancelLeave(id)
    const record = myLeaves.value.find(item => item.id === id)
    if (record) {
      record.status = 'CANCELLED'
    }
    return res.data
  }

  async function doProxyApprove(leaveId: number) {
    const res = await leaveApi.approveLeave(leaveId)
    proxyTasks.value = proxyTasks.value.filter(item => item.leaveId !== leaveId)
    return res.data
  }

  async function doProxyReject(leaveId: number) {
    const res = await leaveApi.rejectLeave(leaveId)
    proxyTasks.value = proxyTasks.value.filter(item => item.leaveId !== leaveId)
    return res.data
  }

  return {
    myLeaves,
    pendingApprovals,
    leaveBalances,
    proxyTasks,
    loading,
    fetchMyLeaves,
    fetchPendingApprovals,
    fetchLeaveBalances,
    fetchProxyTasks,
    doApplyLeave,
    doApprove,
    doReject,
    doCancel,
    doProxyApprove,
    doProxyReject
  }
})
