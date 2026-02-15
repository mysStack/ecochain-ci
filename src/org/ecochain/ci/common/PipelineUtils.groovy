// Unified Pipeline Utilities for ecochain-ci
package org.ecochain.ci.common

class PipelineUtils implements Serializable {
    
    def script
    
    PipelineUtils(script) {
        this.script = script
    }
    
    // ===== 日志功能 =====
    
    def info(String msg) {
        script.echo "[INFO] ${msg}"
    }
    
    def warn(String msg) {
        script.echo "[WARN] ${msg}"
    }
    
    def error(String msg) {
        script.echo "[ERROR] ${msg}"
    }
    
    // ===== 模板验证功能 =====
    
    /**
     * 验证 Pod 模板的基本结构
     */
    def validatePodTemplate(String templateContent, Map requiredPlaceholders = [:]) {
        def errors = []
        
        // 1. 检查必需的 Kubernetes 字段
        if (!templateContent.contains('apiVersion: v1')) {
            errors.add("Missing required field: apiVersion: v1")
        }
        
        if (!templateContent.contains('kind: Pod')) {
            errors.add("Missing required field: kind: Pod")
        }
        
        if (!templateContent.contains('spec:')) {
            errors.add("Missing required field: spec:")
        }
        
        // 2. 检查容器配置
        if (!templateContent.contains('containers:')) {
            errors.add("Missing containers configuration")
        }
        
        // 3. 检查必需的占位符
        requiredPlaceholders.each { placeholder, description ->
            if (!templateContent.contains(placeholder)) {
                errors.add("Missing required placeholder: ${placeholder} (${description})")
            }
        }
        
        // 4. 检查安全配置
        if (templateContent.contains('privileged: true')) {
            errors.add("Security warning: Pod template contains privileged mode")
        }
        
        if (templateContent.contains('hostPath:')) {
            errors.add("Security warning: Pod template uses hostPath volumes")
        }
        
        // 5. 检查资源限制
        if (!templateContent.contains('resources:')) {
            errors.add("Warning: Pod template missing resource limits")
        }
        
        return errors
    }
    
    /**
     * 验证镜像地址格式
     */
    def validateImageUrl(String imageUrl) {
        def errors = []
        
        // 基本格式检查
        if (!imageUrl) {
            errors.add("Image URL cannot be empty")
            return errors
        }
        
        // 检查是否包含标签
        if (!imageUrl.contains(':')) {
            errors.add("Image URL should contain tag (e.g., image:tag)")
        }
        
        // 检查是否使用 latest 标签（不推荐）
        if (imageUrl.endsWith(':latest')) {
            errors.add("Warning: Using 'latest' tag is not recommended for production")
        }
        
        return errors
    }
    
    /**
     * 验证替换后的 YAML 语法
     */
    def validateYamlSyntax(String yamlContent) {
        def errors = []
        
        try {
            // 检查基本的 YAML 结构
            if (yamlContent.contains('  ') && !yamlContent.contains('\n')) {
                errors.add("YAML content appears to have indentation issues")
            }
            
            // 检查未替换的占位符
            if (yamlContent.contains('PLACEHOLDER')) {
                errors.add("Found unreplaced placeholders in YAML content")
            }
            
            // 检查空值
            if (yamlContent.contains(': ""') || yamlContent.contains(': null')) {
                errors.add("YAML contains empty or null values")
            }
            
        } catch (Exception e) {
            errors.add("YAML syntax validation failed: ${e.message}")
        }
        
        return errors
    }
    
    /**
     * 完整的模板验证流程
     */
    def validateCompleteTemplate(String templateContent, Map replacements) {
        def allErrors = []
        
        // 1. 验证模板结构
        def templateErrors = validatePodTemplate(templateContent, [
            'MAVEN_IMAGE_PLACEHOLDER': 'Maven container image',
            'KANIKO_IMAGE_PLACEHOLDER': 'Kaniko container image'
        ])
        allErrors.addAll(templateErrors)
        
        // 2. 验证镜像地址
        replacements.each { key, value ->
            if (key.contains('IMAGE')) {
                def imageErrors = validateImageUrl(value)
                imageErrors.each { error ->
                    allErrors.add("${key}: ${error}")
                }
            }
        }
        
        // 3. 执行替换并验证结果
        def finalYaml = templateContent
        replacements.each { placeholder, value ->
            finalYaml = finalYaml.replace(placeholder, value)
        }
        
        def syntaxErrors = validateYamlSyntax(finalYaml)
        allErrors.addAll(syntaxErrors)
        
        // 记录验证结果
        if (allErrors.isEmpty()) {
            info("Template validation passed")
        } else {
            warn("Template validation found ${allErrors.size()} issues:")
            allErrors.each { error ->
                warn("  - ${error}")
            }
        }
        
        return allErrors
    }
}