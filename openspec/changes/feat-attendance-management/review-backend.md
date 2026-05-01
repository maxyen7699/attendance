# Review: 出缺勤管理系統 - 後端審查報告

## 審查摘要
- **審查日期**: 2026-05-01
- **審查範圍**: 後端 Spring Boot 應用 (Entity / Controller / Service / Repository / DTO / Security / Config)
- **審查基準**: openspec/changes/feat-attendance-management/design.md 設計規格
- **整體結果**: CONDITIONAL PASS

> **結論說明**: 系統整體架構與設計規格高度吻合，6 張資料表的 Entity 欄位基本一致，所有 API 端點均已實作，商業邏輯覆蓋率良好。但存在若干需修正的偏差與潛在風險，詳見下方各節。

---

## 詳細審查結果

### 1. Schema 一致性

| # | 項目 | 狀態 | 說明 |
|---|------|------|------|
| 1.1 | **users 表** | PASS | 所有欄位齊全：id, username(unique, len=50), email(unique, len=100), password_hash, role(ADMIN/USER, default USER), name(len=100), department(len=50), position(len=50), supervisor_id(MFK), annual_leave_days(default 14), is_active(default true), created_at, updated_at。@PrePersist/@PreUpdate 自動填充時間戳。完全一致。 |
| 1.2 | **attendance_records 表** | PASS | 所有欄位齊全：id, user_id(FK, NOT NULL), clock_in(NOT NULL), clock_out(nullable), status(enum NORMAL/LATE/EARLY_LEAVE/ABSENT/FORGOT, len=20), note(len=255), created_at, updated_at。完全一致。 |
| 1.3 | **attendance_records 索引** | FAIL | 設計規格要求索引 `idx_attendance_user_date`，但 Entity 上未使用 `@Index` 或 `@Table(indexes=...)` 宣告此索引。雖然 Repository 的查詢方法 (findByUserAndClockInBetween) 可透過 JPA 實作，但缺少明確索引定義可能影響查詢效能。 |
| 1.4 | **leave_types 表** | PASS | 欄位齊全：id, name(unique, len=50), deductible(NOT NULL, default true)。完全一致。 |
| 1.5 | **leave_balances 表** | PASS | 欄位齊全：id, user_id(FK), leave_type_id(FK), total_days(DECIMAL 5,1), used_days(DECIMAL 5,1, default 0), remaining_days(DECIMAL 5,1), year(INT)。唯一約束 `uk_balance_user_type_year` 透過 @UniqueConstraint(columnNames={"user_id","leave_type_id","year"}) 實作。完全一致。 |
| 1.6 | **leave_applications 表** | PASS | 欄位齊全：id, user_id(FK), leave_type_id(FK), start_date, end_date, total_days(DECIMAL 5,1), reason(len=500), status(PENDING/APPROVED/REJECTED/CANCELLED), approver_id(FK), approved_at, agent_id(FK), created_at。完全一致。 |
| 1.7 | **overtime_applications 表** | PASS | 欄位齊全：id, user_id(FK), type(PRE_APPLY/POST_REPORT), overtime_date, start_time, end_time, hours(DECIMAL 4,1), reason(len=500), status(PENDING/APPROVED/REJECTED), approver_id(FK), approved_at, created_at。完全一致。 |
| 1.8 | **leave_types 預設資料** | PASS | DataInitializer 建立了 annual, personal, sick, compensatory, special 五種假別，與規格完全一致。 |

### 2. API 端點完整性

