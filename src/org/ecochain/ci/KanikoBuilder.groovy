package org.ecochain.ci

class KanikoBuilder implements Serializable {
    def steps
    
    KanikoBuilder(steps) {
        this.steps = steps
    }
    
    def buildAndPush(String dockerfilePath = 'Dockerfile', String contextPath = '.', Map buildArgs = [:], List<String> tags = null) {
        steps.echo "开始使用Kaniko构建并推送镜像..."
        
        // 处理构建参数
        def buildArgsStr = buildArgs.collect { k, v -> "--build-arg ${k}=${v}" }.join(' ')
        
        // 处理镜像标签
        def imageTags = []
        if (tags) {
            imageTags = tags.collect { tag -> "${steps.env.IMAGE_NAME}:${tag}" }
        } else {
            imageTags = ["${steps.env.IMAGE_NAME}:${steps.env.IMAGE_TAG}", "${steps.env.IMAGE_NAME}:latest"]
        }
        
        // 构建目标参数
        def destinations = imageTags.collect { tag -> "--destination=${tag}" }.join(' ')
        
        steps.sh """
            /kaniko/executor \
                --dockerfile=${dockerfilePath} \
                --context=${contextPath} \
                ${buildArgsStr} \
                ${destinations} \
                --cache=true \
                --cache-dir=/cache
        """
        
        steps.echo "镜像构建并推送完成: ${imageTags.join(', ')}"
    }
    
    def buildAndPushWithCustomRegistry(String registry, String project, String appName, 
                                      String imageTag, String dockerfilePath = 'Dockerfile', 
                                      String contextPath = '.', Map buildArgs = [:]) {
        steps.echo "开始使用Kaniko构建并推送镜像到自定义仓库..."
        
        // 处理构建参数
        def buildArgsStr = buildArgs.collect { k, v -> "--build-arg ${k}=${v}" }.join(' ')
        
        // 构建镜像名称
        def imageName = "${registry}/${project}/${appName}"
        
        steps.sh """
            /kaniko/executor \
                --dockerfile=${dockerfilePath} \
                --context=${contextPath} \
                ${buildArgsStr} \
                --destination=${imageName}:${imageTag} \
                --destination=${imageName}:latest \
                --cache=true \
                --cache-dir=/cache
        """
        
        steps.echo "镜像构建并推送完成: ${imageName}:${imageTag} 和 ${imageName}:latest"
    }
}
