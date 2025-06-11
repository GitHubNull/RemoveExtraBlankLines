#!/bin/bash

# 本地构建测试脚本
# 用于在推送到 GitHub 前验证构建是否正常

echo "🚀 开始本地构建测试..."

# 检查 Java 环境
echo "检查 Java 版本..."
java -version
if [ $? -ne 0 ]; then
    echo "❌ Java 未安装或不在 PATH 中"
    exit 1
fi

# 检查 Maven 环境
echo "检查 Maven 版本..."
mvn -version
if [ $? -ne 0 ]; then
    echo "❌ Maven 未安装或不在 PATH 中"
    exit 1
fi

# 清理旧的构建产物
echo "清理构建目录..."
mvn clean

# 编译项目
echo "编译项目..."
mvn compile
if [ $? -ne 0 ]; then
    echo "❌ 编译失败"
    exit 1
fi

# 打包项目
echo "打包项目..."
mvn package -DskipTests
if [ $? -ne 0 ]; then
    echo "❌ 打包失败"
    exit 1
fi

# 验证构建产物
echo "验证构建产物..."
ls -la target/

# 检查 JAR 文件是否生成
JAR_FILES=$(find target/ -name "*.jar" -type f | wc -l)
if [ $JAR_FILES -eq 0 ]; then
    echo "❌ 没有生成 JAR 文件"
    exit 1
fi

echo "✅ 找到 $JAR_FILES 个 JAR 文件:"
find target/ -name "*.jar" -type f -exec ls -lh {} \;

# 验证 JAR 文件内容
MAIN_JAR=$(find target/ -name "RemoveExtraBlankLines-*.jar" -not -name "*sources*" | head -1)
if [ -n "$MAIN_JAR" ]; then
    echo "验证主要 JAR 文件: $MAIN_JAR"
    jar -tf "$MAIN_JAR" | head -10
    echo "..."
    echo "JAR 文件包含 $(jar -tf "$MAIN_JAR" | wc -l) 个文件"
else
    echo "❌ 未找到主要 JAR 文件"
    exit 1
fi

echo "🎉 本地构建测试成功！"
echo "现在可以安全地推送到 GitHub 了。" 