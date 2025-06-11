# 发版指南

本项目已配置自动化发版流程，开发者可以通过简单的 Git 标签操作来发布新版本。

## 🚀 发布新版本步骤

### 1. 确保代码已推送到主分支
```bash
git push origin main
```

### 2. 创建并推送版本标签
```bash
# 创建标签（替换为实际版本号）
git tag v1.0.0

# 推送标签到远程仓库
git push origin v1.0.0
```

### 3. 自动化发版流程

推送标签后，GitHub Actions 会自动执行以下步骤：

1. **环境准备**：设置 Java 17 环境
2. **依赖缓存**：缓存 Maven 依赖以加速构建
3. **版本更新**：自动更新 pom.xml 中的版本号
4. **编译构建**：编译并打包项目
5. **产物验证**：验证 JAR 文件生成成功
6. **发布说明**：自动生成详细的发布说明
7. **创建 Release**：在 GitHub 上创建新的 Release
8. **上传文件**：上传 JAR 文件到 Release

## 📋 版本号规范

### 语义化版本
- **主版本号**：不兼容的 API 修改
- **次版本号**：向下兼容的功能性新增
- **修订版本号**：向下兼容的 bug 修复

### 版本示例
```
v1.0.0    # 第一个正式版本
v1.0.1    # Bug 修复版本
v1.1.0    # 新功能版本
v2.0.0    # 重大版本更新
v1.0.0-alpha.1    # 预发布版本
v1.0.0-rc.1       # 候选版本
```

## 🔍 监控发版过程

### 查看 Actions 状态
1. 打开 GitHub 仓库页面
2. 点击 "Actions" 选项卡
3. 查看 "自动发布 Release" 工作流状态

### 常见问题排查
1. **编译失败**：检查代码是否有语法错误
2. **JAR 文件未生成**：检查 Maven 配置是否正确
3. **权限问题**：确保仓库有 Actions 权限

## 📦 发布产物

成功发版后，将在 GitHub Releases 页面生成：

1. **主要 JAR 文件**：`RemoveExtraBlankLines-v1.0.0.jar`
2. **源码 JAR**：`RemoveExtraBlankLines-v1.0.0-sources.jar`（如果存在）
3. **发布说明**：包含版本特性、安装方法、技术信息等

## 🔄 持续集成

### 自动验证
每次推送到 `main` 或 `develop` 分支时，CI 工作流会：
- 编译项目代码
- 运行单元测试
- 生成构建产物
- 保存构建结果到 Artifacts

### 手动触发
可以在 Actions 页面手动触发发版工作流：
1. 进入 "Actions" 页面
2. 选择 "自动发布 Release" 工作流
3. 点击 "Run workflow" 按钮

## 🛠️ 本地测试

发版前建议在本地测试构建：

```bash
# 编译项目
mvn clean compile

# 运行测试
mvn test

# 打包项目
mvn package

# 验证 JAR 文件
ls -la target/RemoveExtraBlankLines-*.jar
```

## 📧 发版通知

发版完成后，可以通过以下方式通知用户：
1. GitHub Release 页面会显示新版本
2. 在项目 Issues 或 Discussions 中发布公告
3. 更新项目主页的下载链接

---

**提示**：确保在发版前充分测试所有功能，并更新相关文档。 