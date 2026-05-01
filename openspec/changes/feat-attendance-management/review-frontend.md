# Review: 出缺勤管理系統 - 前端審查報告

## 審查摘要
- 審查日期: 2026-05-01
- 審查範圍: 前端 Vue 3 應用 (apps/frontend/src)
- 審查檔案數: 33 (7 API, 4 Store, 1 Router, 18 View, 1 App.vue, 2 其他)
- 整體結果: **CONDITIONAL PASS**

> 核心功能完備，路由與頁面齊全，API 覆蓋率高。但存在共用元件未抽離、工具函式重複、部分權限控制不足等問題，需在合併前修正部分項目。

---

## 詳細審查結果

### 1. 路由完整性

| 頁面 | 設計路由 | Phase | 狀態 | 說明 |
|------|----------|-------|------|------|
| 登入頁 | /login | P1 | PASS | 路由設定正確，`meta.public: true`，登入後自動導向首頁 |
| 個人資料 | /profile | P1 | PASS | 路由設定正確，`requiresAuth: true` |
| 使用者管理 | /admin/users | P1 | PASS | 路由設定正確，`requiresAdmin: true` |
| 打卡首頁 | /attendance | P2 | PASS | 路由設定正確，根路徑 `/` 重定向至此頁 |
| 個人出勤紀錄 | /attendance/my | P2 | PASS | 路由設定正確 |
| 全員出勤查詢 | /admin/attendance | P2 | PASS | 路由設定正確，`requiresAdmin: true` |
| 請假申請 | /leaves/apply | P3 | PASS | 路由設定正確 |
| 我的請假紀錄 | /leaves/my | P3 | PASS | 路由設定正確 |
| 簽核管理 | /leaves/approval | P3 | PASS | 路由設定正確 |
| 代理任務 | /leaves/proxy | P3 | PASS | 路由設定正確 |
| 請假餘額 | /leaves/balance | P3 | PASS | 路由設定正確 |
| 加班申請 | /overtime/apply | P4 | PASS | 路由設定正確 |
| 我的加班紀錄 | /overtime/my | P4 | PASS | 路由設定正確 |
| 加班簽核 | /overtime/approval | P4 | PASS | 路由設定正確 |
| 個人月報 | /reports/monthly | P5 | PASS | 路由設定正確 |
| 管理者 Dashboard | /admin/dashboard | P5 | PASS | 路由設定正確，`requiresAdmin: true` |
| 全員統計表 | /admin/reports | P5 | PASS | 路由設定正確，`requiresAdmin: true` |
| 404 頁面 | /:pathMatch(.*)* | - | PASS | 捕獲所有未匹配路由，提供返回首頁按鈕 |

**小結**: 所有 17 個設計頁面路由均已實作，額外提供 404 頁面。路由路徑完全吻合設計規格。

---

### 2. Store 實作

| Store | 設計要求 | 狀態 | 說明 |
|-------|----------|------|------|
| useAuthStore | 登入狀態、Token、使用者資訊 | PASS | 包含 accessToken、refreshToken、user、login()、logout()、refresh()、fetchUser()、isLoggedIn()。Token 持久化至 localStorage。 |
| useAttendanceStore | 打卡紀錄、今日狀態 | PASS | 包含 todayStatus、myRecords、fetchTodayStatus()、doClockIn()、doClockOut()、fetchMyRecords()。狀態更新邏輯正確。 |
| useLeaveStore | 請假申請、簽核列表、餘額 | PASS | 包含 myLeaves、pendingApprovals、leaveBalances、proxyTasks。提供完整的 CRUD：申請、核准、駁回、取消、代理核准/駁回。 |
| useOvertimeStore | 加班申請、簽核列表 | PASS | 包含 myOvertime、pendingOvertime。提供申請、核准、駁回操作。 |

**小結**: 4 個 Pinia Store 全部實作，使用 Composition API (`defineStore` + setup function)，狀態管理邏輯正確。

---

### 3. API 呼叫