| # | 端點 | 規格 | 狀態 | 說明 |
|---|------|------|------|------|
| 2.1 | POST /api/auth/login | 規格要求 | PASS | AuthController.login() 已實作，回傳 TokenResponse (accessToken + refreshToken)。 |
| 2.2 | POST /api/auth/refresh | 規格要求 | PASS | AuthController.refresh() 已實作，驗證 refresh token 後核發新 token 對。 |
| 2.3 | GET /api/users | 規格要求 | PASS | UserController.listUsers() 已實作，限制 ADMIN 存取。 |
| 2.4 | POST /api/users | 規格要求 | PASS | UserController.createUser() 已實作，限制 ADMIN，支援可選密碼與 initialPassword 回傳。 |
| 2.5 | PUT /api/users/{id} | 規格要求 | PASS | UserController.updateUser() 已實作，限制 ADMIN。 |
| 2.6 | DELETE /api/users/{id} | 規格要求 | PASS | UserController.deleteUser() 已實作，採軟刪除 (isActive=false)，限制 ADMIN。 |
| 2.7 | GET /api/users/me | 規格要求 | PASS | UserController.getMe() 已實作，從 JWT 取得當前使用者。 |
| 2.8 | POST /api/attendance/clock-in | 規格要求 | PASS | AttendanceController.clockIn() 已實作。 |
| 2.9 | POST /api/attendance/clock-out | 規格要求 | PASS | AttendanceController.clockOut() 已實作。 |
| 2.10 | GET /api/attendance/my | 規格要求 | PASS | AttendanceController.getMyRecords() 已實作。 |
| 2.11 | GET /api/attendance/today | 規格要求 | PASS | AttendanceController.getTodayStatus() 已實作。 |
| 2.12 | GET /api/attendance/all | 規格要求 | PASS | AttendanceController.getAllRecords() 已實作，限制 ADMIN。 |
| 2.13 | POST /api/leaves | 規格要求 | PASS | LeaveController.applyLeave() 已實作。 |
| 2.14 | GET /api/leaves/my | 規格要求 | PASS | LeaveController.getMyLeaves() 已實作。 |
| 2.15 | GET /api/leaves/pending | 規格要求 | PASS | LeaveController.getPendingApprovals() 已實作。 |
| 2.16 | PUT /api/leaves/{id}/approve | 規格要求 | PASS | LeaveController.approveLeave() 已實作。 |
| 2.17 | PUT /api/leaves/{id}/reject | 規格要求 | PASS | LeaveController.rejectLeave() 已實作。 |
| 2.18 | PUT /api/leaves/{id}/cancel | 規格要求 | PASS | LeaveController.cancelLeave() 已實作。 |
| 2.19 | GET /api/leaves/balance | 規格要求 | PASS | LeaveController.getLeaveBalances() 已實作。 |
| 2.20 | GET /api/leaves/proxy | 規格要求 | PASS | LeaveController.getProxyTasks() 已實作。 |
| 2.21 | POST /api/overtime | 規格要求 | PASS | OvertimeController.applyOvertime() 已實作。 |
| 2.22 | GET /api/overtime/my | 規格要求 | PASS | OvertimeController.getMyOvertime() 已實作。 |
| 2.23 | GET /api/overtime/pending | 規格要求 | PASS | OvertimeController.getPendingOvertime() 已實作。 |
| 2.24 | PUT /api/overtime/{id}/approve | 規格要求 | PASS | OvertimeController.approveOvertime() 已實作。 |
| 2.25 | PUT /api/overtime/{id}/reject | 規格要求 | PASS | OvertimeController.rejectOvertime() 已實作。 |
| 2.26 | GET /api/overtime/stats | 規格要求 | PASS | OvertimeController.getOvertimeStats() 已實作，限制 ADMIN。 |
| 2.27 | GET /api/reports/monthly | 規格要求 | PASS | ReportController.getMonthlyReport() 已實作。 |
| 2.28 | GET /api/reports/department | 規格要求 | PASS | ReportController.getDepartmentStats() 已實作，限制 ADMIN。 |
| 2.29 | GET /api/reports/leave-stats | 規格要求 | PASS | ReportController.getLeaveStats() 已實作，限制 ADMIN。 |
| 2.30 | GET /api/reports/overtime-stats | 規格要求 | PASS | ReportController.getOvertimeStats() 已實作，限制 ADMIN。 |
| 2.31 | GET /api/dashboard | 規格要求 | PASS | DashboardController.getDashboard() 已實作，限制 ADMIN。 |
| 2.32 | 共用回應格式 | 規格要求 | PASS | 所有 Controller 回傳 `ApiResponse<T>` (success, message, data)。錯誤回應透過 GlobalExceptionHandler 統一格式 (success=false, message)。規格要求的 `errors` 欄位未實作（驗證錯誤時將多個欄位錯誤合併為逗號分隔字串放入 message）。 |

### 3. 商業邏輯正確性

