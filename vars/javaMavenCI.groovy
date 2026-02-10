// Java Maven CI Pipeline
def call(Closure body) {

    // 1️⃣ 接收业务侧参数
    def cfg = [
        appName     : '',
        mavenCmd    : 'mvn -B clean package',
        enableScan  : false,
        enableImage : false
    ]

    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = cfg
    body()

    // 2️⃣ 引入公共能力
    def logger = new org.ecochain.ci.common.Logger(this)

    pipeline {
        agent {
            kubernetes {
                yaml libraryResource('pod/maven.yaml')
            }
        }

        stages {

            stage('Init') {
                steps {
                    script {
                        logger.info("Start CI for ${cfg.appName}")
                    }
                }
            }

            stage('Build') {
                steps {
                    script {
                        logger.info("Run maven build")
                        sh cfg.mavenCmd
                    }
                }
            }

            stage('Scan') {
                when {
                    expression { cfg.enableScan }
                }
                steps {
                    script {
                        logger.info("Code scan enabled (placeholder)")
                        // 后面直接接 sonar 模块
                    }
                }
            }

            stage('Image') {
                when {
                    expression { cfg.enableImage }
                }
                steps {
                    script {
                        logger.info("Image build enabled (placeholder)")
                    }
                }
            }
        }

        post {
            success {
                script {
                    logger.info("CI success for ${cfg.appName}")
                }
            }
            failure {
                script {
                    logger.error("CI failed for ${cfg.appName}")
                }
            }
        }
    }
}
