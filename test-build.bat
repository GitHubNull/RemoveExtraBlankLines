@echo off
setlocal enabledelayedexpansion

REM 本地构建测试脚本 (Windows 版本)
REM 用于在推送到 GitHub 前验证构建是否正常

echo 🚀 开始本地构建测试...

REM 检查 Java 环境
echo 检查 Java 版本...
java -version >nul 2>&1
if errorlevel 1 (
    echo ❌ Java 未安装或不在 PATH 中
    exit /b 1
)

REM 检查 Maven 环境
echo 检查 Maven 版本...
mvn -version >nul 2>&1
if errorlevel 1 (
    echo ❌ Maven 未安装或不在 PATH 中
    exit /b 1
)

REM 清理旧的构建产物
echo 清理构建目录...
mvn clean
if errorlevel 1 (
    echo ❌ 清理失败
    exit /b 1
)

REM 编译项目
echo 编译项目...
mvn compile
if errorlevel 1 (
    echo ❌ 编译失败
    exit /b 1
)

REM 打包项目
echo 打包项目...
mvn package -DskipTests
if errorlevel 1 (
    echo ❌ 打包失败
    exit /b 1
)

REM 验证构建产物
echo 验证构建产物...
dir target\*.jar >nul 2>&1
if errorlevel 1 (
    echo ❌ 没有生成 JAR 文件
    exit /b 1
)

echo ✅ 构建产物验证成功:
dir target\*.jar

echo 🎉 本地构建测试成功！
echo 现在可以安全地推送到 GitHub 了。

pause 