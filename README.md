# Isposed Manager

**沧海免root管理器** — 一款开源的 Android 免 root 管理器，基于 [LSPosed](https://github.com/LSPosed/LSPosed) 与 [AxManager (Axeron)](https://github.com/fahrez182/AxManager) 开发。

> 作者：**万古沧海**（B站同名：万古沧海 · GitHub：https://github.com/1953187487）
> 支持：**Android 9 ~ Android 17**
> 许可：**GPL-3.0**（严格遵守 LSPosed GPL-3.0 与 AxManager Apache-2.0 的开源二改协议）
> 当前版本：**v1.0.1（测试版 / Beta）**

---

## 当前版本说明

**v1.0.1（测试版）** 是全面重构后的版本，也是当前最新版本：

- **全新 Material 3 界面**：全部页面迁移到 Jetpack Compose，单 Activity + 底部导航（首页 / 模块 / 日志 / 设置），浅色与深色主题、支持 Android 12+ 动态取色；
- **AxManager 式无线调试激活**：mDNS 自动发现配对/连接服务 + TLS 配对（6 位配对码）+ TLS 连接，激活页实时输出每一步日志，可切换 ADB 固定端口以便下次免配对直连；
- **强制协议阅读**：首次进入必须逐条阅读并勾选「用户须知协议」「开源协议声明」「AI 使用须知」，三项全部确认后才能使用应用；
- **版本号标注**：应用内展示「测试版」标识。

**v1.0.0** 为首个正式发布版，界面为传统 View/Fragment 实现，已通过 GitHub Release 发布。当前推荐安装 v1.0.1。

## 功能特性

### 1. 免 root 权限激活
- mDNS 自动发现 `_adb-tls-pairing` / `_adb-tls-connect` 服务，无需手动输入 IP 与端口
- TLS 配对 + TLS 连接，全程免 root（Android 11+ 无线调试）
- 激活后可将 ADB 切换为固定 TCP 端口，后续免配对直连
- 内置 Shell 终端与 BusyBox，直接执行系统命令

### 2. 模块管理
- 基于 LSPosed 的模块检测、启用/停用、作用域配置、日志
- 自动安装 root 模块（ZIP：`module.prop` + `customize.sh`），热重启生效
- 作用域编辑器：勾选目标应用后保存到设备

### 3. UI 与设置
- Material 3 设计系统（卡片、状态徽标、测试版徽标、分段式区块）
- 单 Activity 底部导航，页面转场与深色主题适配
- 设置页展示开发作者（万古沧海）与原作者（LSPosed、AxManager）信息

### 4. AI 功能
- 配置自定义服务商 API（OpenAI 兼容）：Base URL / API Key / 模型 / 温度
- 拉取模型列表、测试模型连通性
- **AI 转换模块**：将 Isposed/Xposed 模块转换为本软件适配的模块
- **AI 直接开发**：用自然语言生成完整的 LSPosed 模块工程

### 5. 开源与署名
- 项目开源（GPL-3.0）
- 代码与作者处署名 **万古沧海**，并附开源账号（GitHub）
- 应用内「关于」页完整展示第三方开源声明

## 构建

```bash
# 环境：JDK 17、Android SDK (compileSdk 36 / build-tools 36)
echo "sdk.dir=/path/to/android-sdk" > local.properties
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease
# 产物：app/build/outputs/apk/{debug,release}/app-{debug,release}.apk
```

## 目录结构

```
app/                         主应用（管理器）
├── src/main/java/com/wangucanghai/isposed/
│   ├── adb/                  ADB 客户端：mDNS 发现 / TLS 配对 / 证书生成 / 激活编排
│   ├── ai/                   AI 客户端/配置/转换/代码生成
│   ├── core/                 常量/配置/署名
│   ├── data/                 模块数据层（LSPosed 检测/作用域）
│   ├── lsposed/              LSPosed 守护进程桥接（反射）
│   ├── module/               root 模块安装器（AxManager 插件格式）
│   ├── ui/                   全部 Compose 界面（主题/组件/协议门控/各页面）
│   ├── update/               更新检查器
│   └── util/                 日志
├── licenses/                 上游项目原始许可文本
├── THIRD_PARTY_NOTICES.md    第三方开源声明
└── LICENSE                   GPL-3.0
```

## 开源合规

本项目严格遵守以下两个上游项目的开源二改协议：

| 项目 | 许可 | 引入内容 |
|------|------|---------|
| [LSPosed](https://github.com/LSPosed/LSPosed) | GPL-3.0 | 模块检测/启用停用/作用域/守护进程接口/日志 UI |
| [AxManager (Axeron)](https://github.com/fahrez182/AxManager) | Apache-2.0 | 无线调试激活（mDNS + TLS 配对）/Shell 执行器/插件安装/BusyBox/热重启 |

详见 [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md) 与 `licenses/` 目录。应用内「设置 → 关于」页也完整展示这些信息。

## 免责声明

v1.0.1 为测试版，功能与稳定性可能仍存在问题。修改系统参数、冻结应用、安装模块等操作可能影响设备稳定性，请在操作前做好备份。因使用本软件造成的任何损失，作者不承担责任。
