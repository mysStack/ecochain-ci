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
        
        def title = org.ecochain.ci.MessageBuilder.buildTitle('success', buildInfo.projectName)
        def content = org.ecochain.ci.MessageBuilder.buildSuccessContent(buildInfo, 'markdown')
        
        def message = buildMessageTemplate(title, content, buildInfo, cfg)
        return sendNotification(message, cfg)
    }
    
    @Override
    def sendBuildFailureNotification(Map buildInfo, String errorMessage, Map cfg) {
        if (!isEnabled(cfg)) return
        
        def title = org.ecochain.ci.MessageBuilder.buildTitle('failure', buildInfo.projectName)
        def content = org.ecochain.ci.MessageBuilder.buildFailureContent(buildInfo, errorMessage, 'markdown')
        
        def message = buildMessageTemplate(title, content, buildInfo, cfg)
        message.isAtAll = true  // 失败时@所有人
        
        return sendNotification(message, cfg)
    }
    
    @Override
    def sendBuildUnstableNotification(Map buildInfo, String unstableReason, Map cfg) {
        if (!isEnabled(cfg)) return
        
        def title = org.ecochain.ci.MessageBuilder.buildTitle('unstable', buildInfo.projectName)
        def content = org.ecochain.ci.MessageBuilder.buildUnstableContent(buildInfo, unstableReason, 'markdown')
        
        def message = buildMessageTemplate(title, content, buildInfo, cfg)
        return sendNotification(message, cfg)
    }
    
    @Override
    def sendBuildStartNotification(Map buildInfo, Map cfg) {
        if (!isEnabled(cfg)) return
        
        def title = org.ecochain.ci.MessageBuilder.buildTitle('started', buildInfo.projectName)
        def content = org.ecochain.ci.MessageBuilder.buildStartContent(buildInfo, 'markdown')
        
        def message = buildMessageTemplate(title, content, buildInfo, cfg)
        return sendNotification(message, cfg)
    }
}