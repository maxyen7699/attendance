# Design: 出缺勤管理系統

## 1. 系統架構

### 1.1 整體架構
- **架構模式**: Monolith（階段式開發）
- **前端**: Vue 3 + Vite + Tailwind CSS + Pinia
- **後端**: Spring Boot (Java 17+)
- **資料庫**: MySQL 8.0（本機開發）
- **認證**: JWT（Access Token 2h + Refresh Token 7d）

### 1.2 目錄結構
```
attendance/
├── apps/
│   ├── backend/                    # Spring Boot
│   │   ├── src/main/java/com/attendance/
│   │   │   ├── config/             # Security, CORS, JWT 設定
│   │   │   ├── controller/         # REST API endpoints
│   │   │   ├── dto/                # Request/Response DTOs
│   │   │   ├── entity/             # JPA Entities
│   │   │   ├── repository/         # Spring Data JPA
│   │   │   ├── service/            # Business logic
│   │   │   ├── security/           # JWT filter, UserDetails
│   │   │   └── util/               # 工具類（Email sender）
│   │   └── src/test/java/com/attendance/
│   │       └── ...                 # 單元測試 + 整合測試
│   └── frontend/                   # Vue 3 + Vite
│       └── src/
│           ├── views/              # 頁面組件
│           ├── components/         # 通用組件
│           ├── stores/             # Pinia state management
│           ├── router/             # Vue Router
│           ├── api/                # Axios API calls
│           ├── composables/        # Composables
│           └── assets/             # 靜態資源
├── openspec/                       # 規格文檔
└── docs/                           # 其他文檔
```

---

## 2. 資料庫設計

### 2.1 users 表
| 欄位 | 型態 | 約束 | 說明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主鍵 |
| username | VARCHAR(50) | UNIQUE, NOT NULL | 登入帳號 |
| email | VARCHAR(100) | UNIQUE, NOT NULL | Email |
| password_hash | VARCHAR(255) | NOT NULL | BCrypt 加密密碼 |
| role | ENUM('ADMIN','USER') | NOT NULL, DEFAULT 'USER' | 角色 |
| name | VARCHAR(100) | NOT NULL | 姓名 |
| department | VARCHAR(50) | | 部門 |
| position | VARCHAR(50) | | 職位 |
| supervisor_id | BIGINT | FK → users.id | 直屬主管 |
| annual_leave_days | INT | DEFAULT 14 | 年假天數 |
| is_active | BOOLEAN | DEFAULT TRUE | 是否啟用 |
| created_at | DATETIME | NOT NULL | 建立時間 |
| updated_at | DATETIME | NOT NULL | 更新時間 |

### 2.2 attendance_records 表
| 欄位 | 型態 | 約束 | 說明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主鍵 |
| user_id | BIGINT | FK → users.id, NOT NULL | 使用者 |
| clock_in | DATETIME | NOT NULL | 上班打卡時間 |
| clock_out | DATETIME | NULL | 下班打卡時間 |
| status | ENUM('NORMAL','LATE','EARLY_LEAVE','ABSENT','FORGOT') | NOT NULL | 出勤狀態 |
| note | VARCHAR(255) | | 備註 |
| created_at | DATETIME | NOT NULL | 建立時間 |
| updated_at | DATETIME | NOT NULL | 更新時間 |

**索引**: `idx_attendance_user_date (user_id, DATE(clock_in))` — 每日唯一打卡約束

### 2.3 leave_types 表
| 欄位 | 型態 | 約束 | 說明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主鍵 |
| name | VARCHAR(50) | UNIQUE, NOT NULL | 假別名稱 |
| deductible | BOOLEAN | NOT NULL | 是否扣薪 |

**預設資料**: 年假、事假、病假、補休、特休

### 2.4 leave_balances 表
| 欄位 | 型態 | 約束 | 說明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主鍵 |
| user_id | BIGINT | FK → users.id, NOT NULL | 使用者 |
| leave_type_id | BIGINT | FK → leave_types.id, NOT NULL | 假別 |
| total_days | DECIMAL(5,1) | NOT NULL | 總天數 |
| used_days | DECIMAL(5,1) | NOT NULL, DEFAULT 0 | 已用天數 |
| remaining_days | DECIMAL(5,1) | NOT NULL | 剩餘天數 |
| year | INT | NOT NULL | 年度 |