| # | 項目 | 狀態 | 說明 |
|---|------|------|------|
| 3.1 | **打卡: 每天只能打卡一次** | PASS | AttendanceService.clockIn() 透過 `findByUserAndClockInBetween` 檢查今日是否已有紀錄，若存在則拋出 RuntimeException("今天已經打卡")。正確實作。 |
| 3.2 | **打卡: 遲到判定 > 09:00** | FAIL | 當前實作: `now.getHour() >= 9 && now.getMinute() > 0`。此條件表示 09:01 及之後才算遲到，但 09:00:00 ~ 09:00:59 不算遲到。規格說「遲到 > 09:00」，嚴格解讀為 09:00:01 起即為遲到。當前邏輯在恰好 09:00 時不會標記為遲到，符合多數實務場景（09:00 算準時），但與規格文字有歧義。需釐清 09:00 是否算遲到。 |
| 3.3 | **打卡: 早退判定 < 18:00** | PASS | AttendanceService.clockOut() 檢查 `now.getHour() < 18`，在 18:00 之前下班打卡會標記為 EARLY_LEAVE。若原為 LATE 則保留 LATE 狀態不被覆蓋。正確。 |
| 3.4 | **請假: 天數不超過剩餘額度** | PASS | LeaveService.applyLeave() 檢查 `balance.getRemainingDays().compareTo(req.getTotalDays()) < 0`，若不足則拋出異常。正確。 |
| 3.5 | **請假: 取消僅限 PENDING** | PASS | LeaveService.cancelLeave() 檢查 `application.getStatus() != LeaveStatus.PENDING` 時拋出異常。正確。 |
| 3.6 | **請假: 無主管時自行簽核** | PASS | LeaveService.applyLeave() 第 43-45 行: `if (approver == null) { approver = user; }` 將 approver 設為自己。正確。 |
| 3.7 | **加班: 事前/事後申請** | PASS | OvertimeApplication 支援 PRE_APPLY 與 POST_REPORT 兩種類型。OvertimeService.applyOvertime() 透過 `OvertimeType.valueOf(req.getType())` 轉換。正確。 |
| 3.8 | **加班: 補休結算 8hr=1day** | PASS | OvertimeService.approveOvertime() 第 99-100 行: `application.getHours().divide(BigDecimal.valueOf(8), 1, RoundingMode.HALF_UP)` 計算補休天數。正確。 |
| 3.9 | **加班: 簽核通過自動計入 leave_balances** | PASS | approveOvertime() 找到 compensatory 假別後，若已有餘額紀錄則累加 totalDays 和 remainingDays；若無則新建。正確。 |
| 3.10 | **使用者建立: 可選密碼** | PASS | UserService.createUser() 第 45-47 行: 若 password 為 null 或空白，則自動產生 12 位隨機密碼 (UUID)。正確。 |
| 3.11 | **使用者建立: 回傳 initialPassword** | PASS | createUser() 第 101 行: `response.setInitialPassword(rawPassword)`。正確。 |
| 3.12 | **使用者建立: 自動建立 leave_balances** | PASS | createUser() 第 69-91 行: 建立使用者後自動為所有假別建立當年度 leave_balances。annual 假別天數取自 user.annualLeaveDays，personal=7, sick=30, 其他=0。正確。 |
| 3.13 | **使用者建立: 寄送密碼信** | PASS | createUser() 透過 EmailService 非同步寄送 HTML 格式密碼信，寄送失敗僅 warn 不阻斷建立流程。正確。 |
| 3.14 | **請假核准: 扣除餘額** | PASS | LeaveService.approveLeave() 第 130-131 行: 增加 usedDays 並重新計算 remainingDays = totalDays - usedDays。正確。 |
| 3.15 | **加班簽核: 權限驗證** | WARN | OvertimeService.approveOvertime() 僅檢查 approver_id 是否為當前使用者。LeaveService.approveLeave() 同時允許 approver 和 agent 簽核。加班申請不支援 agent 簽核，此為合理設計但需確認規格是否要求。 |

### 4. 安全性

