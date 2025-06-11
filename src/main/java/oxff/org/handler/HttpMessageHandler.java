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
import oxff.org.config.PluginConfig;
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
 * - 根据配置决定是否处理（模块控制、域名控制）
 * - 委托给专门的处理器进行处理
 * - 记录处理结果和错误信息
 */
public class HttpMessageHandler implements HttpHandler {
    
    private final Logging logging;
    private final MessageProcessor messageProcessor;
    private final ContentAnalyzer contentAnalyzer;
    private final PluginConfig config;
    
    /**
     * 构造函数
     * 
     * @param api Montoya API 实例
     * @param config 插件配置管理器
     */
    public HttpMessageHandler(MontoyaApi api, PluginConfig config) {
        this.logging = api.logging();
        this.messageProcessor = new MessageProcessor(api);
        this.contentAnalyzer = new ContentAnalyzer();
        this.config = config;
    }
    
    @Override
    public RequestToBeSentAction handleHttpRequestToBeSent(HttpRequestToBeSent requestToBeSent) {
        try {
            // 检查是否应该处理这个请求
            if (!shouldProcessRequest(requestToBeSent)) {
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
            if (!shouldProcessResponse(responseReceived)) {
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
     * 判断是否应该处理指定的请求
     * 
     * @param requestToBeSent HTTP 请求
     * @return 如果应该处理返回 true，否则返回 false
     */
    private boolean shouldProcessRequest(HttpRequestToBeSent requestToBeSent) {
        try {
            // 1. 检查当前工具类型是否启用
            if (!config.isModuleEnabled(requestToBeSent.toolSource().toolType())) {
                return false;
            }
            
            // 2. 检查是否在目标范围内
            if (!config.isInTargetScope(requestToBeSent)) {
                return false;
            }
            
            // 3. 检查消息内容是否适合处理
            return shouldProcessMessage(requestToBeSent.toByteArray().getBytes());
            
        } catch (Exception e) {
            logging.logToError("检查请求处理条件时出错: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 判断是否应该处理指定的响应
     * 
     * @param responseReceived HTTP 响应
     * @return 如果应该处理返回 true，否则返回 false
     */
    private boolean shouldProcessResponse(HttpResponseReceived responseReceived) {
        try {
            // 1. 检查当前工具类型是否启用
            if (!config.isModuleEnabled(responseReceived.toolSource().toolType())) {
                return false;
            }
            
            // 2. 检查是否在目标范围内（基于请求）
            if (!config.isInTargetScope(responseReceived.initiatingRequest())) {
                return false;
            }
            
            // 3. 检查消息内容是否适合处理
            return shouldProcessMessage(responseReceived.toByteArray().getBytes());
            
        } catch (Exception e) {
            logging.logToError("检查响应处理条件时出错: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 基于内容判断是否应该处理指定的消息
     * 
     * @param messageBytes 消息字节数组
     * @return 如果应该处理返回 true，否则返回 false
     */
    private boolean shouldProcessMessage(byte[] messageBytes) {
        try {
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