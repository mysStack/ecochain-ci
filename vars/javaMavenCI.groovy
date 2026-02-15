// Java Maven CI Pipeline
def call(Closure body) {

    // 1️⃣ 接收业务侧参数
    def cfg = [
        appName     : '',
        mavenCmd    : 'mvn -B clean package',
        enableScan  : false,
        enableImage : false,
        imageName   : '',
        imageTag    : 'latest',
        registryUrl : '',
        kanikoArgs  : '--cache=true --cache-ttl=24h'
    ]

    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = cfg
    body()

    // 2️⃣ 引入公共能力
    def logger = new org.ecochain.ci.common.Logger(this)

    pipeline {
        agent {
            kubernetes {
                yaml '''
apiVersion: v1
kind: Pod
spec:
  containers:
  - name: maven
    image: crpi-p97knjjid10efrly.cn-shanghai.personal.cr.aliyuncs.com/ecochain/maven:3.8-eclipse-temurin-8
    command: ["cat"]
    tty: true
    volumeMounts:
    - name: workspace
      mountPath: /home/jenkins/agent
  - name: kaniko
    image: gcr.io/kaniko-project/executor:latest
    command: ["cat"]
    tty: true
    volumeMounts:
    - name: workspace
      mountPath: /workspace
    - name: kaniko-secret
      mountPath: /kaniko/.docker
  volumes:
  - name: workspace
    emptyDir: {}
  - name: kaniko-secret
    secret:
      secretName: docker-registry-secret
      items:
      - key: .dockerconfigjson
        path: config.json
'''
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
                        logger.info("Start Kaniko image build for ${cfg.imageName}:${cfg.imageTag}")
                        
                        // 验证镜像构建参数
                        if (!cfg.imageName) {
                            error("Image name is required when enableImage is true")
                        }
                        
                        if (!cfg.registryUrl) {
                            error("Registry URL is required for Kaniko image building")
                        }
                        
                        // 使用 Kaniko 构建镜像（只构建运行阶段）
                        container('kaniko') {
                            // 只复制构建产物和必要的运行文件到 Kaniko 工作目录
                            sh '''
                                echo "Copying build artifacts to Kaniko workspace..."
                                # 复制构建产物
                                cp -r /home/jenkins/agent/target/* /workspace/ || true
                                # 复制 Dockerfile（应该只包含运行阶段）
                                cp /home/jenkins/agent/Dockerfile /workspace/ || true
                                echo "Files in workspace:"
                                ls -la /workspace/
                                echo "Dockerfile content:"
                                cat /workspace/Dockerfile || echo "Dockerfile not found"
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
                        
                        logger.info("Kaniko image build completed successfully")
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