# ⭐ **Genesys Cloud 遷移系統可能出現的風險場景**

我會分成 6 大類：  
- Terraform 相關  
- Genesys Cloud API 相關  
- Flow / Queue / Prompt / User 相關  
- Workspace / File System 相關  
- Credential / Environment 相關  
- User Operation / UI 相關  

---

# 🟥 **A. Terraform 相關風險（詳細版）**

---

## **1️⃣ Name Duplicate（名稱重複）**

### 📌 風險說明  
Terraform 嘗試 create 資源時，如果目標環境已經有同名資源，Genesys Cloud API 會回 400 / 409 Duplicate。

### 📌 為什麼會發生  
- Sandbox 重複 apply  
- venvA → venvB 遷移時，兩邊有同名資源  
- Flow / Queue / Prompt / Role 名稱不能重複

### 📌 實際例子（你已經遇過）  
```
Duplicate value for skill label
Cannot save a role named 'don_sandbox_role_agent'
A prompt called 'don_sandbox_prompt_welcome' already exists
```

### 📌 遷移系統防禦  
- Compare Engine：先比對 venvA / venvB  
- Declarative Import Engine：自動生成 import block  
- WorkspaceBuilder：如果存在 → import；如果不存在 → create

---

## **2️⃣ ID Duplicate（ID 重複）**

### 📌 風險說明  
同名資源但 ID 不同，Terraform state 會混亂，可能導致 destroy 或 apply fail。

### 📌 為什麼會發生  
- venvA / venvB 同名但不同 ID  
- 手動建立資源  
- 遷移後再 apply

### 📌 遷移系統防禦  
- Discovery Engine：讀取 ID  
- Compare Engine：比對 ID  
- Import Engine：自動接管現有資源

---

## **3️⃣ Destroy Risk（誤刪）**

### 📌 風險說明  
如果 workspace 裡面包含未勾選資源，Terraform 會以為「未勾選＝要刪除」。

### 📌 為什麼會發生  
- Terraform 是 declarative  
- state 裡面有資源但 HCL 裡面沒有 → destroy

### 📌 遷移系統防禦  
- Transient Workspace（你已經做）  
- Data Source Downgrade（你已經做）  
- 永不使用 production state

---

## **4️⃣ Provider Bug（nil pointer / crash）**

### 📌 風險說明  
GenesysCloud Provider 有時會：

- Flow YAML 太大 → timeout  
- Prompt audio 空置 → nil pointer  
- Queue wrap-up code missing → crash  
- Architect import block 格式變動

### 📌 為什麼會發生  
Provider 不是官方 GC 團隊維護，有時會有 bug。

### 📌 遷移系統防禦  
- Flow / Prompt 分流  
- Retry 機制  
- Provider version 固定  
- Flow YAML validator

---

## **5️⃣ Version Mismatch（Terraform 版本不一致）**

### 📌 風險說明  
Terraform 1.5 vs 1.6 vs 1.7 行為不同，Provider 也可能不相容。

### 📌 為什麼會發生  
- 本地版本不同  
- Docker 版本不同  
- CI/CD 版本不同

### 📌 遷移系統防禦  
- Docker 固定版本  
- provider lock file  
- version pinning

---

## **6️⃣ Terraform State Corruption（tfstate 壞）**

### 📌 風險說明  
tfstate 損壞會導致：

- destroy  
- apply fail  
- import fail  
- resource drift

### 📌 為什麼會發生  
- apply 中途中斷  
- workspace 未清理  
- 手動修改 state

### 📌 遷移系統防禦  
- Transient Workspace  
- 永不保存 state  
- 每次 job 都重新建立 workspace

---

## **7️⃣ Terraform Lock File 衝突（.terraform.lock.hcl）**

### 📌 風險說明  
Provider 版本衝突會導致：

- init fail  
- provider mismatch  
- apply fail

### 📌 遷移系統防禦  
- 固定 provider 版本  
- 每次 job 重建 workspace  
- 不共享 lock file

