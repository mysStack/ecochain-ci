# Java Runtime Dockerfile Template
# For projects using ecochain-ci shared library
# This template should be used as reference for project-specific Dockerfiles

# Base runtime image (match the build environment)
FROM crpi-p97knjjid10efrly.cn-shanghai.personal.cr.aliyuncs.com/ecochain/maven:3.8-eclipse-temurin-8

# Set working directory
WORKDIR /app

# Copy the built JAR from Maven build stage
# Note: The JAR is built in the Pod's maven container, not in Dockerfile
COPY target/*.jar app.jar

# Create log directory
RUN mkdir -p /logs

# Expose application port
EXPOSE 8080

# Health check (adjust based on application)
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Set entrypoint
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

# Usage in Jenkinsfile:
# javaMavenCI {
#   appName = 'your-service'
#   enableImage = true
#   imageName = 'your-service'
#   registryUrl = 'your-registry'
# }