# 第三方开源声明 / Third-Party Notices

本项目（CangHai Root-Free Manager / 沧海免root管理器）在开发过程中引入了以下开源项目。我们严格遵守各项目的开源许可协议与署名要求。

## 项目许可

本项目整体以 **GPL-3.0**（GNU General Public License v3.0）授权发布，见根目录 [`LICENSE`](LICENSE)。

原因：本项目的管理器底层与模块管理机制衍生自 **LSPosed**（GPL-3.0，强 Copyleft）。依据 GPL-3.0 第 5 条「源代码的传播」与第 13 条，衍生作品必须以相同的 GPL-3.0 许可授权并向接收方提供完整源代码。

## 引入项目清单

### 1. LSPosed（LSPosed Framework）

- 项目主页：https://github.com/LSPosed/LSPosed
- 许可：**GPL-3.0**（原始许可文本见 `licenses/LSPosed-LICENSE.txt`）
- 版权：Copyright (C) 2018-2023 LSPosed Team
- 引入内容：Xposed 模块的检测与元数据解析（`xposedmodule` / `xposedminversion` / `xposeddescription` meta-data）、模块启用/停用与作用域管理机制、LSPosed 守护进程（AIDL）交互接口、日志查看界面。
- 修改说明：基于 LSPosed 管理器（manager）的 UI 与模块管理能力进行二次开发，改为「免 root 管理器」形态，并重写为 Kotlin + Material 3 界面。所有修改文件均保留上游版权头与 GPL-3.0 声明。
- 相关源代码：https://github.com/LSPosed/LSPosed/tree/master/app

### 2. AxManager（Axeron Manager）

- 项目主页：https://github.com/fahrez182/AxManager
- 官网：https://axmanager.app
- 许可：**Apache-2.0**（原始许可文本见 `licenses/AxManager-LICENSE.txt`）
- 版权：Copyright (C) fahrez182
- 引入内容：无线调试（Wireless Debugging / ADB over TCP）激活流程、shell 权限执行器、模块（插件）ZIP 安装机制（`module.prop` + `customize.sh`）、`$AXERONDIR/plugins` 模块目录约定、BusyBox 兼容执行环境、热重启（Re-ignite）生效机制。
- 修改说明：在 Apache-2.0 许可范围内重写与扩展，新增 AI 模块转换与 AI 直接开发能力。依据 Apache-2.0 第 4 条，保留版权声明与本声明文件，并保留其 `NOTICE` 与许可文本。
- 相关源代码：https://github.com/fahrez182/AxManager

### 3. XposedBridge API（de.robv.android.xposed）

- 项目主页：https://github.com/rovo89/XposedBridge
- 许可：**Apache-2.0**
- 引入内容：`de.robv.android.xposed` 兼容 API 常量与注解（用于模块检测与转换）。

### 4. PocketDev（已合并 / 已归档）

- 项目主页：https://github.com/1953187487/PocketDev（仓库已删除，本项目为唯一承载方）
- 许可：**AGPL-3.0**（原始许可文本见 `licenses/PocketDev-LICENSE.txt`）
- 版权：Copyright (C) 2026 万古沧海
- 引入内容（仅设计要点与接口语义，**不复制其源码**）：AI 流式对话（SSE、OpenAI/Anthropic 兼容、模型列表拉取）、仓库托管访问（GitHub/GitLab/Gitee/Gitea + 访问令牌、按令牌直连下载模块源码）、中转服务器（`node:sqlite` 零依赖、账号会话、官方频道消息按时间戳追加拉取实现即时同步）、Shizuku/Dhizuku 设备自动化桥接（状态探测 + 动作执行）、更新检测/公告、主题与过渡动画。
- 合规说明：本项目为 GPL-3.0。PocketDev 为 AGPL-3.0，两者不兼容，故本项目**只参考 PocketDev 的设计要点与公开接口语义**，未复制其 AGPL-3.0 源码，避免 AGPL copyleft 传染；引用细节见 `.monkeycode/specs/2026-11-13-pocketdev-merged/merged.md`。
- 处理方式：PocketDev 发布版本与发行记录不保留；仓库已从 GitHub 删除；仅将其 AI 与仓库开发等功能的设计要点引入本项目。

## 合规要点

1. **整体许可**：本仓库以 GPL-3.0 发布，满足 LSPosed 的 Copyleft 要求。
2. **保留署名**：所有引入或改编的源文件保留原作者版权声明与许可头。
3. **开源许可披露**：应用内「设置 → 关于」页面完整展示本声明、LSPosed 与 AxManager 的来源与许可，见 `ui/AboutActivity.kt`。
4. **源码提供**：本仓库即为完整可编译源代码，满足 GPL-3.0 源码提供义务。
5. **免责声明**：上游项目的作者不对本项目作任何担保或背书。

---

本项目由 **万古沧海**（B站：万古沧海，GitHub：https://github.com/1953187487）开发并开源。