---

## **8️⃣ Terraform Init Failure（provider registry timeout）**

### 📌 風險說明  
Terraform init 可能因為：

- registry timeout  
- network fail  
- provider registry down

### 📌 遷移系統防禦  
- Retry  
- Cache provider  
- Docker image 內置 provider

---

## **9️⃣ Terraform Plan Timeout（Flow 太大）**

### 📌 風險說明  
Flow YAML > 1MB 時，Plan 可能：

- 超時  
- 卡住  
- provider crash

### 📌 遷移系統防禦  
- Async Plan  
- Timeout 防禦  
- Flow YAML 分段處理

---

## **🔟 Terraform Apply Timeout（GC API 慢）**

### 📌 風險說明  
GC API 有時會慢，導致：

- apply 卡住  
- Flow publish fail  
- Queue create fail

### 📌 遷移系統防禦  
- Async Apply  
- Timeout（你已經做）  
- WebSocket Re-attach（你已經做）

---

## **1️⃣1️⃣ Terraform Apply Partial Success（半成功）**

### 📌 風險說明  
部分資源成功，部分失敗 → state 不一致。

### 📌 為什麼會發生  
- Flow publish fail  
- Queue create fail  
- Prompt create fail

### 📌 遷移系統防禦  
- Job State Machine  
- Rollback Engine  
- Audit Log

---

## **1️⃣2️⃣ Terraform Import Block 無效（ID 不存在）**

### 📌 風險說明  
如果 import block 裡面的 ID 錯：

- import fail  
- apply fail  
- resource drift

### 📌 為什麼會發生  
- venvB 裡面資源不存在  
- ID 寫錯  
- GC API 返回空資料

### 📌 遷移系統防禦  
- Discovery Engine  
- Compare Engine  
- Import Engine 自動生成  
- ID validator

---

# 🟧 **B. Genesys Cloud API 相關風險（詳細版）**

---

## **1️⃣3️⃣ GC API Rate Limit（429）**

### 📌 風險說明  
GC API 有 Rate Limit（每秒 request 數量限制）。  
如果遷移時大量 request（Flow import、Queue create、User create），就會被限制。

### 📌 為什麼會發生  
- Terraform apply 同時 create 多個資源  
- Flow publish 會觸發大量 Architect internal API  
- Discovery Engine 會大量 GET request

### 📌 實際例子  
你 apply 時 Flow publish 已經出現：

```
flow publish still creating... [00m20s elapsed]
```

呢個就係 API 被 throttle。

### 📌 遷移系統防禦  
- Request batching  
- Retry with exponential backoff  
- 限制每秒 request 數量  
- Flow publish 改成 async job

---

## **1️⃣4️⃣ GC API Burst Limit（Flow import 大量 request）**

### 📌 風險說明  
Architect Flow publish 會觸發大量 internal API（每個 action 都會 call API）。  
如果 Flow 太大 → Burst limit → publish fail。

### 📌 為什麼會發生  
- Flow 有 50+ tasks  
- Flow 有大量 actions  
- Flow YAML > 1MB

### 📌 實際例子  
你 Flow publish 時：

```
Still creating... [00m20s elapsed]
exit code 100
```

### 📌 遷移系統防禦  
- Flow 分段 publish  
- Flow YAML validator  
- Architect publish retry

---

## **1️⃣5️⃣ GC API Token Refresh Failure**

### 📌 風險說明  
GC OAuth token 有時會 refresh fail → 所有 API call fail。

### 📌 為什麼會發生  
- client_id / client_secret 錯  
- region 錯  
- token 過期  
- network fail

### 📌 實際例子  
你之前 Flow publish error 裡面有：

```
startWithAuthToken
```

代表 token refresh。

### 📌 遷移系統防禦  
- Credential Module  
- Token refresh retry  
- Token expiry detection

---

## **1️⃣6️⃣ GC API Region Mismatch（ap-northeast-1 vs ap-southeast-2）**

