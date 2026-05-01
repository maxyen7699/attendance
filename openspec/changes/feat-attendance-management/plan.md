# Plan: 出缺勤管理系統

## 執行順序總覽

```
P1 基礎建設與認證 ──→ P2 打卡系統 ──→ P3 請假與簽核 ──→ P4 加班管理 ──→ P5 報表與儀表板
```

每個 Phase 內部遵循：**測試先行 → 後端實作 → 前端實作 → 整合測試 → Commit**。

---

## Phase 1: 基礎建設與認證

### 後端任務

#### P1-B1: Spring Boot 專案初始化
- 建立 `apps/backend` Spring Boot 專案（Java 17, Maven）
- 加入 dependencies: Spring Web, Spring Data JPA, Spring Security, MySQL Driver, Lombok, JJwt
- 設定 `application.yml`（MySQL 連線、server port 8080）
- **產出**: 可啟動的空 Spring Boot 應用

#### P1-B2: 資料庫 Schema 與 Entity
- 建立 `users` 表 JPA Entity
- 設定 Hibernate DDL auto（開發階段 `update`）
- Repository: `UserRepository`（含 findByUsername, findByEmail, findByIsActive）
- **測試**: Entity 對應 MySQL 欄位正確

#### P1-B3: JWT 認證實作
- `JwtUtil`: 產生/驗證/解析 Access Token (2h) + Refresh Token (7d)
- `JwtAuthenticationFilter`: OncePerRequestFilter 攔截驗證
- `CustomUserDetailsService`: 實作 UserDetailsService
- Spring Security 設定：public endpoints (/api/auth/**) + 其餘需認證
- **測試**: Token 產生/驗證、過期處理、無效 Token 拒絕

#### P1-B4: 認證 API
- `POST /api/auth/login` — 驗證帳密、回傳 access + refresh token
- `POST /api/auth/refresh` — 用 refresh token 換新 access token
- DTO: LoginRequest, TokenResponse, RefreshRequest
- **測試**: 登入成功/失敗、Token 刷新、過期 Token 處理

#### P1-B5: 使用者 CRUD API
- `GET /api/users` — 列出所有啟用使用者（分頁）
- `POST /api/users` — 新增使用者（隨機密碼 + Email 發送）
- `PUT /api/users/{id}` — 修改使用者資料
- `DELETE /api/users/{id}` — Soft delete（is_active = false）
- `GET /api/users/me` — 取得登入者個人資料
- DTO: UserRequest, UserResponse, UserUpdateRequest
- `@PreAuthorize("hasRole('ADMIN')")` 保護管理 API
- **測試**: CRUD 操作、權限驗證、Email 發送觸發

#### P1-B6: Email 工具
- `EmailUtil`: 使用 JavaMailSender 發送密碼通知信
- 信件模板：帳號 + 初始密碼
- 可配置 SMTP 設定（預設 Gmail SMTP）
- **測試**: Email 發送格式正確

### 前端任務

#### P1-F1: Vue 3 + Vite 專案初始化
- 建立 `apps/frontend` Vue 3 + Vite 專案
- 安裝: Tailwind CSS, Pinia, Vue Router, Axios
- 設定 Tailwind、Vite proxy（/api → localhost:8080）
- **產出**: 可啟動的空 Vue 應用

#### P1-F2: API 層與狀態管理
- `api/client.js` — Axios instance（baseURL, interceptor 加 Token）
- `api/auth.js` — login, refresh
- `api/users.js` — CRUD API calls
- `stores/useAuthStore.js` — Pinia store：Token 存儲、登入/登出、自動刷新

#### P1-F3: 登入頁
- 路由: `/login`
- 帳號 + 密碼表單
- 登入成功 → 導向首頁
- Token 過期 → 自動導回登入頁

#### P1-F4: 個人資料頁
- 路由: `/profile`
- 顯示姓名、部門、職位、Email、主管

#### P1-F5: 使用者管理頁（Admin）
- 路由: `/admin/users`
- 表格列出所有使用者（搜尋、分頁）
- 新增使用者 Modal
- 編輯使用者 Modal
- 停用使用者（確認對話框）

#### P1-F6: 路由守衛
- Vue Router Navigation Guard: 未登入 → 導向 /login
- Admin Guard: 非 ADMIN → 導向首頁
- 404 頁面

### 整合驗證
- [ ] 登入流程 E2E：輸入帳密 → 取得 Token → 進入系統
- [ ] Admin 建立使用者 → Email 發送成功
- [ ] Token 過期 → 自動 refresh → 失敗 → 導回登入
- [ ] Soft delete 使用者 → 無法登入

---

## Phase 2: 打卡系統

### 後端任務

#### P2-B1: 資料庫 Schema
- 建立 `attendance_records` 表 Entity
- Repository: `AttendanceRepository`（含 findByUserId, findByUserIdAndDate）
- **測試**: Entity 對應正確

#### P2-B2: 打卡 API
- `POST /api/attendance/clock-in` — 上班打卡（判斷遲到）
- `POST /api/attendance/clock-out` — 下班打卡（判斷早退）
- `GET /api/attendance/today` — 今日打卡狀態
- `GET /api/attendance/my` — 個人紀錄（分頁、月份篩選）
- `GET /api/attendance/all` — 全員紀錄（Admin，分頁、日期篩選）
- 商業邏輯：每日唯一打卡、遲到/早退自動標記
- **測試**: 打卡成功/重複打卡/遲到標記/早退標記

### 前端任務

#### P2-F1: 打卡首頁
- 路由: `/`
- 大按鈕：上班打卡 / 下班打卡
- 顯示今日狀態（打卡時間、出勤狀態）
- 即時時間顯示

#### P2-F2: 個人出勤紀錄
- 路由: `/attendance/my`
- 表格：日期、上班時間、下班時間、狀態、備註
- 月份篩選

#### P2-F3: 全員出勤查詢（Admin）
- 路由: `/admin/attendance`
- 表格：姓名、部門、日期、打卡時間、狀態
- 日期範圍篩選、部門篩選

### 整合驗證
- [ ] 上班打卡 → 顯示遲到/正常 → 下班打卡 → 顯示早退/正常
- [ ] 重複上班打卡 → 被拒絕
- [ ] Admin 查看全員紀錄 → 篩選功能正常

---

## Phase 3: 請假與簽核

### 後端任務

#### P3-B1: 資料庫 Schema
- 建立 `leave_types`、`leave_balances`、`leave_applications` 表
- Repository 各一
- **測試**: Entity 對應正確

#### P3-B2: 請假類型與額度
- 初始化 leave_types 預設資料（年假/事假/病假/補休/特休）
- 依年度自動建立 leave_balances（年假依 users.annual_leave_days）
- **測試**: 預設資料正確、額度計算正確

#### P3-B3: 請假 API
- `POST /api/leaves` — 提出請假（含代理人指定）
- `GET /api/leaves/my` — 我的請假紀錄
- `GET /api/leaves/balance` — 請假餘額
- 商業邏輯：天數不可超過剩餘額度、自動計算天數
- **測試**: 申請成功/額度不足/日期衝突

#### P3-B4: 簽核 API
- `GET /api/leaves/pending` — 待簽核列表
- `PUT /api/leaves/{id}/approve` — 簽核通過（扣減 leave_balances）
- `PUT /api/leaves/{id}/reject` — 簽核駁回
- 代理人機制：主管請假期間，代理人可代為簽核
- **測試**: 簽核流程、額度扣減、代理人權限

### 前端任務

#### P3-F1: 請假申請表單
- 路由: `/leaves/apply`
- 表單：假別、起迄日期、天數計算、事由、代理人選擇
- 即時顯示剩餘額度

#### P3-F2: 我的請假紀錄
- 路由: `/leaves/my`
- 表格：假別、日期、天數、狀態、簽核主管
- 取消功能（僅 PENDING）

#### P3-F3: 簽核管理頁
- 路由: `/leaves/approval`
- 待簽核列表：申請人、假別、日期、事由
- 核准/駁回按鈕

#### P3-F4: 請假餘額總覽
- 路由: `/leaves/balance`
- 各假別額度表格：總天數、已用、剩餘

#### P3-F5: 代理任務頁
- 路由: `/leaves/proxy`
- 顯示被代理人的待簽核事項
- 可代為簽核

### 整合驗證
- [ ] 請假申請 → 額度扣減 → 主管簽核 → 狀態變更
- [ ] 額度不足 → 申請被拒
- [ ] 代理人代簽核 → 成功

---

## Phase 4: 加班管理

### 後端任務

#### P4-B1: 資料庫 Schema
- 建立 `overtime_applications` 表
- Repository
- **測試**: Entity 對應正確

#### P4-B2: 加班 API
- `POST /api/overtime` — 申請加班（PRE_APPLY / POST_REPORT）
- `GET /api/overtime/my` — 我的加班紀錄
- `GET /api/overtime/pending` — 待簽核加班
- `PUT /api/overtime/{id}/approve` — 簽核通過 → 補休自動計入 leave_balances
- `PUT /api/overtime/{id}/reject` — 簽核駁回
- `GET /api/overtime/stats` — 加班時數統計（Admin）
- **測試**: 申請/簽核/補休結算

#### P4-B3: 補休結算邏輯
- 加班時數 ÷ 8 = 補休天數（可配置）
- 簽核通過後自動新增/更新 leave_balances 的補休額度
- **測試**: 結算計算正確

### 前端任務

#### P4-F1: 加班申請表單
- 路由: `/overtime/apply`
- 表單：類型（事前/事後）、日期、起迄時間、時數自動計算、事由

#### P4-F2: 我的加班紀錄
- 路由: `/overtime/my`
- 表格：日期、時數、類型、狀態

#### P4-F3: 加班簽核頁
- 路由: `/overtime/approval`
- 待簽核列表、核准/駁回

### 整合驗證
- [ ] 加班申請 → 主管簽核 → 補休自動計入
- [ ] 加班時數統計 → 資料正確

---

## Phase 5: 報表與儀表板

### 後端任務

#### P5-B1: 報表 API
- `GET /api/reports/monthly` — 個人月報（出勤天數、遲到次數、請假天數）
- `GET /api/reports/department` — 部門出勤統計
- `GET /api/reports/leave-stats` — 請假統計
- `GET /api/reports/overtime-stats` — 加班統計
- **測試**: 統計數字正確

#### P5-B2: 儀表板 API
- `GET /api/dashboard` — 彙總資料（出勤率、請假率、加班總時數、近期異常）
- **測試**: 資料彙總正確

### 前端任務

#### P5-F1: 個人月報
- 路由: `/reports/monthly`
- 日曆檢視 + 列表檢視
- 月份切換

#### P5-F2: 管理者 Dashboard
- 路由: `/admin/dashboard`
- 卡片：出勤率、請假率、加班總時數
- 圖表：出勤趨勢、請假分佈、加班統計（使用 Chart.js 或 Recharts）

#### P5-F3: 全員統計表
- 路由: `/admin/reports`
- 表格：部門、出勤率、請假天數、加班時數
- 匯出功能（CSV）

### 整合驗證
- [ ] 個人月報 → 資料與打卡/請假紀錄一致
- [ ] Dashboard 圖表正確呈現
- [ ] CSV 匯出功能正常

---

## Phase 間相依性

```
P1 (基礎建設)
 ├── P2 (打卡) ← 需要 users 表 + 認證
 │    └── P3 (請假) ← 需要 attendance + supervisor 關係
 │         └── P4 (加班) ← 需要 leave_balances (補休結算)
 │              └── P5 (報表) ← 需要所有資料
```

每個 Phase 完成後執行 Commit + GitHub Push。
