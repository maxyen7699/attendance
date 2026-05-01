# Review: 出缺勤管理系統 — 最終審查報告

## 審查摘要

| 項目 | 說明 |
|------|------|
| **審查日期** | 2026-05-01 |
| **審查基準** | design.md / plan.md (openspec/changes/feat-attendance-management/) |
| **審查範圍** | 後端 Spring Boot (75 項) + 前端 Vue 3 (68 項) = 共 143 項 |
| **整體結果** | **CONDITIONAL PASS** |

---

## 一、審查統計總覽

### 後端 (75 項)

| 類別 | 總項目 | PASS | FAIL | WARN | INFO |
|------|--------|------|------|------|------|
| Schema 一致性 | 8 | 7 | 1 | 0 | 0 |
| API 端點完整性 | 32 | 32 | 0 | 0 | 0 |
| 商業邏輯正確性 | 15 | 12 | 1 | 2 | 0 |
| 安全性 | 13 | 8 | 0 | 4 | 1 |
| 錯誤處理 | 7 | 4 | 1 | 1 | 1 |
| **小計** | **75** | **63** | **3** | **7** | **2** |

### 前端 (68 項)

| 類別 | 總項目 | PASS | WARN | FAIL |
|------|--------|------|------|------|
| 路由完整性 | 18 | 18 | 0 | 0 |
| Store 實作 | 4 | 4 | 0 | 0 |
| API 呼叫 | 7 | 6 | 1 | 0 |
| UI/UX 一致性 | 14 | 10 | 0 | 4 |
| 權限控制 | 7 | 5 | 2 | 0 |
| 功能完整性 | 18 | 18 | 0 | 0 |
| **小計** | **68** | **61** | **3** | **4** |

---

## 二、必須修正的問題 (Blocking)

### B-1 [CRITICAL] RuntimeException 未被 GlobalExceptionHandler 攔截
- **位置**: AttendanceService, LeaveService, OvertimeService
- **問題**: Service 層使用 `throw new RuntimeException("業務訊息")` 處理業務錯誤，但 GlobalExceptionHandler 無對應攔截器。使用者看到 500 + "An unexpected error occurred"，看不到實際業務錯誤訊息。
- **建議**: 新增自訂 `BusinessException` 並在 GlobalExceptionHandler 中攔截，回傳 400 Bad Request。

### B-2 [HIGH] Refresh Token 的 userId 擷取可能回傳 null
- **位置**: JwtUtil.generateRefreshToken() / getUserIdFromToken()
- **問題**: `generateRefreshToken()` 僅將 userId 放入 subject 而非 claims，`getUserIdFromToken()` 從 claims 的 "userId" 欄位取值，導致 refresh 時取得 null。
- **建議**: 修改 generateRefreshToken() 新增 `.claim("userId", userId)`。

### B-3 [HIGH] 共用元件未抽離
- **位置**: 前端 src/components/ 目錄為空
- **問題**: 設計規格要求 7 個核心共用元件 (Button, Input, Modal, Table, DatePicker, StatusBadge, Pagination)，全部直接寫在各 View 中。
- **建議**: 建立 components/ 目錄，至少抽離 BaseButton、BaseModal、StatusBadge、Pagination。

### B-4 [HIGH] 分頁功能缺失
- **位置**: 所有資料表格頁面
- **問題**: 出勤紀錄、請假紀錄、加班紀錄等表格均未實作分頁，資料量大時有效能問題。
- **建議**: 實作 Pagination 共用元件，表格頁面加入分頁邏輯。

---

## 三、建議修正的問題 (Recommended)

### R-1 [HIGH] 缺少 attendance_records 索引
- 設計規格要求 `idx_attendance_user_date` 索引，Entity 上未宣告。

### R-2 [MEDIUM] LeaveController /pending 授權表達式形同虛設
- `@PreAuthorize("hasRole('ADMIN') or authentication.principal != null")` 中 principal != null 在已認證請求恆為 true。

### R-3 [MEDIUM] 遲到判定邏輯與規格文字有歧義
- 當前 09:00:00 不算遲到，09:01 才算。規格文字為「遲到 > 09:00」。

### R-4 [MEDIUM] ApiResponse 缺少 errors 欄位
- 規格要求錯誤回應含 errors 陣列，目前僅合併為逗號分隔字串。

### R-5 [MEDIUM] CORS 重複設定
- SecurityConfig 和 WebConfig 同時設定 CORS，WebConfig 被覆蓋。

### R-6 [MEDIUM] 工具函式重複
- `formatDateTime()` / `statusLabel()` / `statusClass()` 在多個 View 中重複定義。

### R-7 [MEDIUM] 簽核頁面缺少角色路由守衛
- `/leaves/approval`、`/leaves/proxy`、`/overtime/approval` 僅檢查登入，無角色驗證。

### R-8 [MEDIUM] AdminAttendanceView 未使用 Store
- 直接 import API 呼叫，與其他頁面模式不一致。

---

## 四、無阻塞問題 (Non-blocking)

| # | 嚴重度 | 項目 |
|---|--------|------|
| N-1 | LOW | Refresh Token 無撤銷機制 |
| N-2 | LOW | ProfileView 缺少編輯功能 |
| N-3 | LOW | `deleteUser()` API 前端未使用 (dead code) |
| N-4 | LOW | AdminUsersView/DashboardView catch 靜默吞沒錯誤 |
| N-5 | LOW | API 回應未做防禦性檢查 |
| N-6 | LOW | LeaveApplyView 代理人選擇器無 UI |
| N-7 | LOW | LeaveService.getPendingApprovals() 去重邏輯可簡化 |
| N-8 | LOW | DataInitializer 假別 deductible 值待確認 |
| N-9 | INFO | AttendanceController 注入未使用的 UserRepository |
| N-10 | INFO | CORS 重複設定 (SecurityConfig + WebConfig) |
| N-11 | INFO | 導覽列無手機版適配 |
| N-12 | INFO | 無 loading skeleton |

---

## 五、亮點 (Highlights)

1. **Schema 高度一致**: 6 張資料表 Entity 定義與設計規格 7/8 PASS
2. **API 完整覆蓋**: 全部 32 個後端端點均已實作，路徑/方法/權限正確
3. **路由零遺漏**: 前端 17 個設計頁面全部有對應路由 + 額外 404 頁面
4. **4 個 Pinia Store**: 狀態管理邏輯清晰，涵蓋所有業務模組
5. **JWT 認證完整**: Token 持久化、自動刷新、路由守衛、登入保護
6. **商業邏輯正確**: 打卡唯一限制、請假額度檢查、加班補休結算、使用者自動初始化

---

## 六、結論

本系統在架構設計、API 覆蓋率、Entity 對齊、前端功能完整性等方面表現良好。整體評價為 **CONDITIONAL PASS**。

**4 個 Blocking 問題** (B-1 ~ B-4) 建議在合併前修正。其餘 Recommended / Non-blocking 問題可在後續迭代中處理。

---

*本報告基於 openspec/changes/feat-attendance-management/ 中的 design.md 與 plan.md 規格，逐項比對後端 (75 項) 與前端 (68 項) 原始碼產出。*

*詳細分項報告見: review-backend.md, review-frontend.md*
