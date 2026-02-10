package org.ecochain.ci

/**
 * 公共工具类
 * 注意：此类中的方法需要在 Jenkins Pipeline 步骤上下文中调用
 */
class Utils {
    
    // 这些方法需要在 Jenkins Pipeline 步骤中调用
    // 调用时确保有正确的步骤上下文（如：script.sh, script.echo）
    
    /**
     * 获取项目名称（安全处理 projectKey）
     */
    static String getProjectName(String projectKey, String defaultName = '') {
        try {
            if (projectKey && projectKey.contains(':')) {
                return projectKey.split(':')[1]
            }
            return defaultName ?: projectKey
        } catch (Exception e) {
            echo "获取项目名称失败，使用默认值: ${e.message}"
            return defaultName ?: 'unknown'
        }
    }
    
    /**
     * 安全执行操作，带重试机制
     */
    static def safeExecute(Closure operation, int maxRetries = 3, long delay = 5000) {
        def lastException
        for (int i = 0; i < maxRetries; i++) {
            try {
                return operation.call()
            } catch (Exception e) {
                lastException = e
                echo "操作失败，重试 ${i + 1}/${maxRetries}: ${e.message}"
                if (i < maxRetries - 1) {
                    sleep(delay)
                }
            }
        }
        throw lastException
    }
    
    /**
     * 验证必需参数
     */
    static void validateRequiredParams(Map params, List requiredKeys) {
        def missingKeys = requiredKeys.findAll { !params.containsKey(it) || params[it] == null }
        if (missingKeys) {
            error "缺少必需参数: ${missingKeys.join(', ')}"
        }
    }
    
    /**
     * 获取默认配置模板
     */
    static Map getDefaultConfig(String configType = 'basic') {
        def baseConfig = [
            projectKey: '',
            src: 'src/main/java',
            buildVersion: "v${getTimestamp()}",
            env: 'dev',
            timeout: 60,
            parallel: false,
            dockerImage: 'maven:3.9-eclipse-temurin-17',
            artifacts: '**/target/*.jar,**/target/*.war'
        ]
        
        switch(configType) {
            case 'basic':
                return baseConfig + [
                    enableTest: true,
                    enableScan: false,
                    enableDepScan: false,
                    enableArchive: false,
                    enableKaniko: false,
                    testCmd: 'mvn test',
                    buildCmd: 'mvn -DskipTests package -Dbuild.version=${env.BUILD_VERSION}',
                    mvnOpts: '-Xmx512m'
                ]
            case 'standard':
                return baseConfig + [
                    enableTest: true,
                    enableScan: true,
                    enableDepScan: true,
                    enableArchive: true,
                    enableKaniko: false,
                    testCmd: 'mvn clean test',
                    buildCmd: 'mvn clean package -DskipTests -Dbuild.version=${env.BUILD_VERSION}',
                    mvnOpts: '-Xmx1024m',
                    depScanSeverity: 'HIGH,CRITICAL',
                    depScanSkipDirs: ['target', '.git', '.idea'],
                    projectVersion: '1.0.0',
                    binaries: 'target/classes',
                    exclusions: '**/test/**,**/generated/**',
                    coverage: 'target/site/jacoco/jacoco.xml',
                    sourceEncoding: 'UTF-8',
                    javaVersion: '11'
                ]
            case 'full':
                return baseConfig + [
                    enableTest: true,
                    enableScan: true,
                    enableDepScan: true,
                    enableArchive: true,
                    enableKaniko: true,
                    enableDingTalk: true,  // 完整配置默认启用钉钉通知
                    testCmd: 'mvn clean test',
                    buildCmd: 'mvn clean package -DskipTests -Dbuild.version=${env.BUILD_VERSION}',
                    mvnOpts: '-Xmx1024m',
                    depScanSeverity: 'HIGH,CRITICAL',
                    depScanSkipDirs: ['target', '.git', '.idea'],
                    notifyOnSuccess: true,
                    notifyOnFailure: true,
                    projectVersion: '1.0.0',
                    binaries: 'target/classes',
                    exclusions: '**/test/**,**/generated/**',
                    coverage: 'target/site/jacoco/jacoco.xml',
                    sourceEncoding: 'UTF-8',
                    javaVersion: '11',
                    kanikoDockerfile: 'Dockerfile',
                    kanikoContextPath: '.',
                    kanikoRegistry: 'registry.example.com',
                    kanikoProject: 'default',
                    kanikoCredentialsId: 'kaniko-credentials',
                    // 钉钉通知配置
                    dingtalkWebhook: '',  // 钉钉Webhook地址
                    dingtalkAtMobiles: []  // 需要@的手机号列表
                ]
            default:
                return baseConfig
        }
    }
    /**
     * 获取当前时间戳
     * @return 格式化的时间戳字符串 (yyyyMMddHHmmss)
     */
    static String getTimestamp() {
        return new Date().format('yyyyMMddHHmmss')
    }

