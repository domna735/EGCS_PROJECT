# 📘 **Development Plan.md — Genesys Cloud → Cloud Migration System (venvA → venvB)**  

---

# 1. 專案簡介（Project Overview）

本專案旨在開發一套 **Pure Java + RuoYi Vue** 的 Genesys Cloud 環境遷移系統，  
支援從來源環境（venv A）至目標環境（venv B）的：

- 細粒度資源同步（Flow / Queue / User / Prompt）  
- 自動化 Terraform IaC 部署  
- 動態虛擬工作區隔離（避免 Destroy）  
- DAG 依賴分析  
- A/B 環境差異比對（Diff Viewer）  
- Declarative Import 衝突防禦  
- 二次驗證（Email Passcode + 核銷）  
- 完整審計與回滾（Git Bare Repo + RuoYi）  
- 部署任務狀態機（State Machine）  
- WebSocket 斷線重連補回（Re-attach）  

本 Development Plan 用於指導整個開發週期。

## 1.1 產品目標補強

本次建置要以 **Genesys Cloud BCP Wizard** 的操作體驗為目標，核心原則如下：

- 使用者只需要一次性設定每個 Genesys Cloud 環境的 `region`、`client_id`、`client_secret`。
- 系統必須支援多個 venv / organization profile，並以「來源環境 + 目標環境」的方式建立同步對映。
- 前端必須以循序式 wizard 流程引導使用者完成設定、比對、確認依賴、送審、部署與回滾。
- 後端必須先完成資料持久化與同步邏輯，再擴展 Vue 頁面，避免前後端規格脫節。

## 1.2 後端優先交付原則

第一階段只做後端的正確性與可擴充性，包含：

- 環境憑證設定與儲存
- 多 venv 對映與同步任務建立
- 資源探索、比較、依賴提示
- Workspace 與 Terraform HCL 生成
- 任務狀態、進度、審計、回滾

前端則在後端 API 定型後，依照 Genesys BCP Wizard 的 wizard 順序逐步實作。

## 1.3 v5.1 必做補強項

以下四項在本次開發中列為必做控制點，且要同時落在後端、前端與審計流程中：

- venv A 勾選資源後，先與 venv B 的同名資源做比較，若有 dependency 必須展示並讓使用者決定是否一併 migrate。
- migrate 執行期間，需有獨立的進度監控 UI，顯示正在處理與待處理資源，並提供 timeout 與重試策略。
- 一鍵 publish 前必須先做 Email Passcode 驗證，驗證成功後才能進入 apply / publish。
- 所有關鍵動作都必須寫入 audit log，包含選擇資源、比對結果、passcode 驗證、plan / apply、rollback 與失敗原因。

## 1.4 v6 對應的需求落點

你提供的 `plan v5.md`、`plan v6.md` 與 `risk_matrix.md` 對應到本專案的落地順序，應整理成以下四層：

1. **Environment Registry**：一次性設定 `region / client_id / client_secret`，支援多個 venv profile。
2. **Discovery & Tier Catalog**：把 Genesys Cloud 資源依照 Tier 1 → Tier 7 的建置順序做分類、探索與依賴分析。
3. **Migration Orchestration**：建立 compare、plan、apply、progress、passcode、audit、rollback 的完整任務生命週期。
4. **Wizard UI**：用 RuoYi Vue 實作對應 BCP Wizard 的步驟式介面與診斷頁。

---

# 1.5 資料設計方向（Database First）

為了支援多 venv、審計、回滾與日後擴充，後端需要先建立以下資料概念：

- `migration_env_profile`：每個 Genesys Cloud 環境的設定檔，保存 region、client_id、加密後的 client_secret、名稱、啟用狀態。
- `migration_env_mapping`：來源環境與目標環境的對映關係。
- `migration_job`：每次 compare / plan / apply 任務的主表。
- `migration_job_resource`：任務內勾選的資源與依賴項目。
- `migration_job_progress`：處理中 / 待處理 / 成功 / 失敗的進度資料。
- `migration_audit_log`：完整操作軌跡、錯誤與回滾紀錄。
- `migration_tier_catalog`：資源類型與 Tier 順序定義，作為 wizard 與排程順序的依據。
- `migration_resource_mapping`：跨 Org 資源映射表，處理 name / GUID 對應、衝突與同步狀態。
- `migration_snapshot`：執行前快照，支援 backup / snapshot / restore / merge。
- `migration_version_record`：Git 與資料版本追溯記錄，支援回滾與變更審計。

