package org.ecochain.ci

class Trivy {
    /**
     * 执行 Trivy 依赖扫描
     * @param cfg 配置参数
     *   - scanType: 扫描类型，支持 'fs' (文件系统) 或 'image' (镜像)，默认为 'fs'
     *   - target: 扫描目标，文件系统路径或镜像名称
     *   - severity: 漏洞严重级别，默认为 'HIGH,CRITICAL'
     *   - format: 输出格式，支持 'table', 'json', 'sarif' 等，默认为 'table'
     *   - output: 输出文件路径
     *   - exitCode: 发现漏洞时的退出码，默认为 1
     *   - skipDirs: 跳过的目录列表
     *   - cacheDir: 缓存目录
     *   - timeout: 超时时间（分钟），默认为 30
     */
    static void scan(Map cfg) {
        try {
            // 设置默认值
            def scanType = cfg.scanType ?: 'fs'
            def target = cfg.target ?: '.'
            def severity = cfg.severity ?: 'HIGH,CRITICAL'
            def format = cfg.format ?: 'table'
            def output = cfg.output
            def exitCode = cfg.exitCode ?: 1
            def skipDirs = cfg.skipDirs
            def cacheDir = cfg.cacheDir
            def timeout = cfg.timeout ?: 30

            // 构建基础命令
            def trivyCmd = "trivy ${scanType} ${target}"

            // 添加严重级别
            if (severity) {
                trivyCmd += " --severity ${severity}"
            }

            // 添加输出格式
            if (format) {
                trivyCmd += " --format ${format}"
            }

            // 添加输出文件
            if (output) {
                trivyCmd += " --output ${output}"
            }

            // 添加退出码配置
            if (exitCode != null) {
                trivyCmd += " --exit-code ${exitCode}"
            }

            // 添加跳过目录
            if (skipDirs) {
                skipDirs.each { dir ->
                    trivyCmd += " --skip-dirs ${dir}"
                }
            }

            // 添加缓存目录
            if (cacheDir) {
                trivyCmd += " --cache-dir ${cacheDir}"
            }

            // 执行扫描
            echo "执行 Trivy 扫描..."
            echo "扫描类型: ${scanType}"
            echo "扫描目标: ${target}"
            echo "严重级别: ${severity}"
            echo "命令: ${trivyCmd}"

            timeout(time: timeout, unit: 'MINUTES') {
                sh trivyCmd
            }

            echo "Trivy 扫描完成"
        } catch (Exception e) {
            echo "Trivy 扫描失败: ${e.message}"
            throw e
        }
    }

    /**
     * 扫描文件系统（Maven 项目）
     * @param cfg 配置参数
     */
    static void scanFileSystem(Map cfg) {
        cfg.scanType = 'fs'
        cfg.target = cfg.target ?: '.'
        scan(cfg)
    }

    /**
     * 扫描 Docker 镜像
     * @param cfg 配置参数
     *   - image: 镜像名称（必需）
     */
    static void scanImage(Map cfg) {
        if (!cfg.image) {
            error 'image is required parameter for image scanning'
        }
        cfg.scanType = 'image'
        cfg.target = cfg.image
        scan(cfg)
    }

    /**
     * 扫描 Maven 依赖
     * @param cfg 配置参数
     */
    static void scanMavenDependencies(Map cfg) {
        try {
            echo "扫描 Maven 依赖..."

            // 检查 pom.xml 是否存在
            if (!fileExists('pom.xml')) {
                error 'pom.xml not found, this is not a Maven project'
            }

            // 使用 Trivy 扫描文件系统
            cfg.scanType = 'fs'
            cfg.target = '.'
            cfg.skipDirs = cfg.skipDirs ?: ['target', '.git', '.idea']

            // 生成 SARIF 报告
            def sarifReport = 'trivy-report.sarif'
            cfg.format = 'sarif'
            cfg.output = sarifReport

            // 执行扫描
            scan(cfg)

            // 归档报告
            if (fileExists(sarifReport)) {
                archiveArtifacts artifacts: sarifReport, allowEmptyArchive: true
            }

            echo "Maven 依赖扫描完成"
        } catch (Exception e) {
            echo "Maven 依赖扫描失败: ${e.message}"
            throw e
        }
    }

    /**
     * 检查漏洞数量
     * @param reportFile 报告文件路径
     * @param severity 漏洞严重级别
     * @return 漏洞数量
     */
    static int countVulnerabilities(String reportFile, String severity = 'HIGH,CRITICAL') {
        try {
            if (!fileExists(reportFile)) {
                error "报告文件不存在: ${reportFile}"
            }

            // 解析 JSON 报告
            def report = readJSON file: reportFile
            def count = 0

            // 统计漏洞数量
            if (report.Results) {
                report.Results.each { result ->
                    if (result.Vulnerabilities) {
                        result.Vulnerabilities.each { vuln ->
                            if (severity.contains(vuln.Severity)) {
                                count++
                            }
                        }
                    }
                }
            }

            echo "发现 ${count} 个 ${severity} 级别的漏洞"
            return count
        } catch (Exception e) {
            echo "统计漏洞数量失败: ${e.message}"
            return -1
        }
    }
}
