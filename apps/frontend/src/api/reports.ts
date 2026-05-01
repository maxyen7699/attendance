import client from './client'

export function getMonthlyReport(year?: number, month?: number) {
  const params: Record<string, number> = {}
  if (year) params.year = year
  if (month) params.month = month
  return client.get('/reports/monthly', { params })
}

export function getDepartmentStats(year?: number, month?: number) {
  const params: Record<string, number> = {}
  if (year) params.year = year
  if (month) params.month = month
  return client.get('/reports/department', { params })
}

export function getLeaveStats(year?: number, month?: number) {
  const params: Record<string, number> = {}
  if (year) params.year = year
  if (month) params.month = month
  return client.get('/reports/leave-stats', { params })
}

export function getOvertimeStats(year?: number, month?: number) {
  const params: Record<string, number> = {}
  if (year) params.year = year
  if (month) params.month = month
  return client.get('/reports/overtime-stats', { params })
}

export function getDashboard() {
  return client.get('/dashboard')
}
