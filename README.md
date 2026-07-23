# study-zero-to-one

個人技術學習筆記倉庫。每篇筆記都來自一段蘇格拉底式引導的學習對話，目標不是背定義，而是把「自己推導出來的理解」留下來。

## 筆記索引

### Message Queue

- [MQ 核心術語：從 Queue 到 DLQ 的完整設計邏輯](./notes/mq/2026-07-01-mq-core-terminology.md)
- [Kafka、RabbitMQ、Laravel Queue 核心概念與選型差異](./notes/mq/2026-07-02-kafka-rabbitmq-deep-dive.md)
- [Consumer 失敗處理全流程：Retry Strategy、Failed Job 與 DLQ](./notes/mq/2026-07-03-mq-consumer-failure-retry-dlq.md)
- [冪等性與訊息傳遞語意：Idempotency、At-least-once、Exactly-once](./notes/mq/2026-07-06-idempotency-delivery-semantics.md)
- [Idempotency Key、processed_messages 與 Outbox Pattern 的 Schema 設計與取捨](./notes/mq/2026-07-07-idempotency-outbox-schema.md)
- [Kafka Producer、Consumer、Partition 與 At-least-once Delivery](./notes/mq/2026-07-23-kafka-producer-consumer-partition.md)

### Python

- [Python 內建容器完全解析：list、tuple、set、dict 的設計邏輯與時間複雜度](./notes/python/2026-07-06-python-builtin-containers.md)
- [Python 非同步完全指南：Event Loop、asyncio 與並發模型](./notes/python/2026-07-11-python-async-asyncio-event-loop.md)
- [Python asyncio.Queue 與 asyncio.Semaphore：in-process 並發協調](./notes/python/2026-07-13-python-asyncio-queue-semaphore.md)

### Java

- [Java OOP 核心：Interface、Abstract Class、Generics、Method Overloading](./notes/java/2026-07-15-java-oop-interface-generics-overloading.md)
- [Java Collections 與 Stream API：List、Map、Set 與 Pipeline 思維](./notes/java/2026-07-15-java-collections-stream-api.md)
- [Java 建置環境：Maven 的依賴、編譯、打包與語法總檢核](./notes/java/2026-07-16-java-maven-build-env.md)
- [Spring Boot 心智模型：IoC Container、DI 與啟動流程](./notes/java/2026-07-17-spring-boot-ioc-di-mental-model.md)
- [Spring Boot Web Layer 基礎：Controller、Config 與 Bean Validation](./notes/java/2026-07-18-spring-boot-web-layer-basics.md)
- [Spring Boot JPA 入門：Entity、Repository 與 MySQL 連線設定](./notes/java/2026-07-19-spring-boot-jpa-entity-repository.md)
- [Spring Boot 核心概念：@Component 家族、Laravel 對照與啟動流程](./notes/java/2026-07-20-spring-boot-core-concepts.md)
- [Spring Boot CRUD 三層架構與 DTO 設計](./notes/java/2026-07-21-spring-boot-crud-three-layer-dto.md)
- [Spring Boot 例外處理、交易管理與單元測試](./notes/java/2026-07-22-spring-boot-exception-transaction-testing.md)

### Database

- [Database Index 與 Slow Query：從 B-Tree 到 EXPLAIN](./notes/database/2026-07-19-database-index-slow-query.md)
- [DB 查詢效能核心：Index 失效、EXPLAIN 判讀、N+1 與慢查詢定位流程](./notes/database/2026-07-22-db-query-performance-index-explain-n1-slow-query.md)
- [Index 副作用與生產環境 DDL 鎖表風險](./notes/database/2026-07-23-database-index-trade-off-ddl-lock.md)

### Cache

- [Cache 三大失效問題：Penetration、Avalanche、Breakdown](./notes/cache/2026-07-19-cache-three-failure-modes.md)

### System Design

- [雲端部署基礎：VPC、Subnet、Security Group、Load Balancer、RDS、Redis](./notes/system-design/2026-07-08-cloud-deployment-basics.md)
- [部署邊界與責任：API / Worker / MQ / DB 怎麼切](./notes/system-design/2026-07-09-deployment-boundaries-api-worker-db-mq.md)
- [雲端運算選型：EC2、ECS、Lambda 的管理責任邊界](./notes/system-design/2026-07-10-cloud-deployment-ec2-ecs-lambda.md)
- [雲端監控與擴縮規則：Health Check、Auto Scaling、Queue Length Scaling](./notes/system-design/2026-07-13-health-check-auto-scaling-queue-length-scaling.md)
- [前端 API Request 的雲端旅程：從瀏覽器到後端完整路徑](./notes/system-design/2026-07-14-frontend-api-request-cloud-journey.md)
- [Observability 四柱：Logs、Metrics、Tracing、Alert](./notes/system-design/2026-07-15-observability-logs-metrics-tracing-alert.md)
- [Observability 故障排查實戰：Structured Log、Request ID、Correlation ID](./notes/system-design/2026-07-16-observability-structured-logs-request-correlation-id.md)
- [Observability Day 3：監控指標設計——API Latency、Error Rate 到 Queue 三劍客](./notes/system-design/2026-07-17-observability-metrics-design.md)
- [Observability 故障排查心法：API Latency、Error Rate、Queue Lag 與 SLI/SLO/SLA](./notes/system-design/2026-07-18-observability-incident-investigation.md)
- [Observability 故障排查 Checklist：Worker 失敗、DB Timeout、MQ Delay](./notes/system-design/2026-07-20-observability-troubleshooting-checklist.md)
- [通知系統延遲排查與 Distributed Tracing：P50/P95、Queue Depth、Trace、Span](./notes/system-design/2026-07-21-notification-latency-distributed-tracing.md)

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
