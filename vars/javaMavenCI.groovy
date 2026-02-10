def call(Map cfg) {
    // 参数验证
    if (!cfg.projectKey) {
        error 'projectKey is required parameter'
    }

    // 分支策略判断
    def defaultBranch = env.BRANCH_NAME ?: 'main'
    def branchName = params.BRANCH ?: defaultBranch
    def isFeatureBranch = branchName ==~ /feature\/.*/
    def isReleaseBranch = branchName ==~ /release\/.*/
    def isMainBranch = branchName in ['main', 'master', 'develop']
    def isHotfixBranch = branchName ==~ /hotfix\/.*/

    // 根据分支类型调整默认参数
    def enableTest = cfg.enableTest != null ? cfg.enableTest : true
    def enableScan = cfg.enableScan != null ? cfg.enableScan : !isFeatureBranch
    def enableDepScan = cfg.enableDepScan != null ? cfg.enableDepScan : !isFeatureBranch
    def enableArchive = cfg.enableArchive != null ? cfg.enableArchive : isMainBranch || isReleaseBranch

    // 获取构建信息
    def buildInfo = org.ecochain.ci.Utils.getBuildInfo()

    properties([
    parameters([
        gitParameter(branchFilter: 'origin/(.*)', 
                     defaultValue: defaultBranch, 
                     name: 'BRANCH',
                     type: 'PT_BRANCH',
                     description: '选择要构建的分支'),
        booleanParam(name: 'ENABLE_TEST', defaultValue: enableTest, description: '是否执行单元测试'),
        booleanParam(name: 'ENABLE_SCAN', defaultValue: enableScan, description: '是否执行代码扫描'),
        booleanParam(name: 'ENABLE_DEP_SCAN', defaultValue: enableDepScan, description: '是否执行依赖扫描'),
        booleanParam(name: 'ENABLE_ARCHIVE', defaultValue: enableArchive, description: '是否归档构建产物'),
        string(name: 'MVN_OPTS', defaultValue: cfg.mvnOpts ?: '', description: 'Maven 额外选项'),
        string(name: 'BUILD_VERSION', defaultValue: cfg.buildVersion ?: org.ecochain.ci.Utils.getBuildVersion(), description: '构建版本号')
    ])
])

pipeline {
    agent {
        kubernetes {
            yaml libraryResource("pod/maven.yaml")
        }
    }

    environment {
        MAVEN_OPTS = "${params.MVN_OPTS}"
        BUILD_VERSION = "${params.BUILD_VERSION}"
        BRANCH_NAME = "${params.BRANCH}"
    }

        options {
            timeout(time: 2, unit: 'HOURS')
            timestamps()
            disableConcurrentBuilds()
            buildDiscarder(logRotator(numToKeepStr: '10', artifactNumToKeepStr: '5'))
        }

        stages {
            stage('Init') {
                steps {
                    script {
                        org.ecochain.ci.Utils.printSeparator('Build Start')
                        
                        // 发送构建开始通知
                        if (org.ecochain.ci.Utils.isDingTalkEnabled() && cfg.enableDingTalk != false) {
                            try {
                                def dingTalk = new org.ecochain.ci.DingTalkNotifier(this)
                                def buildInfo = org.ecochain.ci.Utils.getFullBuildInfo(cfg, 'started')
                                buildInfo.dingtalkWebhook = cfg.dingtalkWebhook
                                
                                dingTalk.sendBuildStartNotification(
                                    buildInfo,
                                    cfg.dingtalkWebhook,
                                    org.ecochain.ci.Utils.getAtMobiles(cfg)
                                )
                            } catch (Exception e) {
                                echo "⚠️ 钉钉开始通知发送失败: ${e.message}"
                            }
                        }
                        
                        echo """
                        ========================================
                        构建信息:
                        - 项目: ${cfg.projectKey}
                        - 分支: ${branchName}
                        - 版本: ${env.BUILD_VERSION}
                        - 测试: ${params.ENABLE_TEST}
                        - 扫描: ${params.ENABLE_SCAN}
                        - 依赖扫描: ${params.ENABLE_DEP_SCAN}
                        - 归档: ${params.ENABLE_ARCHIVE}
                        - Kaniko镜像构建: ${cfg.kanikoEnabled ?: false}
                        - 钉钉通知: ${org.ecochain.ci.Utils.isDingTalkEnabled() && cfg.enableDingTalk != false}
                        ========================================
                        """
                    }
                }
            }
                steps {
                    script {
                        org.ecochain.ci.Utils.printSeparator('Build Start')
                        echo """
                        ========================================
                        构建信息:
                        - 项目: ${cfg.projectKey}
                        - 分支: ${branchName}
                        - 版本: ${env.BUILD_VERSION}
                        - 测试: ${params.ENABLE_TEST}
                        - 扫描: ${params.ENABLE_SCAN}
                        - 依赖扫描: ${params.ENABLE_DEP_SCAN}
                        - 归档: ${params.ENABLE_ARCHIVE}
                        ========================================
                        """
                    }
                }
            }

            stage('Checkout') {
                steps {
                    script {
                        org.ecochain.ci.Utils.printSeparator('Checkout')
                    }
                    checkout([
                        $class: 'GitSCM',
                        branches: [[name: "*/${env.BRANCH_NAME}"]],
                        userRemoteConfigs: [[url: scm.userRemoteConfigs[0].url]]
                    ])
                    script {
                        buildInfo = org.ecochain.ci.Utils.getBuildInfo()
                        echo "当前分支: ${env.BRANCH_NAME}"
                        echo "当前提交: ${buildInfo.commit}"
                        echo "提交信息: ${buildInfo.message}"
                    }
                }
            }

            stage('Unit Test') {
                when { expression { params.ENABLE_TEST } }
                steps {
                    script {
                        org.ecochain.ci.Utils.printSeparator('Unit Test')
                    }
                    timeout(time: 30, unit: 'MINUTES') {
                        sh cfg.testCmd ?: 'mvn test'
                    }
                    junit '**/target/surefire-reports/*.xml'
                }
            }

            stage('Code Scan') {
                when { expression { params.ENABLE_SCAN } }
                steps {
                    script {
                        org.ecochain.ci.Utils.printSeparator('Code Scan')
                    }
                    timeout(time: 30, unit: 'MINUTES') {
                        script {
                            org.ecochain.ci.Scan.sonar(cfg)
                            timeout(time: 5, unit: 'MINUTES') {
                                def qg = waitForQualityGate()
                                if (qg.status != 'OK') {
                                    error "Pipeline aborted due to quality gate failure: ${qg.status}"
                                }
                            }
                        }
                    }
                }
            }

            stage('Dependency Scan') {
                when { expression { params.ENABLE_DEP_SCAN } }
                steps {
                    script {
                        org.ecochain.ci.Utils.printSeparator('Dependency Scan')
                    }
                    timeout(time: 30, unit: 'MINUTES') {
                        container('trivy') {
                            script {
                                org.ecochain.ci.Trivy.scanMavenDependencies([
                                    severity: cfg.depScanSeverity ?: 'HIGH,CRITICAL',
                                    skipDirs: cfg.depScanSkipDirs ?: ['target', '.git', '.idea'],
                                    format: 'table',
                                    exitCode: 1
                                ])
                            }
                        }
                    }
                }
            }

            stage('Build') {
                steps {
                    script {
                        org.ecochain.ci.Utils.printSeparator('Build')
                    }
                    timeout(time: 30, unit: 'MINUTES') {
                        sh cfg.buildCmd ?: "mvn -DskipTests package -Dbuild.version=${env.BUILD_VERSION}"
                    }
                }
            }

            stage('Archive') {
                when { expression { params.ENABLE_ARCHIVE } }
                steps {
                    script {
                        org.ecochain.ci.Utils.printSeparator('Archive')
                    }
                    archiveArtifacts artifacts: cfg.artifacts ?: '**/target/*.jar,**/target/*.war', allowEmptyArchive: true
                }
            }

            stage('Kaniko Build') {
                when { 
                    allOf {
                        expression { cfg.kanikoEnabled ?: false }
                        expression { params.ENABLE_ARCHIVE }
                    }
                }
                steps {
                    script {
                        org.ecochain.ci.Utils.printSeparator('Kaniko Build')
                        
                        // 配置Kaniko认证
                        def kanikoHelper = new org.ecochain.ci.KanikoHelper(this)
                        if (cfg.kanikoRegistry && cfg.kanikoCredentialsId) {
                            kanikoHelper.setupKanikoConfig(cfg.kanikoRegistry, cfg.kanikoCredentialsId)
                        }
                        
                        // 执行Kaniko构建
                        def kanikoBuilder = new org.ecochain.ci.KanikoBuilder(this)
                        if (cfg.kanikoRegistry) {
                            // 使用自定义仓库
                            kanikoBuilder.buildAndPushWithCustomRegistry(
                                cfg.kanikoRegistry,
                                cfg.kanikoProject ?: 'default',
                                cfg.kanikoAppName ?: cfg.projectKey.split(':')[1],
                                env.BUILD_VERSION,
                                cfg.kanikoDockerfile ?: 'Dockerfile',
                                cfg.kanikoContextPath ?: '.',
                                cfg.kanikoBuildArgs ?: [:]
                            )
                        } else {
                            // 使用默认配置
                            kanikoBuilder.buildAndPush(
                                cfg.kanikoDockerfile ?: 'Dockerfile',
                                cfg.kanikoContextPath ?: '.',
                                cfg.kanikoBuildArgs ?: [:],
                                cfg.kanikoTags
                            )
                        }
                    }
                }
            }
        }

        post {
            always {
                script {
                    org.ecochain.ci.Utils.printSeparator('Build End')
                }
                // 清理工作空间
                cleanWs()
            }
            success {
                echo '✅ 构建成功！'
                script {
                    // 发送钉钉成功通知
                    if (org.ecochain.ci.Utils.isDingTalkEnabled() && cfg.enableDingTalk != false) {
                        try {
                            def dingTalk = new org.ecochain.ci.DingTalkNotifier(this)
                            def buildInfo = org.ecochain.ci.Utils.getFullBuildInfo(cfg, 'success')
                            
                            // 更新构建统计信息（这里需要根据实际构建结果更新）
                            buildInfo.testStatus = params.ENABLE_TEST
                            buildInfo.scanStatus = params.ENABLE_SCAN
                            buildInfo.depScanStatus = params.ENABLE_DEP_SCAN
                            buildInfo.kanikoStatus = cfg.kanikoEnabled ?: false
                            
                            dingTalk.sendBuildSuccessNotification(
                                buildInfo,
                                cfg.dingtalkWebhook,
                                org.ecochain.ci.Utils.getAtMobiles(cfg)
                            )
                        } catch (Exception e) {
                            echo "⚠️ 钉钉成功通知发送失败: ${e.message}"
                        }
                    }
                    
                    // 原有的邮件通知逻辑
                    if (cfg.notifyOnSuccess) {
                        echo "邮件通知: 构建成功 - ${env.BUILD_VERSION}"
                    }
                }
            }
            failure {
                echo '❌ 构建失败！'
                script {
                    // 发送钉钉失败通知
                    if (org.ecochain.ci.Utils.isDingTalkEnabled() && cfg.enableDingTalk != false) {
                        try {
                            def dingTalk = new org.ecochain.ci.DingTalkNotifier(this)
                            def buildInfo = org.ecochain.ci.Utils.getFullBuildInfo(cfg, 'failure', currentBuild.result)
                            
                            dingTalk.sendBuildFailureNotification(
                                buildInfo,
                                "构建在阶段 '${currentBuild.result}' 失败，请查看构建日志获取详细信息",
                                cfg.dingtalkWebhook,
                                org.ecochain.ci.Utils.getAtMobiles(cfg)
                            )
                        } catch (Exception e) {
                            echo "⚠️ 钉钉失败通知发送失败: ${e.message}"
                        }
                    }
                    
                    // 原有的邮件通知逻辑
                    if (cfg.notifyOnFailure) {
                        echo "邮件通知: 构建失败 - ${env.BUILD_VERSION}"
                    }
                }
            }
            unstable {
                echo '⚠️ 构建不稳定！'
                script {
                    // 发送钉钉不稳定通知
                    if (org.ecochain.ci.Utils.isDingTalkEnabled() && cfg.enableDingTalk != false) {
                        try {
                            def dingTalk = new org.ecochain.ci.DingTalkNotifier(this)
                            def buildInfo = org.ecochain.ci.Utils.getFullBuildInfo(cfg, 'unstable')
                            
                            dingTalk.sendBuildUnstableNotification(
                                buildInfo,
                                "构建结果不稳定，可能存在测试失败或质量门禁未通过的情况",
                                cfg.dingtalkWebhook,
                                org.ecochain.ci.Utils.getAtMobiles(cfg)
                            )
                        } catch (Exception e) {
                            echo "⚠️ 钉钉不稳定通知发送失败: ${e.message}"
                        }
                    }
                }
            }
            aborted {
                echo '⏹️ 构建已中止！'
            }
        }
    }
}