### 📌 風險說明  
如果 region 錯：

- Flow publish fail  
- Queue create fail  
- Prompt create fail  
- User create fail

### 📌 為什麼會發生  
- Sandbox 用 ap-northeast-1  
- Production 用 ap-southeast-2  
- Terraform provider region 寫錯

### 📌 遷移系統防禦  
- Credential Module  
- Region validator  
- WorkspaceBuilder 自動填 region

---

## **1️⃣7️⃣ GC API Timeout（Flow import > 30s）**

### 📌 風險說明  
Architect Flow publish 有 timeout（通常 30 秒）。  
Flow 太大 → publish fail。

### 📌 為什麼會發生  
- Flow 有大量 tasks  
- Flow YAML 太大  
- GC API 忙

### 📌 實際例子  
你 Flow publish：

```
Still creating... [00m20s elapsed]
exit code 100
```

### 📌 遷移系統防禦  
- Async Flow publish  
- Timeout retry  
- Flow 分段 publish

---

## **1️⃣8️⃣ GC API Internal Error（500）**

### 📌 風險說明  
GC API 有時會回 500 Internal Error。

### 📌 為什麼會發生  
- Architect internal bug  
- Routing internal bug  
- User create internal bug  
- Queue create internal bug

### 📌 遷移系統防禦  
- Retry  
- Error classification  
- Audit log

---

## **1️⃣9️⃣ GC API Permission Denied（client_id 無權限）**

### 📌 風險說明  
如果 OAuth client 無權限：

- Flow export fail  
- Flow publish fail  
- Queue create fail  
- Prompt create fail  
- User create fail

### 📌 為什麼會發生  
- OAuth client scope 不足  
- region 錯  
- client_id 錯

### 📌 遷移系統防禦  
- Credential Module  
- Permission validator  
- OAuth scope checker

---

## **2️⃣0️⃣ GC API 返回空資料（Flow export 空白）**

### 📌 風險說明  
Flow export 有時會返回空 YAML。

### 📌 為什麼會發生  
- Flow draft 未 publish  
- Flow corrupted  
- Architect internal error

### 📌 遷移系統防禦  
- Flow export retry  
- Flow version checker  
- Flow draft detector

---

## **2️⃣1️⃣ GC API 返回格式變動（Architect YAML schema 更新）**

### 📌 風險說明  
Architect YAML schema 會更新，導致：

- SnakeYAML parse fail  
- Terraform provider fail  
- Flow publish fail

### 📌 為什麼會發生  
- Genesys Cloud 每星期更新  
- Architect schema 會變

### 📌 遷移系統防禦  
- YAML schema validator  
- Flow Import/Export pipeline  
- SnakeYAML dynamic parser

---

## **2️⃣2️⃣ GC API 不支援某些資源（Data Action / OAuth Client）**

### 📌 風險說明  
有些資源 GC API 不支援：

- Data Action  
- OAuth Client  
- Common Module  
- Integration

### 📌 為什麼會發生  
- GC API 限制  
- Provider 限制

### 📌 遷移系統防禦  
- Scope 限制  
- Resource filter  
- Compare Engine skip

---

# 🟨 **C. Flow / Queue / Prompt / User 相關風險（詳細版）**

---

## **2️⃣3️⃣ Missing Dependency（Queue / Prompt / Skill）**

### 📌 風險說明  
Flow 依賴 Queue、Prompt、Skill，如果遷移時未勾選依賴 → Flow publish fail。

### 📌 為什麼會發生  
- Flow 裡面引用 Queue  
- Flow 裡面引用 Prompt  
- Queue 依賴 Skill  
- 遷移時使用者勾錯資源

### 📌 實際例子  
你嘅 Flow 裡面：

```
targetQueue.lit.name: test don v2
```

如果 queue 唔存在 → Flow publish fail。

### 📌 防禦  
- DAG dependency engine  
- Compare engine 提示 missing dependency  
- UI 阻止使用者 publish

