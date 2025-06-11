/*
 * Copyright (c) 2024. All rights reserved.
 */

package oxff.org.handler;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.handler.HttpHandler;
import burp.api.montoya.http.handler.HttpRequestToBeSent;
import burp.api.montoya.http.handler.HttpResponseReceived;
import burp.api.montoya.http.handler.RequestToBeSentAction;
import burp.api.montoya.http.handler.ResponseReceivedAction;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.logging.Logging;
import oxff.org.processor.MessageProcessor;
import oxff.org.util.ContentAnalyzer;

import static burp.api.montoya.http.handler.RequestToBeSentAction.continueWith;
import static burp.api.montoya.http.handler.ResponseReceivedAction.continueWith;

/**
 * HTTP 消息处理器
 * 
 * 负责拦截和处理所有通过 Burp Suite 的 HTTP 请求和响应
 * 
 * 主要功能：
 * - 拦截 HTTP 请求和响应
 * - 委托给专门的处理器进行处理
 * - 记录处理结果和错误信息
 */
public class HttpMessageHandler implements HttpHandler {
    
    private final Logging logging;
    private final MessageProcessor messageProcessor;
    private final ContentAnalyzer contentAnalyzer;
    
    public HttpMessageHandler(MontoyaApi api) {
        this.logging = api.logging();
        this.messageProcessor = new MessageProcessor(api);
        this.contentAnalyzer = new ContentAnalyzer();
    }
    
    @Override
    public RequestToBeSentAction handleHttpRequestToBeSent(HttpRequestToBeSent requestToBeSent) {
        try {
            // 检查是否应该处理这个请求
            if (!shouldProcessMessage(requestToBeSent)) {
                return continueWith(requestToBeSent);
            }
            
            HttpRequest processedRequest = messageProcessor.processRequest(requestToBeSent);
            
            // 如果请求被修改，返回修改后的请求
            if (!requestToBeSent.toByteArray().equals(processedRequest.toByteArray())) {
                logging.logToOutput("已清理请求中的多余空行: " + requestToBeSent.url());
                return continueWith(processedRequest);
            }
            
            return continueWith(requestToBeSent);
            
        } catch (Exception e) {
            logging.logToError("处理请求时出错: " + e.getMessage());
            return continueWith(requestToBeSent);
        }
    }
    
    @Override
    public ResponseReceivedAction handleHttpResponseReceived(HttpResponseReceived responseReceived) {
        try {
            // 检查是否应该处理这个响应
            if (!shouldProcessMessage(responseReceived)) {
                return continueWith(responseReceived);
            }
            
            HttpResponse processedResponse = messageProcessor.processResponse(responseReceived);
            
            // 如果响应被修改，返回修改后的响应
            if (!responseReceived.toByteArray().equals(processedResponse.toByteArray())) {
                logging.logToOutput("已清理响应中的多余空行");
                return continueWith(processedResponse);
            }
            
            return continueWith(responseReceived);
            
        } catch (Exception e) {
            logging.logToError("处理响应时出错: " + e.getMessage());
            return continueWith(responseReceived);
        }
    }
    
    /**
     * 判断是否应该处理指定的消息
     * 
     * @param message HTTP 消息
     * @return 如果应该处理返回 true，否则返回 false
     */
    private boolean shouldProcessMessage(Object message) {
        try {
            // 检查消息是否包含二进制内容
            byte[] messageBytes;
            if (message instanceof HttpRequestToBeSent) {
                messageBytes = ((HttpRequestToBeSent) message).toByteArray().getBytes();
            } else if (message instanceof HttpResponseReceived) {
                messageBytes = ((HttpResponseReceived) message).toByteArray().getBytes();
            } else {
                return false;
            }
            
            // 如果消息太短或者是纯二进制内容，跳过处理
            if (messageBytes.length < 10) {
                return false;
            }
            
            // 使用内容分析器检查是否包含文本内容
            return contentAnalyzer.containsTextContent(messageBytes);
            
        } catch (Exception e) {
            logging.logToError("检查消息内容时出错: " + e.getMessage());
            return false;
        }
    }
} 