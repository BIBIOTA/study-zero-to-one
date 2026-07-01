# CLAUDE.md

## 專案用途

這是一個個人技術學習倉庫，用來存放透過蘇格拉底式引導學習後整理的技術筆記。

## 目錄結構

```
study-zero-to-one/
├── notes/                  # 學習筆記，依主題分類
│   ├── mq/                 # Message Queue 相關
│   ├── database/           # 資料庫相關
│   ├── algorithm/          # 演算法與資料結構
│   ├── network/            # 網路協定
│   ├── cache/              # 快取策略
│   ├── ai/                 # LLM、RAG、向量資料庫
│   ├── system-design/      # 系統設計
│   ├── security/           # 資安
│   └── misc/               # 跨類或其他
└── .claude/
    └── skills/
        ├── study-practice/ # 蘇格拉底式學習引導 skill
        └── study-notes/    # 學習對話 → 技術筆記 skill
```

## Skills

### `/study-practice`
啟動蘇格拉底式學習引導。不直接講解，透過提問讓使用者自己推導出正確理解。

### `/study-notes`
將 study-practice 的學習對話整理成 Markdown + Mermaid 技術筆記，自動推斷分類並存到對應的 `notes/<category>/` 目錄。筆記寫入後，自動派發一個獨立 agent 進行技術正確性檢核，並將檢核報告一併回報。

## 筆記命名規則

```
notes/<category>/YYYY-MM-DD-<kebab-case-slug>.md
```

## 筆記格式

- Markdown + Mermaid 圖表
- Header 包含學習日期與涵蓋概念
- 必須包含「學習過程的關鍵卡點」章節，記錄原本的誤解與釐清後的理解
