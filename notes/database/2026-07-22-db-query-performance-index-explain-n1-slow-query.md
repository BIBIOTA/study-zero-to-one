# DB 查詢效能核心：Index 失效、EXPLAIN 判讀、N+1 與慢查詢定位流程

> 學習日期：2026-07-22
> 涵蓋概念：B-Tree Index、Index 失效條件、EXPLAIN、N+1、Slow Query Log、慢查詢定位流程

---

## 整體診斷架構

```mermaid
flowchart TD
    A["功能感覺很慢"] --> B{入口}

    B --> C["生產環境主動監控"]
    B --> D["開發 / 已知功能慢"]

    C --> C1["Slow Query Log\n找出嫌疑 query\n閾值建議 ~1s"]
    C1 --> E

    D --> D1["Application Query Log\nClockwork / Telescope"]
    D1 --> D2{同結構 query\n大量重複？}
    D2 -->|是| D3["N+1 問題\n→ Eager Loading 修掉"]
    D2 -->|否| E

    E["EXPLAIN 分析"]
    E --> F["看 type / rows / key / possible_keys"]
    F --> G{type?}
    G -->|ALL + rows 大| H["全表掃描\n考慮加 index 或改寫條件"]
    G -->|ALL + rows 小| I["小表全掃\n可接受，不用處理"]
    G -->|range / ref| J["有走 index\n確認 key 欄位"]
    H --> K{key 是 NULL\npossible_keys 有值？}
    K -->|是| L["Optimizer 主動放棄 index\n檢查 cardinality 或查詢寫法"]
    K -->|否| M["沒有可用 index\n新增 index"]
```

---

## B-Tree Index 的本質

Index 是一本**按原始欄位值排好序的目錄**，以 B-Tree 結構儲存。查詢時從根節點往下走，每一層砍掉一半，達到 O(log n) 的時間複雜度——用儲存空間換取查詢速度。

```mermaid
graph TD
    subgraph "B-Tree Index（按原始值排序）"
        R["2024-01-15"]
        L["2024-01-01"]
        R2["2024-02-01"]
        LL["2024-01-01 00:03:22 → PK=5"]
        LR["2024-01-01 08:15:00 → PK=12"]
        RL["2024-01-20 14:00:00 → PK=7"]
        RR["2024-02-10 09:00:00 → PK=3"]

        R --> L
        R --> R2
        L --> LL
        L --> LR
        R2 --> RL
        R2 --> RR
    end
```

> **InnoDB Secondary Index 注意**：葉節點存的是**主鍵值（PK）**，不是 row 的實體位址。找到葉節點後還需回到 Clustered Index 取得完整資料列（稱為「回表」）。若查詢所需的欄位全都在 index 內（Covering Index），則可省略回表，此時即使 cardinality 低，Optimizer 也可能選擇走 index。

### Index 失效的三種情況

| 情況 | 範例 | 原因 |
|------|------|------|
| 對欄位套函式 | `WHERE date(created_at) = '2024-01-01'` | B-Tree 存原始值，函式轉換後無法直接對應目錄位置 |
| 低 cardinality | 欄位只有 `true/false` 兩種值 | Optimizer 估算走 index 再回表的成本 > 全表掃描，主動放棄（若為 covering index 不需回表，即使 cardinality 低仍可能走 index） |
| 複合 index 順序不符 | 跳過最左欄直接查第二欄 | B-Tree 按左到右欄位排序，跳過第一欄目錄就失效（MySQL 8.0+ 有 Index Skip Scan 例外，但條件限制較多） |

### 函式讓 Index 失效——修正寫法

```sql
-- ❌ 對欄位套函式：B-Tree 無法跳頁，退化成全表掃描
WHERE date(created_at) = '2024-01-01'

-- ✅ 把函式移到值這側：欄位保持原始值，B-Tree 可做範圍掃描
WHERE created_at >= '2024-01-01 00:00:00'
  AND created_at < '2024-01-02 00:00:00'
```

**關鍵原則**：讓欄位側保持原始值，轉換只發生在比對值那側。

---

## EXPLAIN 判讀順序

```mermaid
flowchart LR
    A["執行 EXPLAIN"] --> B["看 type\n掃描方式"]
    B --> C["看 key\n實際走的 index"]
    C --> D["看 possible_keys\n可用但未用的 index"]
    D --> E["看 rows\n估算掃描成本"]
```

### 各欄位含義

**`type`（掃描方式，由差到好）**

| 值 | 意義 | 需要處理？ |
|----|------|-----------|
| `ALL` | 全表掃描 | 配合 `rows` 判斷 |
| `range` | index 範圍掃描 | 通常 OK |
| `ref` | 非唯一索引等值查詢（可能匹配多列） | 很好 |
| `eq_ref` | JOIN 中走唯一索引，每次最多一列 | 很好 |
| `const` | 主鍵/唯一鍵等值查詢，planning 時視為常數 | 最好 |

**`rows`**：Optimizer 估算需掃描的列數。`type: ALL` 時：
- `rows: 3` → 小表全掃，沒問題
- `rows: 500000` → 真正的效能問題

**`key` vs `possible_keys`**

