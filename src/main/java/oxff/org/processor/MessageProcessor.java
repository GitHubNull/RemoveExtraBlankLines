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
import oxff.org.util.ProcessingResult;
import oxff.org.util.HttpProcessingResult;

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
    private final HttpMessageCleaner messageCleaner;
    
    public MessageProcessor(MontoyaApi api) {
        this.logging = api.logging();
        new ContentAnalyzer();
        this.messageCleaner = new HttpMessageCleaner();
    }
    
    /**
     * 处理 HTTP 请求
     * 
     * @param request 原始 HTTP 请求
     * @return 处理结果，包含处理后的请求和修改标记
     */
    public HttpProcessingResult processRequest(HttpRequest request) {
        try {
            ProcessingResult result = processHttpMessageBody(request);
            if (result.wasModified()) {
                // 使用withBody()方法自动更新Content-Length头部
                HttpRequest processedRequest = request.withBody(result.getProcessedBytes());
                return new HttpProcessingResult(processedRequest, true);
            } else {
                return new HttpProcessingResult(request, false);
            }
        } catch (Exception e) {
            logging.logToError("处理 HTTP 请求时出错: " + e.getMessage());
            return new HttpProcessingResult(request, false); // 出错时返回原始请求
        }
    }
    
    /**
     * 处理 HTTP 响应
     * 
     * @param response 原始 HTTP 响应
     * @return 处理结果，包含处理后的响应和修改标记
     */
    public HttpProcessingResult processResponse(HttpResponse response) {
        try {
            ProcessingResult result = processHttpMessageBody(response);
            if (result.wasModified()) {
                // 使用withBody()方法自动更新Content-Length头部
                HttpResponse processedResponse = response.withBody(result.getProcessedBytes());
                return new HttpProcessingResult(processedResponse, true);
            } else {
                return new HttpProcessingResult(response, false);
            }
        } catch (Exception e) {
            logging.logToError("处理 HTTP 响应时出错: " + e.getMessage());
            return new HttpProcessingResult(response, false); // 出错时返回原始响应
        }
    }
    
    /**
     * 处理 HTTP 请求体
     * 
     * @param request 原始HTTP请求
     * @return 清理结果，包含处理后的正文和修改标记
     */
    private ProcessingResult processHttpMessageBody(HttpRequest request) {
        try {
            ByteArray bodyBytes = request.body();
            if (bodyBytes.length() == 0) {
                // 没有请求体，返回空结果
                return new ProcessingResult(ByteArray.byteArray(), false);
            }
            
            // 移除正文开头的多余空行
            return messageCleaner.removeLeadingBlankLinesWithResult(bodyBytes.getBytes());
            
        } catch (Exception e) {
            logging.logToError("处理请求体时出错: " + e.getMessage());
            return new ProcessingResult(request.body(), false);
        }
    }
    
    /**
     * 处理 HTTP 响应体
     * 
     * @param response 原始HTTP响应
     * @return 清理结果，包含处理后的正文和修改标记
     */
    private ProcessingResult processHttpMessageBody(HttpResponse response) {
        try {
            ByteArray bodyBytes = response.body();
            if (bodyBytes.length() == 0) {
                // 没有响应体，返回空结果
                return new ProcessingResult(ByteArray.byteArray(), false);
            }
            
            // 移除正文开头的多余空行
            return messageCleaner.removeLeadingBlankLinesWithResult(bodyBytes.getBytes());
            
        } catch (Exception e) {
            logging.logToError("处理响应体时出错: " + e.getMessage());
            return new ProcessingResult(response.body(), false);
        }
    }

} 