| # | 項目 | 狀態 | 說明 |
|---|------|------|------|
| 4.1 | **JWT 認證機制** | PASS | JwtUtil 使用 HMAC-SHA 簽章，支援 accessToken 與 refreshToken 雙 token 機制，過期時間透過 application properties 設定。金鑰透過 @Value 注入，不硬編碼。 |
| 4.2 | **JWT Filter** | PASS | JwtAuthenticationFilter 繼承 OncePerRequestFilter，從 Authorization header 解析 Bearer token，建立 SecurityContext。未攜帶 token 或 token 無效時不設定認證，交由後續授權檢查處理。 |
| 4.3 | **密碼加密** | PASS | 使用 BCryptPasswordEncoder，在 UserService 與 DataInitializer 中均使用 passwordEncoder.encode()。 |
| 4.4 | **CSRF 防護** | PASS | 因使用 stateless JWT，已正確停用 CSRF: `csrf(AbstractHttpConfigurer::disable)`。 |
| 4.5 | **Session 管理** | PASS | 採 STATELESS 模式: `sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))`。 |
| 4.6 | **CORS 設定** | PASS | SecurityConfig 與 WebConfig 均設定 allowedOrigins 為 localhost:5173，限制 GET/POST/PUT/DELETE/OPTIONS 方法。allowCredentials=true。 |
| 4.7 | **API 存取控制** | PASS | /api/auth/** 公開存取，其餘端點需認證。ADMIN-only 端點使用 @PreAuthorize("hasRole('ADMIN')")。 |
| 4.8 | **使用者停用檢查** | PASS | CustomUserDetailsService.loadUserByUsername() 檢查 isActive 狀態，停用帳號無法登入。AuthService.refreshToken() 亦檢查 isActive。 |
| 4.9 | **密碼不回傳** | PASS | UserResponse DTO 不包含 passwordHash 欄位。initialPassword 僅在 createUser 時回傳。 |
| 4.10 | **Refresh Token 安全** | WARN | refresh token 僅驗證簽章與過期時間，未比對資料庫中的 token 值（未實作 token 黑名單或儲存機制）。若 refresh token 被洩漏，在過期前無法撤銷。 |
| 4.11 | **JwtUtil 使用者 ID 擷取** | WARN | getUserIdFromToken() 從 accessToken 的 claim 中取得 userId，但 refreshToken 的 subject 是 userId 而非 username，getUserIdFromToken() 僅從 "userId" claim 取值。refresh token 呼叫此方法時會回傳 null（因 refresh token 只有 "type" claim 而無 "userId" claim）。目前在 AuthService.refreshToken() 中直接呼叫 getUserIdFromToken()，**refresh token 解析時會發生 NPE 或回傳 null**。 |
| 4.12 | **CORS 重複設定** | INFO | SecurityConfig 與 WebConfig 同時設定 CORS，形成重複。Spring Security 的 CORS 設定優先於 WebConfig，WebConfig 中的設定實際上不會生效。建議移除其一。 |
| 4.13 | **LeaveController /pending 授權** | WARN | `@PreAuthorize("hasRole('ADMIN') or authentication.principal != null")` 此表達式中 `authentication.principal != null` 在所有已認證請求中恆為 true，等同於無限制。應改為僅允許主管角色或查詢屬於自己的待簽核資料。 |

### 5. 錯誤處理

| # | 項目 | 狀態 | 說明 |
|---|------|------|------|
| 5.1 | **GlobalExceptionHandler** | PASS | 統一處理 MethodArgumentNotValidException(400), EntityNotFoundException(404), BadCredentialsException(401), AccessDeniedException(403), IllegalArgumentException(400), Exception(500)。所有回應均使用 ApiResponse 格式。 |
| 5.2 | **RuntimeException 處理** | FAIL | Service 層大量使用 `throw new RuntimeException("...")` 拋出業務錯誤（如 "今天已經打卡"、"使用者不存在"、"請假額度不足" 等）。GlobalExceptionHandler 僅攔截 EntityNotFoundException 和 IllegalArgumentException，未攔截通用 RuntimeException。這些 RuntimeException 會落入 `handleGeneralException()`，回傳 500 Internal Server Error 與泛化訊息 "An unexpected error occurred"，**使用者無法看到實際的業務錯誤訊息**。 |
| 5.3 | **驗證錯誤回應格式** | WARN | 規格要求錯誤回應格式為 `{ success: false, message, errors }`，但 ApiResponse 僅有 success/message/data 三個欄位，缺少 errors 欄位。驗證錯誤時將多個欄位錯誤合併為逗號分隔字串放入 message，非結構化的 errors 陣列。 |
| 5.4 | **打下班卡未上班** | PASS | AttendanceService.clockOut() 使用 orElseThrow() 處理尚未上班打卡的情況。 |
| 5.5 | **重複下班打卡** | PASS | AttendanceService.clockOut() 檢查 record.getClockOut() != null 時拋出異常。 |
| 5.6 | **重複下班打卡後仍正常處理** | INFO | clockOut 成功時沒有 try-catch 包裹， RuntimeException 會正確向上傳遞。 |
| 5.7 | **密碼信寄送失敗處理** | PASS | UserService.createUser() 以 try-catch 包裹寄信邏輯，失敗僅 log.warn 不影響使用者建立。 |

---

## 發現問題清單

| # | 嚴重度 | 項目 | 說明 | 建議 |
|---|--------|------|------|------|
| 1 | **CRITICAL** | RuntimeException 未被 GlobalExceptionHandler 攔截 | Service 層使用 `throw new RuntimeException("業務訊息")` 處理業務邏輯錯誤，但 GlobalExceptionHandler 無對應攔截器。這些例外被最後的通用 Exception handler 攔截，回傳 500 與 "An unexpected error occurred"，使用者看不到實際業務錯誤訊息。受影響的 Service: AttendanceService, LeaveService, OvertimeService。 | (1) 在 GlobalExceptionHandler 新增 `@ExceptionHandler(RuntimeException.class)` 回傳 400 Bad Request；(2) 或改用自訂 BusinessException 並在 GlobalExceptionHandler 中攔截。建議採方案 (2) 以區分業務錯誤與系統錯誤。 |
| 2 | **HIGH** | Refresh Token 的 userId 擷取可能回傳 null | JwtUtil.getUserIdFromToken() 從 claims 中取 "userId" 欄位，但 generateRefreshToken() 僅將 userId 放入 subject 而非 claims。AuthService.refreshToken() 呼叫 getUserIdFromToken(refreshToken) 會取得 null，後續 userRepository.findById(null) 行為未定義。 | 修改 generateRefreshToken() 新增 `.claim("userId", userId)` 或修改 getUserIdFromToken() 搭配 subject 回退機制。 |
| 3 | **HIGH** | 缺少 attendance_records 索引 idx_attendance_user_date | 設計規格明確要求此索引，用於加速按使用者+日期查詢。目前未在 Entity 或 migration 中宣告。 | 在 AttendanceRecord entity 上新增 `@Table(name="attendance_records", indexes = @Index(name="idx_attendance_user_date", columnList="user_id, clock_in"))`。 |
| 4 | **MEDIUM** | LeaveController /pending 端點授權表達式形同虛設 | `@PreAuthorize("hasRole('ADMIN') or authentication.principal != null")` 中 `authentication.principal != null` 在所有已認證請求中恆為 true，等同於任何登入使用者都能存取。 | 修改為 `@PreAuthorize("hasRole('ADMIN')")` 或移除註解（Service 層已透過 userId 過濾自己的待簽核資料）。若僅需檢查是否登入，移除整個 @PreAuthorize 即可。 |
| 5 | **MEDIUM** | 遲到判定邏輯與規格文字不完全吻合 | 當前 `hour >= 9 && minute > 0` 表示 09:00:00-09:00:59 不算遲到，09:01:00 起才算。規格說「遲到>09:00」，若嚴格解讀則 09:00:01 起即為遲到。 | 釐清業務需求。若 09:00 算準時，建議在設計文件中明確寫為「遲到 >= 09:01」。若 09:00 即算遲到，修改條件為 `now.getHour() >= 9`。 |
| 6 | **MEDIUM** | ApiResponse 缺少 errors 欄位 | 規格要求錯誤回應格式含 errors 欄位（結構化錯誤列表），目前僅將驗證錯誤合併為逗號分隔字串放入 message。 | 在 ApiResponse 新增 `List<String> errors` 欄位，並在 GlobalExceptionHandler.handleValidationException() 中將欄位錯誤列表填入 errors。 |
| 7 | **MEDIUM** | CORS 重複設定 | SecurityConfig (Spring Security 層級) 和 WebConfig (Spring MVC 層級) 同時設定 CORS，WebConfig 的設定實際被 SecurityConfig 覆蓋。 | 移除 WebConfig 中的 CORS 設定，保留 SecurityConfig 中的 CORS 作為單一設定點。 |
| 8 | **LOW** | Refresh Token 無撤銷機制 | Refresh token 僅透過過期時間控制，未儲存於資料庫。若 token 洩漏，在過期前無法使其失效。 | 長期建議: 在資料庫或 Redis 中儲存 refresh token，支援主動撤銷。短期: 確保 refresh token 有效期設定合理（如 7 天）。 |
| 9 | **LOW** | LeaveService.getPendingApprovals() 重複判斷邏輯 | 第 88-89 行避免 approver 和 agent 重複時加入的邏輯有誤：條件 `app.getApprover() == null || !app.getApprover().getId().equals(userId)` 當 approver 為 null 且 agent 是同一人時，仍會加入。 | 簡化邏輯，改用 Set<Long> 記錄已加入的 application ID 來去重。 |
| 10 | **LOW** | DataInitializer 假別 deductible 設定值待確認 | annual 設為 deductible=false, personal=true, sick=true, compensatory=false, special=false。規格中僅定義 deductible 欄位存在但未指定各假別的具體值。 | 確認各假別的 deductible 值是否正確（特別是 annual 和 sick 通常為 deductible）。 |
| 11 | **INFO** | AttendanceController 直接注入 UserRepository | AttendanceController 第 23 行注入 UserRepository 但未使用，屬於無用依賴。 | 移除 AttendanceController 中未使用的 UserRepository 注入。 |

---

## 審查統計

| 類別 | 總項目 | PASS | FAIL | WARN | INFO |
|------|--------|------|------|------|------|
| Schema 一致性 | 8 | 7 | 1 | 0 | 0 |
| API 端點完整性 | 32 | 32 | 0 | 0 | 0 |
| 商業邏輯正確性 | 15 | 12 | 1 | 2 | 0 |
| 安全性 | 13 | 8 | 0 | 4 | 1 |
| 錯誤處理 | 7 | 4 | 1 | 1 | 1 |
| **合計** | **75** | **63** | **3** | **7** | **2** |

---

## 結論

### 整體評價: CONDITIONAL PASS

本後端應用在架構設計、API 覆蓋率、Entity 對齊等方面表現良好。6 張資料表的 Entity 定義與設計規格高度一致（7/8 PASS），全部 32 個 API 端點均已實作且路徑、方法、權限控制正確。

### 必須修正 (Blocking Issues)

1. **[CRITICAL-1] RuntimeException 處理**: Service 層的業務錯誤訊息被 GlobalExceptionHandler 的通用 handler 吞掉，使用者看到的是 500 錯誤而非具體業務提示。需新增自訂例外或攔截 RuntimeException。

2. **[HIGH-2] Refresh Token 解析 Bug**: generateRefreshToken() 未將 userId 放入 claims，導致 refresh 流程中 getUserIdFromToken() 回傳 null，可能引發後續錯誤。

### 建議修正 (Recommended Issues)

3. **[HIGH-3] 缺少資料庫索引**: 補上 idx_attendance_user_date 索引宣告。
4. **[MEDIUM-4] 授權表達式修正**: 修正 LeaveController /pending 的 @PreAuthorize。
5. **[MEDIUM-5] 遲到判定釐清**: 與業務方確認 09:00 是否算遲到。
6. **[MEDIUM-6] ApiResponse errors 欄位**: 補充結構化錯誤欄位。
7. **[MEDIUM-7] CORS 重複設定**: 移除 WebConfig 中的 CORS。

### 無阻塞風險 (Non-blocking)

- Refresh Token 撤銷機制 (LOW) 屬於進階安全需求，可在後續迭代中處理。
- 其餘 LOW/INFO 項目為程式碼品質改善建議。

---

*本報告由審查員基於 design.md 設計規格逐項比對後端原始碼產出。所有檔案路徑以 /Users/yanzhongyang/test/attendance/apps/backend/src/main/java/com/attendance/ 為基準。*
