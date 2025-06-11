/*
 * Copyright (c) 2024. All rights reserved.
 */

package oxff.org.util;

import burp.api.montoya.core.ByteArray;

/**
 * 处理结果类
 * 
 * 用于封装处理操作的结果，包含处理后的数据和修改标记
 */
public class ProcessingResult {
    private final ByteArray processedBytes;
    private final boolean wasModified;
    
    public ProcessingResult(ByteArray bytes, boolean modified) {
        this.processedBytes = bytes;
        this.wasModified = modified;
    }
    
    public ProcessingResult(byte[] bytes, boolean modified) {
        this.processedBytes = ByteArray.byteArray(bytes);
        this.wasModified = modified;
    }
    
    public ByteArray getProcessedBytes() {
        return processedBytes;
    }
    
    public boolean wasModified() {
        return wasModified;
    }
} 