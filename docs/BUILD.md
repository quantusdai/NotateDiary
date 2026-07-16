# 构建指南 · Build Guide

本文档给出从源码完整编译、打包 NotateDiary 的**可复现**步骤，以及发布版（Release）
签名打包方法。

> A reproducible guide to building NotateDiary from source, including signed release builds.

---

## 1. 环境要求 · Requirements

| 工具 Tool | 版本 Version | 说明 Notes |
|-----------|-------------|-----------|
| **JDK** | **21**（推荐 Temurin / OpenJDK） | 必须。Gradle 与 Kotlin 编译目标均为 JVM 21 |
| **Android SDK** | **compileSdk 36** | 通过 Android Studio 或 `sdkmanager` 安装 |
| **Android SDK Platform** | `android-36` | |
| **Android SDK Build-Tools** | 36.x | 随 SDK 安装 |
| **Gradle** | **9.2.1**（仓库自带 Wrapper，无需手动安装） | 见 `gradle/wrapper/gradle-wrapper.properties` |
| **AGP** | 8.13.2 | 在 `build.gradle.kts` 中固定 |
| **Kotlin** | 2.2.0 | 在 `build.gradle.kts` 中固定 |
| **Git** | 任意较新版本 | 克隆仓库 |

> 💡 **关于 JDK**：推荐使用 [Android Studio Ladybug+](https://developer.android.com/studio)
> 自带的 **JetBrains Runtime 21**，或单独安装
> [Temurin 21](https://adoptium.net/temurin/releases/?version=21)。
> 设置环境变量 `JAVA_HOME` 指向 JDK 21 目录。

> 💡 **关于 SDK 路径**：首次编译前，在仓库根目录新建 `local.properties`，写入你的
> Android SDK 路径（该文件已被 `.gitignore` 忽略，不会提交）：
> ```properties
> sdk.dir=C:\\Users\\<你的用户名>\\AppData\\Local\\Android\\Sdk      # Windows
> # sdk.dir=/Users/<you>/Library/Android/sdk                         # macOS
> # sdk.dir=/home/<you>/Android/Sdk                                  # Linux
> ```
> 或者设置环境变量 `ANDROID_HOME`，Gradle 会自动识别。

---

## 2. 克隆与首次编译 · Clone & first build

```bash
git clone https://github.com/quantusdai/NotateDiary.git
cd NotateDiary
```

### Windows (PowerShell / CMD)

```powershell
.\gradlew.bat app:assembleDebug
```

### macOS / Linux

```bash
./gradlew app:assembleDebug
```

首次编译会下载 Gradle 发行版与全部依赖，可能需要 **5–15 分钟**（取决于网络）。
成功后 APK 位于：

```
app/build/outputs/apk/debug/app-debug.apk
```

> 🌏 **网络提示**：本仓库的 Gradle Wrapper 默认使用腾讯镜像
> （`mirrors.cloud.tencent.com`）加速国内下载。海外用户如遇问题，可将
> `gradle/wrapper/gradle-wrapper.properties` 中的 `distributionUrl` 改回
> `https\://services.gradle.org/distributions/gradle-9.2.1-bin.zip`。
> Onyx SDK 依赖来自文石官方仓库 `http://repo.boox.com/...`（`settings.gradle.kts`
> 已配置 `isAllowInsecureProtocol = true`，属正常现象）。

---

## 3. 运行测试 · Run tests

```bash
# Windows
.\gradlew.bat app:testDebugUnitTest
# macOS / Linux
./gradlew app:testDebugUnitTest
```

> 单元测试基于 JUnit4 + Robolectric + MockK + Truth。部分测试（如 WebDAV 集成测试）
> 使用 [Testcontainers](https://www.testcontainers.org/)，需要本机有 Docker 环境；
> 没有 Docker 时相关测试会被跳过或失败，不影响 APK 构建。

生成测试覆盖率报告（JaCoCo）：

```bash
.\gradlew.bat app:jacocoTestReport
# 报告: app/build/reports/jacoco/jacocoTestReport/html/index.html
```

---

## 4. 发布版签名打包 · Signed release build

发布版需要签名密钥。**请勿把真实密钥提交到仓库**——`keystore.properties` 与
`release.keystore` 均已被 `.gitignore` 忽略。

### 4.1 生成密钥库（仅首次）

```bash
keytool -genkey -v -keystore release.keystore -alias <你的别名> \
  -keyalg RSA -keysize 2048 -validity 10000
```

### 4.2 配置签名信息

复制示例模板并填入真实值：

```bash
cp keystore.properties.example keystore.properties
```

编辑 `keystore.properties`：

```properties
storeFile=../release.keystore      # 相对 app/ 模块的路径
storePassword=<你的库密码>
keyAlias=<你的别名>
keyPassword=<你的密钥密码>
```

### 4.3 构建 Release APK

```bash
.\gradlew.bat app:assembleRelease
# 输出: app/build/outputs/apk/release/app-release.apk
```

> 🔐 CI 自动发布：推送形如 `v*` 的 tag 会触发 `.github/workflows/release.yml`，
> 使用 GitHub Secrets（`KEYSTORE_BASE64` / `KEY_ALIAS` / `STORE_PASSWORD` /
> `KEY_PASSWORD`）自动签名并创建 Release。详见该 workflow 文件。

---

## 5. 常见编译问题 · Troubleshooting

| 问题 Problem | 解决办法 Fix |
|-------------|-------------|
| `Unsupported class file major version` 或 JDK 相关报错 | 确认 `JAVA_HOME` 指向 **JDK 21**，而非更高/更低版本 |
| `SDK location not found` | 新建 `local.properties` 并写入 `sdk.dir=...`，或设置 `ANDROID_HOME` |
| Onyx 依赖下载失败 / `repo.boox.com` 超时 | 检查网络；该仓库为 HTTP，确保 `settings.gradle.kts` 保留 `isAllowInsecureProtocol = true` |
| Gradle 下载过慢（海外） | 将 wrapper 镜像改回 `services.gradle.org`（见第 2 节） |
| `Secret key` / 签名相关报错（Release） | 确认 `keystore.properties` 字段完整、`storeFile` 路径正确 |
| 更多运行时问题（权限 / API / 设备） | 见 [`FAQ.md`](FAQ.md) |

---

## 6. 依赖一览 · Key dependencies

完整清单见 [`app/build.gradle.kts`](../app/build.gradle.kts)。核心依赖：

- **Jetpack Compose**（BOM 2026.01.00）—— UI
- **Onyx SDK** `onyxsdk-pen:1.5.4` / `onyxsdk-base:1.8.5` —— E-Ink 手写
- **OkHttp 5.3.2** —— 网络
- **androidx.security:security-crypto** —— API Key 加密存储
- **kotlinx.serialization** —— 会话 / 数据序列化
- **PdfBox-Android** —— PDF 导出
- **dav4jvm** —— WebDAV 同步；**Google Drive API** —— 云同步
