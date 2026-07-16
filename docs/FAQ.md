# 常见问题 FAQ · Troubleshooting

针对使用与编译过程中的高频故障，给出对应解决办法。编译类问题另见
[`BUILD.md`](BUILD.md) 第 5 节。

> Solutions to the most common runtime & build issues. For build errors see also
> [`BUILD.md`](BUILD.md) §5.

---

## 一、AI 日记 / 接口调用类

### Q1. 书写后没有回信，或提示「请先配置 API Key」
- 进入 **AI 日记设置**，确认已选择服务商并填入 **API Key**。
- 点 **测试连接（Test Connection）** 验证配置是否可用。
- Key 前后不要有多余空格（App 会自动 `trim`，但粘贴时请确认完整）。

### Q2. 测试连接失败，提示「墨水干涸了」或网络错误
App 会把失败原因分为几类，对症处理：

| 现象 Symptom | 类别 | 排查 Fix |
|-------------|------|---------|
| 无法解析主机 / 连接超时 / 连接被拒绝 | **网络 NETWORK** | ① 确认设备已联网；② 确认 Base URL 拼写正确；③ 部分服务商需科学上网或在服务商后台确认服务区域；④ 公司/校园网可能有防火墙拦截 |
| 返回 `401` | **鉴权 AUTH** | API Key 错误或已失效 / 欠费。到服务商后台重新生成 Key |
| 返回 `400` 或含 `image_url` 字样的报错 | **请求 BAD_REQUEST** | **该模型不支持图片输入**。 handwriting 识别必须要 vision 模型，见 Q3 |
| 其它报错 | OTHER | 把状态栏/日志里的完整错误信息拿到服务商文档比对，或提 Issue |

### Q3. 为什么 DeepSeek 无法识别我的手写？
**DeepSeek 默认模型不支持图片输入**（`supportsImage = false`）。日记是把整页手写
**作为图片**发给模型识别的，因此必须选择**支持视觉（vision）**的服务商：

- ✅ 推荐：**Kimi 开放平台**（`moonshot-v1-8k-vision-preview`，官方明确支持 vision）
- ✅ 或：**自定义 Custom**，填任意 OpenAI 兼容的 vision 端点（OpenAI `gpt-4o`、
  OpenRouter、Gemini 等）
- ❌ DeepSeek 纯文本模型无法读图，请不要用于日记识别

### Q4. 回信语言不对 / 中文手写回了英文
- 正常情况下回复会**自动跟随手写语言**。若历史对话里混入了其它语言，可能造成偏移；
  新版已只保留最近一次对话以降低干扰。
- 仍异常时，可在 AI 设置里**清空对话 / 重置**，或在系统提示词中强调"回复语言必须与
  手写图片一致"。

### Q5. 回信很慢
- 首次回信受模型与网络影响，通常几秒到十几秒。
- 可尝试换用更快的小模型，或检查网络。
- 思考型（reasoning）模型首字更慢，如服务商支持可调低推理强度。

---

## 二、权限类

### Q6. 安装时提示「禁止安装未知来源应用」
在 BOOX / Android 设置中允许「安装未知应用」（对安装来源应用授权）后再安装。

### Q7. 笔记无法保存 / 提示存储权限
App 申请了 `READ/WRITE_EXTERNAL_STORAGE` 用于笔记文件读写。请在
**系统设置 → 应用 → NotateDiary → 权限** 中授予存储权限。
（Android 11+ 上使用分区存储，一般无需额外操作；若自定义了存储目录请确认可写。）

### Q8. 云同步（Google Drive / WebDAV）连不上
- **Google Drive**：需设备有 Google 服务框架（GMS）。多数国产 BOOX 设备无 GMS，
  请改用 **WebDAV**。
- **WebDAV**：确认服务器地址、用户名、密码/应用专用密码正确；坚果云等服务需使用
  「应用密码」而非登录密码。

---

## 三、设备兼容类

### Q9. 非 BOOX 设备能用吗？
可以安装（Android 8.0+），但**零延迟手写**依赖文石 Onyx SDK：
- BOOX 设备：走硬件直写，体验最佳。
- 其它 Android 设备：回退为普通触控手写，仍可使用 AI 日记，但延迟与压感体验下降。

### Q10. 屏幕有残影 / 刷新不正常
- App 会在合适时机做局部 GC 刷新。若残影明显，可尝试翻页或触发一次全刷。
- 不同 BOOX 型号的 E-Ink 控制器略有差异，未实测机型可能有细微差别，欢迎提 Issue 反馈型号。

### Q11. 某型号 BOOX 上手写偏移 / 压感异常
优先确认系统固件为较新版本；仍有问题请带上**具体型号 + 系统版本**提 Issue，
便于针对性适配。

---

## 四、网络类

### Q12. 编译时 Onyx 依赖 / Gradle 下载失败
见 [`BUILD.md`](BUILD.md) 第 2、5 节：切换 wrapper 镜像、确认 `repo.boox.com` 可访问。

### Q13. 公司 / 校园网无法调用大模型 API
部分内网会拦截境外或特定域名。可：
- 切换到手机热点测试；
- 选用国内可直接访问的服务商（如 Kimi）；
- 自建/代理一个 OpenAI 兼容端点，在「自定义」中填入内网可访问地址。

---

## 仍然无法解决？· Still stuck?

请携带以下信息到 [Issues](https://github.com/quantusdai/NotateDiary/issues) 反馈：

- 设备型号与系统版本（如 BOOX NoteAir 4C / Android 11）
- 使用的服务商与模型（**请勿贴出你的 API Key**）
- 完整错误提示（状态栏文字 / 日志）
- 复现步骤