`client_secret` 不可明文儲存，應使用 RuoYi 既有加密機制或獨立金鑰管理層處理。

## 1.6 架構補充：多 Org、備份與版本控制

依照需求與 `risk_matrix.md` 的風險分類，EGCS 後續的核心架構應再補強以下五個面向：

1. **多 Org 遷移與同步**：以 `migration_env_profile` 定義每個 Org 的 region 與憑證，再透過 `migration_resource_mapping` 對應 source / target GUID，避免 name duplicate、ID duplicate 與跨 Org drift。
2. **Mapping Table 衝突處理**：當目標 Org 已有同名資源時，先建立 import mapping；若需要 rename，則由 UI 讓使用者決定 alias name，並在寫入 workspace 前完成替換。
3. **Backup / Snapshot / Restore / Merge**：在 Plan 前自動建立 JSON snapshot；若 apply 失敗，可由 snapshot 還原；若只需要局部同步，則以 merge 模式產出差異資源與 import block。
4. **Version Control / Rollback**：每次 apply 前後都建立版本記錄，保存 selectedResources、main.tf、snapshot 與 audit metadata，回滾時只恢復對應版本，不直接 destroy。
5. **Voice Files 本地儲存**：音檔不直接走高延遲雲端上傳流程，而是先進入本地 cache，再由後端按 hash 差異決定是否同步到 Genesys Cloud。

這一層補強的目標是把 EGCS 從「單次遷移工具」升級成「可重複、可比較、可回復」的多 Org 維運平台。

---

# 2. 系統模組拆解（System Modules）

---

## 2.1 後端模組（Spring Boot）

| 模組名稱 | 說明 | 技術 |
|---------|------|------|
| Credential Module | 管理 venv A / venv B 憑證、Region、OAuth Client | AES-256-GCM、Vault |
| Discovery Engine | 探索資源、建立 ResourceDescriptor、DAG | GC Java SDK |
| DAG Analyzer | Flow → Queue / Prompt / Skill 依賴分析 | SnakeYAML |
| Diff Engine | A/B 環境差異比對 | java-diff-utils |
| Compare & Dependency Gate | venv A / venv B 比較、dependency 提示與勾選確認 | Java Service |
| Granular Sync Engine | 處理使用者勾選資源、依賴補齊 | Java Service |
| Workspace Builder | 建立 Transient Workspace、Tier 目錄 | Java I/O |
| Declarative Import Engine | 同名資源衝突 → 自動生成 import block | Terraform 1.5+ |
| Data Source Downgrade Engine | 未勾選資源轉為 data source | HCL Template |
| De-hardcode Engine | 去除 ID、division、GUID、生成 substitutions | Regex、SnakeYAML |
| Terraform Executor | init / plan / apply（含 Timeout 防禦） | ProcessBuilder |
| Terraform State Machine | 任務狀態持久化、Apply 背景執行 | DB + Java |
| Progress Monitor Engine | 顯示處理中 / 待處理資源與 Job 進度 | DB + WebSocket |
| Passcode Verification Engine | 發送與核銷 Email Passcode | Mail + DB |
| WebSocket Log Streamer | 推送 Terraform 日誌至前端 | WebSocket |
| WebSocket Re-attach | 斷線後補回日誌 | JobStatus + Cache |
| Git Audit Engine | Commit、Tag、Rollback | JGit（路徑：`/data/git_audit/`） |
| Audit Log Engine | 紀錄操作、比對、部署、驗證與回滾事件 | DB + JSON |
| Job Lifecycle Manager | 管理 JobDescriptor、Workspace 清理策略 | DB + Java |

---

## 2.2 前端模組（RuoYi Vue）

