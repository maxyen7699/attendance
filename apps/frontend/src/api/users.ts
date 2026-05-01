import client from './client'

export interface CreateUserPayload {
  username: string
  email: string
  name: string
  department: string
  position: string
  role: string
  annualLeaveDays: number
  password?: string
}

export interface UpdateUserPayload {
  email?: string
  name?: string
  department?: string
  position?: string
  role?: string
  annualLeaveDays?: number
  active?: boolean
}

export function getUsers() {
  return client.get('/users')
}

export function createUser(data: CreateUserPayload) {
  return client.post('/users', data)
}

export function updateUser(id: number, data: UpdateUserPayload) {
  return client.put(`/users/${id}`, data)
}

export function deleteUser(id: number) {
  return client.delete(`/users/${id}`)
}

export function getMe() {
  return client.get('/users/me')
}
