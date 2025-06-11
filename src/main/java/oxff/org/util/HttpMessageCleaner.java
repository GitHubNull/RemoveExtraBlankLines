/*
 * Copyright (c) 2024. All rights reserved.
 */

package oxff.org.util;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * HTTP 消息清理器
 * 
 * 负责清理 HTTP 消息中的多余空行
 * 
 * 主要功能：
 * - 安全地清理文本内容中的空行
 * - 处理不同的换行符格式
 * - 保护二进制数据不被破坏
 */
public class HttpMessageCleaner {
    
    /**
     * 移除字节数组开头的多余空行（仅处理字节级别的空行）
     * 
     * @param bytes 要处理的字节数组
     * @return 清理后的字节数组
     */
    public byte[] removeLeadingBlankLines(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return bytes;
        }
        
        int start = 0;
        boolean inBlankLine = true;
        boolean foundFirstNonBlankLine = false;
        
        for (int i = 0; i < bytes.length; i++) {
            byte b = bytes[i];
            
            if (b == '\r' || b == '\n') {
                if (inBlankLine && !foundFirstNonBlankLine) {
                    // 跳过开头的空行
                    start = i + 1;
                    if (b == '\r' && i + 1 < bytes.length && bytes[i + 1] == '\n') {
                        i++; // 跳过 \r\n 中的 \n
                        start = i + 1;
                    }
                }
                inBlankLine = true;
            } else if (b == ' ' || b == '\t') {
                // 空白字符，继续当前状态
            } else {
                // 非空白字符
                if (inBlankLine) {
                    foundFirstNonBlankLine = true;
                    inBlankLine = false;
                }
            }
        }
        
        // 如果所有内容都是空行，返回单个换行符
        if (start >= bytes.length) {
            return new byte[]{'\n'};
        }
        
        // 返回清理后的内容
        byte[] result = new byte[bytes.length - start];
        System.arraycopy(bytes, start, result, 0, result.length);
        return result;
    }
    
    /**
     * 清理文本内容中的多余空行
     * 
     * @param bytes 要处理的字节数组
     * @return 清理后的字节数组
     */
    public byte[] cleanTextContent(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return bytes;
        }
        
        try {
            // 尝试将字节数组转换为字符串
            String content = new String(bytes, StandardCharsets.UTF_8);
            String cleanedContent = removeExtraBlankLines(content);
            return cleanedContent.getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            // 如果转换失败，使用字节级别的处理
            return removeLeadingBlankLines(bytes);
        }
    }
    
    /**
     * 移除字符串中的多余空行
     * 
     * @param content 要处理的字符串
     * @return 清理后的字符串
     */
    private String removeExtraBlankLines(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }
        
        // 分割行
        String[] lines = content.split("\r?\n", -1);
        if (lines.length <= 1) {
            return content;
        }
        
        List<String> cleanedLines = new ArrayList<>();
        boolean previousLineWasBlank = false;
        boolean isFirstLine = true;
        
        for (String line : lines) {
            boolean currentLineIsBlank = line.trim().isEmpty();
            
            if (currentLineIsBlank) {
                if (isFirstLine) {
                    // 跳过开头的空行
                    continue;
                } else if (!previousLineWasBlank) {
                    // 保留第一个空行
                    cleanedLines.add(line);
                }
                // 跳过连续的空行
            } else {
                cleanedLines.add(line);
                isFirstLine = false;
            }
            
            previousLineWasBlank = currentLineIsBlank;
        }
        
        // 重新组装字符串
        return String.join(detectLineEnding(content), cleanedLines);
    }
    
    /**
     * 检测文本中使用的换行符类型
     * 
     * @param text 要检测的文本
     * @return 检测到的换行符
     */
    private String detectLineEnding(String text) {
        if (text == null || text.isEmpty()) {
            return "\n";
        }
        
        // 检查是否包含 \r\n
        if (text.contains("\r\n")) {
            return "\r\n";
        }
        
        // 检查是否包含 \r
        if (text.contains("\r")) {
            return "\r";
        }
        
        // 默认使用 \n
        return "\n";
    }
    
    /**
     * 清理指定位置开始的空行
     * 
     * @param bytes 字节数组
     * @param startPosition 开始位置
     * @return 清理后的字节数组
     */
    public byte[] cleanFromPosition(byte[] bytes, int startPosition) {
        if (bytes == null || startPosition >= bytes.length) {
            return bytes;
        }
        
        byte[] prefix = new byte[startPosition];
        System.arraycopy(bytes, 0, prefix, 0, startPosition);
        
        byte[] suffix = new byte[bytes.length - startPosition];
        System.arraycopy(bytes, startPosition, suffix, 0, suffix.length);
        
        byte[] cleanedSuffix = removeLeadingBlankLines(suffix);
        
        byte[] result = new byte[prefix.length + cleanedSuffix.length];
        System.arraycopy(prefix, 0, result, 0, prefix.length);
        System.arraycopy(cleanedSuffix, 0, result, prefix.length, cleanedSuffix.length);
        
        return result;
    }
    
    /**
     * 检查字节数组是否只包含空白字符
     * 
     * @param bytes 要检查的字节数组
     * @return 如果只包含空白字符返回 true
     */
    public boolean isOnlyWhitespace(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return true;
        }
        
        for (byte b : bytes) {
            if (b != ' ' && b != '\t' && b != '\r' && b != '\n') {
                return false;
            }
        }
        
        return true;
    }
} 