import client from './client'

export interface LeaveApplication {
  leaveTypeId: number
  startDate: string
  endDate: string
  totalDays: number
  reason: string
  agentId?: number
}

export interface LeaveRecord {
  id: number
  userId: number
  userName: string
  leaveTypeId: number
  leaveTypeName: string
  startDate: string
  endDate: string
  totalDays: number
  reason: string
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED'
  approverName: string | null
  agentName: string | null
  createdAt: string
}

export interface LeaveBalance {
  leaveTypeId: number
  leaveTypeName: string
  totalDays: number
  usedDays: number
  remainingDays: number
}

export interface ProxyTask {
  id: number
  originalApproverId: number
  originalApproverName: string
  leaveId: number
  leaveTypeName: string
  applicantName: string
  startDate: string
  endDate: string
  totalDays: number
  reason: string
}

export function applyLeave(data: LeaveApplication) {
  return client.post('/leaves', data)
}

export function getMyLeaves() {
  return client.get('/leaves/my')
}

export function getPendingApprovals() {
  return client.get('/leaves/pending')
}

export function approveLeave(id: number) {
  return client.put(`/leaves/${id}/approve`)
}

export function rejectLeave(id: number) {
  return client.put(`/leaves/${id}/reject`)
}

export function cancelLeave(id: number) {
  return client.put(`/leaves/${id}/cancel`)
}

export function getLeaveBalances(year?: number) {
  const params: Record<string, number> = {}
  if (year) params.year = year
  return client.get('/leaves/balance', { params })
}

export function getProxyTasks() {
  return client.get('/leaves/proxy')
}
