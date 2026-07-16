# 安全策略 · Security Policy

## 密钥与凭证管理 · Secrets & credentials

本仓库**不存储任何真实密钥、API Key、令牌或私有凭证**。我们做过专项排查，
结论与机制如下：

> This repository contains **no real secrets, API keys, tokens, or private
> credentials**. Current state and safeguards:

| 项目 Item | 状态 Status |
|-----------|------------|
| 源码中的硬编码密钥 Hard-coded secrets in source | ✅ 无 None |
| 私有接口地址 Private endpoints | ✅ 无（默认均为公开 LLM 端点） |
| 个人隐私信息（用户名 / 路径 / IP / 邮箱 / 手机号） | ✅ 已跟踪文件中无 |
| git 历史中的敏感内容 Secrets in git history | ✅ 无（CI 仅用 `${{ secrets.* }}` 引用） |

### 运行时凭证如何存储 · How runtime credentials are stored

- **AI 日记 API Key** 与 **云同步密码**：仅保存在设备本地，通过
  **`EncryptedSharedPreferences`**（AES256_GCM / AES256_SIV，见
  `AIDiaryPreferences.kt`、`SyncPreferencesManager.kt`）加密，**从不上传**到任何
  第三方服务器。
- 每一页手写仅渲染成位图发送给**用户自己配置**的服务商，无遥测、无埋点。

### 本地敏感文件（已被 `.gitignore` 忽略，请勿提交）

```
local.properties        # 本机 Android SDK 路径（含本机用户名）
keystore.properties     # 发布签名密码
release.keystore        # 发布签名密钥库
*.apk
```

对应的**无敏感信息模板**为 `keystore.properties.example`（可复制后填入真实值，
真实文件永远不会入库）。

### 误提交密钥怎么办 · If you accidentally commit a secret

1. **立即吊销 / 轮换该密钥**（到服务商后台作废重发）——这是唯一真正有效的补救。
2. 再用 `git rm --cached <file>` 将文件移出跟踪并加入 `.gitignore`。
3. 若密钥已进入 git 历史，需改写历史（如
   [`git filter-repo`](https://github.com/newren/git-filter-repo) 或
   [BFG Repo-Cleaner](https://rtyley.github.io/bfg-repo-cleaner/)），随后强制推送，
   并通知所有协作者重新克隆。参考 GitHub 官方指南
   《[Removing sensitive data from a repository](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/removing-sensitive-data-from-a-repository)》。

---

## 支持版本 · Supported versions

| 版本 Version | 支持状态 Supported |
|-------------|--------------------|
| 最新发布版 Latest release | ✅ |
| 旧版本 Older releases | ❌ 请升级到最新版 |

---

## 报告安全问题 · Reporting a vulnerability

如果你发现安全漏洞（如凭证泄露、数据外发、加密存储缺陷等），**请优先私下报告**，
不要直接开公开 Issue：

- 通过 GitHub 的 **[Security Advisories](https://github.com/quantusdai/NotateDiary/security/advisories/new)**
  私密提交；
- 或在 Issue 中隐去敏感细节，仅说明"存在一个安全问题，希望私下沟通"。

我们会尽快响应。感谢负责任的披露。

> Please report vulnerabilities privately via GitHub Security Advisories rather than
> public issues. We appreciate responsible disclosure.
