// Java Maven CI Pipeline
def call(Closure body) {

    // 1️⃣ 接收业务侧参数
    def cfg = [
        appName          : '',
        mavenCmd         : 'mvn -B clean package',
        enableScan       : false,
        enableImage      : false,
        imageName        : '',
        imageTag         : 'latest',
        registryUrl      : '',
        kanikoArgs       : '--cache=true --cache-ttl=24h',
        mavenImage       : 'crpi-p97knjjid10efrly.cn-shanghai.personal.cr.aliyuncs.com/ecochain/maven:3.8-eclipse-temurin-8',
        kanikoImage      : 'gcr.io/kaniko-project/executor:latest'
    ]

    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = cfg
    body()

    // 2️⃣ 引入公共能力
    def utils = new org.ecochain.ci.common.PipelineUtils(this)

    pipeline {
        agent {
            kubernetes {
                yaml libraryResource('pod/multi-container-template.yaml')
                    .replace('MAVEN_IMAGE_PLACEHOLDER', cfg.mavenImage)
                    .replace('KANIKO_IMAGE_PLACEHOLDER', cfg.kanikoImage)
            }
        }

        stages {

            stage('Init') {
                steps {
                    script {
                        utils.info("Start CI for ${cfg.appName}")
                        
                        // 验证 Pod 模板配置（在构建阶段会重用此模板）
                        def replacements = [
                            'MAVEN_IMAGE_PLACEHOLDER': cfg.mavenImage,
                            'KANIKO_IMAGE_PLACEHOLDER': cfg.kanikoImage
                        ]
                        
                        def validationErrors = utils.validateCompleteTemplate(libraryResource('pod/multi-container-template.yaml'), replacements)
                        
                        if (!validationErrors.isEmpty()) {
                            utils.warn("Template validation completed with ${validationErrors.size()} warnings")
                            // 可以设置为错误阻止构建：error("Template validation failed")
                        }
                    }
                }
            }

            stage('Build') {
                steps {
                    script {
                        utils.info("Run maven build")
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
                        utils.info("Code scan enabled (placeholder)")
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
                        utils.info("Start Kaniko image build for ${cfg.imageName}:${cfg.imageTag}")
                        
                        // 验证镜像构建参数
                        if (!cfg.imageName) {
                            error("Image name is required when enableImage is true")
                        }
                        
                        if (!cfg.registryUrl) {
                            error("Registry URL is required for Kaniko image building")
                        }
                        
                        // 检查 Dockerfile 是否存在
                        if (!fileExists('Dockerfile')) {
                            error("Dockerfile not found in project root directory. Please add a Dockerfile to enable image building.")
                        }
                        
                        // 使用 Kaniko 构建镜像（只构建运行阶段）
                        container('kaniko') {
                            // 只复制构建产物和必要的运行文件到 Kaniko 工作目录
                            sh '''
                                echo "Copying build artifacts to Kaniko workspace..."
                                # 检查并复制构建产物
                                if [ -d "/home/jenkins/agent/target" ] && [ "$(ls -A /home/jenkins/agent/target)" ]; then
                                    cp -r /home/jenkins/agent/target/* /workspace/
                                    echo "Build artifacts copied successfully"
                                else
                                    echo "WARNING: No build artifacts found in target directory"
                                    ls -la /home/jenkins/agent/ || echo "Agent directory not accessible"
                                fi
                                
                                # 复制 Dockerfile
                                if [ -f "/home/jenkins/agent/Dockerfile" ]; then
                                    cp /home/jenkins/agent/Dockerfile /workspace/
                                    echo "Dockerfile copied successfully"
                                else
                                    echo "ERROR: Dockerfile not found in agent workspace"
                                    exit 1
                                fi
                                
                                echo "Files in workspace:"
                                ls -la /workspace/
                                echo "Dockerfile content:"
                                cat /workspace/Dockerfile
                            '''
                            
                            // 执行 Kaniko 构建命令
                            sh """
                                echo "Starting Kaniko runtime image build for ${cfg.registryUrl}/${cfg.imageName}:${cfg.imageTag}"
                                /kaniko/executor \
                                    --dockerfile=Dockerfile \
                                    --context=dir:///workspace \
                                    --destination=${cfg.registryUrl}/${cfg.imageName}:${cfg.imageTag} \
                                    ${cfg.kanikoArgs}
                            """
                        }
                        
                        utils.info("Kaniko image build completed successfully")
                    }
                }
            }
        }

        post {
            success {
                script {
                    utils.info("CI success for ${cfg.appName}")
                }
            }
            failure {
                script {
                    utils.error("CI failed for ${cfg.appName}")
                }
            }
        }
    }
}