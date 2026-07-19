# Cache 三大失效問題：Penetration、Avalanche、Breakdown

> 學習日期：2026-07-19
> 涵蓋概念：Cache Miss、Cache Penetration、Cache Avalanche、Cache Breakdown、Bloom Filter、TTL Jitter、Mutex Lock

---

## 整體架構：三種問題的共同本質

```mermaid
flowchart TD
    REQ["使用者請求"] --> CACHE{"Cache 有資料？"}
    CACHE -- "有（Cache Hit）" --> RETURN["直接回傳，不打 DB"]
    CACHE -- "沒有（Cache Miss）" --> DB["打 DB 查詢"]
    DB --> WRITE["結果寫回 Cache"]
    DB --> RETURN2["回傳結果"]

    P["⚠️ Penetration\n查不存在的資料\n永遠 Cache Miss"] --> DB
    A["⚠️ Avalanche\n大量 Key 同時過期\n瞬間大量 Cache Miss"] --> DB
    B["⚠️ Breakdown\n單一熱點 Key 過期\n大量並發 Cache Miss"] --> DB

    DB --> OVERLOAD["DB 被打爆"]
```

三種問題有共同的破壞模式：**大量請求繞過 Cache 層直接打到 DB**，差異在於觸發原因不同。

---

## Cache Miss（正常現象）

首次查詢某筆資料時，Cache 裡沒有，打 DB 取得後寫回 Cache。下次同樣的查詢就能直接從 Cache 回傳。

這是 Cache 的正常運作流程，不是問題。問題在於「異常大量的 Cache Miss 集中發生」的三種場景。

---

## Cache Penetration（快取穿透）

### 問題

查詢一筆**根本不存在**的資料（例如 `user_id = -1`）。Cache 裡永遠不會有，DB 查了也沒有，結果也沒有可以寫回 Cache 的值。下次同樣的查詢，一樣穿透到 DB。

如果惡意用腳本大量查詢不存在的 ID，每一筆都直接打 DB，DB 承受大量無效查詢。

### 解法

```mermaid
flowchart TD
    REQ["查詢 user_id = -1"]

    subgraph "解法一：Cache Null Value"
        CN1["DB 查無結果"]
        CN2["將『查無此資料』也 cache 住\n短 TTL，如 60 秒"]
        CN3["下次同樣查詢直接從 Cache 回傳空結果"]
        CN1 --> CN2 --> CN3
    end

    subgraph "解法二：Bloom Filter"
        BF1["請求先過 Bloom Filter"]
        BF2{"這個 ID 可能存在？"}
        BF3["才去查 Cache / DB"]
        BF4["直接回傳不存在，不查 DB"]
        BF1 --> BF2
        BF2 -- "可能存在" --> BF3
        BF2 -- "一定不存在" --> BF4
    end
```

| 解法 | 原理 | 適用情境 |
|------|------|---------|
| Cache Null Value | 把空結果也 cache 住，下次直接回空 | 簡單場景，key 種類不多 |
| Bloom Filter | 在打 Cache/DB 前先過濾掉必定不存在的 key | 高流量、key 種類極多的場景 |

> Bloom Filter 有 **false positive**（過濾器說「可能存在」，但查 DB 仍可能查無資料），無法完全阻止所有無效查詢。通常搭配 Cache Null Value 作為第二道防線，兩者互補。

---

## Cache Avalanche（快取雪崩）

### 問題

大量 Cache Key 在**同一時間**集體過期，導致瞬間大量 Cache Miss，所有請求同時打到 DB，輕則 DB 變慢，重則 DB 當機。

常見觸發場景：系統啟動時一次性把大量 Key 載入 Cache，且都設了相同的 TTL，時間到了就一起失效。

### 解法

```mermaid
flowchart LR
    subgraph "❌ 問題：統一 TTL"
        K1["Key A: TTL 3600s"] --> EX["3600s 後同時過期"]
        K2["Key B: TTL 3600s"] --> EX
        K3["Key C: TTL 3600s"] --> EX
        EX --> DB1["瞬間大量請求打 DB"]
    end

    subgraph "✅ 解法：TTL + Random Jitter"
        K4["Key A: TTL 3600 ± 隨機 0~600s"]
        K5["Key B: TTL 3600 ± 隨機 0~600s"]
        K6["Key C: TTL 3600 ± 隨機 0~600s"]
        K4 & K5 & K6 --> SPREAD["過期時間分散開來"]
        SPREAD --> DB2["DB 請求平均分布，不爆"]
    end
```

**核心手法**：在基礎 TTL 上加上隨機的 jitter（偏移量），讓各個 Key 的過期時間錯開，避免集中失效。

---

## Cache Breakdown（快取擊穿）

### 問題

**單一熱點 Key** 過期的瞬間，大量並發請求同時發現 Cache Miss，全部同時打到 DB 查詢同一筆資料。

與 Avalanche 的差異：

| | Cache Avalanche | Cache Breakdown |
|-|----------------|----------------|
| Key 數量 | 大量 Key 同時失效 | 單一熱點 Key 失效 |
| 觸發規模 | 整體流量爆增 | 熱點資料的並發爆增 |
| 典型情境 | 系統重啟、批次載入 | 明星頁面、爆紅商品詳情 |

### 解法：Mutex Lock（互斥鎖）

```mermaid
sequenceDiagram
    participant R1 as 請求 #1
    participant R2 as 請求 #2 ~ #N
    participant Cache
    participant DB

    R1->>Cache: 查詢（Cache Miss）
    R2->>Cache: 查詢（Cache Miss）
    R1->>R1: 搶到 Lock
    R2->>R2: 等待 Lock 釋放
    R1->>DB: 查詢 DB
    DB-->>R1: 回傳資料
    R1->>Cache: 寫回 Cache
    R1->>R1: 釋放 Lock
    R2->>Cache: 再次查詢（Cache Hit）
    Cache-->>R2: 直接回傳，不再打 DB
```

只讓第一個搶到 Lock 的請求去 DB 查詢並回寫 Cache，其他請求等待 Lock 釋放後直接從 Cache 拿結果，不再打 DB。

> Lock 必須設定超時（TTL），避免持有 Lock 的請求異常時造成死鎖。等待的請求通常以短暫 sleep + retry 的方式輪詢 Cache，直到取得資料。

---

## 三種問題對比總覽

| | Penetration 穿透 | Avalanche 雪崩 | Breakdown 擊穿 |
|-|----------------|---------------|--------------|
| **根本原因** | 查不存在的資料 | 大量 Key 同時過期 | 單一熱點 Key 過期 |
| **Cache Miss 特徵** | 永遠 Miss（查無此資料） | 瞬間大量 Miss | 熱點並發 Miss |
| **主要解法** | Cache Null / Bloom Filter | TTL + Random Jitter | Mutex Lock |
| **常見觸發場景** | 惡意攻擊、異常查詢 | 系統啟動、批次載入 | 爆紅熱點資料 |

---

## 學習過程的關鍵卡點

**原本以為**：Cache 就是快取資料加速查詢，了解 Cache Miss 就差不多了，沒想到 Cache 本身也有三種會打垮 DB 的失效問題。

**實際上**：Cache Penetration、Avalanche、Breakdown 都是「Cache Miss 集中爆發」，但觸發原因不同，解法方向也完全不同——Penetration 要防止無效查詢打到 DB；Avalanche 要把過期時間分散；Breakdown 要用鎖讓並發請求排隊，只有第一個打 DB。理解這三種問題，才能在設計 Cache 策略時預防它們。
