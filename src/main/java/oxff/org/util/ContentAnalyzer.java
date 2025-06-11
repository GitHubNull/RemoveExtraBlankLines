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
        // 图像文件
        {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}, // JPEG
        {(byte) 0x89, 0x50, 0x4E, 0x47}, // PNG
        {0x47, 0x49, 0x46, 0x38}, // GIF
        {0x42, 0x4D}, // BMP
        {0x49, 0x49, 0x2A, 0x00}, // TIFF (little endian)
        {0x4D, 0x4D, 0x00, 0x2A}, // TIFF (big endian)
        {0x52, 0x49, 0x46, 0x46}, // WEBP (RIFF container)
        {(byte) 0x8B, 0x50, 0x4E, 0x47}, // PNG alternative
        
        // 压缩文件
        {0x50, 0x4B, 0x03, 0x04}, // ZIP
        {0x50, 0x4B, 0x05, 0x06}, // ZIP (empty)
        {0x50, 0x4B, 0x07, 0x08}, // ZIP (spanned)
        {0x52, 0x61, 0x72, 0x21}, // RAR v1.5+
        {0x52, 0x61, 0x72, 0x21, 0x1A, 0x07, 0x00}, // RAR v1.5+ extended
        {0x37, 0x7A, (byte) 0xBC, (byte) 0xAF}, // 7Z
        {0x1F, (byte) 0x8B}, // GZIP
        {0x42, 0x5A, 0x68}, // BZIP2
        {0x75, 0x73, 0x74, 0x61, 0x72}, // TAR
        {(byte) 0xFD, 0x37, 0x7A, 0x58, 0x5A, 0x00}, // XZ
        
        // 音频文件
        {0x49, 0x44, 0x33}, // MP3 (ID3v2)
        {(byte) 0xFF, (byte) 0xFB}, // MP3 (MPEG-1 Layer 3)
        {(byte) 0xFF, (byte) 0xF3}, // MP3 (MPEG-2 Layer 3)
        {(byte) 0xFF, (byte) 0xF2}, // MP3 (MPEG-2.5 Layer 3)
        {0x52, 0x49, 0x46, 0x46}, // WAV/AVI (RIFF)
        {0x66, 0x4C, 0x61, 0x43}, // FLAC
        {0x4F, 0x67, 0x67, 0x53}, // OGG
        {0x4D, 0x34, 0x41, 0x20}, // M4A
        {0x66, 0x74, 0x79, 0x70}, // M4A/MP4 (ftyp)
        
        // 视频文件
        {0x00, 0x00, 0x00, 0x18, 0x66, 0x74, 0x79, 0x70}, // MP4
        {0x00, 0x00, 0x00, 0x20, 0x66, 0x74, 0x79, 0x70}, // MP4 alternative
        {0x66, 0x74, 0x79, 0x70, 0x69, 0x73, 0x6F, 0x6D}, // MP4 ISOM
        {0x1A, 0x45, (byte) 0xDF, (byte) 0xA3}, // MKV/WEBM
        {0x00, 0x00, 0x01, (byte) 0xBA}, // MPEG video
        {0x00, 0x00, 0x01, (byte) 0xB3}, // MPEG video
        {0x46, 0x4C, 0x56, 0x01}, // FLV
        
        // 文档文件
        {0x25, 0x50, 0x44, 0x46}, // PDF
        {(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0}, // MS Office (DOC, XLS, PPT)
        {0x50, 0x4B, 0x03, 0x04}, // Modern Office (DOCX, XLSX, PPTX) - ZIP based
        {0x7B, 0x5C, 0x72, 0x74, 0x66, 0x31}, // RTF
        
        // 可执行文件
        {0x4D, 0x5A}, // Windows PE/DLL/EXE
        {0x7F, 0x45, 0x4C, 0x46}, // ELF (Linux)
        {(byte) 0xFE, (byte) 0xED, (byte) 0xFA, (byte) 0xCE}, // Mach-O (macOS) 32-bit
        {(byte) 0xFE, (byte) 0xED, (byte) 0xFA, (byte) 0xCF}, // Mach-O (macOS) 64-bit
        {(byte) 0xCE, (byte) 0xFA, (byte) 0xED, (byte) 0xFE}, // Mach-O (macOS) reverse
        {(byte) 0xCF, (byte) 0xFA, (byte) 0xED, (byte) 0xFE}, // Mach-O (macOS) 64-bit reverse
        {0x4C, 0x01}, // Windows Object File
        {0x4C, 0x02}, // Windows Object File (x64)
        
        // 数据库文件
        {0x53, 0x51, 0x4C, 0x69, 0x74, 0x65, 0x20, 0x66}, // SQLite
        {0x00, 0x01, 0x00, 0x00}, // MS Access (MDB)
        {0x53, 0x74, 0x61, 0x6E, 0x64, 0x61, 0x72, 0x64}, // MS Access (ACCDB)
        
        // 字体文件
        {0x00, 0x01, 0x00, 0x00, 0x00}, // TTF
        {0x4F, 0x54, 0x54, 0x4F}, // OTF
        {0x77, 0x4F, 0x46, 0x46}, // WOFF
        {0x77, 0x4F, 0x46, 0x32}, // WOFF2
        
        // 其他常见二进制格式
        {(byte) 0x89, 0x48, 0x44, 0x46}, // HDF5
        {0x38, 0x42, 0x50, 0x53}, // PSD (Photoshop)
        {0x00, 0x00, 0x02, 0x00}, // ICO
        {0x00, 0x00, 0x01, 0x00}, // ICO alternative
        {(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE}, // Java Class
        {0x50, 0x4B}, // Generic ZIP-based (short signature)
        {(byte) 0x8A, 0x4D, 0x52, 0x18}, // MRML
        {0x4E, 0x45, 0x53, 0x1A}, // NES ROM
        
        // 加密/安全文件
        {0x2D, 0x2D, 0x2D, 0x2D, 0x2D}, // PEM certificate/key (-----BEGIN)
        {0x30, (byte) 0x82}, // DER certificate/key
        {0x4B, 0x65, 0x79, 0x42, 0x6F, 0x78}, // KeyBox
        
        // 虚拟机/容器格式
        {0x76, 0x6D, 0x64, 0x6B}, // VMDK
        {0x23, 0x20, 0x44, 0x69, 0x73, 0x6B}, // VDI
        {0x3C, 0x3C, 0x3C, 0x20, 0x4F, 0x72, 0x61, 0x63}, // VDI Oracle
        
        // 备份和磁盘镜像
        {0x37, 0x7A, (byte) 0xBC, (byte) 0xAF, 0x27, 0x1C}, // 7Z extended
        {0x45, 0x52, 0x02, 0x00}, // Ghost image
        {0x49, 0x53, 0x4F}, // ISO 9660 CD image (partial)
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