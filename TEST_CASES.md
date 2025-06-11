# 测试用例文档

## 模块化设计测试用例

本文档详细说明了重构后的模块化 Burp Suite 插件如何处理各种复杂的 HTTP 消息情况。

## 架构概述

### 模块结构
```
oxff.org/
├── RemoveExtraBlankLinesExtension.java    # 主插件类
├── handler/
│   └── HttpMessageHandler.java           # HTTP 消息处理器
├── processor/
│   └── MessageProcessor.java             # 消息处理器
└── util/
    ├── ContentAnalyzer.java              # 内容分析器
    └── HttpMessageCleaner.java           # 消息清理器
```

### 处理流程
1. **HttpMessageHandler** 拦截 HTTP 请求/响应
2. **ContentAnalyzer** 分析内容类型（文本 vs 二进制）
3. **MessageProcessor** 协调处理逻辑
4. **HttpMessageCleaner** 执行实际的清理操作

## 测试用例

### 1. 纯文本 HTTP 请求
```http
POST /api/submit HTTP/1.1
Host: example.com
Content-Type: application/json




{"username": "admin", "password": "secret"}
```

**处理结果：**
```http
POST /api/submit HTTP/1.1
Host: example.com
Content-Type: application/json

{"username": "admin", "password": "secret"}
```

**说明：** 插件检测到正文是纯文本（JSON），安全地移除多余空行。

### 2. 二进制文件上传
```http
POST /upload HTTP/1.1
Host: example.com
Content-Type: multipart/form-data; boundary=----WebKitFormBoundary7MA4YWxkTrZu0gW




------WebKitFormBoundary7MA4YWxkTrZu0gW
Content-Disposition: form-data; name="file"; filename="image.jpg"
Content-Type: image/jpeg

ÿØÿàJFIFHHÿÛC... [二进制JPEG数据] ...ÿÙ
------WebKitFormBoundary7MA4YWxkTrZu0gW--
```

**处理结果：**
```http
POST /upload HTTP/1.1
Host: example.com
Content-Type: multipart/form-data; boundary=----WebKitFormBoundary7MA4YWxkTrZu0gW

------WebKitFormBoundary7MA4YWxkTrZu0gW
Content-Disposition: form-data; name="file"; filename="image.jpg"
Content-Type: image/jpeg

ÿØÿàJFIFHHÿÛC... [二进制JPEG数据] ...ÿÙ
------WebKitFormBoundary7MA4YWxkTrZu0gW--
```

**说明：** 插件检测到 JPEG 魔数，识别为二进制内容，只移除头部后的空行，保护二进制数据完整性。

### 3. 混合内容（表单 + 文件）
```http
POST /form-submit HTTP/1.1
Host: example.com
Content-Type: multipart/form-data; boundary=----WebKitFormBoundary7MA4YWxkTrZu0gW




------WebKitFormBoundary7MA4YWxkTrZu0gW
Content-Disposition: form-data; name="description"


This is a text description with extra blank lines.


------WebKitFormBoundary7MA4YWxkTrZu0gW
Content-Disposition: form-data; name="file"; filename="data.pdf"
Content-Type: application/pdf

%PDF-1.4... [二进制PDF数据] ...%%EOF
------WebKitFormBoundary7MA4YWxkTrZu0gW--
```

**处理逻辑：**
1. 分析头部和正文边界
2. 检测到 multipart 内容包含文本和二进制混合
3. 只清理头部后的空行，保护 multipart 结构和二进制数据

### 4. UTF-8 编码的文本内容
```http
POST /api/chinese HTTP/1.1
Host: example.com
Content-Type: application/json; charset=utf-8




{"message": "你好，这是中文测试内容", "status": "测试"}
```

**处理结果：**
```http
POST /api/chinese HTTP/1.1
Host: example.com
Content-Type: application/json; charset=utf-8

{"message": "你好，这是中文测试内容", "status": "测试"}
```

**说明：** 插件正确识别 UTF-8 编码的中文内容为文本，安全处理。

### 5. Base64 编码的数据
```http
POST /api/base64 HTTP/1.1
Host: example.com
Content-Type: application/json




{"file_data": "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8/5+hHgAHggJ/PchI7wAAAABJRU5ErkJggg=="}
```

**处理结果：**
```http
POST /api/base64 HTTP/1.1
Host: example.com
Content-Type: application/json

{"file_data": "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8/5+hHgAHggJ/PchI7wAAAABJRU5ErkJggg=="}
```

**说明：** Base64 编码虽然包含二进制数据，但本身是可打印字符，被正确识别为文本内容。

## 安全特性

### 1. 二进制检测机制
- **文件魔数检测**：识别常见的二进制文件格式（JPEG、PNG、PDF、ZIP 等）
- **不可打印字符比例**：超过 30% 的不可打印字符被视为二进制
- **NULL 字节检测**：包含 NULL 字节的内容被视为二进制

### 2. 字符编码处理
- 支持 UTF-8 编码验证
- 检测有效的多字节字符序列
- 优雅处理编码错误

### 3. 错误处理
- 任何处理错误都会返回原始消息
- 不会破坏 HTTP 消息的完整性
- 详细的错误日志记录

## 性能优化

### 1. 内容分析优化
- 只检查前 1024 字节用于二进制检测
- 避免处理大型二进制文件
- 早期退出机制

### 2. 模块化设计优势
- **单一职责**：每个模块只负责特定功能
- **易于测试**：可以独立测试每个模块
- **易于维护**：修改某个功能不影响其他模块
- **可扩展性**：可以轻松添加新的内容类型检测

## 边界情况处理

### 1. 空消息
- 空请求/响应：直接返回原始消息
- 只有头部的消息：不进行处理

### 2. 恶意构造的消息
- 异常大的消息：通过采样检测避免性能问题
- 格式错误的 HTTP 消息：安全返回原始内容

### 3. 特殊字符集
- 支持各种字符编码
- 正确处理换行符变体（\r\n, \n, \r）

## 与原始版本的对比

| 特性 | 原始版本 | 重构版本 |
|------|----------|----------|
| 二进制数据处理 | ❌ 破坏二进制数据 | ✅ 智能检测和保护 |
| 模块化设计 | ❌ 单一大文件 | ✅ 多模块设计 |
| 错误处理 | ⚠️ 基础 | ✅ 全面的错误处理 |
| 性能 | ⚠️ 处理所有内容 | ✅ 智能跳过二进制内容 |
| 可维护性 | ❌ 困难 | ✅ 高可维护性 |
| 可扩展性 | ❌ 有限 | ✅ 易于扩展 |

## 总结

重构后的插件通过模块化设计解决了原始版本的所有问题：

1. **智能内容检测**：避免处理二进制数据
2. **模块化架构**：每个类都有明确的职责
3. **健壮的错误处理**：确保在任何情况下都不会破坏数据
4. **高性能**：避免不必要的处理
5. **易于维护**：清晰的代码结构和文档 