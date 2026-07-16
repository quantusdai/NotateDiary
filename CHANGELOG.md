# 更新日志 · Changelog

本项目的所有重要变更都会记录在此文件。

格式遵循 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)，
版本号遵循 [语义化版本](https://semver.org/lang/zh-CN/)。

> All notable changes to this project are documented here. The format is based on
> [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres
> to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### 文档 Documentation
- 重构 README 为中英双语，补充安装教程、AI 日记配置、隐私说明、字体版权与致谢。
- 新增 `docs/BUILD.md`（可复现构建）、`docs/FAQ.md`（常见问题）、`SECURITY.md`、
  `NOTICE`、`CHANGELOG.md`、`CONTRIBUTING.md`。
- 将 `onyx.md` 迁移至 `docs/ONYX.md`，整理项目目录结构。
- `LICENSE` 补充衍生作品双重署名（Aleksandr Dremov 与 quantusdai）。

## [1.0.0] - 2026-07-16

首个公开发布版本。First public release.

### 新增 Added
- **AI 魔法日记**：在墨水屏上手写输入，停顿后字迹自动隐去，调用视觉大模型以
  汤姆·里德尔人设生成回复，回复以手写字体"墨迹渐入"动画浮现，再次提笔时旧内容淡出。
- **多语言回复**：自动跟随手写语言（中文 / 英文）。
- **AI 服务商**：内置 DeepSeek、Kimi（开放平台 / Code）、Agnes AI 及自定义
  OpenAI 兼容端点；API Key 经 `EncryptedSharedPreferences` 加密存储。
- **底层手写引擎**（源自 [alexdremov/notate](https://github.com/alexdremov/notate)）：
  无限画布、零延迟 E-Ink 手写、图形识别、多种橡皮、图片粘贴、深度链接、PDF 导出、
  Google Drive / WebDAV 云同步。

### 适配 Platforms
- 已验证：文石 BOOX NoteAir 4C（Android 11+）。
- 支持任意 Android 8.0+（`minSdk 26`）设备安装；零延迟手写依赖 Onyx SDK。

---

[Unreleased]: https://github.com/quantusdai/NotateDiary/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/quantusdai/NotateDiary/releases/tag/v1.0.0
