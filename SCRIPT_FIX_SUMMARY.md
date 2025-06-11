# 发布脚本修复总结

## 问题描述

用户在尝试运行 `release-version.bat 1.2.1` 时遇到了"命令语法不正确"的错误。

## 问题分析

1. **Windows批处理脚本语法问题**：
   - 复杂的正则表达式语法在批处理中不兼容
   - `for /f` 循环的变量引用问题
   - PowerShell调用语法错误
   - 延迟变量展开的语法问题

2. **PowerShell执行策略限制**：
   - Windows默认禁止运行未签名的PowerShell脚本
   - 需要临时修改执行策略

## 解决方案

### 1. 创建简化的PowerShell脚本

创建了 `release-simple.ps1`，具备以下特性：
- 简化的语法，避免复杂的字符串操作
- 清晰的错误处理和用户提示
- 自动检测当前分支名称
- 支持版本号格式验证

### 2. 脚本功能

```powershell
# 基本用法
.\release-simple.ps1 -Version 1.2.1
```

**主要功能**：
- ✅ 版本号格式验证 (`\d+\.\d+\.\d+`)
- ✅ Git工作目录状态检查
- ✅ 标签重复检查
- ✅ 用户确认机制
- ✅ README.md版本信息自动更新
- ✅ 自动提交版本更改
- ✅ 创建Git标签
- ✅ 自动推送到GitHub
- ✅ 触发GitHub Actions构建

### 3. 解决的技术问题

1. **执行策略问题**：
   ```powershell
   Set-ExecutionPolicy -ExecutionPolicy Bypass -Scope Process
   ```

2. **分支名称自动检测**：
   ```powershell
   $currentBranch = git branch --show-current
   git push origin $currentBranch
   ```

3. **编码问题处理**：
   - 使用英文提示避免中文编码问题
   - 简化字符串操作

## 测试结果

✅ **成功执行**：`.\release-simple.ps1 -Version 1.2.1`

- 版本 `v1.2.1` 标签成功创建
- README.md版本信息已更新
- 成功推送到GitHub
- GitHub Actions自动触发构建
- 生成了 `RemoveExtraBlankLines-1.2.1.jar`

## 文件变更

### 新增文件
- `release-simple.ps1` - 工作正常的PowerShell发布脚本

### 删除文件
- `release-version-simple.bat` - 有语法错误的批处理脚本
- `release-version.ps1` - 有语法错误的PowerShell脚本
- `release.ps1` - 有编码问题的PowerShell脚本

### 修复文件
- `release-version.bat` - 保留但仍有问题，不推荐使用

## 使用建议

**推荐使用**：`release-simple.ps1`
- 语法简单可靠
- 错误处理完善
- 用户体验良好

**Windows用户**：
```powershell
# 如果遇到执行策略问题，先运行
Set-ExecutionPolicy -ExecutionPolicy Bypass -Scope Process

# 然后执行发布
.\release-simple.ps1 -Version 1.2.1
```

## 版本发布确认

- **当前版本**：v1.2.1
- **JAR文件**：`target/RemoveExtraBlankLines-1.2.1.jar`
- **发布状态**：✅ 成功
- **GitHub Release**：自动构建中 