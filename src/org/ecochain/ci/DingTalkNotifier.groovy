package org.ecochain.ci

/**
 * 钉钉机器人通知工具类
 * 支持构建成功、失败、不稳定等状态的通知
 */
class DingTalkNotifier extends BaseNotifier {
    
    DingTalkNotifier(steps) {
        super(steps)
    }
    
    @Override
    String getChannelName() {
        return 'dingtalk'
    }
    
    @Override
    def sendNotification(Map message, Map cfg) {
        def webhookUrl = getWebhookUrl(cfg)
        if (!webhookUrl) {
            steps.echo "⚠️ 未配置钉钉Webhook地址，跳过通知"
            return [success: false, error: '未配置Webhook地址']
        }
        
        def payload = buildDingTalkPayload(message, cfg)
        return sendHttpRequest(webhookUrl, payload)
    }
    
    /**
     * 构建钉钉通知负载
     */
    private Map buildDingTalkPayload(Map message, Map cfg) {
        def atUsers = getAtUsers(cfg)
        def isAtAll = message.isAtAll ?: false
        
        return [
            msgtype: 'markdown',
            markdown: [
                title: message.title,
                text: message.content
            ],
            at: [
                atMobiles: atUsers,
                isAtAll: isAtAll
            ]
        ]
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
    
    /**
     * 构建成功通知内容
     */
    private String buildSuccessContent(Map buildInfo) {
        return """
## ✅ 构建成功通知

**项目名称**: ${buildInfo.projectName}  
**构建版本**: ${buildInfo.buildVersion}  
**构建环境**: ${buildInfo.env}  
**构建分支**: ${buildInfo.branch}  
**构建时间**: ${buildInfo.timestamp}  
**构建时长**: ${buildInfo.duration}  
**提交信息**: ${buildInfo.commitMessage}  
**构建链接**: [查看详情](${buildInfo.buildUrl})

---
💡 **构建信息**  
- 单元测试: ${buildInfo.testStatus ? '✅ 通过' : '❌ 失败'}  
- 代码扫描: ${buildInfo.scanStatus ? '✅ 通过' : '❌ 失败'}  
- 依赖扫描: ${buildInfo.depScanStatus ? '✅ 通过' : '❌ 失败'}  
- 镜像构建: ${buildInfo.kanikoStatus ? '✅ 完成' : '⏸️ 未执行'}

📊 **构建统计**  
- 测试覆盖率: ${buildInfo.coverage ?: 'N/A'}  
- 代码质量: ${buildInfo.qualityGate ?: 'N/A'}  
- 漏洞数量: ${buildInfo.vulnerabilities ?: 0}
        """.trim()
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
        message.isAtAll = true  // 失败时@所有人
        
        return sendNotification(message, cfg)
    }
    
    /**
     * 构建失败通知内容
     */
    private String buildFailureContent(Map buildInfo, String errorMessage) {
        return """
## ❌ 构建失败通知

**项目名称**: ${buildInfo.projectName}  
**构建版本**: ${buildInfo.buildVersion}  
**构建环境**: ${buildInfo.env}  
**失败阶段**: ${buildInfo.failedStage ?: '未知'}  
**失败时间**: ${buildInfo.timestamp}  
**构建链接**: [查看详情](${buildInfo.buildUrl})

---
### 🔴 错误信息
${errorMessage}

### 📋 失败原因分析
- 请检查代码提交是否包含语法错误
- 确认依赖包是否可用
- 验证构建环境配置
- 查看详细日志定位问题

💡 **建议操作**  
1. 查看构建日志分析具体错误  
2. 检查相关配置是否正确  
3. 联系开发人员处理问题  
4. 重新触发构建
        """.trim()
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
     * 构建不稳定通知内容
     */
    private String buildUnstableContent(Map buildInfo, String unstableReason) {
        return """
## ⚠️ 构建不稳定通知

**项目名称**: ${buildInfo.projectName}  
**构建版本**: ${buildInfo.buildVersion}  
**构建环境**: ${buildInfo.env}  
**不稳定原因**: ${unstableReason}  
**构建时间**: ${buildInfo.timestamp}  
**构建链接**: [查看详情](${buildInfo.buildUrl})

---
### 📊 构建状态
- 构建结果: 不稳定  
- 质量门禁: 未通过  
- 测试结果: 可能存在失败用例

💡 **处理建议**  
1. 检查测试用例失败原因  
2. 分析代码质量报告  
3. 修复相关问题后重新构建  
4. 确认是否影响生产部署
        """.trim()
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
    
    /**
     * 构建开始通知内容
     */
    private String buildStartContent(Map buildInfo) {
        return """
## 🚀 构建开始通知

**项目名称**: ${buildInfo.projectName}  
**构建版本**: ${buildInfo.buildVersion}  
**构建环境**: ${buildInfo.env}  
**构建分支**: ${buildInfo.branch}  
**开始时间**: ${buildInfo.timestamp}  
**构建链接**: [查看进度](${buildInfo.buildUrl})

---
### 📋 构建计划
- ✅ 代码检出  
- ✅ 单元测试  
- ✅ 代码质量扫描  
- ✅ 依赖安全扫描  
- ✅ 构建打包  
- ✅ 镜像构建（如启用）

⏰ **预计时长**: ${buildInfo.estimatedDuration ?: '10-15分钟'}
        """.trim()
    }
}package org.ecochain.ci

/**
 * 钉钉机器人通知工具类
 * 支持构建成功、失败、不稳定等状态的通知
 */
class DingTalkNotifier extends BaseNotifier {
    
    DingTalkNotifier(steps) {
        super(steps)
    }
    
    @Override
    String getChannelName() {
        return 'dingtalk'
    }
    
    @Override
    def sendNotification(Map message, Map cfg) {
        def webhookUrl = getWebhookUrl(cfg)
        if (!webhookUrl) {
            steps.echo "⚠️ 未配置钉钉Webhook地址，跳过通知"
            return [success: false, error: '未配置Webhook地址']
        }
        
        def payload = buildDingTalkPayload(message, cfg)
        return sendHttpRequest(webhookUrl, payload)
    }
    
    /**
     * 构建钉钉通知负载
     */
    private Map buildDingTalkPayload(Map message, Map cfg) {
        def atUsers = getAtUsers(cfg)
        def isAtAll = message.isAtAll ?: false
        
        return [
            msgtype: 'markdown',
            markdown: [
                title: message.title,
                text: message.content
            ],
            at: [
                atMobiles: atUsers,
                isAtAll: isAtAll
            ]
        ]
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
    
    /**
     * 构建成功通知内容
     */
    private String buildSuccessContent(Map buildInfo) {
        return """
## ✅ 构建成功通知

**项目名称**: ${buildInfo.projectName}  
**构建版本**: ${buildInfo.buildVersion}  
**构建环境**: ${buildInfo.env}  
**构建分支**: ${buildInfo.branch}  
**构建时间**: ${buildInfo.timestamp}  
**构建时长**: ${buildInfo.duration}  
**提交信息**: ${buildInfo.commitMessage}  
**构建链接**: [查看详情](${buildInfo.buildUrl})

---
💡 **构建信息**  
- 单元测试: ${buildInfo.testStatus ? '✅ 通过' : '❌ 失败'}  
- 代码扫描: ${buildInfo.scanStatus ? '✅ 通过' : '❌ 失败'}  
- 依赖扫描: ${buildInfo.depScanStatus ? '✅ 通过' : '❌ 失败'}  
- 镜像构建: ${buildInfo.kanikoStatus ? '✅ 完成' : '⏸️ 未执行'}

📊 **构建统计**  
- 测试覆盖率: ${buildInfo.coverage ?: 'N/A'}  
- 代码质量: ${buildInfo.qualityGate ?: 'N/A'}  
- 漏洞数量: ${buildInfo.vulnerabilities ?: 0}
        """.trim()
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
        message.isAtAll = true  // 失败时@所有人
        
        return sendNotification(message, cfg)
    }
    
    /**
     * 构建失败通知内容
     */
    private String buildFailureContent(Map buildInfo, String errorMessage) {
        return """
## ❌ 构建失败通知

**项目名称**: ${buildInfo.projectName}  
**构建版本**: ${buildInfo.buildVersion}  
**构建环境**: ${buildInfo.env}  
**失败阶段**: ${buildInfo.failedStage ?: '未知'}  
**失败时间**: ${buildInfo.timestamp}  
**构建链接**: [查看详情](${buildInfo.buildUrl})

---
### 🔴 错误信息
${errorMessage}

### 📋 失败原因分析
- 请检查代码提交是否包含语法错误
- 确认依赖包是否可用
- 验证构建环境配置
- 查看详细日志定位问题

💡 **建议操作**  
1. 查看构建日志分析具体错误  
2. 检查相关配置是否正确  
3. 联系开发人员处理问题  
4. 重新触发构建
        """.trim()
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
     * 构建不稳定通知内容
     */
    private String buildUnstableContent(Map buildInfo, String unstableReason) {
        return """
## ⚠️ 构建不稳定通知

**项目名称**: ${buildInfo.projectName}  
**构建版本**: ${buildInfo.buildVersion}  
**构建环境**: ${buildInfo.env}  
**不稳定原因**: ${unstableReason}  
**构建时间**: ${buildInfo.timestamp}  
**构建链接**: [查看详情](${buildInfo.buildUrl})

---
### 📊 构建状态
- 构建结果: 不稳定  
- 质量门禁: 未通过  
- 测试结果: 可能存在失败用例

💡 **处理建议**  
1. 检查测试用例失败原因  
2. 分析代码质量报告  
3. 修复相关问题后重新构建  
4. 确认是否影响生产部署
        """.trim()
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
    
    /**
     * 构建开始通知内容
     */
    private String buildStartContent(Map buildInfo) {
        return """
## 🚀 构建开始通知

**项目名称**: ${buildInfo.projectName}  
**构建版本**: ${buildInfo.buildVersion}  
**构建环境**: ${buildInfo.env}  
**构建分支**: ${buildInfo.branch}  
**开始时间**: ${buildInfo.timestamp}  
**构建链接**: [查看进度](${buildInfo.buildUrl})

---
### 📋 构建计划
- ✅ 代码检出  
- ✅ 单元测试  
- ✅ 代码质量扫描  
- ✅ 依赖安全扫描  
- ✅ 构建打包  
- ✅ 镜像构建（如启用）

⏰ **预计时长**: ${buildInfo.estimatedDuration ?: '10-15分钟'}
        """.trim()
    }
}