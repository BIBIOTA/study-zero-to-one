# MQ 核心術語：從 Queue 到 DLQ 的完整設計邏輯

> 學習日期：2026-07-01
> 涵蓋概念：Queue、Topic、Producer、Consumer、ACK、Retry、DLQ

---

## 整體架構一眼看懂

```mermaid
graph LR
    P([Producer]) -->|發送訊息| B[Broker]
    B --> Q1[Queue / Topic]
    Q1 -->|分配| C1([Consumer 1])
    Q1 -->|分配| C2([Consumer 2])
    C1 -->|ACK 成功| B
    C1 -->|NACK 失敗| R{Retry?}
    R -->|次數未超限| Q1
    R -->|超過上限| DLQ[Dead Letter Queue]
```

---

## 基礎架構

### Queue vs Topic：一對一 vs 一對多

這是最常被混淆的第一個概念。兩者都是「訊息的容器」，但消費模型根本不同。

```mermaid
graph TB
    subgraph Queue["Queue 模式（競爭消費）"]
        direction LR
        P1([Producer]) --> Q[Queue]
        Q -->|訊息 A| C1([Consumer 1])
        Q -->|訊息 B| C2([Consumer 2])
    end

    subgraph Topic["Topic 模式（廣播訂閱）"]
        direction LR
        P2([Producer]) --> T[Topic]
        T -->|完整副本| G1[Consumer Group 1]
        T -->|完整副本| G2[Consumer Group 2]
    end
```

| 維度 | Queue | Topic |
|------|-------|-------|
| 消費模型 | 競爭消費，每則訊息只被一個 Consumer 處理 | 廣播，每個 Consumer Group 都收到完整訊息 |
| 適用情境 | 任務派發、一次性處理 | 多個獨立服務訂閱同一事件流 |
| 典型代表 | RabbitMQ、SQS | Kafka、RocketMQ |

**核心記憶點：**
- Queue → 「大家搶著做同一批工作」
- Topic → 「同一個事件廣播給多個不同部門各自知道」

### Partition（分區）

Topic 底下的實際儲存單位（Kafka 用語），是 Kafka 實現水平擴展與並行消費的基本單位。每個 Partition 由一個 Broker 負責（Leader），Producer 依據 key 或輪詢策略決定訊息要寫入哪個 Partition，**同一 Partition 內保序，跨 Partition 不保序**。

```mermaid
graph LR
    T[Topic] --> PA[Partition A]
    T --> PB[Partition B]
    T --> PC[Partition C]
    PA -->|保序消費| C1([Consumer 1])
    PB -->|保序消費| C2([Consumer 2])
    PC -->|保序消費| C3([Consumer 3])
```

---

## 角色定義

| 角色 | 說明 |
|------|------|
| **Producer（生產者）** | 發送訊息到 Queue/Topic 的一方，通常是上游服務或 API Server |
| **Broker** | MQ 系統的伺服器節點，負責儲存與轉發訊息 |
| **Consumer（消費者）** | 從 Queue/Topic 取出並處理訊息的一方 |
| **Consumer Group** | 一組 Consumer 共同分攤消費同一 Topic；**同一 Partition 在同個 Group 內只會被一個 Consumer 消費**，避免重複處理 |

---

## 訊息保證機制

### ACK（確認機制）

ACK 是 Consumer 告知 Broker「訊息已成功處理完成」的信號；Broker 收到後才會標記訊息已消費或將其移除。沒有 ACK，Broker 無從得知訊息有沒有被成功消費。

```mermaid
sequenceDiagram
    participant B as Broker
    participant C as Consumer

    B->>C: 投遞訊息
    C->>C: 執行業務邏輯

    alt 處理成功
        C->>B: ACK（已處理完成）
        B->>B: 標記訊息已消費 / 移除
    else 處理失敗
        C->>B: NACK（明確告知失敗）
        B->>B: 依設定重新入列 or 轉入 DLQ
    end
```

### Auto-ack vs Manual-ack

| 模式 | 時機 | 風險 |
|------|------|------|
| **Auto-ack** | 訊息一送達 Consumer 就視為成功 | Consumer 收到訊息後若當機，訊息遺失 |
| **Manual-ack** | 業務邏輯執行完成後才手動發送 ACK | 較安全，但需要自行處理重複消費的冪等問題 |

### 三種傳遞語意

| 語意 | 說明 | 代價 |
|------|------|------|
| **At-most-once** | 最多一次，可能遺失但不重複 | 最低（不需重試或 ACK 確認） |
| **At-least-once** | 至少一次，不遺失但可能重複 | 中（需 ACK + Retry，Consumer 需具備冪等性） |
| **Exactly-once** | 恰好一次，不重複不遺失 | 最高（需分散式事務或冪等設計） |