| 模組名稱 | 說明 |
|---------|------|
| 環境管理頁 | 管理 venv A / venv B 憑證 |
| 資源 Tree Table | 顯示所有資源、支援勾選、依賴高亮 |
| DAG 拓撲預覽 | 顯示 Flow 依賴的 Queue / Prompt |
| 資源比較與依賴確認頁 | 顯示 venv A / venv B 比較結果，讓使用者確認 dependency 是否一併 migrate |
| Diff Viewer | 顯示 A/B 環境差異 |
| 部署終端（xterm.js） | 顯示 Terraform Plan / Apply 日誌 |
| 部署進度監控頁 | 顯示目前處理 / 待處理資源、Tier 進度、Job 狀態、超時提醒 |
| 驗證碼核銷頁 | 發送 Email Passcode 並完成核銷 |
| 二次驗證頁 | 輸入 Passcode |
| 審計與回滾頁 | 顯示 Git Tag、任務紀錄、回滾按鈕 |
| WebSocket Re-attach | 斷線後自動補回日誌 |

---

## 2.3 基礎設施模組（Infra）

| 模組 | 說明 |
|------|------|
| Docker Multi-Stage | 前端 Nginx + 後端 Spring Boot |
| Terraform CLI | 固定版本安裝於後端容器 |
| **/data/git_audit/** | **常駐 Git 審計裸倉庫（Bare Repo）**，託管所有 Commit / Tag |
| **/data/workspace/** | **動態臨時工作區**，任務結束後依狀態自動清理 |
| Vault | 憑證安全管理 |

---

# 3. 開發階段（Development Phases）

---

## **Phase 1：基礎環境與憑證管理**

### 交付物
- Spring Boot 專案初始化  
- RuoYi Vue 專案初始化  
- Credential Module  
- Environment Registry 與 profile mapping  
- Vault 或欄位加密整合  
- venv A / venv B 測試連線成功  

### 後端驗收標準
- 能新增、編輯、停用、查詢多個 Genesys Cloud profile。
- 能以 profile 產生同步任務的來源 / 目標對映。
- 能避免把 `client_secret` 明文寫入設定檔、log 或前端 payload。

---

## **Phase 2：Discovery Engine + DAG**

### 交付物
- ResourceDescriptor  
- DAG Analyzer  
- `/api/v1/migration/discovery`  
- `/api/v1/migration/resources`  
- Tier catalog 查詢與排序  
- 來源 / 目標環境資源樹  

### 後端驗收標準
- 能把 Genesys Cloud 資源分類到 Tier 1 → Tier 7。
- 能輸出可供 wizard 顯示的資源樹與依賴資訊。

---

## **Phase 3：Granular Sync Engine**

### 交付物
- 使用者勾選資源 → 後端接收  
- DAG 自動補齊依賴  
- SyncMode（Flow Only / With Dependencies / Custom）  

---

## **Phase 4：A/B Diff Engine**

### 交付物
- Diff Viewer  
- venv A / venv B 資源比較與 dependency 確認  
- `/api/v1/migration/job/diff`  
- Flow / Queue / Prompt / User 差異比對  

---

## **Phase 5：Transient Workspace**

### 交付物
- 動態建立工作區  
- Tier-Based 目錄  
- Workspace 清理策略（成功刪除 / 失敗保留 / 回滾重建）  
- Declarative Import Block（同名衝突防禦）  
- Snapshot / Restore / Merge 基礎資料結構  

---

## **Phase 6：De-hardcode Engine**

### 交付物
- Regex 去除 ID  
- Flow YAML → Java Map  
- substitutions 生成  

---

## **Phase 7：Terraform Executor + State Machine**

### 交付物
- Terraform init / plan / apply  
- WebSocket 日誌串流  
- xterm.js 終端  
- **部署任務狀態機（JobStatus）持久化**  
- **Apply 背景執行，不依賴 WebSocket**  
- **WebSocket Re-attach（斷線補回）**  
- **Timeout 防禦（30 分鐘）**  
- **部署進度監控 API / UI**  
- **處理中 / 待處理資源清單**  

---

## **Phase 8：二次驗證**

### 交付物
- `/send-passcode`  
- `/verify-passcode`  
- Passcode 錯誤次數限制  
- 任務鎖定機制  
- **發布前驗證閘門（Passcode Gate）**  

---

## **Phase 9：Git Audit + Rollback**

### 交付物
- Git commit  
- Git Tag  
- Rollback  
- 審計頁面  
- **Audit Log 明細與查詢**  
- **Version Record 與 Snapshot Trace**  

---

## **Phase 10：整合測試**

### 測試項目
- Flow Only  
- Flow + Queue  
- Flow + Prompt  
- User Only  
- 全量 Tier 測試  
- Destroy 防禦測試  
- Provider Bug 測試  
- 超時測試（Async + WebSocket）  
- Passcode 測試  
- Rollback 測試  
- Workspace 清理策略  
- Declarative Import 測試  

---

# 4. 任務拆解（Task Breakdown）

---

## Backend Tasks

- [ ] Credential Module  
- [ ] Vault 整合  
- [ ] Discovery Engine  
- [ ] Resource Mapping Table / Conflict Resolver  
- [ ] Compare & Dependency Gate  
- [ ] DAG Analyzer  
- [ ] Diff Engine  
- [ ] ResourceDescriptor Model  
- [ ] JobDescriptor Model  
- [ ] Granular Sync Engine  
- [ ] WorkspaceBuilder  
- [ ] Tier Directory Generator  
- [ ] Declarative Import Engine  
- [ ] Data Source Downgrade Engine  
- [ ] De-hardcode Engine  
- [ ] Terraform Executor  
- [ ] Snapshot / Restore / Merge Engine  
- [ ] **Terraform Apply State Machine（v5.1）**  
- [ ] **Progress Monitor Engine（v5.1）**  
- [ ] **WebSocket Re-attach（v5.1）**  
- [ ] **Timeout 防禦（v5.1）**  
- [ ] **Passcode Verification Engine（v5.1）**  
- [ ] Git Audit Engine  
- [ ] Audit Log Engine  
- [ ] Rollback Engine  
- [ ] Version Record Engine  

---

## Frontend Tasks

- [ ] 環境管理頁  
- [ ] Tree Table 資源清單  
- [ ] 資源比較與 dependency 確認頁  
- [ ] DAG 拓撲預覽  
- [ ] Diff Viewer  
- [ ] xterm.js 終端  
- [ ] 部署進度監控頁  
- [ ] 二次驗證頁  
- [ ] 驗證碼核銷頁  
- [ ] 審計與回滾頁  
- [ ] **前端重連後自動補回日誌（v5.1）**

---

# 5. 測試計畫（Testing Plan）

| 測試類型 | 說明 |
|---------|------|
| 單元測試 | ResourceDescriptor、DAG、De-hardcode |
| 整合測試 | Terraform Plan / Apply、WebSocket |
| 壓力測試 | 大型 Flow YAML、100+ Queue |
| 安全測試 | Vault、AES-256-GCM |
| Destroy 防禦測試 | Data Source 降級、Transient Workspace |
| 回滾測試 | git revert + terraform apply |
| Diff 測試 | Flow / Queue / Prompt / User |
| Import 測試 | Declarative Import Block |
| Compare 測試 | venv A / venv B 差異、dependency 提示與使用者確認 |
| **WebSocket 中斷測試** | Apply 背景執行不中斷、前端重連補回日誌 |
| **Progress Monitor 測試** | 顯示處理中 / 待處理資源，並驗證 timeout 與重試行為 |
| **State Machine 測試** | 任務狀態正確持久化、重啟後可恢復任務 |
| **Passcode 測試** | 發送、核銷、錯誤次數、任務鎖定 |
| **Audit Log 測試** | 驗證操作、比對、驗證、部署與回滾事件都有記錄 |

---

# 6. 里程碑（Milestones）

| 里程碑 |
|--------|
| Discovery 完成 |
| DAG + Tree Table |
| Granular Sync 完成 |
| Diff Engine 完成 |
| Transient Workspace + Import 完成 |
| De-hardcode 完成 |
| Terraform Executor + State Machine 完成 |
| Passcode 完成 |
| Git Audit + Rollback 完成 |
| 全系統整合測試完成 |

---

# 7. 風險控管（Risk Control）

沿用 v5.1 Risk Matrix：

- Destroy 防禦（Transient Workspace + Data Source）  
- Provider Bug（Flow / Prompt 分流）  
- 同名衝突（Declarative Import）  
- YAML 損壞（Dual Parsing）  
- 超時問題（Timeout 防禦）  
- WebSocket 中斷（Re-attach）  
- Passcode 驗證失敗（任務鎖定）  

---

# 8. 專案交付物（Deliverables）

- 完整後端 Spring Boot 專案  
- 完整前端 RuoYi Vue 專案  
- Terraform IaC 模板  
- Git Audit Repo（`/data/git_audit/`）  
- 系統架構白皮書（v5.1）  
- Development Plan（本文件）  

---