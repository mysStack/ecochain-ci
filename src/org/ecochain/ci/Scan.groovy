package org.ecochain.ci

class Scan {
    /**
     * 执行 SonarQube 代码扫描
     * @param cfg 配置参数
     *   - projectKey: 项目标识（必需）
     *   - src: 源代码目录，默认为 '.'
     *   - binaries: 编译后的class文件目录，默认为 'target/classes'
     *   - host: SonarQube 服务器地址
     *   - login: SonarQube 认证令牌
     *   - exclusions: 排除的文件模式
     *   - coverage: 覆盖率报告路径
     */
    static void sonar(Map cfg) {
        try {
            // 构建基础参数
            def sonarParams = [
                "-Dsonar.projectKey=${cfg.projectKey}",
                "-Dsonar.projectName=${cfg.projectName ?: cfg.projectKey}",
                "-Dsonar.projectVersion=${cfg.projectVersion ?: '1.0.0'}",
                "-Dsonar.sources=${cfg.src ?: '.'}",
                "-Dsonar.java.binaries=${cfg.binaries ?: 'target/classes'}"
            ]

            // 添加可选参数
            if (cfg.host) {
                sonarParams << "-Dsonar.host.url=${cfg.host}"
            }
            if (cfg.login) {
                sonarParams << "-Dsonar.login=${cfg.login}"
            }
            if (cfg.exclusions) {
                sonarParams << "-Dsonar.exclusions=${cfg.exclusions}"
            }
            if (cfg.coverage) {
                sonarParams << "-Dsonar.coverage.jacoco.xmlReportPaths=${cfg.coverage}"
            }
            if (cfg.sourceEncoding) {
                sonarParams << "-Dsonar.sourceEncoding=${cfg.sourceEncoding}"
            }
            if (cfg.javaVersion) {
                sonarParams << "-Dsonar.java.source=${cfg.javaVersion}"
            }

            // 添加自定义参数
            if (cfg.extraParams) {
                sonarParams.addAll(cfg.extraParams)
            }

            // 执行扫描
            def scanCmd = "sonar-scanner ${sonarParams.join(' ')}"
            echo "执行 SonarQube 扫描..."
            echo "命令: ${scanCmd}"

            sh scanCmd

            echo "SonarQube 扫描完成"
        } catch (Exception e) {
            echo "SonarQube 扫描失败: ${e.message}"
            throw e
        }
    }

    /**
     * 检查质量门禁状态
     * @return 质量门禁结果对象
     */
    static Map checkQualityGate() {
        try {
            def qg = waitForQualityGate()
            return [
                status: qg.status,
                message: "质量门禁状态: ${qg.status}"
            ]
        } catch (Exception e) {
            echo "检查质量门禁失败: ${e.message}"
            throw e
        }
    }
}