---

## **2️⃣4️⃣ Flow YAML Parsing Error（SnakeYAML fail）**

### 📌 風險說明  
Flow YAML 結構複雜，SnakeYAML 可能 parse fail。

### 📌 為什麼會發生  
- Architect YAML schema 更新  
- YAML 有特殊字元  
- YAML 有錯誤縮排  
- Provider rewrite 後 YAML 壞咗

### 📌 實際例子  
你之前遇過：

```
playAudio: 前面冇 dash → YAML 壞咗
```

### 📌 防禦  
- Flow YAML validator  
- SnakeYAML safe loader  
- Architect Export → Template 化（你而家採用的正確方法）

---

## **2️⃣5️⃣ Flow 內部引用不存在（FindQueueByName 找唔到）**

### 📌 風險說明  
Flow publish 時，Architect 會用 Queue Name 去查找 Queue，如果找唔到 → publish fail。

### 📌 為什麼會發生  
- Queue 名稱錯  
- Queue 未 create  
- Flow YAML 用咗變數（{{queue_name}}）

### 📌 實際例子  
你剛剛遇到：

```
could not find the 'queue' by name using the value '{{queue_name}}'
```

### 📌 防禦  
- Flow Template Builder（Java）  
- 用 GC SDK 查 queue name  
- Flow YAML 必須用純文字 queue name

---

## **2️⃣6️⃣ Queue wrap-up code missing**

### 📌 風險說明  
Queue wrap-up code 如果缺失，Terraform provider 會 crash。

### 📌 為什麼會發生  
- Queue 有 wrap-up code  
- Provider 期望 wrap-up code list  
- YAML 裡面冇 wrap-up code block

### 📌 防禦  
- Queue discovery  
- Queue validator  
- Provider version pinning

---

## **2️⃣7️⃣ Prompt audio missing / corrupted**

### 📌 風險說明  
Prompt audio 如果缺失或 corrupted → publish fail。

### 📌 為什麼會發生  
- Prompt export incomplete  
- Prompt audio file missing  
- Architect internal error

### 📌 防禦  
- Prompt discovery  
- Prompt audio validator  
- Prompt fallback tts

---

## **2️⃣8️⃣ User attribute missing（division / role）**

### 📌 風險說明  
User create 時 division 或 role 缺失 → create fail。

### 📌 為什麼會發生  
- division mismatch  
- role hierarchy conflict  
- user attribute incomplete

### 📌 防禦  
- User discovery  
- User attribute validator  
- Division mapping engine

---

## **2️⃣9️⃣ Role hierarchy conflict（custom role）**

### 📌 風險說明  
Custom role hierarchy 可能導致：

- role create fail  
- role import fail  
- permission mismatch

### 📌 防禦  
- Role discovery  
- Role compare engine  
- Role import engine

---

## **3️⃣0️⃣ Division mismatch（Sandbox vs Production）**

### 📌 風險說明  
Flow / Queue / User 必須屬於 division，如果 division 不一致 → publish fail。

### 📌 防禦  
- Division discovery  
- Division mapping  
- WorkspaceBuilder 自動填 division

---

## **3️⃣1️⃣ Language mismatch（en-US vs zh-HK）**

### 📌 風險說明  
Flow YAML 裡面嘅語言設定如果不一致 → publish fail。

### 📌 防禦  
- Language discovery  
- Language validator  
- Flow template builder

---

## **3️⃣2️⃣ Flow version conflict（Draft vs Published）**

### 📌 風險說明  
Flow export draft vs published → YAML 結構不同 → publish fail。

### 📌 防禦  
- Flow version checker  
- Always export published version  
- Draft fallback

---

## **3️⃣3️⃣ Architect Flow 只能用 Queue Name，不能用 Queue ID**

### 📌 風險說明  
Flow YAML 裡面嘅 `targetQueue.lit.name` 必須是純文字 Queue Name。

### 📌 為什麼會發生  
Architect publish 時會：

