/*
 * Copyright (c) 2024. All rights reserved.
 */

package oxff.org.ui;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ToolType;
import oxff.org.config.PluginConfig;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Remove Extra Blank Lines 插件设置面板
 * 
 * 提供图形化界面用于配置：
 * - 模块生效控制（Proxy、Repeater、Intruder、Extensions）
 * - 目标域控制（是否仅对Burp Suite目标域生效）
 */
public class SettingsPanel extends JPanel {
    
    private final MontoyaApi api;
    private final PluginConfig config;
    
    // UI组件
    private Map<ToolType, JCheckBox> moduleCheckboxes;
    private JCheckBox targetScopeCheckbox;
    private JButton resetButton;
    private JButton applyButton;
    private JLabel statusLabel;
    
    /**
     * 构造函数
     * 
     * @param api Montoya API
     * @param config 插件配置管理器
     */
    public SettingsPanel(MontoyaApi api, PluginConfig config) {
        this.api = api;
        this.config = config;
        initializeUI();
        loadCurrentSettings();
    }
    
    /**
     * 初始化用户界面
     */
    private void initializeUI() {
        this.setLayout(new BorderLayout());
        this.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // 创建主要内容面板
        JPanel contentPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        
        // 标题
        JLabel titleLabel = new JLabel("Remove Extra Blank Lines - 插件设置");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(0, 0, 20, 0);
        gbc.anchor = GridBagConstraints.WEST;
        contentPanel.add(titleLabel, gbc);
        
        // 模块控制面板
        JPanel modulePanel = createModuleControlPanel();
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 20, 0);
        contentPanel.add(modulePanel, gbc);
        