**唯一約束**: `uk_balance_user_type_year (user_id, leave_type_id, year)`

### 2.5 leave_applications 表
| 欄位 | 型態 | 約束 | 說明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主鍵 |
| user_id | BIGINT | FK → users.id, NOT NULL | 申請人 |
| leave_type_id | BIGINT | FK → leave_types.id, NOT NULL | 假別 |
| start_date | DATE | NOT NULL | 起始日期 |
| end_date | DATE | NOT NULL | 結束日期 |
| total_days | DECIMAL(5,1) | NOT NULL | 請假天數 |
| reason | VARCHAR(500) | NOT NULL | 事由 |
| status | ENUM('PENDING','APPROVED','REJECTED','CANCELLED') | NOT NULL, DEFAULT 'PENDING' | 狀態 |
| approver_id | BIGINT | FK → users.id | 簽核主管 |
| approved_at | DATETIME | NULL | 簽核時間 |
| agent_id | BIGINT | FK → users.id | 代理人 |
| created_at | DATETIME | NOT NULL | 建立時間 |

### 2.6 overtime_applications 表
| 欄位 | 型態 | 約束 | 說明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主鍵 |
| user_id | BIGINT | FK → users.id, NOT NULL | 申請人 |
| type | ENUM('PRE_APPLY','POST_REPORT') | NOT NULL | 事前/事後 |
| overtime_date | DATE | NOT NULL | 加班日期 |
| start_time | TIME | NOT NULL | 開始時間 |
| end_time | TIME | NOT NULL | 結束時間 |
| hours | DECIMAL(4,1) | NOT NULL | 加班時數 |
| reason | VARCHAR(500) | NOT NULL | 事由 |
| status | ENUM('PENDING','APPROVED','REJECTED') | NOT NULL, DEFAULT 'PENDING' | 狀態 |
| approver_id | BIGINT | FK → users.id | 簽核主管 |
| approved_at | DATETIME | NULL | 簽核時間 |
| created_at | DATETIME | NOT NULL | 建立時間 |

---

## 3. API 設計

### 3.1 認證 API
| Method | Path | 說明 | 權限 |
|--------|------|------|------|
| POST | /api/auth/login | 登入 | Public |
| POST | /api/auth/refresh | 換 Token | Authenticated |

### 3.2 使用者管理 API
| Method | Path | 說明 | 權限 |
|--------|------|------|------|
| GET | /api/users | 列出使用者 | ADMIN |
| POST | /api/users | 新增使用者（觸發 Email） | ADMIN |
| PUT | /api/users/{id} | 修改使用者 | ADMIN |
| DELETE | /api/users/{id} | 停用使用者（soft delete） | ADMIN |
| GET | /api/users/me | 取得個人資料 | USER |

### 3.3 打卡 API
| Method | Path | 說明 | 權限 |
|--------|------|------|------|
| POST | /api/attendance/clock-in | 上班打卡 | USER |
| POST | /api/attendance/clock-out | 下班打卡 | USER |
| GET | /api/attendance/my | 查看自己紀錄 | USER |
| GET | /api/attendance/today | 查看今日打卡狀態 | USER |
| GET | /api/attendance/all | 全員紀錄 | ADMIN |

### 3.4 請假 API
| Method | Path | 說明 | 權限 |
|--------|------|------|------|
| POST | /api/leaves | 提出請假 | USER |
| GET | /api/leaves/my | 我的請假紀錄 | USER |
| GET | /api/leaves/pending | 待我簽核 | 主管 |
| PUT | /api/leaves/{id}/approve | 簽核通過 | 主管 |
| PUT | /api/leaves/{id}/reject | 簽核駁回 | 主管 |
| GET | /api/leaves/balance | 請假餘額 | USER |

### 3.5 加班 API
| Method | Path | 說明 | 權限 |
|--------|------|------|------|
| POST | /api/overtime | 申請加班 | USER |
| GET | /api/overtime/my | 我的加班紀錄 | USER |
| GET | /api/overtime/pending | 待簽核加班 | 主管 |
| PUT | /api/overtime/{id}/approve | 簽核通過 | 主管 |
| PUT | /api/overtime/{id}/reject | 簽核駁回 | 主管 |
| GET | /api/overtime/stats | 加班時數統計 | ADMIN |

