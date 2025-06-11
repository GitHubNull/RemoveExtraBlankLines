# Remove Extra Blank Lines 插件配置指南

## 概述

本文档详细说明了如何配置和使用 Remove Extra Blank Lines 插件的高级功能，包括模块生效控制和目标域控制。

## 🎯 模块生效控制

### 基本概念

模块生效控制允许您选择性地在特定的 Burp Suite 模块中启用插件功能，而不影响其他模块的正常工作。

### 支持的模块

| 模块 | ToolType | 描述 | 推荐使用场景 |
|------|----------|------|------------|
| **Proxy** | `ToolType.PROXY` | 代理模块，拦截浏览器流量 | 日常浏览、流量清理 |
| **Repeater** | `ToolType.REPEATER` | 重发器模块，手动重发请求 | 手动测试、请求调试 |
| **Intruder** | `ToolType.INTRUDER` | 入侵者模块，自动化攻击 | 批量测试、参数爆破 |
| **Extensions** | `ToolType.EXTENSIONS` | 扩展模块，其他插件的请求 | 扩展集成、自动化脚本 |

### 配置方法

#### 方法1: 设置启用的模块集合

```java
// 只启用 Proxy 和 Repeater 模块
config.setEnabledModules(EnumSet.of(ToolType.PROXY, ToolType.REPEATER));

// 只启用 Proxy 模块
config.setEnabledModules(EnumSet.of(ToolType.PROXY));

// 启用所有模块（默认）
config.setEnabledModules(EnumSet.of(
    ToolType.PROXY, 
    ToolType.REPEATER, 
    ToolType.INTRUDER, 
    ToolType.EXTENSIONS
));
```

#### 方法2: 单独启用/禁用模块

```java
// 禁用 Intruder 模块
config.disableModule(ToolType.INTRUDER);

// 启用 Repeater 模块
config.enableModule(ToolType.REPEATER);
```

#### 方法3: 重置为默认配置

```java
// 重置为默认配置（所有模块启用）
config.resetToDefaults();
```

### 实际应用场景

#### 🔧 开发调试场景
```java
// 只在代理和重发器中清理空行，避免影响 Intruder 的批量测试
config.setEnabledModules(EnumSet.of(ToolType.PROXY, ToolType.REPEATER));
```

#### 🛡️ 渗透测试场景
```java
// 只在手动测试工具中使用，保持自动化工具的原始行为
config.setEnabledModules(EnumSet.of(ToolType.REPEATER));
```

#### 🌊 流量清理场景
```java
// 只在代理模块中清理，确保浏览器流量的整洁
config.setEnabledModules(EnumSet.of(ToolType.PROXY));
```

## 🌐 目标域控制

### 基本概念

目标域控制允许插件只处理在 Burp Suite 目标范围（Target Scope）内的域名，避免处理不相关的流量。

### 配置选项

- **全域模式**（默认）：`setTargetScopeOnly(false)`
  - 处理所有通过 Burp Suite 的 HTTP 流量
  - 适用于广泛的流量清理

- **目标域模式**：`setTargetScopeOnly(true)`
  - 只处理目标范围内的域名
  - 适用于专注的渗透测试

### 设置目标域的步骤

#### 步骤1: 在 Burp Suite 中设置目标域

1. **方法A: 通过 Site Map 添加**
   - 转到 "Target" -> "Site map"
   - 右键点击要添加的域名或 URL
   - 选择 "Add to scope"

2. **方法B: 通过 Scope 设置添加**
   - 转到 "Target" -> "Scope"
   - 点击 "Add" 按钮
   - 输入目标域名或 URL 模式
   - 设置包含/排除规则

#### 步骤2: 在插件中启用目标域控制

```java
// 启用目标域控制
config.setTargetScopeOnly(true);
```

#### 步骤3: 验证配置

检查插件输出日志，确认配置生效：

```
插件配置已更新:
  启用的模块: [PROXY, REPEATER, INTRUDER, EXTENSIONS]
  目标域限制: 仅目标域
```

