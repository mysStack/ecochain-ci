def call(Map cfg) {
    // 使用错误处理器
    def errorHandler = new org.ecochain.ci.ErrorHandler(this)
    
    try {
        // 验证必需参数
        errorHandler.validateConfig(cfg, ['projectKey'])
        
        // 使用公共配置模板
        def fullConfig = org.ecochain.ci.Utils.getDefaultConfig('full')
        
        // 设置项目特定参数
        fullConfig.projectKey = cfg.projectKey
        fullConfig.projectName = cfg.projectName ?: org.ecochain.ci.Utils.getProjectName(cfg.projectKey)
        fullConfig.kanikoAppName = cfg.kanikoAppName ?: org.ecochain.ci.Utils.getProjectName(cfg.projectKey)
        
        // 处理Kaniko相关配置
        if (cfg.enableKaniko != null) {
            fullConfig.enableKaniko = cfg.enableKaniko
        }
        
        // 合并用户提供的配置（覆盖默认值）
        def finalConfig = fullConfig + cfg
        
        echo "✅ 完整配置验证通过，开始构建..."
        
        // 调用主方法
        javaMavenCI(finalConfig)
        
    } catch (Exception e) {
        errorHandler.logError("完整配置初始化失败", e)
        errorHandler.sendErrorNotification("完整配置初始化失败: ${e.message}", cfg.projectKey)
        throw e
    }
}