import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  clockIn as apiClockIn,
  clockOut as apiClockOut,
  getTodayStatus as apiGetTodayStatus,
  getMyRecords as apiGetMyRecords
} from '@/api/attendance'

export interface TodayStatus {
  hasClockedIn: boolean
  hasClockedOut: boolean
  clockIn: string | null
  clockOut: string | null
  status: string | null
}

export interface AttendanceRecord {
  id: number
  date: string
  clockIn: string | null
  clockOut: string | null
  status: string
  remark: string | null
}

export const useAttendanceStore = defineStore('attendance', () => {
  const todayStatus = ref<TodayStatus | null>(null)
  const myRecords = ref<AttendanceRecord[]>([])
  const loading = ref(false)

  async function fetchTodayStatus() {
    loading.value = true
    try {
      const res = await apiGetTodayStatus()
      todayStatus.value = res.data.data
    } finally {
      loading.value = false
    }
  }

  async function doClockIn() {
    const res = await apiClockIn()
    todayStatus.value = {
      hasClockedIn: true,
      hasClockedOut: false,
      clockIn: res.data.data.clockIn,
      clockOut: null,
      status: res.data.data.status
    }
    return res.data
  }

  async function doClockOut() {
    const res = await apiClockOut()
    todayStatus.value = {
      ...todayStatus.value!,
      hasClockedOut: true,
      clockOut: res.data.data.clockOut,
      status: res.data.data.status
    }
    return res.data
  }

  async function fetchMyRecords() {
    const res = await apiGetMyRecords()
    myRecords.value = res.data.data
  }

  return { todayStatus, myRecords, loading, fetchTodayStatus, doClockIn, doClockOut, fetchMyRecords }
})