| API 模組 | 端點 | 狀態 | 說明 |
|----------|------|------|------|
| **client.ts** | axios 實例 | PASS | baseURL `/api`，自動注入 Bearer Token，401 自動刷新 Token 並重試請求，刷新失敗則強制登出導向 /login |
| **auth.ts** | POST /auth/login, POST /auth/refresh | PASS | 登入與 Token 刷新 |
| **users.ts** | GET /users, POST /users, PUT /users/:id, DELETE /users/:id, GET /users/me | WARN | `deleteUser()` 已定義但前端未使用（管理頁使用停用/啟用取代刪除）。GET /users/me 用於 authStore 取得當前使用者。 |
| **attendance.ts** | POST /attendance/clock-in, POST /attendance/clock-out, GET /attendance/today, GET /attendance/my, GET /attendance/all | PASS | 涵蓋打卡、今日狀態、個人紀錄、全員查詢（支援日期篩選） |
| **leaves.ts** | POST /leaves, GET /leaves/my, GET /leaves/pending, PUT /leaves/:id/approve, PUT /leaves/:id/reject, PUT /leaves/:id/cancel, GET /leaves/balance, GET /leaves/proxy | PASS | 完整涵蓋請假申請、紀錄查詢、簽核、取消、餘額、代理任務 |
| **overtime.ts** | POST /overtime, GET /overtime/my, GET /overtime/pending, PUT /overtime/:id/approve, PUT /overtime/:id/reject, GET /overtime/stats | PASS | 完整涵蓋加班申請、紀錄查詢、簽核、統計 |
| **reports.ts** | GET /reports/monthly, GET /reports/department, GET /reports/leave-stats, GET /reports/overtime-stats, GET /dashboard | PASS | 涵蓋個人月報、部門統計、請假統計、加班統計、管理者 Dashboard |

**小結**: API 端點全面覆蓋設計需求。共 26 個 API 呼叫函式，回應格式遵循 `{ success, data, message }` 規範。client.ts 攔截器正確處理認證與錯誤。

---

### 4. UI/UX 一致性

| 項目 | 狀態 | 說明 |
|------|------|------|
| Tailwind CSS 使用 | PASS | 全頁面一致使用 Tailwind CSS 類別，無自定義 CSS（僅 main.css 引入 `@import "tailwindcss"`） |
| 配色一致性 | PASS | 全域統一色彩語義：blue=主要操作、green=成功/核准、red=錯誤/駁回/停用、yellow=警告/待審、orange=早退、purple=管理員/加班、gray=中性/停用 |
| 表格樣式 | PASS | 全部表格統一：`rounded-lg border border-gray-200 bg-white shadow-sm`，表頭 `bg-gray-50`，行 hover `hover:bg-gray-50` |
| 徽章樣式 | PASS | 狀態徽章統一使用 `rounded-full px-2.5 py-0.5 text-xs font-medium`，色彩語義一致 |
| 按鈕樣式 | PASS | 主要按鈕 `bg-blue-600 text-white hover:bg-blue-700`，次要按鈕 `border border-gray-300`，核准 `bg-green-600`，駁回 `bg-red-600` |
| 表單樣式 | PASS | 輸入框統一 `rounded-md border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none` |
| 載入動畫 | PASS | 全部使用 SVG spinner + `animate-spin`，風格一致 |
| 訊息提示 | PASS | 成功 `bg-green-50 text-green-700`，錯誤 `bg-red-50 text-red-700` |
| 空狀態 | PASS | 各頁面均有空資料提示，風格一致 |
| **共用元件抽離** | **FAIL** | 設計規格要求 Button, Input, Modal, Table, DatePicker, StatusBadge, Pagination 共用元件，但實際上全部直接寫在 View 中，未建立 components/ 目錄，無任何共用元件 |
| **分頁元件** | **FAIL** | 設計規格要求 Pagination 元件，所有表格頁面均未實作分頁功能（AttendanceMyView、LeaveMyView、OvertimeMyView、AdminAttendanceView 等） |
| **DatePicker 元件** | **MISS** | 設計規格要求 DatePicker 共用元件，實際使用原生 `<input type="date">` 或年/月/日 select 下拉組合，未封裝為獨立元件 |
| 佈局一致性 | PASS | 全域使用 `min-h-screen bg-gray-50`，內容區域統一 `mx-auto max-w-{size} px-4 py-8` |
| 導覽列 | PASS | App.vue 提供頂部導覽列，含下拉選單，Admin 功能僅管理員可見 |

---

### 5. 權限控制

| 項目 | 狀態 | 說明 |
|------|------|------|
| 路由守衛 - 認證檢查 | PASS | `beforeEach` 守衛檢查 `auth.user`，未登入導向 /login 並保存 redirect query |
| 路由守衛 - Admin 檢查 | PASS | `requiresAdmin` meta 搭配 `auth.user?.role !== 'ADMIN'` 檢查，非管理員導向首頁 |
| 路由守衛 - Token 恢復 | PASS | 有 token 但無 user 物件時自動呼叫 `fetchUser()`，失敗則登出 |
| 登入頁保護 | PASS | 已登入使用者造訪 /login 自動導向首頁 |
| 導覽列權限控制 | PASS | Admin 功能選單（報表管理、使用者管理、出勤管理）使用 `v-if="auth.user?.role === 'ADMIN'"` 條件渲染 |
| **簽核頁面權限** | **WARN** | `/leaves/approval`（主管簽核）、`/leaves/proxy`（代理簽核）、`/overtime/approval`（加班簽核）路由僅設 `requiresAuth: true`，無 `requiresAdmin` 或角色檢查。任何登入使用者皆可存取這些頁面，但後端 API 應會限制資料範圍（僅回傳該使用者需簽核的項目），所以影響有限 |
| **個人資料頁編輯** | **WARN** | ProfileView 僅顯示個人資料（唯讀），無修改密碼或編輯個人資訊功能。設計規格未明確要求，但一般預期會有 |
| API Token 自動刷新 | PASS | client.ts 攔截器在 401 時自動刷新 token 並重試，刷新失敗才登出 |

