/*
 * Copyright (c) 2024. All rights reserved.
 */

package oxff.org.util;

import java.nio.charset.StandardCharsets;

/**
 * 内容分析器
 * 
 * 负责分析 HTTP 消息内容的类型，判断是否为二进制内容
 * 
 * 主要功能：
 * - 检测二进制内容
 * - 判断是否可以安全地作为文本处理
 * - 分析内容的字符编码
 */
public class ContentAnalyzer {
    
    // 常见的二进制文件魔数
    private static final byte[][] BINARY_SIGNATURES = {
        {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}, // JPEG
        {(byte) 0x89, 0x50, 0x4E, 0x47}, // PNG
        {0x47, 0x49, 0x46, 0x38}, // GIF
        {0x50, 0x4B, 0x03, 0x04}, // ZIP
        {0x50, 0x4B, 0x05, 0x06}, // ZIP (empty)
        {0x50, 0x4B, 0x07, 0x08}, // ZIP (spanned)
        {0x25, 0x50, 0x44, 0x46}, // PDF
        {(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0}, // MS Office
        {0x4D, 0x5A}, // Windows PE/DLL
        {0x7F, 0x45, 0x4C, 0x46}, // ELF
    };
    
    // 不可打印字符的阈值
    private static final double BINARY_THRESHOLD = 0.3; // 30% 的不可打印字符即认为是二进制
    
    /**
     * 检查内容是否包含文本
     * 
     * @param bytes 要检查的字节数组
     * @return 如果包含文本内容返回 true
     */
    public boolean containsTextContent(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return false;
        }
        
        // 检查是否为已知的二进制格式
        if (startsWithBinarySignature(bytes)) {
            return false;
        }
        
        // 统计不可打印字符的比例
        return calculateBinaryRatio(bytes) < BINARY_THRESHOLD;
    }
    
    /**
     * 检查是否可以安全地作为文本处理
     * 
     * @param bytes 要检查的字节数组
     * @return 如果可以安全处理返回 true
     */
    public boolean isSafeToProcessAsText(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return true; // 空内容可以安全处理
        }
        
        // 检查是否为已知的二进制格式
        if (startsWithBinarySignature(bytes)) {
            return false;
        }
        
        // 检查是否包含 null 字节（强烈暗示二进制内容）
        for (byte b : bytes) {
            if (b == 0) {
                return false;
            }
        }
        
        // 检查不可打印字符比例
        double binaryRatio = calculateBinaryRatio(bytes);
        return binaryRatio < BINARY_THRESHOLD;
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
     * 计算不可打印字符的比例
     * 
     * @param bytes 要分析的字节数组
     * @return 不可打印字符的比例 (0.0 到 1.0)
     */
    private double calculateBinaryRatio(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return 0.0;
        }
        
        int nonPrintableCount = 0;
        int sampleSize = Math.min(bytes.length, 1024); // 只检查前 1024 字节以提高性能
        
        for (int i = 0; i < sampleSize; i++) {
            byte b = bytes[i];
            
            // 检查是否为可打印字符
            // ASCII 范围：32-126, 以及一些常见的空白字符
            if (b < 32 && b != 9 && b != 10 && b != 13) { // 不是 tab, LF, CR
                nonPrintableCount++;
            } else if (b > 126 && !isValidUTF8Continuation(bytes, i)) {
                nonPrintableCount++;
            }
        }
        
        return (double) nonPrintableCount / sampleSize;
    }
    
    /**
     * 检查是否为有效的 UTF-8 继续字节
     * 
     * @param bytes 字节数组
     * @param index 当前位置
     * @return 如果是有效的 UTF-8 序列返回 true
     */
    private boolean isValidUTF8Continuation(byte[] bytes, int index) {
        try {
            // 尝试将这个位置开始的几个字节解码为 UTF-8
            int remainingBytes = Math.min(4, bytes.length - index);
            byte[] testBytes = new byte[remainingBytes];
            System.arraycopy(bytes, index, testBytes, 0, remainingBytes);
            
            String testString = new String(testBytes, StandardCharsets.UTF_8);
            byte[] reencoded = testString.getBytes(StandardCharsets.UTF_8);
            
            // 如果重新编码后的长度相同，说明是有效的 UTF-8
            return reencoded.length > 0;
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
        return isSafeToProcessAsText(bytes) && calculateBinaryRatio(bytes) < 0.1;
    }
} 