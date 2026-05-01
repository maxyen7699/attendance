# Brainstorming: 出缺勤管理系統

## 1. GSD 環境掃描
- **當前上下文狀態**: 全新 Monorepo 專案，apps/frontend + apps/backend 空目錄
- **專案健康度評估**: 初始化完成，無技術債

## 1.5 現有系統分析 (As-Is Analysis)
- **受影響的現有模組**: 無（全新專案）
- **現有邏輯盲點/技術債**: 無

## 2. 問題定義
- **核心目標**: 建構一套出缺勤管理系統，支援員工打卡、請假、加班申請與管理
- **利害關係人需求**:
  - 員工：打卡上下班、請假申請、加班申請、查看個人出勤紀錄
  - 主管：簽核假單/加班單、查看部門出勤
  - 管理者（Admin）：使用者管理、全系統配置、報表查看
- **技術限制與邊界**:
  - 小型規模（<50 人）
  - 前端：Vite + Vue.js
  - 後端：Java Spring Boot
  - 資料庫：本機 MySQL
  - 架構：Monolith（方案 A — 階段式）

## 3. 解決方案方案探索
### 方案 A: 階段式 Monolith（已採用）
- **優點**: 架構簡單、開發快速、部署容易、適合小規模
- **缺點/代價**: 所有功能在同一部署單元
### 方案 B: 前後端完全獨立 + API Gateway
- **優點**: 前後端可獨立部署擴展
- **缺點/代價**: 對 <50 人系統過度設計
### 方案 C: 微服務架構
- **優點**: 完全解耦
- **缺點/代價**: 大量基礎設施開銷，嚴重過度設計

## 4. 最終決策路徑
- **選定方案**: 方案 A — 階段式 Monolith
- **關鍵理由**: 小型規模不需分散式架構，單體應用足夠應付，模組邊界清楚便於未來拆分

---

## 階段劃分（5 Phases）

| Phase | 名稱 | 核心功能 |
|-------|------|----------|
| P1 | 基礎建設與認證 | Spring Boot 初始化、MySQL Schema、JWT 認證、角色權限（Admin/User）、使用者 CRUD、Email 發送密碼 |
| P2 | 打卡系統 | 上下班打卡、出缺勤記錄查詢、管理者查看全員紀錄、異常打卡標記 |
| P3 | 請假與簽核 | 請假申請、單層簽核、年假/補休額度、代理人指定與權限移轉 |
| P4 | 加班管理 | 加班申請（事前+事後）、主管簽核、加班時數統計、補休自動結算 |
| P5 | 報表與儀表板 | 個人月報、全員統計、圖表 Dashboard |

---

## P1 基礎建設與認證

### 後端結構
```
apps/backend/src/main/java/com/attendance/
├── config/           # Spring Security、CORS、JWT 設定
├── controller/       # REST API endpoints
├── dto/              # Request/Response DTOs
├── entity/           # JPA Entities
├── repository/       # Spring Data JPA Repositories
├── service/          # Business logic
├── security/         # JWT filter、UserDetails
└── util/             # 工具類（Email sender）
```

### 資料庫 — users 表
| 欄位 | 型態 | 說明 |
|------|------|------|
| id | BIGINT PK | 主鍵 |
| username | VARCHAR(50) UNIQUE | 登入帳號 |
| email | VARCHAR(100) UNIQUE | Email |
| password_hash | VARCHAR(255) | BCrypt 加密密碼 |
| role | ENUM('ADMIN','USER') | 角色 |
| name | VARCHAR(100) | 姓名 |
| department | VARCHAR(50) | 部門 |
| position | VARCHAR(50) | 職位 |
| supervisor_id | BIGINT FK → users.id | 直屬主管 |
| annual_leave_days | INT DEFAULT 14 | 年假天數 |
| is_active | BOOLEAN DEFAULT TRUE | 是否啟用 |
| created_at | DATETIME | 建立時間 |
| updated_at | DATETIME | 更新時間 |

### 認證流程
- Spring Security + JWT（Access Token 2h + Refresh Token 7d）
- Admin 新增使用者 → 自動產生隨機密碼 → Email 發送
- `@PreAuthorize` 區分 ADMIN / USER 權限

### API（P1）
| Method | Path | 說明 | 權限 |
|--------|------|------|------|
| POST | /api/auth/login | 登入 | Public |
| POST | /api/auth/refresh | 換 Token | Authenticated |
| GET | /api/users | 列出使用者 | ADMIN |
| POST | /api/users | 新增使用者（觸發 Email） | ADMIN |
| PUT | /api/users/{id} | 修改使用者 | ADMIN |
| DELETE | /api/users/{id} | 停用使用者（soft delete） | ADMIN |

### 前端結構
```
apps/frontend/src/
├── views/            # 頁面組件
├── components/       # 通用組件
├── stores/           # Pinia state management
├── router/           # Vue Router
├── api/              # Axios API calls
└── assets/           # 靜態資源
```

### 前端頁面（P1）
- 登入頁
- 使用者管理頁（Admin）
- 個人資料頁（User）

---

## P2 打卡系統

### 資料庫 — attendance_records 表
| 欄位 | 型態 | 說明 |
|------|------|------|
| id | BIGINT PK | 主鍵 |
| user_id | BIGINT FK → users.id | 使用者 |
| clock_in | DATETIME | 上班打卡時間 |
| clock_out | DATETIME NULL | 下班打卡時間 |
| status | ENUM('NORMAL','LATE','EARLY_LEAVE','ABSENT','FORGOT') | 出勤狀態 |
| note | VARCHAR(255) | 備註 |
| created_at | DATETIME | 建立時間 |
| updated_at | DATETIME | 更新時間 |

