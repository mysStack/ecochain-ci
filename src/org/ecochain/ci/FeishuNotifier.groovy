package org.ecochain.ci

/**
 * 飞书机器人通知工具类
 * 支持构建状态通知
 */
class FeishuNotifier extends BaseNotifier {
    
    FeishuNotifier(steps) {
        super(steps)
    }
    
    @Override
    String getChannelName() {
        return 'feishu'
    }
    
    @Override
    def sendNotification(Map message, Map cfg) {
        def webhookUrl = getWebhookUrl(cfg)
        if (!webhookUrl) {
            steps.echo "⚠️ 未配置飞书Webhook地址，跳过通知"
            return [success: false, error: '未配置Webhook地址']
        }
        
        def payload = buildFeishuPayload(message, cfg)
        return sendHttpRequest(webhookUrl, payload)
    }
    
    /**
     * 构建飞书通知负载
     */
    private Map buildFeishuPayload(Map message, Map cfg) {
        def atUsers = getAtUsers(cfg)
        def isAtAll = message.isAtAll ?: false
        
        // 飞书支持更丰富的消息格式
        return [
            msg_type: 'interactive',
            card: [
                header: [
                    title: [
                        tag: 'plain_text',
                        content: message.title
                    ],
                    template: getColorTemplate(message.title)
                ],
                elements: [
                    [
                        tag: 'div',
                        text: [
                            tag: 'lark_md',
                            content: message.content
                        ]
                    ],
                    [
                        tag: 'hr'
                    ],
                    [
                        tag: 'note',
                        elements: [
                            [
                                tag: 'plain_text',
                                content: "构建时间: ${message.timestamp}"
                            ]
                        ]
                    ]
                ]
            ]
        ]
    }
    
    /**
     * 根据消息类型获取颜色模板
     */
    private String getColorTemplate(String title) {
        if (title.contains('✅') || title.contains('成功')) {
            return 'green'
        } else if (title.contains('❌') || title.contains('失败')) {
            return 'red'
        } else if (title.contains('⚠️') || title.contains('不稳定')) {
            return 'orange'
        } else {
            return 'blue'
        }
    }
    
    @Override
    def sendBuildStartNotification(Map buildInfo, Map cfg) {
        if (!isEnabled(cfg)) return
        
        def title = org.ecochain.ci.MessageBuilder.buildTitle('started', buildInfo.projectName)
        def content = org.ecochain.ci.MessageBuilder.buildStartContent(buildInfo, 'feishu')
        
        def message = buildMessageTemplate(title, content, buildInfo, cfg)
        return sendNotification(message, cfg)
    }
    
    @Override
    def sendBuildSuccessNotification(Map buildInfo, Map cfg) {
        if (!isEnabled(cfg)) return
        
        def title = org.ecochain.ci.MessageBuilder.buildTitle('success', buildInfo.projectName)
        def content = org.ecochain.ci.MessageBuilder.buildSuccessContent(buildInfo, 'feishu')
        
        def message = buildMessageTemplate(title, content, buildInfo, cfg)
        return sendNotification(message, cfg)
    }
    
    @Override
    def sendBuildFailureNotification(Map buildInfo, String errorMessage, Map cfg) {
        if (!isEnabled(cfg)) return
        
        def title = org.ecochain.ci.MessageBuilder.buildTitle('failure', buildInfo.projectName)
        def content = org.ecochain.ci.MessageBuilder.buildFailureContent(buildInfo, errorMessage, 'feishu')
        
        def message = buildMessageTemplate(title, content, buildInfo, cfg)
        message.isAtAll = true
        
        return sendNotification(message, cfg)
    }
    
    @Override
    def sendBuildUnstableNotification(Map buildInfo, String unstableReason, Map cfg) {
        if (!isEnabled(cfg)) return
        
        def title = org.ecochain.ci.MessageBuilder.buildTitle('unstable', buildInfo.projectName)
        def content = org.ecochain.ci.MessageBuilder.buildUnstableContent(buildInfo, unstableReason, 'feishu')
        
        def message = buildMessageTemplate(title, content, buildInfo, cfg)
        return sendNotification(message, cfg)
    }
    
    /**
     * 构建开始通知内容（飞书格式）
     */
    private String buildStartContent(Map buildInfo) {
        return """**项目名称**: ${buildInfo.projectName}  
**构建版本**: ${buildInfo.buildVersion}  
**环境**: ${buildInfo.env} | **分支**: ${buildInfo.branch}  
**开始时间**: ${buildInfo.timestamp}  
**构建链接**: [查看进度](${buildInfo.buildUrl})

📋 **构建计划**  
• 代码检出  
• 单元测试  
• 代码质量扫描  
• 依赖安全扫描  
• 构建打包  
• 镜像构建（如启用）

⏰ **预计时长**: ${buildInfo.estimatedDuration ?: '10-15分钟'}
        """.trim()
    }
    
