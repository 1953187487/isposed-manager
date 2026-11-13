# PocketDev 已合并 / 引入说明

> 本文件记录 PocketDev（`1953187487/PocketDev`，手机端 AI 开发工具，Expo/React Native，AGPL-3.0）
> 合并到 Isposed Manager 的说明。PocketDev 仓库已删除（见文末），其核心设计要点与可借鉴实现
> 在此登记，供本项目实现 1.1.0 的相关功能时参考。

## 处理方式

- 不保留 PocketDev 的发布版本 / 发行记录。
- 只引入其中的 **AI 功能** 与 **仓库开发功能**（仓库令牌下载、Shizuku 激活、中转服务器即时同步、
  模型管理、更新公告）的设计要点到本项目。
- PocketDev 仓库已从 GitHub 删除，本项目为唯一承载方。

## 引入 / 借用了 PocketDev 的什么内容

| 能力 | PocketDev 原始位置 | 本项目承接为 | 说明 |
|------|--------------------|--------------|------|
| AI 流式对话（SSE、OpenAI/Anthropic 兼容、拉模型） | `src/api/ai.ts`、`src/api/stream.ts`、`src/api/models.ts` | 本项目 AI 服务商配置 / AI 开发 | 复用流式增量解析、模型列表拉取、测试连接的设计 |
| 仓库托管访问（GitHub/GitLab/Gitee/Gitea + 访问令牌） | `src/api/git.ts`、`src/api/gitClone.ts` | 仓库开发功能：令牌直连下载模块源码 | 令牌鉴权头（Bearer / PRIVATE-TOKEN / token）、跳过二进制与依赖目录的策略 |
| 中转服务器（SQLite、账号会话、官方频道即时同步） | `backend/server.js`、`backend/lib/*` | 独立服务器应用：密码 + 消息瞬间同步 | 单文件 Node 服务器，`node:sqlite` 零依赖，channel 端点按 `created_at` 追加拉取实现即时同步 |
| Shizuku / Dhizuku 设备自动化桥接 | `src/api/device.ts` | Shizuku 授权激活通道 | 桥接状态探测（supported/installed/running/permission）+ 动作执行 |
| 更新检测 / 公告 | `src/api/updates.ts`、`src/api/announcements.ts` | 更新通道 / 公告 | 在线检测、下载进度、安装回调、公告读写 |
| 主题 / 过渡动画 | `src/theme`、`src/ui` | UI 底层与过渡动画 | 液态玻璃、极光渐变、启动动画 |

## 关键技术要点（供 1.1.0 实现参考）

### 1. 仓库令牌直连下载模块源码（对应需求「通过令牌获得模块下载源」）

来自 `gitClone.ts` / `git.ts` 的设计：

- 平台识别：`github`/`gitlab`/`gitee`/`gitea`，各平台 API 基址与鉴权头不同：
  - GitHub / Gitea：`Authorization: Bearer <token>`
  - GitLab：`PRIVATE-TOKEN: <token>`
  - Gitee：`Authorization: token <token>`
- 拉取源码时跳过：`node_modules`、`.git`、`build`、`dist`、`.gradle`、`.idea`、`.expo`、`.next`、
  `target`、`__pycache__` 以及二进制/大文件扩展名（png/jpg/zip/apk/jks/keystore/db/so/jar/aar 等）。
- 单文件上限默认 5MB，文件数上限默认 2000，可中断（AbortSignal），带进度回调。

### 2. 中转服务器即时同步（对应需求「独立服务器应用 + 密码 + 消息瞬间同步」）

来自 `backend/server.js` 的设计：

- 单文件 Node.js HTTP 服务器，`node:sqlite`（Node 22+ 内置，零依赖）。
- 表：`users`（邮箱+密码哈希）、`sessions`（Bearer token，单活跃会话）、`codes`（邮箱验证码）、
  `cloud_saves`、`announcements`、`channel_messages`。
- `channel_messages(id, user_id, from_side, text, created_at)`：频道消息按 `created_at ASC LIMIT 200`
  追加拉取；发送时若命中官方自动回复规则（`officialReply`）则同事务写入一条 `official` 消息。
- 密码哈希：`hashPassword(email, password)`（带盐的 scrypt/pbkdf2 风格，见 `lib/crypto.js`）。
- 健康检查：`GET /health` 返回 `{ ok, service, version, port, mail }`。
- 默认端口 `17888`，环境变量 `PORT` / `HOST` / `OFFICIAL_EMAIL` / `OFFICIAL_PASSWORD`。
- 客户端基址：`DEFAULT_BASE = 'http://107.173.180.64:17888'`（内网穿透），候选 `http`/`https` 变体自动探测。

### 3. Shizuku 授权激活

来自 `device.ts` 的设计：

- 桥接状态：`{ supported, platform, bridge: 'shizuku'|'dhizuku'|'none', installed, running, permission, message }`。
- 动作类型：`tap` / `swipe` / `input` / `key` / `openApp` / `screenshot`，每个动作逐条弹窗确认后执行。
- 原生桥接通过 `NativeModules.DeviceBridge` 注入，未注入时走降级检测路径。

### 4. AI 流式对话

来自 `ai.ts` / `stream.ts` 的设计：

- 支持 `openai` / `anthropic` 两种接口类型，`buildEndpoint` / `buildModelsEndpoint` 组合基址与路径。
- SSE 流式解析 `extractDelta`，增量渲染，可中断。
- `chat(messages, { stream })` 返回流；`listModels()` 拉取模型列表。

## 删除记录

- PocketDev 仓库 `1953187487/PocketDev` 已从 GitHub 删除。
- 本地克隆副本已清理。
- 本项目的 `values/strings.xml` 感谢列表（credits）已同步：在「上游项目」与「设计与实现」分区
  说明借用了 PocketDev 的 AI / 仓库 / 服务器 / Shizuku 设计。

---

Copyright (C) 2026 万古沧海