```
find queue by name "xxx"
```

如果用 ID → 找唔到  
如果用變數 → 找唔到

### 📌 實際例子  
你剛剛遇到：

```
could not find the 'queue' by name using the value '{{queue_name}}'
```

### 📌 防禦  
- Flow Template Builder（Java）  
- 用 GC SDK 查 queue name  
- Flow YAML 必須用真實 queue name

---

## **3️⃣4️⃣ Architect Flow 只能用 Prompt Name，不能用 Prompt ID**

### 📌 風險說明  
Flow YAML 裡面嘅 `promptName` 必須是純文字 Prompt Name。

### 📌 防禦  
- Prompt discovery  
- Flow Template Builder  
- Flow YAML 必須用真實 prompt name

---

## **3️⃣5️⃣ Architect Flow 不支援 Terraform substitutions**

### 📌 風險說明  
Flow YAML 裡面不能出現 `{{xxx}}`。

### 📌 為什麼會發生  
Terraform provider 支援 substitutions  
Architect Flow 不支援 substitutions

### 📌 實際例子  
你剛剛遇到：

```
{{queue_name}} → no matches
```

### 📌 防禦  
- Flow Template Builder（Java）  
- Flow YAML 必須用純文字  
- 不使用 Terraform substitutions

---

## **3️⃣6️⃣ Architect Flow 的 startUpRef 必須指向真實 task refId**

### 📌 風險說明  
如果 task block 被破壞 → Flow publish fail。

### 📌 實際例子  
你之前遇到：

```
Property 'task_start' does not exist
```

### 📌 防禦  
- Flow YAML validator  
- Provider rewrite 防禦  
- Architect Export → Template 化

---

## **3️⃣7️⃣ Architect Flow YAML 被 Provider Rewrite 破壞**

### 📌 風險說明  
Terraform provider 會 rewrite YAML，如果 YAML 結構唔標準 → rewrite 後縮排錯誤。

### 📌 實際例子  
你之前遇到：

```
playAudio: 前面冇 dash → YAML 壞咗
```

### 📌 防禦  
- Flow Import/Export pipeline  
- Flow Template Builder  
- 不手寫 YAML（你已經改用 Architect Export）

---

# 🟩 **D. Workspace / File System 相關風險（6 項 — 正確編號版）**

---

## **38️⃣ Workspace Permission Error（無權限）**

### 📌 風險說明  
Terraform workspace 需要讀寫檔案，如果 OS 或 Docker volume 權限不足 → init / plan / apply 全部會 fail。

### 📌 為什麼會發生  
- Windows / Linux 權限不足  
- Docker volume 無寫入權限  
- CI/CD runner 無權限  
- 路徑屬於 root / system

### 📌 遷移系統防禦  
- WorkspaceBuilder 建立 workspace 時檢查權限  
- 使用固定 volume path  
- 自動執行 chmod / chown 修正權限

---

## **39️⃣ Workspace 未清理（apply fail）**

### 📌 風險說明  
上一次 job 的 workspace 未清理 → tfstate、lock file、provider cache 會干擾下一次 apply。

### 📌 為什麼會發生  
- apply 中途中斷  
- job cancel  
- workspace reuse  
- tfstate 殘留

### 📌 遷移系統防禦  
- 每個 job 使用全新 workspace（你已經做）  
- apply 後自動清理  
- WorkspaceBuilder 強制刪除舊 workspace

---

## **40️⃣ Workspace 路徑過長（Windows 限制）**

### 📌 風險說明  
Windows 有 MAX_PATH 限制（260 字元）。  
Terraform workspace 路徑太深 → provider fail / YAML load fail。

### 📌 為什麼會發生  
- jobId 太長  
- workspace path 太深  
- Windows 系統限制

### 📌 遷移系統防禦  
- workspace path 固定為短路徑  
- jobId 使用短 UUID  
- 避免 nested folder

---

## **41️⃣ Git Audit Repo Permission Error**

