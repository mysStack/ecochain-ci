package org.ecochain.ci

class KanikoHelper implements Serializable {
    def steps
    
    KanikoHelper(steps) {
        this.steps = steps
    }
    
    def setupKanikoConfig(String registry, String credentialsId) {
        steps.echo "配置Kaniko认证..."
        
        steps.withCredentials([steps.usernamePassword(credentialsId: credentialsId, usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
            steps.sh """
                mkdir -p /kaniko/.docker
                echo '{\"auths\":{\"${registry}\":{\"auth\":\"'$(echo -n ${USERNAME}:${PASSWORD} | base64)'\"}}}' > /kaniko/.docker/config.json
            """
        }
        
        steps.echo "Kaniko认证配置完成"
    }
    
    def clearCache() {
        steps.echo "清理Kaniko缓存..."
        
        steps.sh """
            rm -rf /cache/*
        """
        
        steps.echo "Kaniko缓存清理完成"
    }
}