### 3.6 報表 API
| Method | Path | 說明 | 權限 |
|--------|------|------|------|
| GET | /api/reports/monthly | 個人月報 | USER |
| GET | /api/reports/department | 部門出勤統計 | ADMIN |
| GET | /api/reports/leave-stats | 請假統計 | ADMIN |
| GET | /api/reports/overtime-stats | 加班統計 | ADMIN |
| GET | /api/dashboard | 管理者儀表板 | ADMIN |

### 3.7 共用回應格式
```json
{
  "success": true,
  "data": {},
  "message": "操作成功"
}
```

錯誤回應：
```json
{
  "success": false,
  "message": "錯誤描述",
  "errors": []
}
```

---

## 4. 前端設計

### 4.1 頁面清單
| 頁面 | 路由 | 權限 | Phase |
|------|------|------|-------|
| 登入頁 | /login | Public | P1 |
| 個人資料 | /profile | USER | P1 |
| 使用者管理 | /admin/users | ADMIN | P1 |
| 打卡首頁 | / | USER | P2 |
| 個人出勤紀錄 | /attendance/my | USER | P2 |
| 全員出勤查詢 | /admin/attendance | ADMIN | P2 |
| 請假申請 | /leaves/apply | USER | P3 |
| 我的請假紀錄 | /leaves/my | USER | P3 |
| 簽核管理 | /leaves/approval | 主管 | P3 |
| 代理任務 | /leaves/proxy | 代理人 | P3 |
| 請假餘額 | /leaves/balance | USER | P3 |
| 加班申請 | /overtime/apply | USER | P4 |
| 我的加班紀錄 | /overtime/my | USER | P4 |
| 加班簽核 | /overtime/approval | 主管 | P4 |
| 個人月報 | /reports/monthly | USER | P5 |
| 管理者 Dashboard | /admin/dashboard | ADMIN | P5 |
| 全員統計表 | /admin/reports | ADMIN | P5 |

### 4.2 狀態管理（Pinia Stores）
- `useAuthStore` — 登入狀態、Token、使用者資訊
- `useAttendanceStore` — 打卡紀錄、今日狀態
- `useLeaveStore` — 請假申請、簽核列表、餘額
- `useOvertimeStore` — 加班申請、簽核列表
- `useUserStore` — 使用者管理（Admin）

### 4.3 UI 元件庫
- 使用 Tailwind CSS 打造一致化 UI
- 核心共用元件：Button, Input, Modal, Table, DatePicker, StatusBadge, Pagination

---

## 5. 安全性設計

### 5.1 認證機制
- Spring Security + JWT
- Access Token 有效期 2 小時
- Refresh Token 有效期 7 天
- BCrypt 密碼加密

### 5.2 授權控制
- `@PreAuthorize("hasRole('ADMIN')")` 管理者 API
- `@PreAuthorize("hasRole('USER')")` 一般使用者 API
- 簽核權限：僅直屬主管或指定代理人可簽核

### 5.3 其他安全措施
- CORS 設定：僅允許前端開發域名
- SQL Injection 防護：JPA 參數化查詢
- XSS 防護：前端輸入轉義 + 後端驗證
- Soft Delete：使用者停用而非刪除

---

## 6. 商業邏輯規則

### 6.1 打卡規則
- 每天只能打卡一次（user_id + DATE(clock_in) 唯一）
- 遲到判定：clock_in > 09:00（可配置）
- 早退判定：clock_out < 18:00（可配置）
- 忘記打卡由 Admin 手動補登（status = FORGOT）

### 6.2 請假規則
- 請假天數不可超過剩餘額度
- 代理人指定：請假期間，待簽核事項自動移轉給代理人
- 取消請假：僅 PENDING 狀態可取消

### 6.3 加班規則
- 事前申請（PRE_APPLY）與事後補報（POST_REPORT）
- 補休結算：8 小時加班 = 1 天補休（可配置），簽核通過後自動計入 leave_balances
- 加班時數 = end_time - start_time

### 6.4 簽核流程
- 單層簽核：申請人 → 直屬主管
- 代理人機制：主管請假時，待簽核事項移轉給代理人
