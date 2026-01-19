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
                    // 可以在这里添加通知逻辑
                    if (cfg.notifyOnSuccess) {
                        echo "通知: 构建成功 - ${env.BUILD_VERSION}"
                    }
                }
            }
            failure {
                echo '❌ 构建失败！'
                script {
                    // 可以在这里添加通知逻辑
                    if (cfg.notifyOnFailure) {
                        echo "通知: 构建失败 - ${env.BUILD_VERSION}"
                    }
                }
            }
            unstable {
                echo '⚠️ 构建不稳定！'
            }
            aborted {
                echo '⏹️ 构建已中止！'
            }
        }
    }
}
