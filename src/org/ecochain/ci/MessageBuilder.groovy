package org.ecochain.ci

/**
 * 统一消息构建器
 * 消除各通知器中的重复消息内容构建代码
 */
class MessageBuilder {
    
    /**
     * 构建开始通知内容
     */
    static String buildStartContent(Map buildInfo, String format = 'markdown') {
        def content = """
**项目名称**: ${buildInfo.projectName}  
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
        
        return formatContent(content, format)
    }
    
    /**
     * 构建成功通知内容
     */
    static String buildSuccessContent(Map buildInfo, String format = 'markdown') {
        def content = """
**项目名称**: ${buildInfo.projectName}  
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
        
        return formatContent(content, format)
    }
    
    /**
     * 构建失败通知内容
     */
    static String buildFailureContent(Map buildInfo, String errorMessage, String format = 'markdown') {
        def content = """
**项目名称**: ${buildInfo.projectName}  
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
        
        return formatContent(content, format)
    }
    
    /**
     * 构建不稳定通知内容
     */
    static String buildUnstableContent(Map buildInfo, String unstableReason, String format = 'markdown') {
        def content = """
**项目名称**: ${buildInfo.projectName}  
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
        
        return formatContent(content, format)
    }
    
    /**
     * 根据格式格式化内容
     */
    private static String formatContent(String content, String format) {
        switch(format.toLowerCase()) {
            case 'feishu':
                // 飞书格式调整
                return content.replaceAll('\*\*([^*]+)\*\*', '**$1**') // 保持粗体
            case 'wecom':
                // 企业微信格式调整
                return content.replaceAll('\[([^\]]+)\]\(([^)]+)\)', '$1($2)') // 简化链接
            default:
                return content // 默认Markdown格式
        }
    }
    
    /**
     * 构建通知标题
     */
    static String buildTitle(String buildStatus, String projectName) {
        def statusIcons = [
            'started': '🚀',
            'success': '✅', 
            'failure': '❌',
            'unstable': '⚠️'
        ]
        
        def statusTexts = [
            'started': '开始构建',
            'success': '构建成功',
            'failure': '构建失败', 
            'unstable': '构建不稳定'
        ]
        
        def icon = statusIcons[buildStatus] ?: '📢'
        def text = statusTexts[buildStatus] ?: '构建通知'
        
        return "${icon} ${text} - ${projectName}"
    }
}