```
possible_keys = Optimizer 掃過 schema 後，理論上可以用的 index 清單
key           = Optimizer 最終選擇走的 index
```

- `key` 有值：有走 index
- `key: NULL`，`possible_keys` 有值：Optimizer **主動放棄** index（通常是 cardinality 太低）
- `key: NULL`，`possible_keys` 也是 NULL：**沒有可用的 index**，需要新增

---

## N+1 問題

### 問題本質

N+1 是**次數**問題，不是單次效能問題。

```mermaid
sequenceDiagram
    participant App
    participant DB

    Note over App,DB: ❌ N+1（101 queries for 100 posts）

    App->>DB: SELECT * FROM posts （1 query）
    DB-->>App: 100 筆 posts

    loop 每一筆 post
        App->>DB: SELECT * FROM comments WHERE post_id = N
        DB-->>App: 該 post 的 comments
    end

    Note over App,DB: ✅ Eager Loading（2 queries）

    App->>DB: SELECT * FROM posts （1 query）
    DB-->>App: 100 筆 posts
    App->>DB: SELECT * FROM comments WHERE post_id IN (1,2,...,100)
    DB-->>App: 所有 comments 一次回來
```

### 為什麼 EXPLAIN 看不出 N+1

每個 query 本身結構正確、走 index、執行速度快——EXPLAIN 顯示完全正常。問題在於這個 query 被打了 100 次，EXPLAIN 只看得到單次。

### 診斷訊號

用 Clockwork / Laravel Telescope / Debugbar 看 request 級別的 query log：

> **訊號**：同結構的 query 大量重複，只有參數不同（`post_id = 1`, `post_id = 2`, ...）

### 解法

```php
// ❌ 會產生 N+1
$posts = Post::all();
foreach ($posts as $post) {
    $post->comments; // 每次存取觸發一個新 query
}

// ✅ Eager Loading
$posts = Post::with('comments')->get(); // 2 queries
```

---

## Slow Query Log

MySQL 的慢查詢記錄機制，把執行時間超過閾值的 query 寫入 log。

| 項目 | 說明 |
|------|------|
| 預設閾值 | 10 秒（太高，實務上應調低） |
| 建議閾值 | 1 秒以下（生產環境視情況調整） |
| 設定參數 | `long_query_time` |
| 能抓到 N+1？ | **不能**——每個 query 都很快，不會超過閾值 |

**Slow query log 是生產環境的嫌疑犯清單，適合主動監控；Application query log 才能抓 N+1。**

---

## 慢查詢定位流程（完整版）

```mermaid
flowchart TD
    subgraph "生產環境：主動監控入口"
        P1["Slow Query Log\n設閾值 ~1s\n找出實際耗時的 query"]
        P1 --> P2["對該 query 跑 EXPLAIN"]
    end

    subgraph "開發環境：功能已知慢"
        D1["Clockwork / Telescope\n看 request 的 query 清單"]
        D1 --> D2{同結構 query 重複？}
        D2 -->|"是 → N+1"| D3["加 eager loading\nwith('relation')"]
        D2 -->|"否 → 單次慢"| P2
    end

    P2["EXPLAIN 分析"] --> P3["type + rows 判斷掃描成本"]
    P3 --> P4["key / possible_keys 判斷 index 使用狀況"]
    P4 --> P5{根因}
    P5 -->|"函式包欄位"| S1["改寫查詢條件\n函式移到值這側"]
    P5 -->|"缺少 index"| S2["新增適當 index"]
    P5 -->|"Optimizer 放棄 index"| S3["檢查 cardinality\n或 force index 測試"]
```

---

## 學習過程的關鍵卡點

**卡點一：知道函式會讓 index 失效，但不知道為什麼**

**原本以為**：`date()` 是某種特殊函式，DB 對它特別處理所以用不了 index。

**實際上**：根本原因是 B-Tree 存的是**欄位的原始值**。對欄位套任何函式，DB 都必須對每一列先算一次函式結果才能比對，沒辦法利用排好序的目錄結構跳頁——不是函式本身的問題，是「轉換發生在欄位這側」讓目錄失效。

這個卡點值得記：知道修法（把函式移到值側）之前，先搞懂**目錄失效的機制**，才不會在其他類似情境（如 `YEAR(created_at)`, `LOWER(email)`）又踩一次。

---

**卡點二：以為 EXPLAIN 是萬能的慢查詢偵測工具**

**原本以為**：所有查詢效能問題都能用 EXPLAIN 找到。

**實際上**：EXPLAIN 只分析**單一 query 的執行計畫**。N+1 的問題不在某一個 query 的執行方式，而在於這個 query 被重複執行了 N 次——EXPLAIN 完全正常，但整體效能很差。需要 request 級別的 query log 工具（Clockwork / Telescope）才能看到「次數」這個維度。

---

**卡點三：slow query log 應該是診斷終點，還是起點？**

**原本以為**：先排除 N+1、跑 EXPLAIN 之後，最後才看 slow query log。

**實際上**：在生產環境，你**不知道哪個 query 慢**——slow query log 是主動監控的入口，幫你把嫌疑犯找出來，之後再對嫌疑犯跑 EXPLAIN。在開發環境，你已知是哪個功能慢，才可能從 application log 出發。兩個入口各有適用情境。
