# study-zero-to-one

個人技術學習筆記倉庫。每篇筆記都來自一段蘇格拉底式引導的學習對話，目標不是背定義，而是把「自己推導出來的理解」留下來。

## 筆記索引

### Message Queue

- [MQ 核心術語：從 Queue 到 DLQ 的完整設計邏輯](./notes/mq/2026-07-01-mq-core-terminology.md)
- [Kafka、RabbitMQ、Laravel Queue 核心概念與選型差異](./notes/mq/2026-07-02-kafka-rabbitmq-deep-dive.md)
- [Consumer 失敗處理全流程：Retry Strategy、Failed Job 與 DLQ](./notes/mq/2026-07-03-mq-consumer-failure-retry-dlq.md)
- [冪等性與訊息傳遞語意：Idempotency、At-least-once、Exactly-once](./notes/mq/2026-07-06-idempotency-delivery-semantics.md)

### Python

- [Python 內建容器完全解析：list、tuple、set、dict 的設計邏輯與時間複雜度](./notes/python/2026-07-06-python-builtin-containers.md)

## 學習流程

```
/study-practice  →  學習對話  →  /study-notes  →  筆記存檔 + 自動 review
```

1. `/study-practice`：啟動蘇格拉底式引導，透過提問推導出正確理解
2. 貼上對話記錄後執行 `/study-notes`：自動整理成 Markdown + Mermaid 筆記，並由獨立 agent 進行技術正確性檢核

## 目錄結構

```
notes/
├── mq/
├── database/
├── algorithm/
├── network/
├── cache/
├── ai/
├── system-design/
├── security/
└── misc/
```