        // 目标域控制面板
        JPanel scopePanel = createScopeControlPanel();
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 20, 0);
        contentPanel.add(scopePanel, gbc);
        
        // 按钮面板
        JPanel buttonPanel = createButtonPanel();
        gbc.gridx = 0; gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 10, 0);
        contentPanel.add(buttonPanel, gbc);
        
        // 状态标签
        statusLabel = new JLabel("就绪");
        statusLabel.setForeground(Color.BLUE);
        gbc.gridx = 0; gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(0, 0, 0, 0);
        contentPanel.add(statusLabel, gbc);
        
        this.add(contentPanel, BorderLayout.NORTH);
    }
    
    /**
     * 创建模块控制面板
     */
    private JPanel createModuleControlPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new TitledBorder("模块生效控制"));
        
        moduleCheckboxes = new HashMap<>();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 10, 5, 10);
        
        // 说明文字
        JLabel descLabel = new JLabel("选择插件在哪些Burp Suite模块中生效：");
        descLabel.setFont(descLabel.getFont().deriveFont(Font.PLAIN, 12f));
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(5, 10, 10, 10);
        panel.add(descLabel, gbc);
        
        // 创建模块复选框
        ToolType[] modules = {ToolType.PROXY, ToolType.REPEATER, ToolType.INTRUDER, ToolType.EXTENSIONS};
        String[] moduleNames = {"Proxy (代理模块)", "Repeater (重发器)", "Intruder (入侵者)", "Extensions (扩展)"};
        String[] moduleDescs = {
            "拦截和修改浏览器与目标服务器之间的流量",
            "手动重发和修改HTTP请求",
            "自动化的HTTP请求攻击测试",
            "来自其他扩展的HTTP请求"
        };
        
        for (int i = 0; i < modules.length; i++) {
            JCheckBox checkbox = new JCheckBox(moduleNames[i]);
            checkbox.setToolTipText(moduleDescs[i]);
            moduleCheckboxes.put(modules[i], checkbox);
            
            gbc.gridx = i % 2; gbc.gridy = 1 + i / 2;
            gbc.gridwidth = 1;
            gbc.insets = new Insets(2, 10, 2, 10);
            panel.add(checkbox, gbc);
        }
        
        return panel;
    }
    
    /**
     * 创建目标域控制面板
     */
    private JPanel createScopeControlPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new TitledBorder("目标域控制"));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 10, 5, 10);
        
        // 说明文字
        JLabel descLabel = new JLabel("控制插件的作用域范围：");
        descLabel.setFont(descLabel.getFont().deriveFont(Font.PLAIN, 12f));
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(5, 10, 10, 10);
        panel.add(descLabel, gbc);
        
        // 目标域复选框
        targetScopeCheckbox = new JCheckBox("仅对Burp Suite目标域生效");
        targetScopeCheckbox.setToolTipText("启用后，插件只处理Target->Scope中定义的目标域的HTTP流量");
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(2, 10, 2, 10);
        panel.add(targetScopeCheckbox, gbc);
        
        // 提示信息
        JLabel hintLabel = new JLabel("<html><i>提示：可在Target->Scope中设置目标域范围</i></html>");
        hintLabel.setForeground(Color.GRAY);
        hintLabel.setFont(hintLabel.getFont().deriveFont(Font.PLAIN, 11f));
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(5, 10, 5, 10);
        panel.add(hintLabel, gbc);
        
        return panel;
    }
    
    /**
     * 创建按钮面板
     */
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        
        applyButton = new JButton("应用设置");
        applyButton.setToolTipText("保存并应用当前设置");
        applyButton.addActionListener(new ApplyActionListener());
        
        resetButton = new JButton("重置为默认");
        resetButton.setToolTipText("恢复为插件默认设置");
        resetButton.addActionListener(new ResetActionListener());
        
        panel.add(applyButton);
        panel.add(resetButton);
        
        return panel;
    }
    
    /**
     * 从配置管理器加载当前设置到UI
     */
    private void loadCurrentSettings() {
        Set<ToolType> enabledModules = config.getEnabledModules();
        
        // 更新模块复选框状态
        for (Map.Entry<ToolType, JCheckBox> entry : moduleCheckboxes.entrySet()) {
            entry.getValue().setSelected(enabledModules.contains(entry.getKey()));
        }
        
        // 更新目标域复选框状态
        targetScopeCheckbox.setSelected(config.isTargetScopeOnly());
        
        updateStatusLabel("配置已加载", Color.BLUE);
    }
    
    /**
     * 将当前UI设置保存到配置管理器
     */
    private void saveCurrentSettings() {
        // 获取选中的模块
        Set<ToolType> selectedModules = EnumSet.noneOf(ToolType.class);
        for (Map.Entry<ToolType, JCheckBox> entry : moduleCheckboxes.entrySet()) {
            if (entry.getValue().isSelected()) {
                selectedModules.add(entry.getKey());
            }
        }
        
        // 更新配置
        config.setEnabledModules(selectedModules);
        config.setTargetScopeOnly(targetScopeCheckbox.isSelected());
        
        updateStatusLabel("设置已保存并应用", Color.GREEN);
        
        // 记录日志
        api.logging().logToOutput("用户通过设置面板更新了插件配置");
    }
    
    /**
     * 重置为默认设置
     */
    private void resetToDefaults() {
        config.resetToDefaults();
        loadCurrentSettings();
        updateStatusLabel("已重置为默认设置", Color.ORANGE);
    }
    
    /**
     * 更新状态标签
     */
    private void updateStatusLabel(String message, Color color) {
        statusLabel.setText(message);
        statusLabel.setForeground(color);
        
        // 3秒后恢复默认状态
        Timer timer = new Timer(3000, e -> {
            statusLabel.setText("就绪");
            statusLabel.setForeground(Color.BLUE);
        });
        timer.setRepeats(false);
        timer.start();
    }
    
    /**
     * 获取插件标题
     */
    public String getTitle() {
        return "Remove Extra Blank Lines";
    }
    
    /**
     * 应用按钮动作监听器
     */
    private class ApplyActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            saveCurrentSettings();
        }
    }
    
    /**
     * 重置按钮动作监听器
     */
    private class ResetActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            int result = JOptionPane.showConfirmDialog(
                SettingsPanel.this,
                "确定要重置为默认设置吗？这将清除所有自定义配置。",
                "确认重置",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
            );
            
            if (result == JOptionPane.YES_OPTION) {
                resetToDefaults();
            }
        }
    }
} 