    /**
     * 构建成功通知内容（飞书格式）
     */
    private String buildSuccessContent(Map buildInfo) {
        return """**项目名称**: ${buildInfo.projectName}  
**构建版本**: ${buildInfo.buildVersion}  
**环境**: ${buildInfo.env} | **分支**: ${buildInfo.branch}  
**构建时间**: ${buildInfo.timestamp}  
**构建链接**: [查看详情](${buildInfo.buildUrl})

💡 **构建结果**  
• 单元测试: ${buildInfo.testStatus ? '✅ 通过' : '❌ 失败'}  
• 代码扫描: ${buildInfo.scanStatus ? '✅ 通过' : '❌ 失败'}  
• 依赖扫描: ${buildInfo.depScanStatus ? '✅ 通过' : '❌ 失败'}  
• 镜像构建: ${buildInfo.kanikoStatus ? '✅ 完成' : '⏸️ 未执行'}

📊 **质量统计**  
• 测试覆盖率: ${buildInfo.coverage ?: 'N/A'}  
• 代码质量: ${buildInfo.qualityGate ?: 'N/A'}  
• 漏洞数量: ${buildInfo.vulnerabilities ?: 0}
        """.trim()
    }
    
    /**
     * 构建失败通知内容（飞书格式）
     */
    private String buildFailureContent(Map buildInfo, String errorMessage) {
        return """**项目名称**: ${buildInfo.projectName}  
**构建版本**: ${buildInfo.buildVersion}  
**环境**: ${buildInfo.env} | **失败阶段**: ${buildInfo.failedStage ?: '未知'}  
**失败时间**: ${buildInfo.timestamp}  
**构建链接**: [查看详情](${buildInfo.buildUrl})

🔴 **错误信息**  
${errorMessage}

📋 **处理建议**  
1. 查看构建日志分析错误  
2. 检查相关配置是否正确  
3. 联系开发人员处理问题  
4. 重新触发构建
        """.trim()
    }
    
    /**
     * 构建不稳定通知内容（飞书格式）
     */
    private String buildUnstableContent(Map buildInfo, String unstableReason) {
        return """**项目名称**: ${buildInfo.projectName}  
**构建版本**: ${buildInfo.buildVersion}  
**环境**: ${buildInfo.env} | **状态**: 不稳定  
**时间**: ${buildInfo.timestamp}  
**构建链接**: [查看详情](${buildInfo.buildUrl})

⚠️ **不稳定原因**  
${unstableReason}

💡 **处理建议**  
1. 检查测试用例失败原因  
2. 分析代码质量报告  
3. 修复相关问题后重新构建  
4. 确认是否影响生产部署
        """.trim()
    }
}package org.ecochain.ci

/**
 * 飞书机器人通知工具类
 * 支持构建状态通知
 */
class FeishuNotifier extends BaseNotifier {
    
    FeishuNotifier(steps) {
        super(steps)
    }
    
    @Override
    String getChannelName() {
        return 'feishu'
    }
    
    @Override
    def sendNotification(Map message, Map cfg) {
        def webhookUrl = getWebhookUrl(cfg)
        if (!webhookUrl) {
            steps.echo "⚠️ 未配置飞书Webhook地址，跳过通知"
            return [success: false, error: '未配置Webhook地址']
        }
        
        def payload = buildFeishuPayload(message, cfg)
        return sendHttpRequest(webhookUrl, payload)
    }
    
    /**
     * 构建飞书通知负载
     */
    private Map buildFeishuPayload(Map message, Map cfg) {
        def atUsers = getAtUsers(cfg)
        def isAtAll = message.isAtAll ?: false
        
        // 飞书支持更丰富的消息格式
        return [
            msg_type: 'interactive',
            card: [
                header: [
                    title: [
                        tag: 'plain_text',
                        content: message.title
                    ],
                    template: getColorTemplate(message.title)
                ],
                elements: [
                    [
                        tag: 'div',
                        text: [
                            tag: 'lark_md',
                            content: message.content
                        ]
                    ],
                    [
                        tag: 'hr'
                    ],
                    [
                        tag: 'note',
                        elements: [
                            [
                                tag: 'plain_text',
                                content: "构建时间: ${message.timestamp}"
                            ]
                        ]
                    ]
                ]
            ]
        ]
    }
    
    /**
     * 根据消息类型获取颜色模板
     */
    private String getColorTemplate(String title) {
        if (title.contains('✅') || title.contains('成功')) {
            return 'green'
        } else if (title.contains('❌') || title.contains('失败')) {
            return 'red'
        } else if (title.contains('⚠️') || title.contains('不稳定')) {
            return 'orange'
        } else {
            return 'blue'
        }
    }
    
