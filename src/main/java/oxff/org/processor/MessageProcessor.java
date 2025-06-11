/*
 * Copyright (c) 2024. All rights reserved.
 */

package oxff.org.processor;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ByteArray;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.logging.Logging;
import oxff.org.util.ContentAnalyzer;
import oxff.org.util.HttpMessageCleaner;

/**
 * 消息处理器
 * 
 * 负责处理 HTTP 消息中的空行清理逻辑
 * 
 * 主要功能：
 * - 安全地处理文本内容
 * - 避免处理二进制数据
 * - 清理头部和正文之间的多余空行
 */
public class MessageProcessor {
    
    private final Logging logging;
    private final ContentAnalyzer contentAnalyzer;
    private final HttpMessageCleaner messageCleaner;
    
    public MessageProcessor(MontoyaApi api) {
        this.logging = api.logging();
        this.contentAnalyzer = new ContentAnalyzer();
        this.messageCleaner = new HttpMessageCleaner();
    }
    
    /**
     * 处理 HTTP 请求
     * 
     * @param request 原始 HTTP 请求
     * @return 处理后的 HTTP 请求
     */
    public HttpRequest processRequest(HttpRequest request) {
        try {
            ByteArray processedBytes = processHttpMessage(request.toByteArray());
            return HttpRequest.httpRequest(processedBytes);
        } catch (Exception e) {
            logging.logToError("处理 HTTP 请求时出错: " + e.getMessage());
            return request; // 出错时返回原始请求
        }
    }
    
    /**
     * 处理 HTTP 响应
     * 
     * @param response 原始 HTTP 响应
     * @return 处理后的 HTTP 响应
     */
    public HttpResponse processResponse(HttpResponse response) {
        try {
            ByteArray processedBytes = processHttpMessage(response.toByteArray());
            return HttpResponse.httpResponse(processedBytes);
        } catch (Exception e) {
            logging.logToError("处理 HTTP 响应时出错: " + e.getMessage());
            return response; // 出错时返回原始响应
        }
    }
    
    /**
     * 处理 HTTP 消息字节数组
     * 
     * @param messageBytes 原始消息字节数组
     * @return 处理后的消息字节数组
     */
    private ByteArray processHttpMessage(ByteArray messageBytes) {
        try {
            byte[] bytes = messageBytes.getBytes();
            
            // 查找 HTTP 头部结束位置 (\r\n\r\n 或 \n\n)
            int separatorStart = findSeparatorStart(bytes);
            if (separatorStart == -1) {
                // 没有找到头部结束标志，返回原始消息
                return messageBytes;
            }
            
            // 确定分隔符类型和长度
            boolean isWindowsStyle = (separatorStart + 3 < bytes.length && 
                bytes[separatorStart] == '\r' && bytes[separatorStart + 1] == '\n' &&
                bytes[separatorStart + 2] == '\r' && bytes[separatorStart + 3] == '\n');
            
            int separatorLength = isWindowsStyle ? 4 : 2;
            int bodyStart = separatorStart + separatorLength;
            
            // 分离头部、分隔符和正文
            byte[] headerBytes = new byte[separatorStart];
            System.arraycopy(bytes, 0, headerBytes, 0, separatorStart);
            
            if (bodyStart >= bytes.length) {
                // 没有正文，直接返回原始消息
                return messageBytes;
            }
            
            byte[] bodyBytes = new byte[bytes.length - bodyStart];
            System.arraycopy(bytes, bodyStart, bodyBytes, 0, bodyBytes.length);
            
            // 移除正文开头的多余空行，但保留HTTP协议要求的分隔符
            byte[] cleanedBodyBytes = messageCleaner.removeLeadingBlankLines(bodyBytes);
            
            // 重新组合消息：头部 + 标准分隔符 + 清理后的正文
            byte[] separator = isWindowsStyle ? new byte[]{'\r', '\n', '\r', '\n'} : new byte[]{'\n', '\n'};
            byte[] result = new byte[headerBytes.length + separator.length + cleanedBodyBytes.length];
            
            System.arraycopy(headerBytes, 0, result, 0, headerBytes.length);
            System.arraycopy(separator, 0, result, headerBytes.length, separator.length);
            System.arraycopy(cleanedBodyBytes, 0, result, headerBytes.length + separator.length, cleanedBodyBytes.length);
            
            return ByteArray.byteArray(result);
            
        } catch (Exception e) {
            logging.logToError("处理消息时出错: " + e.getMessage());
            return messageBytes; // 出错时返回原始消息
        }
    }
    
    /**
     * 查找 HTTP 头部与正文分隔符的开始位置
     * 
     * @param bytes 消息字节数组
     * @return 分隔符开始位置，如果没有找到返回 -1
     */
    private int findSeparatorStart(byte[] bytes) {
        // 查找 \r\n\r\n
        for (int i = 0; i < bytes.length - 3; i++) {
            if (bytes[i] == '\r' && bytes[i + 1] == '\n' && 
                bytes[i + 2] == '\r' && bytes[i + 3] == '\n') {
                return i; // 返回分隔符开始位置
            }
        }
        
        // 查找 \n\n
        for (int i = 0; i < bytes.length - 1; i++) {
            if (bytes[i] == '\n' && bytes[i + 1] == '\n') {
                return i; // 返回分隔符开始位置
            }
        }
        
        return -1; // 没有找到头部结束标志
    }

} 