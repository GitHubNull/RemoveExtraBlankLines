/*
 * Copyright (c) 2024. All rights reserved.
 */

package oxff.org.util;

import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;

/**
 * HTTP消息处理结果类
 * 
 * 用于封装HTTP请求或响应的处理结果，包含处理后的消息和修改标记
 */
public class HttpProcessingResult {
    private final HttpRequest processedRequest;
    private final HttpResponse processedResponse;
    private final boolean wasModified;
    
    public HttpProcessingResult(HttpRequest request, boolean modified) {
        this.processedRequest = request;
        this.processedResponse = null;
        this.wasModified = modified;
    }
    
    public HttpProcessingResult(HttpResponse response, boolean modified) {
        this.processedRequest = null;
        this.processedResponse = response;
        this.wasModified = modified;
    }
    
    public HttpRequest getProcessedRequest() {
        return processedRequest;
    }
    
    public HttpResponse getProcessedResponse() {
        return processedResponse;
    }
    
    public boolean wasModified() {
        return wasModified;
    }
} 