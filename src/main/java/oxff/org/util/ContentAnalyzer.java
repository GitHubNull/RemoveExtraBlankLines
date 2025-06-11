/*
 * Copyright (c) 2024. All rights reserved.
 */

package oxff.org.util;

import burp.api.montoya.http.message.HttpHeader;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 内容分析器
 * 
 * 负责分析 HTTP 消息内容的类型，判断是否为二进制内容
 * 
 * 主要功能：
 * - 优先检查HTTP头部的Content-Type信息
 * - 检测二进制内容
 * - 判断是否可以安全地作为文本处理
 * - 正确处理UTF-8编码的字符
 */
public class ContentAnalyzer {
    
    // 文本类型的Content-Type正则表达式
    private static final Pattern TEXT_CONTENT_TYPE_PATTERN = Pattern.compile(
        "^(text/.*|application/json|application/xml|application/javascript|application/x-javascript|" +
        "application/ecmascript|application/x-www-form-urlencoded|application/graphql|" +
        "application/x-yaml|application/yaml|application/rss\\+xml|application/atom\\+xml|" +
        "application/xhtml\\+xml|application/soap\\+xml|application/vnd\\.api\\+json|" +
        "application/ld\\+json|application/hal\\+json|application/problem\\+json).*",
        Pattern.CASE_INSENSITIVE
    );
    
    // 二进制类型的Content-Type正则表达式
    private static final Pattern BINARY_CONTENT_TYPE_PATTERN = Pattern.compile(
        "^(image/.*|audio/.*|video/.*|application/octet-stream|application/pdf|" +
        "application/zip|application/x-zip-compressed|application/x-rar-compressed|" +
        "application/x-7z-compressed|application/x-tar|application/gzip|" +
        "application/x-executable|application/x-msdownload|application/x-msdos-program|" +
        "application/java-archive|application/x-java-archive|font/.*|" +
        "application/vnd\\.ms-.*|application/msword|application/vnd\\.openxmlformats-.*|" +
        "application/x-shockwave-flash|application/x-font-.*|application/font-.*).*",
        Pattern.CASE_INSENSITIVE
    );
    
