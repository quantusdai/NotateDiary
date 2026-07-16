# 贡献指南 · Contributing

感谢你对 NotateDiary 的兴趣！无论是反馈 bug、提建议，还是提交代码，都非常欢迎。

> Thanks for your interest in contributing — bug reports, ideas, and pull requests
> are all welcome.

---

## 开始之前 · Before you start

- **反馈问题**：请先搜索 [Issues](https://github.com/quantusdai/NotateDiary/issues)
  是否已有相同问题；没有则新建，并按模板附上设备型号、系统版本、复现步骤。
- **安全漏洞**：请走私密渠道，见 [`SECURITY.md`](SECURITY.md)，**勿公开披露**。
- **大改动**：建议先开 Issue 讨论方案，再动手，避免方向性返工。

---

## 本地开发 · Development setup

1. Fork 并克隆本仓库。
2. 按 [`docs/BUILD.md`](docs/BUILD.md) 配置环境（JDK 21 + Android SDK 36）。
3. 编译运行：`./gradlew app:assembleDebug`，跑测试：`./gradlew app:testDebugUnitTest`。

## 代码约定 · Conventions

- 语言：**Kotlin**，UI 用 **Jetpack Compose**。
- 风格尽量与周边代码一致；新增关键逻辑请补必要的 KDoc / 注释。
- 提交前确保 `app:testDebugUnitTest` 通过。

## ⚠️ 重要：不要提交敏感信息 · Do NOT commit secrets

- 切勿提交 `local.properties`、`keystore.properties`、`release.keystore`、任何
  API Key / 密码。它们已被 `.gitignore` 忽略。
- 详见 [`SECURITY.md`](SECURITY.md)。

## 提交 Pull Request

1. 基于 `main` 建分支（如 `feature/xxx` 或 `fix/xxx`）。
2. 提交信息清晰描述改动（建议英文或中英对照）。
3. 在 PR 描述中说明：**改了什么、为什么、如何验证**；UI 改动请附截图。
4. 涉及 AI 日记 / 大模型行为的改动，请说明测试所用的服务商与模型。

## 合规提醒 · Licensing note

本项目为 MIT 衍生作品，源自 [alexdremov/notate](https://github.com/alexdremov/notate)
与 [MaximeRivest/riddle](https://github.com/MaximeRivest/riddle)。提交的代码默认以
MIT 发布，并需保留既有版权声明（见 `LICENSE` / `NOTICE`）。
**请勿引入版权不明的资源**（尤其是字体），详见 `README.md` 的 Fonts 章节。
