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
import burp.api.montoya.core.ToolType;
import oxff.org.config.PluginConfig;
import oxff.org.handler.HttpMessageHandler;

import java.util.EnumSet;

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
 * - 模块生效控制（proxy, repeater, intruder, extensions）
 * - 目标域控制（可基于Burp Suite的目标范围）
 */
public class RemoveExtraBlankLinesExtension implements BurpExtension {
    
    @SuppressWarnings("unused")
    private MontoyaApi api;
    private PluginConfig config;
    
    @Override
    public void initialize(MontoyaApi api) {
        this.api = api;
        
        // 设置插件名称
        api.extension().setName("Remove Extra Blank Lines");
        
        // 初始化配置管理器
        this.config = new PluginConfig(api);
        
        // 注册 HTTP 处理器，传入配置
        api.http().registerHttpHandler(new HttpMessageHandler(api, config));
        
        // 输出初始化日志
        api.logging().logToOutput("Remove Extra Blank Lines 插件已加载");
        api.logging().logToOutput("支持智能二进制检测和模块化设计");
        api.logging().logToOutput("支持模块生效控制和目标域控制");
        
        // 输出当前配置信息
        api.logging().logToOutput(config.getConfigDescription());
        
        // 示例：演示如何动态修改配置（可注释掉或移除）
        demonstrateConfigUsage(api);
    }
    
    /**
     * 演示配置功能的使用方法
     * 在实际使用中，这些配置可以通过UI或其他方式进行修改
     * 
     * @param api Montoya API
     */
    private void demonstrateConfigUsage(MontoyaApi api) {
        api.logging().logToOutput("\n=== 配置功能演示 ===");
        
        // 示例1: 只启用 Proxy 和 Repeater 模块
        // config.setEnabledModules(EnumSet.of(ToolType.PROXY, ToolType.REPEATER));
        
        // 示例2: 只对目标域生效
        // config.setTargetScopeOnly(true);
        
        // 示例3: 禁用 Intruder 模块
        // config.disableModule(ToolType.INTRUDER);
        
        // 示例4: 启用所有模块但只对目标域生效
        // config.setEnabledModules(EnumSet.of(ToolType.PROXY, ToolType.REPEATER, ToolType.INTRUDER, ToolType.EXTENSIONS));
        // config.setTargetScopeOnly(true);
        
        api.logging().logToOutput("配置功能演示完成，当前使用默认配置");
        api.logging().logToOutput("可通过修改代码中的示例来测试不同配置");
        api.logging().logToOutput("===================\n");
    }
    
    /**
     * 获取插件配置管理器
     * 
     * @return 配置管理器实例
     */
    public PluginConfig getConfig() {
        return config;
    }
} 