package org.ecochain.ci

/**
 * 基础通知器抽象类
 * 定义统一的通知接口，各渠道通知器需要实现此接口
 */
abstract class BaseNotifier implements Serializable {
    def steps
    
    BaseNotifier(steps) {
        this.steps = steps
    }
    
    /**
     * 发送构建开始通知
     */
    abstract def sendBuildStartNotification(Map buildInfo, Map cfg)
    
    /**
     * 发送构建成功通知
     */
    abstract def sendBuildSuccessNotification(Map buildInfo, Map cfg)
    
    /**
     * 发送构建失败通知
     */
    abstract def sendBuildFailureNotification(Map buildInfo, String errorMessage, Map cfg)
    
    /**
     * 发送构建不稳定通知
     */
    abstract def sendBuildUnstableNotification(Map buildInfo, String unstableReason, Map cfg)
    
    /**
     * 通用通知方法
     */
    abstract def sendNotification(Map message, Map cfg)
    
    /**
     * 获取渠道名称
     */
    abstract String getChannelName()
    
    /**
     * 检查是否启用该渠道通知
     */
    boolean isEnabled(Map cfg) {
        def channelEnabled = cfg."enable${getChannelName().capitalize()}" ?: false
        def globalEnabled = cfg.enableNotification ?: true
        return channelEnabled && globalEnabled
    }
    
    /**
     * 获取Webhook URL
     */
    String getWebhookUrl(Map cfg) {
        return cfg."${getChannelName().toLowerCase()}Webhook" ?: 
               System.getenv("${getChannelName().toUpperCase()}_WEBHOOK_URL")
    }
    
    /**
     * 获取需要@的人员列表
     */
    List getAtUsers(Map cfg) {
        def atUsers = cfg."${getChannelName().toLowerCase()}AtUsers" ?: []
        def envAtUsers = System.getenv("${getChannelName().toUpperCase()}_AT_USERS")
        
        if (envAtUsers) {
            atUsers.addAll(envAtUsers.split(',').collect { it.trim() })
        }
        
        return atUsers.unique()
    }
    
    /**
     * 构建通用的通知消息模板
     */
    Map buildMessageTemplate(String title, String content, Map buildInfo, Map cfg) {
        return [
            channel: getChannelName(),
            title: title,
            content: content,
            buildInfo: buildInfo,
            timestamp: new Date().format('yyyy-MM-dd HH:mm:ss'),
            atUsers: getAtUsers(cfg),
            isAtAll: buildInfo.buildStatus == 'failure'  // 失败时@所有人
        ]
    }
    
    /**
     * 发送HTTP请求
     */
    def sendHttpRequest(String url, Map payload, int timeout = 30) {
        try {
            steps.httpRequest([
                url: url,
                httpMode: 'POST',
                contentType: 'APPLICATION_JSON',
                requestBody: steps.writeJSON(json: payload),
                validResponseCodes: '200',
                timeout: timeout
            ])
            return [success: true]
        } catch (Exception e) {
            steps.echo "⚠️ ${getChannelName()} HTTP请求失败: ${e.message}"
            return [success: false, error: e.message]
        }
    }
}package org.ecochain.ci

/**
 * 基础通知器抽象类
 * 定义统一的通知接口，各渠道通知器需要实现此接口
 */
abstract class BaseNotifier implements Serializable {
    def steps
    
    BaseNotifier(steps) {
        this.steps = steps
    }
    
    /**
     * 发送构建开始通知
     */
    abstract def sendBuildStartNotification(Map buildInfo, Map cfg)
    
    /**
     * 发送构建成功通知
     */
    abstract def sendBuildSuccessNotification(Map buildInfo, Map cfg)
    
    /**
     * 发送构建失败通知
     */
    abstract def sendBuildFailureNotification(Map buildInfo, String errorMessage, Map cfg)
    
    /**
     * 发送构建不稳定通知
     */
    abstract def sendBuildUnstableNotification(Map buildInfo, String unstableReason, Map cfg)
    
    /**
     * 通用通知方法
     */
    abstract def sendNotification(Map message, Map cfg)
    
    /**
     * 获取渠道名称
     */
    abstract String getChannelName()
    
    /**
     * 检查是否启用该渠道通知
     */
    boolean isEnabled(Map cfg) {
        def channelEnabled = cfg."enable${getChannelName().capitalize()}" ?: false
        def globalEnabled = cfg.enableNotification ?: true
        return channelEnabled && globalEnabled
    }
    
    /**
     * 获取Webhook URL
     */
    String getWebhookUrl(Map cfg) {
        return cfg."${getChannelName().toLowerCase()}Webhook" ?: 
               System.getenv("${getChannelName().toUpperCase()}_WEBHOOK_URL")
    }
    
    /**
     * 获取需要@的人员列表
     */
    List getAtUsers(Map cfg) {
        def atUsers = cfg."${getChannelName().toLowerCase()}AtUsers" ?: []
        def envAtUsers = System.getenv("${getChannelName().toUpperCase()}_AT_USERS")
        
        if (envAtUsers) {
            atUsers.addAll(envAtUsers.split(',').collect { it.trim() })
        }
        
        return atUsers.unique()
    }
    
    /**
     * 构建通用的通知消息模板
     */
    Map buildMessageTemplate(String title, String content, Map buildInfo, Map cfg) {
        return [
            channel: getChannelName(),
            title: title,
            content: content,
            buildInfo: buildInfo,
            timestamp: new Date().format('yyyy-MM-dd HH:mm:ss'),
            atUsers: getAtUsers(cfg),
            isAtAll: buildInfo.buildStatus == 'failure'  // 失败时@所有人
        ]
    }
    
    /**
     * 发送HTTP请求
     */
    def sendHttpRequest(String url, Map payload, int timeout = 30) {
        try {
            steps.httpRequest([
                url: url,
                httpMode: 'POST',
                contentType: 'APPLICATION_JSON',
                requestBody: steps.writeJSON(json: payload),
                validResponseCodes: '200',
                timeout: timeout
            ])
            return [success: true]
        } catch (Exception e) {
            steps.echo "⚠️ ${getChannelName()} HTTP请求失败: ${e.message}"
            return [success: false, error: e.message]
        }
    }
}