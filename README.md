# ecochain-ci

Jenkins Shared Library for ecochain CI/CD.

## Usage

```groovy
@Library('ecochain-ci') _
javaMavenCI {
  appName = 'order-service'
  enableScan = true
}
```

### Basic Java Maven Project

1. **Add Dockerfile to your project root** (Development Team Responsibility):

```dockerfile
# Example: Runtime-only Dockerfile (Recommended)
# Refer to resources/docker/java-runtime.Dockerfile for template
FROM crpi-p97knjjid10efrly.cn-shanghai.personal.cr.aliyuncs.com/ecochain/maven:3.8-eclipse-temurin-8
WORKDIR /app
COPY target/*.jar app.jar  # JAR built by Maven in Pod
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

**Important**: The Dockerfile should only contain runtime configuration. 
Maven build happens in the Pod, not in the Dockerfile.