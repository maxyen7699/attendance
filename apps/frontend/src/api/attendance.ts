import client from './client'

export function clockIn() {
  return client.post('/attendance/clock-in')
}

export function clockOut() {
  return client.post('/attendance/clock-out')
}

export function getTodayStatus() {
  return client.get('/attendance/today')
}

export function getMyRecords() {
  return client.get('/attendance/my')
}

export function getAllRecords(startDate?: string, endDate?: string) {
  const params: Record<string, string> = {}
  if (startDate) params.startDate = startDate
  if (endDate) params.endDate = endDate
  return client.get('/attendance/all', { params })
}