### 📌 風險說明  
Audit log repo（git）如果無權限 → commit fail → audit log missing。

### 📌 為什麼會發生  
- repo 無寫入權限  
- CI/CD runner 無權限  
- Windows / Linux 權限錯誤  
- SSH key / token 無效

### 📌 遷移系統防禦  
- AuditService 檢查 repo 權限  
- commit 前做權限驗證  
- fallback 到 JSONL audit log

---

## **42️⃣ Git Commit Fail（binary file too large）**

### 📌 風險說明  
Flow YAML、Prompt audio、tfstate 等檔案可能太大 → git commit fail。

### 📌 為什麼會發生  
- Flow YAML > 1MB  
- Prompt audio > 10MB  
- Git LFS 未啟用  
- repo 限制 binary

### 📌 遷移系統防禦  
- Audit repo 不存 binary  
- Flow YAML 存 text only  
- Prompt audio 不進 git  
- 使用 JSONL audit log

---

## **43️⃣ Disk Full（workspace 無法寫入）**

### 📌 風險說明  
磁碟滿 → workspace 無法寫入 → Terraform init / plan / apply fail。

### 📌 為什麼會發生  
- workspace 太大  
- audit log 太多  
- CI/CD runner disk 滿  
- Docker volume 滿

### 📌 遷移系統防禦  
- WorkspaceBuilder 檢查磁碟空間  
- 定期清理 workspace  
- audit log rotation  
- disk usage monitor

---

# 🟦 **E. Credential / Environment 相關風險（6 項 — 正確編號版）**

---

## **44️⃣ Credential Error（region / client_id / secret 錯）**

### 📌 風險說明  
Genesys Cloud OAuth 憑證錯誤會導致所有 API 無法使用，包括：

- Flow export  
- Flow publish  
- Queue create  
- Prompt create  
- User create  
- Terraform provider init

### 📌 為什麼會發生  
- region 寫錯（例如 ap-northeast-1 → ap-southeast-2）  
- client_id 錯  
- client_secret 錯  
- OAuth client 被刪除  
- OAuth scope 不完整

### 📌 遷移系統防禦  
- Credential Module  
- region validator  
- OAuth client scope checker  
- token refresh validator  
- apply 前做 credential health check

---

## **45️⃣ Credential Expired（OAuth token 過期）**

### 📌 風險說明  
OAuth token 有有效期，如果過期或 refresh fail → 所有 API call fail。

### 📌 為什麼會發生  
- 長時間 apply  
- Flow publish job 超時  
- token refresh fail  
- OAuth client 被 disable

### 📌 遷移系統防禦  
- 自動 refresh token  
- token expiry detection  
- retry with exponential backoff  
- Credential Module 提供健康檢查 API

---

## **46️⃣ venvA / venvB region 不一致**

### 📌 風險說明  
如果兩個環境 region 不一致：

- Flow publish fail  
- Queue create fail  
- Prompt create fail  
- User create fail  
- Architect internal error

### 📌 為什麼會發生  
- Sandbox 用 ap-northeast-1  
- Production 用 ap-southeast-2  
- Terraform provider region 寫錯

### 📌 遷移系統防禦  
- Credential Module  
- region mapping  
- WorkspaceBuilder 自動填 region  
- Compare Engine 提示 region mismatch

---

## **47️⃣ venvA / venvB permission 不一致**

### 📌 風險說明  
如果兩個環境的 OAuth client scope 不一致：

- Flow export fail  
- Flow publish fail  
- Queue create fail  
- Prompt create fail  
- User create fail

### 📌 為什麼會發生  
- Sandbox OAuth client scope 完整  
- Production OAuth client scope 不完整  
- 遷移時使用錯誤 OAuth client

### 📌 遷移系統防禦  
- OAuth scope discovery  
- Permission compare engine  
- Credential Module 自動檢查 scope

---

## **48️⃣ venvA / venvB resource naming policy 不一致**

