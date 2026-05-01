<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getUsers, createUser, updateUser, type CreateUserPayload, type UpdateUserPayload } from '@/api/users'

interface UserRow {
  id: number
  username: string
  email: string
  name: string
  department: string
  position: string
  role: string
  active: boolean
  annualLeaveDays: number
}

const users = ref<UserRow[]>([])
const loading = ref(false)
const showModal = ref(false)
const editingUser = ref<UserRow | null>(null)
const saving = ref(false)
const confirmDisable = ref<UserRow | null>(null)
const createdPassword = ref<string | null>(null)

const form = ref<CreateUserPayload & { password: string }>({
  username: '',
  email: '',
  name: '',
  department: '',
  position: '',
  role: 'USER',
  annualLeaveDays: 14,
  password: ''
})

async function fetchUsers() {
  loading.value = true
  try {
    const res = await getUsers()
    users.value = res.data.data
  } catch {
    // handled silently
  } finally {
    loading.value = false
  }
}

function openCreateModal() {
  editingUser.value = null
  createdPassword.value = null
  form.value = {
    username: '',
    email: '',
    name: '',
    department: '',
    position: '',
    role: 'USER',
    annualLeaveDays: 14,
    password: ''
  }
  showModal.value = true
}

function openEditModal(user: UserRow) {
  editingUser.value = user
  form.value = {
    username: user.username,
    email: user.email,
    name: user.name,
    department: user.department,
    position: user.position,
    role: user.role,
    annualLeaveDays: user.annualLeaveDays,
    password: ''
  }
  showModal.value = true
}

function closeModal() {
  showModal.value = false
  editingUser.value = null
}

async function handleSave() {
  saving.value = true
  try {
    if (editingUser.value) {
      const payload: UpdateUserPayload = {
        email: form.value.email,
        name: form.value.name,
        department: form.value.department,
        position: form.value.position,
        role: form.value.role,
        annualLeaveDays: form.value.annualLeaveDays
      }
      await updateUser(editingUser.value.id, payload)
    } else {
      const res = await createUser(form.value)
      createdPassword.value = res.data.data?.initialPassword || null
    }
    closeModal()
    await fetchUsers()
  } catch {
    // handled silently
  } finally {
    saving.value = false
  }
}

async function handleDisable(user: UserRow) {
  try {
    await updateUser(user.id, { active: !user.active })
    confirmDisable.value = null
    await fetchUsers()
  } catch {
    // handled silently
  }
}

onMounted(fetchUsers)
</script>

