package org.ecochain.ci

/**
 * 通知工厂类
 * 支持钉钉、飞书、企业微信等多种通知渠道
 */
class NotifierFactory implements Serializable {
    def steps
    
    NotifierFactory(steps) {
        this.steps = steps
    }
    
    /**
     * 创建通知器实例
     * @param channel 通知渠道类型：dingtalk, feishu, wecom, custom
     * @return 对应的通知器实例
     */
    def createNotifier(String channel = 'dingtalk') {
        switch(channel.toLowerCase()) {
            case 'dingtalk':
                return new DingTalkNotifier(steps)
            case 'feishu':
                return new FeishuNotifier(steps)
            case 'wecom':
            case 'wechatwork':
                return new WeComNotifier(steps)
            case 'custom':
                return new CustomNotifier(steps)
            default:
                steps.echo "⚠️ 不支持的通知渠道: ${channel}，使用默认的钉钉通知器"
                return new DingTalkNotifier(steps)
        }
    }
    
    /**
     * 根据配置自动选择通知器
     * @param cfg 构建配置
     * @return 通知器实例，如果没有配置则返回null
     */
    def createNotifierFromConfig(Map cfg) {
        // 优先使用显式配置的渠道
        if (cfg.notificationChannel) {
            return createNotifier(cfg.notificationChannel)
        }
        
        // 根据Webhook URL自动检测渠道
        def webhookUrl = cfg.notificationWebhook ?: System.getenv('NOTIFICATION_WEBHOOK_URL')
        if (webhookUrl) {
            def channel = detectChannelFromUrl(webhookUrl)
            if (channel) {
                return createNotifier(channel)
            }
        }
        
        // 检查环境变量配置的渠道
        def envChannel = System.getenv('NOTIFICATION_CHANNEL')
        if (envChannel) {
            return createNotifier(envChannel)
        }
        
        return null
    }
    
    /**
     * 根据Webhook URL自动检测通知渠道
     */
    private String detectChannelFromUrl(String webhookUrl) {
        if (webhookUrl.contains('dingtalk') || webhookUrl.contains('oapi.dingtalk.com')) {
            return 'dingtalk'
        } else if (webhookUrl.contains('feishu') || webhookUrl.contains('open.feishu.cn')) {
            return 'feishu'
        } else if (webhookUrl.contains('wecom') || webhookUrl.contains('qyapi.weixin.qq.com')) {
            return 'wecom'
        }
        return null
    }
    
    /**
     * 发送多渠道通知
     * @param channels 渠道列表
     * @param message 消息内容
     * @param cfg 构建配置
     */
    def sendMultiChannelNotification(List channels, Map message, Map cfg) {
        def results = [:]
        channels.each { channel ->
            try {
                def notifier = createNotifier(channel)
                def result = notifier.sendNotification(message, cfg)
                results[channel] = result
                steps.echo "✅ ${channel}通知发送成功"
            } catch (Exception e) {
                steps.echo "⚠️ ${channel}通知发送失败: ${e.message}"
                results[channel] = [success: false, error: e.message]
            }
        }
        return results
    }
}package org.ecochain.ci

/**
 * 通知工厂类
 * 支持钉钉、飞书、企业微信等多种通知渠道
 */
class NotifierFactory implements Serializable {
    def steps
    
    NotifierFactory(steps) {
        this.steps = steps
    }
    
    /**
     * 创建通知器实例
     * @param channel 通知渠道类型：dingtalk, feishu, wecom, custom
     * @return 对应的通知器实例
     */
    def createNotifier(String channel = 'dingtalk') {
        switch(channel.toLowerCase()) {
            case 'dingtalk':
                return new DingTalkNotifier(steps)
            case 'feishu':
                return new FeishuNotifier(steps)
            case 'wecom':
            case 'wechatwork':
                return new WeComNotifier(steps)
            case 'custom':
                return new CustomNotifier(steps)
            default:
                steps.echo "⚠️ 不支持的通知渠道: ${channel}，使用默认的钉钉通知器"
                return new DingTalkNotifier(steps)
        }
    }
    
    /**
     * 根据配置自动选择通知器
     * @param cfg 构建配置
     * @return 通知器实例，如果没有配置则返回null
     */
    def createNotifierFromConfig(Map cfg) {
        // 优先使用显式配置的渠道
        if (cfg.notificationChannel) {
            return createNotifier(cfg.notificationChannel)
        }
        
        // 根据Webhook URL自动检测渠道
        def webhookUrl = cfg.notificationWebhook ?: System.getenv('NOTIFICATION_WEBHOOK_URL')
        if (webhookUrl) {
            def channel = detectChannelFromUrl(webhookUrl)
            if (channel) {
                return createNotifier(channel)
            }
        }
        
        // 检查环境变量配置的渠道
        def envChannel = System.getenv('NOTIFICATION_CHANNEL')
        if (envChannel) {
            return createNotifier(envChannel)
        }
        
        return null
    }
    
    /**
     * 根据Webhook URL自动检测通知渠道
     */
    private String detectChannelFromUrl(String webhookUrl) {
        if (webhookUrl.contains('dingtalk') || webhookUrl.contains('oapi.dingtalk.com')) {
            return 'dingtalk'
        } else if (webhookUrl.contains('feishu') || webhookUrl.contains('open.feishu.cn')) {
            return 'feishu'
        } else if (webhookUrl.contains('wecom') || webhookUrl.contains('qyapi.weixin.qq.com')) {
            return 'wecom'
        }
        return null
    }
    
    /**
     * 发送多渠道通知
     * @param channels 渠道列表
     * @param message 消息内容
     * @param cfg 构建配置
     */
    def sendMultiChannelNotification(List channels, Map message, Map cfg) {
        def results = [:]
        channels.each { channel ->
            try {
                def notifier = createNotifier(channel)
                def result = notifier.sendNotification(message, cfg)
                results[channel] = result
                steps.echo "✅ ${channel}通知发送成功"
            } catch (Exception e) {
                steps.echo "⚠️ ${channel}通知发送失败: ${e.message}"
                results[channel] = [success: false, error: e.message]
            }
        }
        return results
    }
}