### 📌 風險說明  
不同環境可能有不同命名規則，例如：

- Queue 名稱不能包含空格  
- Prompt 名稱不能包含大寫  
- Flow 名稱不能包含特殊字元

### 📌 為什麼會發生  
- Sandbox 無限制  
- Production 有命名規範  
- 遷移時名稱不合法 → create fail / publish fail

### 📌 遷移系統防禦  
- Naming policy validator  
- Compare Engine 提示 naming conflict  
- WorkspaceBuilder 自動修正名稱

---

## **49️⃣ venvA / venvB division 不一致**

### 📌 風險說明  
Flow / Queue / User 必須屬於 division，如果 division 不一致 → publish fail / create fail。

### 📌 為什麼會發生  
- Sandbox 用 Home division  
- Production 用 custom division  
- 遷移時 division mapping 錯誤

### 📌 遷移系統防禦  
- Division discovery  
- Division mapping engine  
- WorkspaceBuilder 自動填 division  
- Compare Engine 提示 division mismatch

---

# 🟪 **F. User Operation / UI 相關風險（6 項 — 正確編號版）**

---

## **50️⃣ User 勾錯資源（Flow 無 Queue）**

### 📌 風險說明  
使用者在 UI 勾選 Flow，但沒有勾選 Flow 依賴的 Queue / Prompt / Skill → Flow publish fail。

### 📌 為什麼會發生  
- 使用者不熟悉 Flow dependency  
- Flow 有多層依賴（Queue → Skill → Division）  
- UI 沒有強制檢查

### 📌 遷移系統防禦  
- DAG dependency engine  
- Compare engine 自動提示 missing dependency  
- UI 阻止使用者 publish incomplete selection

---

## **51️⃣ User 勾太多資源（100+ Flow）**

### 📌 風險說明  
使用者一次勾選太多資源 → Terraform apply 時：

- API rate limit  
- Flow publish timeout  
- Workspace 太大  
- job fail

### 📌 為什麼會發生  
- 大型客戶  
- 使用者不熟悉 GC API 限制  
- 一次遷移大量 Flow

### 📌 遷移系統防禦  
- UI 限制一次最多勾選 20 Flow  
- 分批遷移  
- Async apply queue

---

## **52️⃣ User 中途取消 Apply**

### 📌 風險說明  
使用者在 apply 過程中：

- reload  
- close browser  
- cancel job  
- network drop

會導致：

- workspace incomplete  
- tfstate incomplete  
- Flow publish half-done  
- job fail

### 📌 遷移系統防禦  
- Job State Machine  
- WebSocket reconnect  
- apply resume  
- audit log checkpoint

---

## **53️⃣ WebSocket Disconnect（前端 reload）**

### 📌 風險說明  
前端 reload / network drop → WebSocket 斷線 → 使用者以為 apply fail，但實際 apply 仍在後端執行。

### 📌 為什麼會發生  
- network unstable  
- user reload  
- browser crash

### 📌 遷移系統防禦  
- WebSocket re-attach（你已經做）  
- job status polling  
- apply progress recovery

---

## **54️⃣ Passcode Verification Failure（錯誤 / 過期）**

### 📌 風險說明  
遷移前需要 passcode 驗證，如果：

- passcode 錯  
- passcode 過期  
- passcode 被重複使用

會導致 job 無法開始。

### 📌 為什麼會發生  
- 使用者輸入錯誤  
- passcode timeout  
- passcode reuse

### 📌 遷移系統防禦  
- passcode expiry  
- passcode one-time use  
- UI 提示 passcode 錯誤原因

---

## **55️⃣ Audit Log Missing（JSONL 寫入失敗）**

### 📌 風險說明  
Audit log 是遷移系統最重要的安全機制，如果寫入失敗：

- 無法追蹤 apply  
- 無法追蹤錯誤  
- 無法追蹤使用者操作  
- 無法做 rollback

### 📌 為什麼會發生  
- disk full  
- permission error  
- file lock  
- concurrent write