---

## 錯誤處理

### Retry 策略

Consumer 失敗後，讓訊息重新被消費的機制。關鍵是**錯誤類型的判斷發生在進入下一次重試之前**：

```mermaid
flowchart TD
    F[Consumer 處理失敗] --> J{判斷錯誤類型}
    J -->|可重試\n網路 timeout / 5xx| R{重試次數 < 上限?}
    J -->|不可重試\n格式錯誤 / 資料驗證失敗| D[直接轉入 DLQ]
    R -->|是| W[等待（指數退避）]
    W --> C[重新消費]
    R -->|否| D
```

**指數退避（Exponential Backoff）**：每次重試等待時間加倍（例如 1s → 2s → 4s → 8s → 16s），避免失敗服務被持續轟炸，也讓暫時性故障有時間自行恢復。

### DLQ（Dead Letter Queue，死信佇列）

DLQ 是訊息在 Retry 機制所有機會用完後的去處，是**系統自動化能力的終點**。

```mermaid
flowchart LR
    M([原始訊息]) --> RQ[原始 Queue]
    RQ --> C([Consumer])
    C -->|失敗 + 重試次數耗盡| DLQ[Dead Letter Queue]
    C -->|不可重試錯誤| DLQ
    DLQ --> H([人工介入])
    H -->|修正後重送| RQ
```

**為什麼需要 DLQ？**

如果沒有 DLQ，失敗的訊息只有兩個下場：
1. 直接丟棄 → 資料遺失，且沒有任何痕跡讓人知道需要修正
2. 無限重試 → 卡住整個 Queue，阻塞後續所有正常訊息（Poison Message Problem）

**重要觀念釐清：DLQ 階段不再做自動判斷**

走到 DLQ 的訊息，代表：
- 錯誤類型的判斷（可重試 vs 不可重試）**已在 Retry 階段完成**
- 所有的自動重試機會都已耗盡
- 剩下的，通常是程式邏輯本身的問題（格式錯誤、邊界情況未處理），而非暫時性的服務中斷

因此，DLQ 裡的訊息**預設就是需要人工介入**——排查程式邏輯錯誤、修正資料，再視情況重新送回原 Queue 重新處理。

| | Retry 階段 | DLQ 階段 |
|-|-----------|---------|
| **主要任務** | 自動恢復暫時性故障 | 保存失敗訊息供人工排查 |
| **執行方式** | 系統自動（依錯誤類型決定） | 人工介入 |
| **判斷依據** | 錯誤類型（可重試 / 不可重試）、重試次數 | 無自動判斷，一律轉人工 |

**DLQ 的可觀測性價值**：監控 DLQ 訊息數量的異常增長，可以及早發現系統性的程式錯誤，而不是等到客訴或資料不一致才發現問題。

---

## 快速記憶脈絡

```mermaid
mindmap
  root((MQ))
    架構
      Queue 競爭消費
      Topic 廣播訂閱
      Partition 分區保序
    角色
      Producer 上游發送
      Broker 中介儲存
      Consumer 下游消費
      Consumer Group 分攤消費
    保證機制
      ACK 確認已處理
      NACK 明確告知失敗
      At-most-once
      At-least-once
      Exactly-once
    錯誤處理
      Retry 指數退避
      DLQ 系統終點
      人工介入
```

三句話記住核心關係：
1. **Queue vs Topic**：Queue 是「搶工作」，Topic 是「廣播通知」
2. **ACK 是信任確認**：沒有 ACK，Broker 不知道訊息是否真的處理完成
3. **Retry + DLQ 是一組**：Retry 救暫時性失敗，DLQ 兜底持續性失敗，兩者搭配才能避免訊息無限循環或無聲遺失

---

## 學習過程的關鍵卡點

這次學習中最值得記住的一個認知修正：

**原本以為**：走到 DLQ 的訊息還可以進一步分流，一部分自動重試（服務中斷型）、一部分人工處理（程式錯誤型）。

**實際上**：這個分流判斷（依錯誤類型決定要不要重試）發生在 Retry 階段，不是 DLQ 階段。能自動解決的早就在 DLQ 之前被處理掉了——真正走到 DLQ 的訊息，系統已經沒有更多自動化手段，預設就是交給人工介入。

這個卡點也提醒了一個重要的設計思維：**系統架構中的每個「兜底機制」都有它的邊界**，DLQ 是 Retry 的邊界，Retry 是正常消費的邊界，每一層都清楚地知道自己能做什麼、不能做什麼。
