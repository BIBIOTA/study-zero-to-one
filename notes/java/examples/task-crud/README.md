# Task CRUD — 三層架構範例

Day 8 學習範例，示範 Spring Boot 的 Controller → Service → Repository 分層，以及 DTO 的進出控制。

## 專案結構

```
src/main/java/com/example/taskcrud/
├── controller/
│   └── TaskController.java     # HTTP 路由，接收 Request DTO、回傳 Response DTO
├── service/
│   └── TaskService.java        # 業務邏輯，呼叫 Repository
├── repository/
│   └── TaskRepository.java     # 繼承 JpaRepository，自動實作 CRUD
├── mapper/
│   └── TaskMapper.java         # Entity ↔ DTO 轉換
├── dto/
│   ├── TaskRequest.java        # 進來：只接收可修改的欄位
│   └── TaskResponse.java       # 出去：只暴露對外安全的欄位
└── entity/
    └── Task.java               # JPA Entity，對應 tasks 資料表
```

## API

| Method | Path        | 說明         |
|--------|-------------|--------------|
| GET    | /tasks      | 取得全部 Task |
| GET    | /tasks/{id} | 取得單筆 Task |
| POST   | /tasks      | 建立 Task     |
| PUT    | /tasks/{id} | 更新 Task     |
| DELETE | /tasks/{id} | 刪除 Task     |

## 啟動前

1. 建立資料庫：`CREATE DATABASE task_crud_db;`
2. 修改 `application.properties` 中的帳號密碼
3. `ddl-auto=update` 會自動建立 `tasks` 資料表