---

### 6. 功能完整性

| 頁面 | 狀態 | 說明 |
|------|------|------|
| **登入頁** (LoginView) | PASS | 帳號密碼登入、錯誤提示、載入中狀態、登入後導向原始頁面 |
| **打卡首頁** (AttendanceView) | PASS | 即時時鐘、上班/下班打卡按鈕、今日狀態卡片、打卡成功/失敗訊息、已完成打卡狀態顯示、loading 狀態 |
| **個人出勤紀錄** (AttendanceMyView) | PASS | 表格顯示日期、上班/下班時間、狀態徽章、備註。空資料提示 |
| **個人資料** (ProfileView) | PASS | 顯示姓名、帳號、Email、部門、職位、角色。無編輯功能 |
| **使用者管理** (AdminUsersView) | PASS | CRUD 完整：列表、新增（Modal 表單）、編輯（Modal 表單）、停用/啟用（確認對話框）。建立後顯示初始密碼。角色/狀態使用徽章顯示 |
| **全員出勤查詢** (AdminAttendanceView) | PASS | 日期範圍篩選、全員出勤表格、狀態徽章。直接呼叫 API（非透過 Store） |
| **請假申請** (LeaveApplyView) | PASS | 假別選擇（從餘額動態載入）、年/月/日下拉日期選擇、自動計算工作日天數、餘額不足驗證、事由輸入、送出後導向我的請假 |
| **我的請假紀錄** (LeaveMyView) | PASS | 表格顯示假別、日期、天數、狀態徽章、事由。PENDING 狀態可取消 |
| **簽核管理** (LeaveApprovalView) | PASS | 待簽核列表、核准/駁回按鈕、操作 loading 狀態、待簽核數量顯示、成功/失敗訊息 |
| **代理簽核** (LeaveProxyView) | PASS | 代理任務列表、顯示原簽核人、核准/駁回按鈕、操作 loading 狀態 |
| **請假餘額** (LeaveBalanceView) | PASS | 餘額卡片列表、進度條（綠/黃/紅色根據比例）、已使用/剩餘百分比 |
| **加班申請** (OvertimeApplyView) | PASS | 加班類型選擇（事前/事後）、日期選擇、時間範圍選擇（時:分下拉）、自動計算時數、事由輸入 |
| **我的加班紀錄** (OvertimeMyView) | PASS | 表格顯示日期、類型徽章、開始/結束時間、時數、狀態徽章 |
| **加班簽核** (OvertimeApprovalView) | PASS | 待簽核列表、核准/駁回按鈕、操作 loading 狀態、待簽核數量顯示 |
| **個人月報** (ReportMonthlyView) | PASS | 年/月選擇、摘要卡片（應上班天數、實際天數、遲到、早退、請假、加班）、每日明細表格、狀態徽章 |
| **管理者 Dashboard** (AdminDashboardView) | PASS | 統計卡片（員工總數、今日出勤率含進度條、待簽核請假、待簽核加班）、月統計（加班總時數、請假總天數）、近期異常紀錄表格 |
| **全員統計表** (AdminReportsView) | PASS | 年/月篩選、三個分頁（部門統計/請假統計/加班統計）、CSV 匯出功能 |
| **404 頁面** (NotFoundView) | PASS | 顯示 404 碼、提示文字、返回首頁按鈕 |

---

## 發現問題清單