### 📌 遷移系統防禦  
- JSONL append-only  
- audit log rotation  
- audit log fallback  
- audit log integrity check

---

# 🟫 **G. EGCS 核心設計補充（新增）**

以下五點不是新的風險項目，而是針對 `risk_matrix.md` 中已列出的風險，EGCS 必須採用的設計落點。

## **56️⃣ 多 Org 資料遷移與同步**

### 📌 設計重點
EGCS 必須支援多個 Org 之間的資料遷移與同步，且每個 Org 都可以獨立保存 `region / client_id / client_secret`。

### 📌 核心方案
- 透過 `migration_env_profile` 管理多 Org 憑證與環境設定
- 透過 `migration_resource_mapping` 管理 source GUID 與 target GUID 的對映
- Compare / Discovery 先檢查 name duplicate、ID duplicate 與 division mismatch

### 📌 對應風險
- 1️⃣ Name Duplicate
- 2️⃣ ID Duplicate
- 23️⃣ Missing Dependency
- 48️⃣ Naming policy mismatch
- 49️⃣ Division mismatch

## **57️⃣ Backup / Snapshot / Restore / Merge 機制**

### 📌 設計重點
系統必須支援備份、快照、恢復與合併四種操作模式，用於 apply 失敗、資料 drift 與局部同步情境。

### 📌 核心方案
- `backup`：匯出當下 Org 的配置資料，保存為標準 JSON / YAML
- `snapshot`：每次 plan 前自動建立時間戳快照
- `restore`：以 snapshot 作為回復基準，重新建置 workspace 後 apply
- `merge`：只產出差異項，不影響未變動資源

### 📌 對應風險
- 6️⃣ Terraform State Corruption
- 11️⃣ Terraform Apply Partial Success
- 12️⃣ Terraform Import Block 無效
- 39️⃣ Workspace 未清理

## **58️⃣ 版本控制與回滾**

### 📌 設計重點
EGCS 必須保存每次部署的版本軌跡，讓使用者可以追溯設定變更並回到前一個可用版本。

### 📌 核心方案
- 保存 selectedResources、main.tf、selectedResources.json 與 snapshot metadata
- 版本紀錄與 Git audit 互相對照，但避免把大型 binary 寫入版本庫
- 回滾採用版本復原後再次 apply，而不是直接 destroy

### 📌 對應風險
- 3️⃣ Destroy Risk
- 11️⃣ Terraform Apply Partial Success
- 41️⃣ Git Audit Repo Permission Error
- 42️⃣ Git Commit Fail（binary file too large）
- 52️⃣ User 中途取消 Apply

## **59️⃣ Voice Files 本地儲存模式**

### 📌 設計重點
Voice files 不應被當成一般雲端同步檔案直接上傳，而應優先採用本地儲存與差異同步。

### 📌 核心方案
- 將音檔暫存於本地 cache，例如 `/data/media_cache/{envKey}/`
- 透過 hash 比對判斷是否需要重新同步
- 避免在 Terraform apply 主流程中處理大量音檔上傳

### 📌 對應風險
- 13️⃣ GC API Rate Limit
- 17️⃣ GC API Timeout
- 42️⃣ Git Commit Fail（binary file too large）
- 43️⃣ Disk Full

## **60️⃣ BCP Wizard 式 UI 與流程設計**

### 📌 設計重點
EGCS UI 必須對齊 Genesys Cloud BCP Wizard 的成熟設計思路，以循序式流程降低操作錯誤。

### 📌 核心方案
- Step 1：Env auth 與 profile setup
- Step 2：Discovery / Compare
- Step 3：Mapping Table conflict resolution
- Step 4：Plan / OTP / Approve
- Step 5：Async execution / WebSocket monitor / audit

### 📌 對應風險
- 23️⃣ Missing Dependency
- 50️⃣ User 勾錯資源
- 53️⃣ WebSocket Disconnect
- 54️⃣ Passcode Verification Failure


---