### 实际应用场景

#### 🎯 专项测试
```java
// 只处理特定目标站点，避免处理其他无关流量
config.setTargetScopeOnly(true);
config.setEnabledModules(EnumSet.of(ToolType.PROXY, ToolType.REPEATER));
```

#### 🔍 大规模扫描
```java
// 在大规模扫描时，只处理目标域的流量以提高性能
config.setTargetScopeOnly(true);
```

#### 🌍 混合环境
```java
// 在混合测试环境中，只清理测试目标的流量
config.setTargetScopeOnly(true);
config.setEnabledModules(EnumSet.of(ToolType.PROXY, ToolType.REPEATER));
```

## 📋 配置模板

### 模板1: 日常开发使用
```java
// 配置：代理模块 + 全域处理
config.setEnabledModules(EnumSet.of(ToolType.PROXY));
config.setTargetScopeOnly(false);
```

### 模板2: 渗透测试使用
```java
// 配置：手动工具 + 目标域限制
config.setEnabledModules(EnumSet.of(ToolType.REPEATER));
config.setTargetScopeOnly(true);
```

### 模板3: 综合测试使用
```java
// 配置：所有模块 + 目标域限制
config.setEnabledModules(EnumSet.of(
    ToolType.PROXY, 
    ToolType.REPEATER, 
    ToolType.INTRUDER, 
    ToolType.EXTENSIONS
));
config.setTargetScopeOnly(true);
```

### 模板4: 保守使用
```java
// 配置：只在代理和重发器中使用，限制目标域
config.setEnabledModules(EnumSet.of(ToolType.PROXY, ToolType.REPEATER));
config.setTargetScopeOnly(true);
```

## 🔧 配置修改方法

### 方法1: 修改源代码（推荐）

1. 打开 `src/main/java/oxff/org/RemoveExtraBlankLinesExtension.java`
2. 找到 `demonstrateConfigUsage` 方法
3. 取消注释您需要的配置代码
4. 重新编译项目：`mvn clean package`
5. 重新加载插件

### 方法2: 运行时配置（高级）

如果您熟悉 Burp Suite 扩展开发，可以通过以下方式获取配置实例：

```java
// 获取插件实例（需要在扩展内部或通过反射）
RemoveExtraBlankLinesExtension extension = ...;
PluginConfig config = extension.getConfig();

// 修改配置
config.setTargetScopeOnly(true);
config.disableModule(ToolType.INTRUDER);
```

## 📊 配置验证

### 日志验证

插件启动时会在 Extensions -> Output 面板显示详细的配置信息：

```
Remove Extra Blank Lines 插件已加载
支持智能二进制检测和模块化设计
支持模块生效控制和目标域控制

Remove Extra Blank Lines 插件配置:
启用的模块: [PROXY, REPEATER, INTRUDER, EXTENSIONS]
作用范围: 所有域

=== 配置功能演示 ===
配置功能演示完成，当前使用默认配置
可通过修改代码中的示例来测试不同配置
===================
```

### 行为验证

1. **模块控制验证**：
   - 在禁用的模块中发送请求，检查是否不处理多余空行
   - 在启用的模块中发送请求，检查是否正常处理

2. **目标域控制验证**：
   - 向目标域外的站点发送请求，检查是否跳过处理
   - 向目标域内的站点发送请求，检查是否正常处理

## ⚠️ 注意事项

1. **性能考虑**：启用目标域控制可以减少不必要的处理，提高性能
2. **兼容性**：确保目标域设置与您的测试需求匹配
3. **调试建议**：修改配置后，建议查看日志确认配置生效
4. **备份配置**：修改前建议备份原有配置代码

## 🔗 相关资源

- [Burp Suite Target Scope 文档](https://portswigger.net/burp/documentation/desktop/tools/target/scope)
- [Montoya API ToolType 文档](https://portswigger.github.io/burp-extensions-montoya-api/javadoc/burp/api/montoya/core/ToolType.html)
- [插件主要文档](README.md)
- [测试用例文档](TEST_CASES.md) 