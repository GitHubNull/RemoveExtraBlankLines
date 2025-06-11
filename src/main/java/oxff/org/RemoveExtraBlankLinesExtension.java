/*
 * Copyright (c) 2024. All rights reserved.
 *
 * This code may be used to extend the functionality of Burp Suite Community Edition
 * and Burp Suite Professional, provided that this usage does not violate the
 * license terms for those products.
 */

package oxff.org;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import oxff.org.handler.HttpMessageHandler;

/**
 * Burp Suite 插件: 去除 HTTP 请求或响应报文头和体之间的多余空行
 * 
 * 此插件会自动检测并删除 HTTP 消息头部和正文之间超过一个的空行，
 * 保持标准的 HTTP 协议格式（头部和正文之间只保留一个空行）
 * 
 * 主要特性：
 * - 智能检测二进制内容，避免处理二进制数据
 * - 支持混合内容（二进制+文本）的安全处理
 * - 模块化设计，便于维护和扩展
 */
public class RemoveExtraBlankLinesExtension implements BurpExtension {
    
    @SuppressWarnings("unused")
    private MontoyaApi api;
    
    @Override
    public void initialize(MontoyaApi api) {
        this.api = api;
        
        // 设置插件名称
        api.extension().setName("Remove Extra Blank Lines");
        
        // 注册 HTTP 处理器
        api.http().registerHttpHandler(new HttpMessageHandler(api));
        
        // 输出初始化日志
        api.logging().logToOutput("Remove Extra Blank Lines 插件已加载");
        api.logging().logToOutput("支持智能二进制检测和模块化设计");
    }
} 