<template>
  <div class="mx-auto max-w-6xl p-6">
    <div class="mb-6 flex items-center justify-between">
      <h1 class="text-2xl font-bold text-gray-800">使用者管理</h1>
      <button
        class="rounded-md bg-blue-600 px-4 py-2 text-white hover:bg-blue-700"
        @click="openCreateModal"
      >
        新增使用者
      </button>
    </div>

    <div class="overflow-x-auto rounded-lg bg-white shadow-md">
      <table class="w-full text-left text-sm">
        <thead class="bg-gray-50 text-xs uppercase text-gray-500">
          <tr>
            <th class="px-6 py-3">姓名</th>
            <th class="px-6 py-3">帳號</th>
            <th class="px-6 py-3">Email</th>
            <th class="px-6 py-3">部門</th>
            <th class="px-6 py-3">角色</th>
            <th class="px-6 py-3">狀態</th>
            <th class="px-6 py-3">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading">
            <td colspan="7" class="px-6 py-8 text-center text-gray-500">載入中...</td>
          </tr>
          <tr v-else-if="users.length === 0">
            <td colspan="7" class="px-6 py-8 text-center text-gray-500">尚無使用者</td>
          </tr>
          <tr
            v-for="u in users"
            :key="u.id"
            class="border-b border-gray-200 hover:bg-gray-50"
          >
            <td class="px-6 py-4 font-medium text-gray-800">{{ u.name }}</td>
            <td class="px-6 py-4 text-gray-600">{{ u.username }}</td>
            <td class="px-6 py-4 text-gray-600">{{ u.email }}</td>
            <td class="px-6 py-4 text-gray-600">{{ u.department }}</td>
            <td class="px-6 py-4">
              <span
                class="rounded-full px-2 py-0.5 text-xs font-semibold"
                :class="
                  u.role === 'ADMIN'
                    ? 'bg-purple-100 text-purple-800'
                    : 'bg-blue-100 text-blue-800'
                "
              >
                {{ u.role === 'ADMIN' ? '管理員' : '使用者' }}
              </span>
            </td>
            <td class="px-6 py-4">
              <span
                class="rounded-full px-2 py-0.5 text-xs font-semibold"
                :class="
                  u.active
                    ? 'bg-green-100 text-green-800'
                    : 'bg-red-100 text-red-800'
                "
              >
                {{ u.active ? '啟用' : '停用' }}
              </span>
            </td>
            <td class="flex gap-2 px-6 py-4">
              <button
                class="rounded border border-gray-300 px-3 py-1 text-sm text-gray-700 hover:bg-gray-100"
                @click="openEditModal(u)"
              >
                編輯
              </button>
              <button
                class="rounded border px-3 py-1 text-sm"
                :class="
                  u.active
                    ? 'border-red-300 text-red-600 hover:bg-red-50'
                    : 'border-green-300 text-green-600 hover:bg-green-50'
                "
                @click="confirmDisable = u"
              >
                {{ u.active ? '停用' : '啟用' }}
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Create / Edit Modal -->
    <div
      v-if="showModal"
      class="fixed inset-0 z-50 flex items-center justify-center bg-black/50"
      @click.self="closeModal"
    >
      <div class="w-full max-w-lg rounded-lg bg-white p-6 shadow-xl">
        <h2 class="mb-4 text-lg font-bold text-gray-800">
          {{ editingUser ? '編輯使用者' : '新增使用者' }}
        </h2>

        <form @submit.prevent="handleSave" class="space-y-4">
          <!-- Show password after creation -->
          <div v-if="createdPassword" class="rounded-lg bg-yellow-50 border border-yellow-200 p-4">
            <p class="text-sm font-medium text-yellow-800">使用者已建立！初始密碼：</p>
            <p class="mt-1 rounded bg-white px-3 py-2 font-mono text-lg text-gray-900 select-all">{{ createdPassword }}</p>
            <p class="mt-1 text-xs text-yellow-600">請將此密碼交給使用者，關閉後將無法再查看。</p>
          </div>

          <template v-if="!createdPassword">
          <div v-if="!editingUser">
            <label class="mb-1 block text-sm font-medium text-gray-700">帳號</label>
            <input
              v-model="form.username"
              type="text"
              required
              class="w-full rounded-md border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none"
            />
          </div>
          <div v-if="!editingUser">
            <label class="mb-1 block text-sm font-medium text-gray-700">密碼（留空則自動產生）</label>
            <input
              v-model="form.password"
              type="text"
              class="w-full rounded-md border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none"
              placeholder="留空自動產生隨機密碼"
            />
          </div>
          <div>
            <label class="mb-1 block text-sm font-medium text-gray-700">姓名</label>
            <input
              v-model="form.name"
              type="text"
              required
              class="w-full rounded-md border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none"
            />
          </div>
          <div>
            <label class="mb-1 block text-sm font-medium text-gray-700">Email</label>
            <input
              v-model="form.email"
              type="email"
              required
              class="w-full rounded-md border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none"
            />
          </div>
          <div class="flex gap-4">
            <div class="flex-1">
              <label class="mb-1 block text-sm font-medium text-gray-700">部門</label>
              <input
                v-model="form.department"
                type="text"
                required
                class="w-full rounded-md border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none"
              />
            </div>
            <div class="flex-1">
              <label class="mb-1 block text-sm font-medium text-gray-700">職位</label>
              <input
                v-model="form.position"
                type="text"
                required
                class="w-full rounded-md border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none"
              />
            </div>
          </div>
          <div class="flex gap-4">
            <div class="flex-1">
              <label class="mb-1 block text-sm font-medium text-gray-700">角色</label>
              <select
                v-model="form.role"
                class="w-full rounded-md border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none"
              >
                <option value="USER">使用者</option>
                <option value="ADMIN">管理員</option>
              </select>
            </div>
            <div class="flex-1">
              <label class="mb-1 block text-sm font-medium text-gray-700"
                >年假天數</label
              >
              <input
                v-model.number="form.annualLeaveDays"
                type="number"
                min="0"
                required
                class="w-full rounded-md border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none"
              />
            </div>
          </div>
          </template>

          <div class="flex justify-end gap-3 pt-2">
            <button
              type="button"
              class="rounded-md border border-gray-300 px-4 py-2 text-gray-700 hover:bg-gray-100"
              @click="closeModal"
            >
              {{ createdPassword ? '關閉' : '取消' }}
            </button>
            <button
              v-if="!createdPassword"
              type="submit"
              :disabled="saving"
              class="rounded-md bg-blue-600 px-4 py-2 text-white hover:bg-blue-700 disabled:opacity-50"
            >
              {{ saving ? '儲存中...' : '儲存' }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- Confirm Disable Dialog -->
    <div
      v-if="confirmDisable"
      class="fixed inset-0 z-50 flex items-center justify-center bg-black/50"
      @click.self="confirmDisable = null"
    >
      <div class="w-full max-w-sm rounded-lg bg-white p-6 shadow-xl">
        <h2 class="mb-4 text-lg font-bold text-gray-800">
          {{ confirmDisable.active ? '停用使用者' : '啟用使用者' }}
        </h2>
        <p class="mb-6 text-gray-600">
          確定要{{ confirmDisable.active ? '停用' : '啟用' }}使用者「{{
            confirmDisable.name
          }}」嗎？
        </p>
        <div class="flex justify-end gap-3">
          <button
            class="rounded-md border border-gray-300 px-4 py-2 text-gray-700 hover:bg-gray-100"
            @click="confirmDisable = null"
          >
            取消
          </button>
          <button
            class="rounded-md px-4 py-2 text-white"
            :class="
              confirmDisable.active
                ? 'bg-red-600 hover:bg-red-700'
                : 'bg-green-600 hover:bg-green-700'
            "
            @click="handleDisable(confirmDisable!)"
          >
            確定
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
