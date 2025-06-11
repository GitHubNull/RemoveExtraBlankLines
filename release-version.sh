#!/bin/bash

# Release Version Script for Remove Extra Blank Lines Plugin
# Usage: ./release-version.sh [version]
# Example: ./release-version.sh 1.0.2

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 打印带颜色的消息
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查Git状态
check_git_status() {
    print_info "检查Git工作目录状态..."
    
    if [ -n "$(git status --porcelain)" ]; then
        print_error "工作目录不干净，请先提交或暂存所有更改："
        git status --short
        exit 1
    fi
    
    if ! git diff --quiet HEAD origin/main; then
        print_warning "本地分支与远程main分支不同步"
        print_info "正在同步..."
        git push origin main
    fi
    
    print_success "Git状态检查通过"
}

# 验证版本号格式
validate_version() {
    local version=$1
    if [[ ! $version =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
        print_error "版本号格式无效: $version"
        print_info "请使用格式: X.Y.Z (例如: 1.0.2)"
        exit 1
    fi
}

# 检查版本是否已存在
check_version_exists() {
    local version=$1
    local tag="v$version"
    
    if git tag -l | grep -q "^$tag$"; then
        print_error "版本标签 $tag 已存在"
        print_info "现有标签:"
        git tag -l | grep "^v" | sort -V
        exit 1
    fi
}

# 更新README中的版本信息
update_readme_version() {
    local version=$1
    print_info "更新README.md中的版本信息..."
    
    # 更新JAR文件名
    sed -i.bak "s/RemoveExtraBlankLines-[0-9]\+\.[0-9]\+\.[0-9]\+\.jar/RemoveExtraBlankLines-$version.jar/g" README.md
    
    # 更新项目版本
    sed -i.bak "s/项目版本：[0-9]\+\.[0-9]\+\.[0-9]\+/项目版本：$version/g" README.md
    
    # 清理备份文件
    rm -f README.md.bak
    
    print_success "README.md版本信息已更新"
}

# 生成更新日志条目
generate_changelog_entry() {
    local version=$1
    local date=$(date +"%Y-%m-%d")
    
    print_info "为版本 $version 生成更新日志条目..."
    
    # 获取最新的标签
    local last_tag=$(git tag -l "v*" | sort -V | tail -n 1)
    
    if [ -n "$last_tag" ]; then
        print_info "获取从 $last_tag 以来的提交..."
        local commits=$(git log --pretty=format:"- %s" --no-merges "$last_tag..HEAD")
        
        if [ -n "$commits" ]; then
            print_info "发现以下新提交:"
            echo "$commits"
            
            # 询问是否添加到README
            echo ""
            read -p "是否将这些更改添加到README.md的更新日志中? (y/N): " add_changelog
            
            if [[ $add_changelog =~ ^[Yy]$ ]]; then
                # 在更新日志部分添加新版本
                local changelog_entry="### v$version ($date)
$commits

"
                
                # 在## 更新日志下面插入新版本
                if grep -q "## 更新日志" README.md; then
                    # 使用sed在"## 更新日志"后插入新内容
                    sed -i.bak "/## 更新日志/a\\
\\
$changelog_entry" README.md
                    rm -f README.md.bak
                    print_success "更新日志已添加到README.md"
                else
                    print_warning "README.md中未找到'## 更新日志'部分"
                fi
            fi
        else
            print_info "没有找到新的提交"
        fi
    else
        print_info "这是第一个版本，跳过提交历史分析"
    fi
}

# 创建版本标签
create_version_tag() {
    local version=$1
    local tag="v$version"
    
    print_info "创建版本标签: $tag"
    
    # 提交README更改（如果有）
    if [ -n "$(git status --porcelain README.md)" ]; then
        git add README.md
        git commit -m "📝 更新版本信息到 v$version"
        print_success "版本信息更改已提交"
    fi
    
    # 创建标签
    git tag -a "$tag" -m "🚀 Release version $version

自动生成的版本标签
- 版本: $version
- 创建时间: $(date)
- 提交: $(git rev-parse HEAD)"
    
    print_success "标签 $tag 创建成功"
}

# 推送标签和触发发布
push_and_release() {
    local version=$1
    local tag="v$version"
    
    print_info "推送标签到远程仓库..."
    
    # 推送所有更改和标签
    git push origin main
    git push origin "$tag"
    
    print_success "标签已推送到远程仓库"
    print_info "GitHub Actions将自动开始构建和发布流程"
    print_info "查看进度: https://github.com/$(git remote get-url origin | sed 's/.*github.com[:/]\([^/]*\/[^/]*\)\.git.*/\1/')/actions"
}

# 主函数
main() {
    echo -e "${BLUE}=== Remove Extra Blank Lines 版本发布工具 ===${NC}"
    echo ""
    
    # 检查参数
    if [ $# -eq 0 ]; then
        echo "用法: $0 <版本号>"
        echo "示例: $0 1.0.2"
        echo ""
        echo "当前已有版本:"
        git tag -l | grep "^v" | sort -V | sed 's/^/  /'
        exit 1
    fi
    
    local version=$1
    
    # 验证版本号
    validate_version "$version"
    
    # 检查Git状态
    check_git_status
    
    # 检查版本是否已存在
    check_version_exists "$version"
    
    # 更新版本信息
    update_readme_version "$version"
    
    # 生成更新日志
    generate_changelog_entry "$version"
    
    # 确认发布
    echo ""
    print_warning "即将发布版本 v$version"
    print_info "这将会:"
    echo "  1. 创建版本标签 v$version"
    echo "  2. 推送到GitHub"
    echo "  3. 触发自动构建和发布"
    echo ""
    
    read -p "确认继续? (y/N): " confirm
    if [[ ! $confirm =~ ^[Yy]$ ]]; then
        print_info "发布已取消"
        exit 0
    fi
    
    # 创建标签
    create_version_tag "$version"
    
    # 推送和发布
    push_and_release "$version"
    
    echo ""
    print_success "🎉 版本 v$version 发布流程已启动!"
    print_info "GitHub Actions正在构建..."
    print_info "发布完成后，您可以在以下位置找到文件:"
    print_info "  https://github.com/$(git remote get-url origin | sed 's/.*github.com[:/]\([^/]*\/[^/]*\)\.git.*/\1/')/releases/tag/v$version"
}

# 执行主函数
main "$@" 