    // 常见的二进制文件魔数
    private static final byte[][] BINARY_SIGNATURES = {
        // 图像文件
        {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}, // JPEG
        {(byte) 0x89, 0x50, 0x4E, 0x47}, // PNG
        {0x47, 0x49, 0x46, 0x38}, // GIF
        {0x42, 0x4D}, // BMP
        {0x49, 0x49, 0x2A, 0x00}, // TIFF (little endian)
        {0x4D, 0x4D, 0x00, 0x2A}, // TIFF (big endian)
        {0x52, 0x49, 0x46, 0x46}, // WEBP (RIFF container)
        
        // 压缩文件
        {0x50, 0x4B, 0x03, 0x04}, // ZIP
        {0x50, 0x4B, 0x05, 0x06}, // ZIP (empty)
        {0x50, 0x4B, 0x07, 0x08}, // ZIP (spanned)
        {0x52, 0x61, 0x72, 0x21}, // RAR v1.5+
        {0x37, 0x7A, (byte) 0xBC, (byte) 0xAF}, // 7Z
        {0x1F, (byte) 0x8B}, // GZIP
        {0x42, 0x5A, 0x68}, // BZIP2
        
        // 音频文件
        {0x49, 0x44, 0x33}, // MP3 (ID3v2)
        {(byte) 0xFF, (byte) 0xFB}, // MP3 (MPEG-1 Layer 3)
        {(byte) 0xFF, (byte) 0xF3}, // MP3 (MPEG-2 Layer 3)
        {0x52, 0x49, 0x46, 0x46}, // WAV/AVI (RIFF)
        {0x66, 0x4C, 0x61, 0x43}, // FLAC
        {0x4F, 0x67, 0x67, 0x53}, // OGG
        
        // 视频文件
        {0x00, 0x00, 0x00, 0x18, 0x66, 0x74, 0x79, 0x70}, // MP4
        {0x00, 0x00, 0x00, 0x20, 0x66, 0x74, 0x79, 0x70}, // MP4 alternative
        {0x1A, 0x45, (byte) 0xDF, (byte) 0xA3}, // MKV/WEBM
        {0x46, 0x4C, 0x56, 0x01}, // FLV
        
        // 文档文件
        {0x25, 0x50, 0x44, 0x46}, // PDF
        {(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0}, // MS Office (DOC, XLS, PPT)
        {0x7B, 0x5C, 0x72, 0x74, 0x66, 0x31}, // RTF
        
        // 可执行文件
        {0x4D, 0x5A}, // Windows PE/DLL/EXE
        {0x7F, 0x45, 0x4C, 0x46}, // ELF (Linux)
        {(byte) 0xFE, (byte) 0xED, (byte) 0xFA, (byte) 0xCE}, // Mach-O (macOS) 32-bit
        {(byte) 0xFE, (byte) 0xED, (byte) 0xFA, (byte) 0xCF}, // Mach-O (macOS) 64-bit
        
        // 其他常见二进制格式
        {0x00, 0x00, 0x02, 0x00}, // ICO
        {0x00, 0x00, 0x01, 0x00}, // ICO alternative
        {(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE}, // Java Class
    };
    
    /**
     * 检查HTTP请求内容是否包含文本（优先检查Content-Type）
     * 
     * @param request HTTP请求对象
     * @return 如果包含文本内容返回 true
     */
    public boolean containsTextContent(HttpRequest request) {
        // 1. 首先检查Content-Type头部
        String contentType = getContentType(request.headers());
        if (contentType != null) {
            if (isTextContentType(contentType)) {
                return true;
            }
            if (isBinaryContentType(contentType)) {
                return false;
            }
        }
        
        // 2. 如果Content-Type无法确定，则检查请求体内容
        byte[] bodyBytes = request.body().getBytes();
        if (bodyBytes.length > 0) {
            return containsTextContent(bodyBytes);
        }
        
        // 3. 没有请求体的情况下，认为是文本（大多数GET请求）
        return true;
    }
    
    /**
     * 检查HTTP响应内容是否包含文本（优先检查Content-Type）
     * 
     * @param response HTTP响应对象
     * @return 如果包含文本内容返回 true
     */
    public boolean containsTextContent(HttpResponse response) {
        // 1. 首先检查Content-Type头部
        String contentType = getContentType(response.headers());
        if (contentType != null) {
            if (isTextContentType(contentType)) {
                return true;
            }
            if (isBinaryContentType(contentType)) {
                return false;
            }
        }
        
        // 2. 如果Content-Type无法确定，则检查响应体内容
        byte[] bodyBytes = response.body().getBytes();
        if (bodyBytes.length > 0) {
            return containsTextContent(bodyBytes);
        }
        
        // 3. 没有响应体的情况下，认为是文本
        return true;
    }
    
    /**
     * 检查字节数组内容是否包含文本（仅当无法从HTTP头部确定时使用）
     * 
     * @param bytes 要检查的字节数组
     * @return 如果包含文本内容返回 true
     */
    public boolean containsTextContent(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return true; // 空内容视为文本
        }
        
        // 检查是否为已知的二进制格式
        if (startsWithBinarySignature(bytes)) {
            return false;
        }
        
        // 检查是否包含NULL字节（强烈暗示二进制内容）
        if (containsNullBytes(bytes)) {
            return false;
        }
        
        // 检查是否为有效的UTF-8文本
        return isValidUTF8Text(bytes);
    }
    
    /**
     * 检查是否可以安全地作为文本处理
     * 
     * @param request HTTP请求对象
     * @return 如果可以安全处理返回 true
     */
    public boolean isSafeToProcessAsText(HttpRequest request) {
        return containsTextContent(request);
    }
    
    /**
     * 检查是否可以安全地作为文本处理  
     * 
     * @param response HTTP响应对象
     * @return 如果可以安全处理返回 true
     */
    public boolean isSafeToProcessAsText(HttpResponse response) {
        return containsTextContent(response);
    }
    
    /**
     * 检查字节数组是否可以安全地作为文本处理
     * 
     * @param bytes 要检查的字节数组
     * @return 如果可以安全处理返回 true
     */
    public boolean isSafeToProcessAsText(byte[] bytes) {
        return containsTextContent(bytes);
    }
    
    /**
     * 从HTTP头部列表中获取Content-Type值
     * 
     * @param headers HTTP头部列表
     * @return Content-Type值，如果不存在返回null
     */
    private String getContentType(List<HttpHeader> headers) {
        for (HttpHeader header : headers) {
            if ("content-type".equalsIgnoreCase(header.name())) {
                return header.value().trim();
            }
        }
        return null;
    }
    
    /**
     * 检查Content-Type是否为文本类型
     * 
     * @param contentType Content-Type头部值
     * @return 如果是文本类型返回 true
     */
    private boolean isTextContentType(String contentType) {
        return TEXT_CONTENT_TYPE_PATTERN.matcher(contentType).matches();
    }
    
    /**
     * 检查Content-Type是否为二进制类型
     * 
     * @param contentType Content-Type头部值
     * @return 如果是二进制类型返回 true
     */
    private boolean isBinaryContentType(String contentType) {
        return BINARY_CONTENT_TYPE_PATTERN.matcher(contentType).matches();
    }
    
    /**
     * 检查字节数组是否以已知的二进制签名开始
     * 
     * @param bytes 要检查的字节数组
     * @return 如果匹配二进制签名返回 true
     */
    private boolean startsWithBinarySignature(byte[] bytes) {
        if (bytes == null || bytes.length < 2) {
            return false;
        }
        
        for (byte[] signature : BINARY_SIGNATURES) {
            if (bytes.length >= signature.length) {
                boolean matches = true;
                for (int i = 0; i < signature.length; i++) {
                    if (bytes[i] != signature[i]) {
                        matches = false;
                        break;
                    }
                }
                if (matches) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * 检查字节数组是否包含NULL字节
     * 
     * @param bytes 要检查的字节数组
     * @return 如果包含NULL字节返回 true
     */
    private boolean containsNullBytes(byte[] bytes) {
        for (byte b : bytes) {
            if (b == 0) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 检查字节数组是否为有效的UTF-8文本
     * 
     * @param bytes 要检查的字节数组
     * @return 如果是有效的UTF-8文本返回 true
     */
    private boolean isValidUTF8Text(byte[] bytes) {
        try {
            // 尝试解码为UTF-8字符串
            String text = new String(bytes, StandardCharsets.UTF_8);
            
            // 重新编码并比较
            byte[] reencoded = text.getBytes(StandardCharsets.UTF_8);
            
            // 如果重新编码后的长度差距太大，可能不是有效的UTF-8
            if (Math.abs(reencoded.length - bytes.length) > bytes.length * 0.1) {
                return false;
            }
            
            // 检查是否包含太多替换字符（U+FFFD）
            long replacementCount = text.codePoints()
                .filter(cp -> cp == 0xFFFD)
                .count();
            
            // 如果替换字符超过10%，认为不是有效文本
            double replacementRatio = (double) replacementCount / text.length();
            if (replacementRatio > 0.1) {
                return false;
            }
            
            // 通过所有检查，认为是有效的UTF-8文本
            return true;
            
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 检查内容是否主要是文本
     * 
     * @param bytes 要检查的字节数组
     * @return 如果主要是文本返回 true
     */
    public boolean isPrimarilyText(byte[] bytes) {
        return containsTextContent(bytes);
    }
} 