    /**
     * 获取构建版本号
     * @return 带版本前缀的构建号 (v{timestamp})
     */
    static String getBuildVersion() {
        return "v${getTimestamp()}"
    }

    /**
     * 打印分隔线
     * @param title 分隔线标题
     */
    static void printSeparator(String title = '') {
        def line = "========================================"
        if (title) {
            println "${line} ${title} ${line}"
        } else {
            println line
        }
    }

    /**
     * 获取 Git 分支名
     * @return 当前分支名
     */
    static String getGitBranch() {
        try {
            return sh(script: 'git rev-parse --abbrev-ref HEAD', returnStdout: true).trim()
        } catch (Exception e) {
            echo "获取 Git 分支失败: ${e.message}"
            return 'unknown'
        }
    }

    /**
     * 获取 Git 提交哈希值
     * @return 短哈希值 (7位)
     */
    static String getGitCommitHash() {
        try {
            return sh(script: 'git rev-parse --short=7 HEAD', returnStdout: true).trim()
        } catch (Exception e) {
            echo "获取 Git 提交哈希失败: ${e.message}"
            return 'unknown'
        }
    }

    /**
     * 获取 Git 提交信息
     * @return 提交信息
     */
    static String getGitCommitMessage() {
        try {
            return sh(script: 'git log -1 --pretty=%B', returnStdout: true).trim()
        } catch (Exception e) {
            echo "获取 Git 提交信息失败: ${e.message}"
            return 'unknown'
        }
    }

    /**
     * 获取 Git 提交者信息
     * @return 提交者姓名和邮箱
     */
    static String getGitCommitter() {
        try {
            def name = sh(script: 'git log -1 --pretty=%cn', returnStdout: true).trim()
            def email = sh(script: 'git log -1 --pretty=%ce', returnStdout: true).trim()
            return "${name} <${email}>"
        } catch (Exception e) {
            echo "获取 Git 提交者失败: ${e.message}"
            return 'unknown'
        }
    }

    /**
     * 获取构建信息
     * @return 包含分支、提交、时间等信息的 Map
     */
    static Map getBuildInfo() {
        return [
            branch: getGitBranch(),
            commit: getGitCommitHash(),
            message: getGitCommitMessage(),
            committer: getGitCommitter(),
            timestamp: getTimestamp(),
            version: getBuildVersion()
        ]
    }
    
    /**
     * 获取完整的构建信息（用于钉钉通知）
     */
    static Map getFullBuildInfo(Map cfg, String buildStatus = 'unknown', String failedStage = null) {
        def buildInfo = getBuildInfo()
        def buildUrl = System.getenv('BUILD_URL') ?: 'N/A'
        
        return buildInfo + [
            projectName: cfg.projectName ?: getProjectName(cfg.projectKey),
            projectKey: cfg.projectKey,
            buildVersion: cfg.buildVersion ?: buildInfo.version,
            env: cfg.env ?: 'dev',
            buildStatus: buildStatus,
            buildUrl: buildUrl,
            failedStage: failedStage,
            timestamp: new Date().format('yyyy-MM-dd HH:mm:ss'),
            // 构建统计信息（需要在构建过程中收集）
            testStatus: false,
            scanStatus: false,
            depScanStatus: false,
            kanikoStatus: false,
            coverage: 'N/A',
            qualityGate: 'N/A',
            vulnerabilities: 0,
            duration: 'N/A'
        ]
    }
    
    /**
     * 检查是否启用钉钉通知
     */
    static boolean isDingTalkEnabled() {
        def webhookUrl = System.getenv('DINGTALK_WEBHOOK_URL')
        return webhookUrl != null && !webhookUrl.trim().isEmpty()
    }
    
    /**
     * 获取需要@的手机号列表
     */
    static List getAtMobiles(Map cfg) {
        def atMobiles = []
        
        // 从配置中获取
        if (cfg.dingtalkAtMobiles instanceof List) {
            atMobiles.addAll(cfg.dingtalkAtMobiles)
        }
        
        // 从环境变量获取
        def envAtMobiles = System.getenv('DINGTALK_AT_MOBILES')
        if (envAtMobiles) {
            atMobiles.addAll(envAtMobiles.split(',').collect { it.trim() })
        }
        
        return atMobiles.unique()
    }
    
