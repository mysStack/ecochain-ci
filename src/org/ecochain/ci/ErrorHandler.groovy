package org.ecochain.ci

/**
 * 错误处理工具类
 * 提供统一的错误处理、重试和回滚机制
 */
class ErrorHandler implements Serializable {
    def steps
    
    ErrorHandler(steps) {
        this.steps = steps
    }
    
    /**
     * 带重试的执行操作
     */
    def executeWithRetry(Closure operation, String operationName = '操作', 
                        int maxRetries = 3, long delay = 5000) {
        def lastException
        for (int i = 0; i < maxRetries; i++) {
            try {
                steps.echo "${operationName} - 尝试 ${i + 1}/${maxRetries}"
                return operation.call()
            } catch (Exception e) {
                lastException = e
                steps.echo "${operationName}失败: ${e.message}"
                
                if (i < maxRetries - 1) {
                    steps.echo "等待 ${delay/1000}秒后重试..."
                    steps.sleep(delay)
                }
            }
        }
        
        steps.echo "${operationName}重试 ${maxRetries}次后仍失败"
        throw new Exception("${operationName}失败: ${lastException.message}", lastException)
    }
    
    /**
     * 安全执行阶段操作
     */
    def safeStage(String stageName, Closure stageOperation, Closure cleanupOperation = null) {
        try {
            steps.stage(stageName) {
                return stageOperation.call()
            }
        } catch (Exception e) {
            steps.echo "❌ 阶段 '${stageName}' 执行失败: ${e.message}"
            
            // 执行清理操作
            if (cleanupOperation) {
                try {
                    steps.echo "执行清理操作..."
                    cleanupOperation.call()
                } catch (Exception cleanupEx) {
                    steps.echo "⚠️ 清理操作失败: ${cleanupEx.message}"
                }
            }
            
            throw e
        }
    }
    
    /**
     * 验证配置参数
     */
    void validateConfig(Map config, List requiredParams = ['projectKey']) {
        def missingParams = requiredParams.findAll { !config[it] }
        if (missingParams) {
            throw new IllegalArgumentException("缺少必需参数: ${missingParams.join(', ')}")
        }
        
        // 验证参数类型和范围
        if (config.timeout && config.timeout < 1) {
            throw new IllegalArgumentException("超时时间必须大于0")
        }
        
        if (config.env && !['dev', 'test', 'prod'].contains(config.env)) {
            throw new IllegalArgumentException("环境参数必须是 dev、test 或 prod")
        }
    }
    
    /**
     * 记录错误日志
     */
    void logError(String message, Exception e = null) {
        steps.echo "🚨 错误: ${message}"
        if (e) {
            steps.echo "异常详情: ${e.getClass().name}: ${e.message}"
            // 在生产环境中可以添加更详细的日志记录
            if (steps.env.DEBUG_MODE) {
                steps.echo "堆栈跟踪: ${e.stackTrace.join('\\n')}"
            }
        }
    }
    
    /**
     * 发送错误通知
     */
    void sendErrorNotification(String errorMessage, String projectName = '未知项目') {
        steps.echo "📧 发送错误通知: ${errorMessage}"
        
        // 这里可以集成邮件、Slack等通知渠道
        try {
            // 示例：邮件通知
            if (steps.env.NOTIFICATION_EMAIL) {
                steps.echo "发送邮件通知到: ${steps.env.NOTIFICATION_EMAIL}"
                // steps.mail(to: steps.env.NOTIFICATION_EMAIL, subject: "构建失败 - ${projectName}", body: errorMessage)
            }
            
            // 示例：Slack通知
            if (steps.env.SLACK_WEBHOOK_URL) {
                steps.echo "发送Slack通知"
                // steps.slackSend(channel: '#builds', message: errorMessage)
            }
        } catch (Exception e) {
            steps.echo "⚠️ 发送通知失败: ${e.message}"
        }
    }
}