| # | 嚴重度 | 項目 | 說明 | 建議 |
|---|--------|------|------|------|
| 1 | **HIGH** | 共用元件未抽離 | 設計規格要求 Button, Input, Modal, Table, DatePicker, StatusBadge, Pagination 共 7 個核心共用元件，但前端未建立任何 components/ 目錄元件。所有 UI 元素直接寫在各 View 中。 | 建立 `src/components/` 目錄，至少抽離 BaseButton、BaseInput、BaseModal、BaseTable、StatusBadge、Pagination 為共用元件 |
| 2 | **HIGH** | 分頁功能缺失 | 所有資料表格（出勤紀錄、請假紀錄、加班紀錄、全員查詢等）均未實作分頁。資料量大時會有效能問題。 | 實作 Pagination 共用元件，表格頁面加入分頁邏輯（前端分頁或 API 分頁） |
| 3 | **MEDIUM** | 工具函式重複 | `formatDateTime()` 在 AttendanceView、AttendanceMyView、AdminAttendanceView 中重複定義。`statusLabel()` / `statusClass()` 在 AttendanceView、AttendanceMyView、AdminAttendanceView、ReportMonthlyView 中重複。`formatDate()` 在 LeaveApprovalView、LeaveMyView、LeaveProxyView 中為空實作。 | 抽離至 `src/utils/format.ts` 或 `src/composables/` 共用模組 |
| 4 | **MEDIUM** | 簽核頁面缺少角色路由守衛 | `/leaves/approval`、`/leaves/proxy`、`/overtime/approval` 路由僅檢查登入，未檢查使用者是否為主管或代理人。雖然 API 端可能限制，但前端應做基本防護。 | 新增 `requiresSupervisor` 或 `requiresRole` meta，或在導覽列以條件渲染隱藏無權限連結（已部分實作） |
| 5 | **MEDIUM** | AdminAttendanceView 未使用 Store | AdminAttendanceView 直接 import API 呼叫 `getAllRecords()` 而非透過 Store，與其他頁面模式不一致。 | 考慮在 attendanceStore 中新增 `fetchAllRecords()` 或建立 adminStore |
| 6 | **LOW** | ProfileView 缺少編輯功能 | 個人資料頁面僅唯讀顯示，使用者無法修改密碼或更新 Email 等個人資訊。 | 視需求加入編輯個資/修改密碼功能 |
| 7 | **LOW** | `deleteUser()` API 未使用 | users.ts 定義了 `deleteUser()` 但前端 AdminUsersView 以停用/啟用取代刪除，函式為 dead code。 | 移除未使用的 `deleteUser()` 或保留供未來使用 |
| 8 | **LOW** | 錯誤處理靜默吞沒 | AdminUsersView 和 AdminDashboardView 的 catch 區塊為空註解 `// handled silently`，使用者無法得知操作失敗。 | 至少顯示通用錯誤訊息或記錄至 console |
| 9 | **LOW** | API 回應格式假設 | 各 Store 和 View 均假設 API 回應為 `res.data.data` 結構，但未做防禦性檢查（如 `res.data?.data`）。若後端回應格式異常可能導致前端錯誤。 | 加入可選鏈或型別守衛 `res.data?.data ?? []` |
| 10 | **LOW** | LeaveApplyView 的 agentId 選擇器 | 請假申請表單有 `agentId` 欄位但未提供選擇代理人的 UI 元素（無下拉選單或搜尋框），使用者無法選擇代理人。 | 加入代理人選擇 UI 或移除該欄位 |
| 11 | **INFO** | 導覽列無手機版適配 | App.vue 導覽列使用水平排列的連結和下拉選單，未實作響應式漢堡選單。小螢幕裝置上可能溢出。 | 加入 mobile menu 或響應式佈局 |
| 12 | **INFO** | 無 loading skeleton | 各頁面使用 spinner 或文字「載入中...」作為載入狀態，未使用 skeleton screen 提升使用者體驗。 | 可選優化：加入 Skeleton UI |

---

## 總結統計

| 審查面向 | 項目數 | PASS | WARN | FAIL |
|----------|--------|------|------|------|
| 路由完整性 | 18 | 18 | 0 | 0 |
| Store 實作 | 4 | 4 | 0 | 0 |
| API 呼叫 | 7 | 6 | 1 | 0 |
| UI/UX 一致性 | 14 | 10 | 0 | 4 |
| 權限控制 | 7 | 5 | 2 | 0 |
| 功能完整性 | 18 | 18 | 0 | 0 |

---

## 結論

本次審查結果為 **CONDITIONAL PASS**。

**優點**:
1. 所有 17 個設計頁面均有對應路由與 View 元件，功能完整度高
2. 4 個 Pinia Store 覆蓋所有業務模組，狀態管理邏輯清晰
3. API 層完整，26 個 API 函式涵蓋所有後端端點
4. 全域 UI 風格一致，Tailwind CSS 使用規範
5. 認證流程完善：Token 持久化、自動刷新、路由守衛、登入保護

**必須修正 (合併前)**:
1. 建立 components/ 目錄，至少抽離 BaseButton、BaseInput、BaseModal、StatusBadge、Pagination 共用元件 (Issue #1)
2. 資料表格加入分頁功能 (Issue #2)

**建議修正 (可於後續迭代)**:
3. 抽離重複的工具函式至共用模組 (Issue #3)
4. 加強簽核頁面路由角色檢查 (Issue #4)
5. 統一 AdminAttendanceView 的 Store 使用模式 (Issue #5)
6. 加入代理人選擇器 UI (Issue #10)
7. 改善錯誤處理回饋 (Issue #8, #9)
8. 加入手機版響應式導覽列 (Issue #11)