    /**
     * 获取完整的构建信息（用于钉钉通知）
     */
    static Map getFullBuildInfo(Map cfg, String buildStatus = 'unknown', String failedStage = null) {
        def buildInfo = getBuildInfo()
        def buildUrl = System.getenv('BUILD_URL') ?: 'N/A'
        
        return buildInfo + [
            projectName: cfg.projectName ?: getProjectName(cfg.projectKey),
            projectKey: cfg.projectKey,
            buildVersion: cfg.buildVersion ?: buildInfo.version,
            env: cfg.env ?: 'dev',
            buildStatus: buildStatus,
            buildUrl: buildUrl,
            failedStage: failedStage,
            timestamp: new Date().format('yyyy-MM-dd HH:mm:ss'),
            // 构建统计信息（需要在构建过程中收集）
            testStatus: false,
            scanStatus: false,
            depScanStatus: false,
            kanikoStatus: false,
            coverage: 'N/A',
            qualityGate: 'N/A',
            vulnerabilities: 0,
            duration: 'N/A'
        ]
    }
    
    /**
     * 检查是否启用钉钉通知
     */
    static boolean isDingTalkEnabled() {
        def webhookUrl = System.getenv('DINGTALK_WEBHOOK_URL')
        return webhookUrl != null && !webhookUrl.trim().isEmpty()
    }
    
    /**
     * 获取需要@的手机号列表
     */
    static List getAtMobiles(Map cfg) {
        def atMobiles = []
        
        // 从配置中获取
        if (cfg.dingtalkAtMobiles instanceof List) {
            atMobiles.addAll(cfg.dingtalkAtMobiles)
        }
        
        // 从环境变量获取
        def envAtMobiles = System.getenv('DINGTALK_AT_MOBILES')
        if (envAtMobiles) {
            atMobiles.addAll(envAtMobiles.split(',').collect { it.trim() })
        }
        
        return atMobiles.unique()
    }

    /**
     * 执行 Maven 命令
     * @param cmd Maven 命令
     */
    static void maven(String cmd) {
        try {
            echo "执行 Maven 命令: mvn ${cmd}"
            sh "mvn ${cmd}"
        } catch (Exception e) {
            echo "Maven 命令执行失败: ${e.message}"
            throw e
        }
    }

    /**
     * 获取项目版本号
     * @return 从 pom.xml 读取的版本号
     */
    static String getProjectVersion() {
        try {
            return sh(
                script: 'mvn help:evaluate -Dexpression=project.version -q -DforceStdout',
                returnStdout: true
            ).trim()
        } catch (Exception e) {
            echo "获取项目版本失败: ${e.message}"
            return 'unknown'
        }
    }

    /**
     * 获取项目 Artifact ID
     * @return 从 pom.xml 读取的 artifactId
     */
    static String getProjectArtifactId() {
        try {
            return sh(
                script: 'mvn help:evaluate -Dexpression=project.artifactId -q -DforceStdout',
                returnStdout: true
            ).trim()
        } catch (Exception e) {
            echo "获取项目 Artifact ID 失败: ${e.message}"
            return 'unknown'
        }
    }

    /**
     * 获取项目 Group ID
     * @return 从 pom.xml 读取的 groupId
     */
    static String getProjectGroupId() {
        try {
            return sh(
                script: 'mvn help:evaluate -Dexpression=project.groupId -q -DforceStdout',
                returnStdout: true
            ).trim()
        } catch (Exception e) {
            echo "获取项目 Group ID 失败: ${e.message}"
            return 'unknown'
        }
    }

    /**
     * 获取完整的项目坐标
     * @return groupId:artifactId:version
     */
    static String getProjectCoordinates() {
        return "${getProjectGroupId()}:${getProjectArtifactId()}:${getProjectVersion()}"
    }

    /**
     * 检查文件是否存在
     * @param filePath 文件路径
     * @return 文件是否存在
     */
    static boolean fileExists(String filePath) {
        try {
            return fileExists(filePath)
        } catch (Exception e) {
            echo "检查文件存在性失败: ${e.message}"
            return false
        }
    }

    /**
     * 读取文件内容
     * @param filePath 文件路径
     * @return 文件内容
     */
    static String readFile(String filePath) {
        try {
            return readFile(filePath).trim()
        } catch (Exception e) {
            echo "读取文件失败: ${e.message}"
            return ''
        }
    }

    /**
     * 写入文件内容
     * @param filePath 文件路径
     * @param content 文件内容
     */
    static void writeFile(String filePath, String content) {
        try {
            writeFile file: filePath, text: content
        } catch (Exception e) {
            echo "写入文件失败: ${e.message}"
            throw e
        }
    }
}