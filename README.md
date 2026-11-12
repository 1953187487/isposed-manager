# Isposed Manager

**沧海免root管理器** — 一款开源的 Android 免 root 管理器，基于 [LSPosed](https://github.com/LSPosed/LSPosed) 与 [AxManager (Axeron)](https://github.com/fahrez182/AxManager) 开发。

> 作者：**万古沧海**（B站同名：万古沧海 · GitHub：https://github.com/1953187487）
> 支持：**Android 9 ~ Android 17**
> 许可：**GPL-3.0**（严格遵守 LSPosed GPL-3.0 与 AxManager Apache-2.0 的开源二改协议）

---

## 简介

Isposed Manager 是一款面向不 root 用户的 Xposed/LSPosed 模块管理器。它：

- 基于 **LSPosed** 的 UI 底层与全部功能（模块检测、启用/停用、作用域、日志）；
- 借鉴 **AxManager** 的免 root 思路，通过 **ADB 无线调试**激活系统级权限，无需 root；
- 支持 **自动安装 root 模块**（ZIP：`module.prop` + `customize.sh`），并通过热重启（Re-ignite）生效；
- 内置 **AI 能力**：自定义服务商 API 配置、拉取/测试模型、AI 转换模块、AI 直接开发模块。

## 功能特性

### 1. 基础架构与功能
- 基于 LSPosed 的 UI 底层与全部功能
- 支持自动安装 root 模块（AxManager 插件格式）
- 无线调试一键激活（免 root，ADB over TCP + RSA 配对）

### 2. UI 与设置
- Material 3 界面，整体 UI 优化，大量 UI 元素（状态卡、统计、快捷功能、底部导航）
- 设置页展示开发作者（万古沧海）与原作者（LSPosed、AxManager）信息

### 3. AI 功能
- 设置中配置自定义服务商 API（OpenAI 兼容），支持**拉取模型**与**测试模型**
- **AI 转换模块**：将 Isposed/Xposed 模块转换为本软件适配的模块
- **AI 直接开发**：在「模块」页即可用 AI 直接生成可编译的 LSPosed 模块

### 4. 开源与署名
- 项目开源（GPL-3.0）
- 代码与作者处署名 **万古沧海**，并附开源账号（GitHub）

## 构建

```bash
# 环境：JDK 17、Android SDK (compileSdk 36 / build-tools 36)
echo "sdk.dir=/path/to/android-sdk" > local.properties
./gradlew :app:assembleRelease
# 产物：app/build/outputs/apk/release/app-release.apk
```

## 目录结构

```
app/                         主应用（管理器）
├── src/main/java/com/wangucanghai/isposed/
│   ├── adb/                  ADB 无线调试客户端（免root核心）
│   ├── ai/                   AI 客户端/配置/转换/代码生成
│   ├── core/                 常量/配置/署名
│   ├── data/                 模块数据层（LSPosed 检测/作用域）
│   ├── lsposed/              LSPosed 守护进程桥接（反射）
│   ├── module/               root 模块安装器（AxManager 插件格式）
│   ├── ui/                   全部界面
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
| [AxManager (Axeron)](https://github.com/fahrez182/AxManager) | Apache-2.0 | 无线调试激活/Shell 执行器/插件安装/BusyBox/热重启 |

详见 [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md) 与 `licenses/` 目录。应用内「设置 → 关于」页也完整展示这些信息。

## 免责声明

修改系统参数、冻结应用、安装模块等操作可能影响设备稳定性，请在操作前做好备份。因使用本软件造成的任何损失，作者不承担责任。