    @Override
    def sendBuildStartNotification(Map buildInfo, Map cfg) {
        if (!isEnabled(cfg)) return
        
        def message = buildMessageTemplate(
            "🚀 开始构建 - ${buildInfo.projectName}",
            buildStartContent(buildInfo),
            buildInfo,
            cfg
        )
        
        return sendNotification(message, cfg)
    }
    
    @Override
    def sendBuildSuccessNotification(Map buildInfo, Map cfg) {
        if (!isEnabled(cfg)) return
        
        def message = buildMessageTemplate(
            "✅ 构建成功 - ${buildInfo.projectName}",
            buildSuccessContent(buildInfo),
            buildInfo,
            cfg
        )
        
        return sendNotification(message, cfg)
    }
    
    @Override
    def sendBuildFailureNotification(Map buildInfo, String errorMessage, Map cfg) {
        if (!isEnabled(cfg)) return
        
        def message = buildMessageTemplate(
            "❌ 构建失败 - ${buildInfo.projectName}",
            buildFailureContent(buildInfo, errorMessage),
            buildInfo,
            cfg
        )
        message.isAtAll = true
        
        return sendNotification(message, cfg)
    }
    
    @Override
    def sendBuildUnstableNotification(Map buildInfo, String unstableReason, Map cfg) {
        if (!isEnabled(cfg)) return
        
        def message = buildMessageTemplate(
            "⚠️ 构建不稳定 - ${buildInfo.projectName}",
            buildUnstableContent(buildInfo, unstableReason),
            buildInfo,
            cfg
        )
        
        return sendNotification(message, cfg)
    }
    
    /**
     * 构建开始通知内容（飞书格式）
     */
    private String buildStartContent(Map buildInfo) {
        return """**项目名称**: ${buildInfo.projectName}  
**构建版本**: ${buildInfo.buildVersion}  
**环境**: ${buildInfo.env} | **分支**: ${buildInfo.branch}  
**开始时间**: ${buildInfo.timestamp}  
**构建链接**: [查看进度](${buildInfo.buildUrl})

📋 **构建计划**  
• 代码检出  
• 单元测试  
• 代码质量扫描  
• 依赖安全扫描  
• 构建打包  
• 镜像构建（如启用）

⏰ **预计时长**: ${buildInfo.estimatedDuration ?: '10-15分钟'}
        """.trim()
    }
    
    /**
     * 构建成功通知内容（飞书格式）
     */
    private String buildSuccessContent(Map buildInfo) {
        return """**项目名称**: ${buildInfo.projectName}  
**构建版本**: ${buildInfo.buildVersion}  
**环境**: ${buildInfo.env} | **分支**: ${buildInfo.branch}  
**构建时间**: ${buildInfo.timestamp}  
**构建链接**: [查看详情](${buildInfo.buildUrl})

💡 **构建结果**  
• 单元测试: ${buildInfo.testStatus ? '✅ 通过' : '❌ 失败'}  
• 代码扫描: ${buildInfo.scanStatus ? '✅ 通过' : '❌ 失败'}  
• 依赖扫描: ${buildInfo.depScanStatus ? '✅ 通过' : '❌ 失败'}  
• 镜像构建: ${buildInfo.kanikoStatus ? '✅ 完成' : '⏸️ 未执行'}

📊 **质量统计**  
• 测试覆盖率: ${buildInfo.coverage ?: 'N/A'}  
• 代码质量: ${buildInfo.qualityGate ?: 'N/A'}  
• 漏洞数量: ${buildInfo.vulnerabilities ?: 0}
        """.trim()
    }
    
    /**
     * 构建失败通知内容（飞书格式）
     */
    private String buildFailureContent(Map buildInfo, String errorMessage) {
        return """**项目名称**: ${buildInfo.projectName}  
**构建版本**: ${buildInfo.buildVersion}  
**环境**: ${buildInfo.env} | **失败阶段**: ${buildInfo.failedStage ?: '未知'}  
**失败时间**: ${buildInfo.timestamp}  
**构建链接**: [查看详情](${buildInfo.buildUrl})

🔴 **错误信息**  
${errorMessage}

📋 **处理建议**  
1. 查看构建日志分析错误  
2. 检查相关配置是否正确  
3. 联系开发人员处理问题  
4. 重新触发构建
        """.trim()
    }
    
    /**
     * 构建不稳定通知内容（飞书格式）
     */
    private String buildUnstableContent(Map buildInfo, String unstableReason) {
        return """**项目名称**: ${buildInfo.projectName}  
**构建版本**: ${buildInfo.buildVersion}  
**环境**: ${buildInfo.env} | **状态**: 不稳定  
**时间**: ${buildInfo.timestamp}  
**构建链接**: [查看详情](${buildInfo.buildUrl})

⚠️ **不稳定原因**  
${unstableReason}

💡 **处理建议**  
1. 检查测试用例失败原因  
2. 分析代码质量报告  
3. 修复相关问题后重新构建  
4. 确认是否影响生产部署
        """.trim()
    }
}