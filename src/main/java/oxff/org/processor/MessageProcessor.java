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
            
            // 查找 HTTP 头部结束位置
            int headerEndPosition = findHeaderEndPosition(bytes);
            if (headerEndPosition == -1) {
                // 没有找到头部结束标志，返回原始消息
                return messageBytes;
            }
            
            // 分离头部和正文
            byte[] headerBytes = new byte[headerEndPosition];
            System.arraycopy(bytes, 0, headerBytes, 0, headerEndPosition);
            
            byte[] bodyBytes = new byte[bytes.length - headerEndPosition];
            System.arraycopy(bytes, headerEndPosition, bodyBytes, 0, bodyBytes.length);
            
            // 检查正文是否包含文本内容
            if (!contentAnalyzer.isSafeToProcessAsText(bodyBytes)) {
                // 正文是二进制数据，只处理头部后的空行
                byte[] cleanedBodyBytes = messageCleaner.removeLeadingBlankLines(bodyBytes);
                return combineHeaderAndBody(headerBytes, cleanedBodyBytes);
            }
            
            // 正文是文本内容，可以安全处理
            byte[] cleanedBodyBytes = messageCleaner.cleanTextContent(bodyBytes);
            return combineHeaderAndBody(headerBytes, cleanedBodyBytes);
            
        } catch (Exception e) {
            logging.logToError("处理消息时出错: " + e.getMessage());
            return messageBytes; // 出错时返回原始消息
        }
    }
    
    /**
     * 查找 HTTP 头部结束位置
     * 
     * @param bytes 消息字节数组
     * @return 头部结束位置，如果没有找到返回 -1
     */
    private int findHeaderEndPosition(byte[] bytes) {
        // 查找 \r\n\r\n 或 \n\n
        for (int i = 0; i < bytes.length - 3; i++) {
            if (bytes[i] == '\r' && bytes[i + 1] == '\n' && 
                bytes[i + 2] == '\r' && bytes[i + 3] == '\n') {
                return i + 4; // 返回正文开始位置
            }
        }
        
        // 查找 \n\n
        for (int i = 0; i < bytes.length - 1; i++) {
            if (bytes[i] == '\n' && bytes[i + 1] == '\n') {
                return i + 2; // 返回正文开始位置
            }
        }
        
        return -1; // 没有找到头部结束标志
    }
    
    /**
     * 组合头部和正文
     * 
     * @param headerBytes 头部字节数组
     * @param bodyBytes 正文字节数组
     * @return 组合后的消息字节数组
     */
    private ByteArray combineHeaderAndBody(byte[] headerBytes, byte[] bodyBytes) {
        // 确保头部和正文之间有正确的分隔符
        byte[] separatorBytes;
        if (headerBytes.length >= 2 && 
            headerBytes[headerBytes.length - 2] == '\r' && 
            headerBytes[headerBytes.length - 1] == '\n') {
            // 使用 Windows 风格的换行符
            separatorBytes = new byte[]{'\r', '\n'};
        } else {
            // 使用 Unix 风格的换行符
            separatorBytes = new byte[]{'\n'};
        }
        
        byte[] result = new byte[headerBytes.length + separatorBytes.length + bodyBytes.length];
        System.arraycopy(headerBytes, 0, result, 0, headerBytes.length);
        System.arraycopy(separatorBytes, 0, result, headerBytes.length, separatorBytes.length);
        System.arraycopy(bodyBytes, 0, result, headerBytes.length + separatorBytes.length, bodyBytes.length);
        
        return ByteArray.byteArray(result);
    }
} 