### API（P2）
| Method | Path | 說明 | 權限 |
|--------|------|------|------|
| POST | /api/attendance/clock-in | 上班打卡 | USER |
| POST | /api/attendance/clock-out | 下班打卡 | USER |
| GET | /api/attendance/my | 查看自己紀錄 | USER |
| GET | /api/attendance/all | 全員紀錄 | ADMIN |

### 核心邏輯
- 每天只能打卡一次（已有未下班紀錄不可再次上班打卡）
- 遲到/早退規則可配置（預設 09:00 上班、18:00 下班）
- 忘記打卡由 Admin 手動補登

### 前端頁面（P2）
- 打卡首頁（大按鈕 + 當日狀態）
- 個人出勤紀錄列表
- 管理者全員出勤查詢

---

## P3 請假與簽核

### 資料庫 — leave_types 表
| 欄位 | 型態 | 說明 |
|------|------|------|
| id | BIGINT PK | 主鍵 |
| name | VARCHAR(50) | 假別名稱（年假/事假/病假/補休/特休） |
| deductible | BOOLEAN | 是否扣薪 |

### 資料庫 — leave_balances 表
| 欄位 | 型態 | 說明 |
|------|------|------|
| id | BIGINT PK | 主鍵 |
| user_id | BIGINT FK → users.id | 使用者 |
| leave_type_id | BIGINT FK → leave_types.id | 假別 |
| total_days | DECIMAL(5,1) | 總天數 |
| used_days | DECIMAL(5,1) | 已用天數 |
| remaining_days | DECIMAL(5,1) | 剩餘天數 |
| year | INT | 年度 |

### 資料庫 — leave_applications 表
| 欄位 | 型態 | 說明 |
|------|------|------|
| id | BIGINT PK | 主鍵 |
| user_id | BIGINT FK → users.id | 申請人 |
| leave_type_id | BIGINT FK → leave_types.id | 假別 |
| start_date | DATE | 起始日期 |
| end_date | DATE | 結束日期 |
| total_days | DECIMAL(5,1) | 請假天數 |
| reason | VARCHAR(500) | 事由 |
| status | ENUM('PENDING','APPROVED','REJECTED','CANCELLED') | 狀態 |
| approver_id | BIGINT FK → users.id | 簽核主管 |
| approved_at | DATETIME NULL | 簽核時間 |
| agent_id | BIGINT FK → users.id | 代理人 |
| created_at | DATETIME | 建立時間 |

### API（P3）
| Method | Path | 說明 | 權限 |
|--------|------|------|------|
| POST | /api/leaves | 提出請假（含代理人） | USER |
| GET | /api/leaves/my | 我的請假紀錄 | USER |
| GET | /api/leaves/pending | 待我簽核 | 主管 |
| PUT | /api/leaves/{id}/approve | 簽核通過 | 主管 |
| PUT | /api/leaves/{id}/reject | 簽核駁回 | 主管 |
| GET | /api/leaves/balance | 請假餘額 | USER |

### 代理人機制
- 請假時指定代理人 → 代理人獲得被代理人的「待簽核事項」處理權限
- 代理人可在「代理任務」頁面代為審核假單

### 前端頁面（P3）
- 請假申請表單
- 我的請假紀錄
- 簽核管理頁（主管）
- 代理任務頁
- 請假餘額總覽

---

## P4 加班管理

### 資料庫 — overtime_applications 表
| 欄位 | 型態 | 說明 |
|------|------|------|
| id | BIGINT PK | 主鍵 |
| user_id | BIGINT FK → users.id | 申請人 |
| type | ENUM('PRE_APPLY','POST_REPORT') | 事前/事後 |
| overtime_date | DATE | 加班日期 |
| start_time | TIME | 開始時間 |
| end_time | TIME | 結束時間 |
| hours | DECIMAL(4,1) | 加班時數 |
| reason | VARCHAR(500) | 事由 |
| status | ENUM('PENDING','APPROVED','REJECTED') | 狀態 |
| approver_id | BIGINT FK → users.id | 簽核主管 |
| approved_at | DATETIME NULL | 簽核時間 |
| created_at | DATETIME | 建立時間 |

### API（P4）
| Method | Path | 說明 | 權限 |
|--------|------|------|------|
| POST | /api/overtime | 申請加班 | USER |
| GET | /api/overtime/my | 我的加班紀錄 | USER |
| GET | /api/overtime/pending | 待簽核加班 | 主管 |
| PUT | /api/overtime/{id}/approve | 簽核通過 | 主管 |
| PUT | /api/overtime/{id}/reject | 簽核駁回 | 主管 |
| GET | /api/overtime/stats | 加班時數統計 | ADMIN |

### 補休結算
- 加班時數可轉換為補休天數（預設 8 小時 = 1 天補休，規則可配置）
- 簽核通過後自動計入 leave_balances

### 前端頁面（P4）
- 加班申請表單
- 我的加班紀錄
- 加班簽核頁（主管）

---

## P5 報表與儀表板

### API（P5）
| Method | Path | 說明 | 權限 |
|--------|------|------|------|
| GET | /api/reports/monthly | 個人月報 | USER |
| GET | /api/reports/department | 部門出勤統計 | ADMIN |
| GET | /api/reports/leave-stats | 請假統計 | ADMIN |
| GET | /api/reports/overtime-stats | 加班統計 | ADMIN |
| GET | /api/dashboard | 管理者儀表板 | ADMIN |

### 前端頁面（P5）
- 個人出勤月報（日曆檢視 + 列表）
- 管理者 Dashboard（出勤率、請假率、加班統計圖表）
- 全員出勤統計表（可匯出）
