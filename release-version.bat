@echo off
setlocal enabledelayedexpansion

:: Release Version Script for Remove Extra Blank Lines Plugin
:: Usage: release-version.bat [version]
:: Example: release-version.bat 1.0.2

if "%~1"=="" (
    echo 用法: %0 ^<版本号^>
    echo 示例: %0 1.0.2
    echo.
    echo 当前已有版本:
    git tag -l | findstr "^v"
    exit /b 1
)

set VERSION=%~1

echo === Remove Extra Blank Lines 版本发布工具 ===
echo.

:: 验证版本号格式 - 简化的检查
echo [INFO] 验证版本号格式: %VERSION%
echo %VERSION% | findstr /r "^[0-9][0-9]*\.[0-9][0-9]*\.[0-9][0-9]*$" >nul
if errorlevel 1 (
    echo [ERROR] 版本号格式无效: %VERSION%
    echo [INFO] 请使用格式: X.Y.Z 例如: 1.0.2
    exit /b 1
)

:: 检查Git状态
echo [INFO] 检查Git工作目录状态...
git status --porcelain > temp_status.txt 2>nul
if exist temp_status.txt (
    for /f %%i in ('type temp_status.txt ^| find /c /v ""') do set STATUS_COUNT=%%i
    if !STATUS_COUNT! GTR 0 (
        echo [ERROR] 工作目录不干净，请先提交或暂存所有更改
        git status --short
        del temp_status.txt
        exit /b 1
    )
    del temp_status.txt
)
echo [SUCCESS] Git状态检查通过

:: 检查版本是否已存在
echo [INFO] 检查版本标签是否已存在...
git tag -l | findstr "^v%VERSION%$" >nul
if not errorlevel 1 (
    echo [ERROR] 版本标签 v%VERSION% 已存在
    echo [INFO] 现有标签:
    git tag -l | findstr "^v"
    exit /b 1
)

:: 更新README中的版本信息
echo [INFO] 更新README.md中的版本信息...

:: 使用PowerShell进行文件替换
powershell -Command "$content = Get-Content 'README.md' -Raw; $content = $content -replace 'RemoveExtraBlankLines-[\d\.]+\.jar', 'RemoveExtraBlankLines-%VERSION%.jar'; $content = $content -replace '项目版本：[\d\.]+', '项目版本：%VERSION%'; Set-Content 'README.md' $content -NoNewline"

if errorlevel 1 (
    echo [ERROR] 更新README.md失败
    exit /b 1
)

echo [SUCCESS] README.md版本信息已更新

:: 确认发布
echo.
echo [WARNING] 即将发布版本 v%VERSION%
echo [INFO] 这将会:
echo   1. 创建版本标签 v%VERSION%
echo   2. 推送到GitHub
echo   3. 触发自动构建和发布
echo.

set /p CONFIRM="确认继续? (y/N): "
if /i not "%CONFIRM%"=="y" (
    echo [INFO] 发布已取消
    exit /b 0
)

:: 检查README是否有更改
git status --porcelain README.md > temp_readme_status.txt 2>nul
if exist temp_readme_status.txt (
    for /f %%i in ('type temp_readme_status.txt ^| find /c /v ""') do set README_CHANGED=%%i
    if !README_CHANGED! GTR 0 (
        echo [INFO] 提交README更改...
        git add README.md
        git commit -m "📝 更新版本信息到 v%VERSION%"
        if errorlevel 1 (
            echo [ERROR] 提交失败
            del temp_readme_status.txt
            exit /b 1
        )
        echo [SUCCESS] 版本信息更改已提交
    )
    del temp_readme_status.txt
)

:: 创建标签
echo [INFO] 创建版本标签: v%VERSION%
git tag -a "v%VERSION%" -m "🚀 Release version %VERSION%

自动生成的版本标签
- 版本: %VERSION%
- 创建时间: %date% %time%"

if errorlevel 1 (
    echo [ERROR] 创建标签失败
    exit /b 1
)

echo [SUCCESS] 标签 v%VERSION% 创建成功

:: 推送标签
echo [INFO] 推送标签到远程仓库...
git push origin main
if errorlevel 1 (
    echo [ERROR] 推送main分支失败
    exit /b 1
)

git push origin "v%VERSION%"
if errorlevel 1 (
    echo [ERROR] 推送标签失败
    exit /b 1
)

echo [SUCCESS] 标签已推送到远程仓库
echo [INFO] GitHub Actions将自动开始构建和发布流程

:: 获取仓库URL用于显示链接
for /f "tokens=*" %%i in ('git remote get-url origin 2^>nul') do set REPO_URL=%%i
if defined REPO_URL (
    set REPO_URL=!REPO_URL:https://github.com/=!
    set REPO_URL=!REPO_URL:.git=!
)

echo.
echo [SUCCESS] 🎉 版本 v%VERSION% 发布流程已启动!
echo [INFO] GitHub Actions正在构建...
if defined REPO_URL (
    echo [INFO] 查看进度: https://github.com/!REPO_URL!/actions
    echo [INFO] 发布完成后，您可以在以下位置找到文件:
    echo [INFO]   https://github.com/!REPO_URL!/releases/tag/v%VERSION%
)

pause 