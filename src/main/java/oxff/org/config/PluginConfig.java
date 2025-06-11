/*
 * Copyright (c) 2024. All rights reserved.
 */

package oxff.org.config;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ToolType;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.scope.Scope;

import java.util.EnumSet;
import java.util.Set;

/**
 * 插件配置管理器
 * 
 * 负责管理插件的各种配置选项：
 * - 模块生效控制（proxy, repeater, intruder, extensions）
 * - 目标域名控制（基于Burp Suite的目标范围设置）
 */
public class PluginConfig {
    
    // 默认启用的模块（所有支持的模块）
    private static final Set<ToolType> DEFAULT_ENABLED_MODULES = EnumSet.of(
        ToolType.PROXY,
        ToolType.REPEATER, 
        ToolType.INTRUDER,
        ToolType.EXTENSIONS
    );
    
    private final MontoyaApi api;
    private final Scope scope;
    
    // 当前启用的模块
    private Set<ToolType> enabledModules;
    
    // 是否只对目标域生效（true=仅目标域，false=所有域）
    private boolean targetScopeOnly;
    
    /**
     * 构造函数
     * 
     * @param api Montoya API 实例
     */
    public PluginConfig(MontoyaApi api) {
        this.api = api;
        this.scope = api.scope();
        
        // 使用默认配置
        this.enabledModules = EnumSet.copyOf(DEFAULT_ENABLED_MODULES);
        this.targetScopeOnly = false; // 默认对所有域生效
        
        logCurrentConfig();
    }
    
    /**
     * 检查指定的工具类型是否启用
     * 
     * @param toolType 工具类型
     * @return 如果启用返回 true，否则返回 false
     */
    public boolean isModuleEnabled(ToolType toolType) {
        return enabledModules.contains(toolType);
    }
    
    /**
     * 设置启用的模块
     * 
     * @param modules 要启用的模块集合
     */
    public void setEnabledModules(Set<ToolType> modules) {
        this.enabledModules = EnumSet.copyOf(modules);
    }
    
    /**
     * 启用指定模块
     * 
     * @param toolType 要启用的模块
     */
    public void enableModule(ToolType toolType) {
        enabledModules.add(toolType);
    }
    
    /**
     * 禁用指定模块
     * 
     * @param toolType 要禁用的模块
     */
    public void disableModule(ToolType toolType) {
        enabledModules.remove(toolType);
    }
    
    /**
     * 检查HTTP请求是否在目标范围内
     * 
     * @param request HTTP请求
     * @return 如果在范围内返回 true，否则返回 false
     */
    public boolean isInTargetScope(HttpRequest request) {
        if (!targetScopeOnly) {
            // 如果没有启用目标域限制，所有请求都被允许
            return true;
        }
        
        try {
            // 检查请求是否在Burp Suite定义的目标范围内
            return scope.isInScope(request.url());
        } catch (Exception e) {
            api.logging().logToError("检查目标范围时出错: " + e.getMessage());
            // 出错时默认允许处理
            return true;
        }
    }
    
    /**
     * 检查HTTP请求响应对是否在目标范围内
     * 
     * @param requestResponse HTTP请求响应对
     * @return 如果在范围内返回 true，否则返回 false
     */
    public boolean isInTargetScope(HttpRequestResponse requestResponse) {
        return isInTargetScope(requestResponse.request());
    }
    
    /**
     * 设置是否只对目标域生效
     * 
     * @param targetScopeOnly true=仅对目标域生效，false=对所有域生效
     */
    public void setTargetScopeOnly(boolean targetScopeOnly) {
        this.targetScopeOnly = targetScopeOnly;
    }
    
    /**
     * 获取当前是否只对目标域生效
     * 
     * @return true=仅对目标域生效，false=对所有域生效
     */
    public boolean isTargetScopeOnly() {
        return targetScopeOnly;
    }
    
    /**
     * 获取当前启用的模块集合（只读）
     * 
     * @return 当前启用的模块集合
     */
    public Set<ToolType> getEnabledModules() {
        return EnumSet.copyOf(enabledModules);
    }
    
    /**
     * 重置为默认配置
     */
    public void resetToDefaults() {
        this.enabledModules = EnumSet.copyOf(DEFAULT_ENABLED_MODULES);
        this.targetScopeOnly = false;
    }
    
    /**
     * 记录当前配置到日志
     */
    private void logCurrentConfig() {
        api.logging().logToOutput("插件配置已更新:");
        api.logging().logToOutput("  启用的模块: " + enabledModules.toString());
        api.logging().logToOutput("  目标域限制: " + (targetScopeOnly ? "仅目标域" : "所有域"));
    }
    
    /**
     * 获取配置的文本描述
     * 
     * @return 配置描述字符串
     */
    public String getConfigDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append("Remove Extra Blank Lines 插件配置:\n");
        sb.append("启用的模块: ").append(enabledModules).append("\n");
        sb.append("作用范围: ").append(targetScopeOnly ? "仅Burp Suite目标域" : "所有域");
        return sb.toString();
    }
} 