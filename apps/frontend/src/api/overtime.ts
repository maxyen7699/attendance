import client from './client'

export interface OvertimeApplication {
  type: 'PRE_APPLY' | 'POST_REPORT'
  overtimeDate: string
  startTime: string
  endTime: string
  hours: number
  reason: string
}

export interface OvertimeRecord {
  id: number
  userId: number
  userName: string
  type: 'PRE_APPLY' | 'POST_REPORT'
  overtimeDate: string
  startTime: string
  endTime: string
  hours: number
  reason: string
  status: 'PENDING' | 'APPROVED' | 'REJECTED'
  approverName: string | null
  createdAt: string
}

export function applyOvertime(data: OvertimeApplication) {
  return client.post('/overtime', data)
}

export function getMyOvertime() {
  return client.get('/overtime/my')
}

export function getPendingOvertime() {
  return client.get('/overtime/pending')
}

export function approveOvertime(id: number) {
  return client.put(`/overtime/${id}/approve`)
}

export function rejectOvertime(id: number) {
  return client.put(`/overtime/${id}/reject`)
}

export function getOvertimeStats(startDate?: string, endDate?: string) {
  const params: Record<string, string> = {}
  if (startDate) params.startDate = startDate
  if (endDate) params.endDate = endDate
  return client.get('/overtime/stats', { params })
}
