package org.ecochain.ci

class Utils {
    /**
     * 获取当前时间戳
     * @return 格式化的时间戳字符串 (yyyyMMddHHmmss)
     */
    static String getTimestamp() {
        return new Date().format('yyyyMMddHHmmss')
    }

    /**
     * 获取构建版本号
     * @return 带版本前缀的构建号 (v{timestamp})
     */
    static String getBuildVersion() {
        return "v${getTimestamp()}"
    }

    /**
     * 打印分隔线
     * @param title 分隔线标题
     */
    static void printSeparator(String title = '') {
        def line = "========================================"
        if (title) {
            println "${line} ${title} ${line}"
        } else {
            println line
        }
    }

    /**
     * 获取 Git 分支名
     * @return 当前分支名
     */
    static String getGitBranch() {
        try {
            return sh(script: 'git rev-parse --abbrev-ref HEAD', returnStdout: true).trim()
        } catch (Exception e) {
            echo "获取 Git 分支失败: ${e.message}"
            return 'unknown'
        }
    }

    /**
     * 获取 Git 提交哈希值
     * @return 短哈希值 (7位)
     */
    static String getGitCommitHash() {
        try {
            return sh(script: 'git rev-parse --short=7 HEAD', returnStdout: true).trim()
        } catch (Exception e) {
            echo "获取 Git 提交哈希失败: ${e.message}"
            return 'unknown'
        }
    }

    /**
     * 获取 Git 提交信息
     * @return 提交信息
     */
    static String getGitCommitMessage() {
        try {
            return sh(script: 'git log -1 --pretty=%B', returnStdout: true).trim()
        } catch (Exception e) {
            echo "获取 Git 提交信息失败: ${e.message}"
            return 'unknown'
        }
    }

    /**
     * 获取 Git 提交者信息
     * @return 提交者姓名和邮箱
     */
    static String getGitCommitter() {
        try {
            def name = sh(script: 'git log -1 --pretty=%cn', returnStdout: true).trim()
            def email = sh(script: 'git log -1 --pretty=%ce', returnStdout: true).trim()
            return "${name} <${email}>"
        } catch (Exception e) {
            echo "获取 Git 提交者失败: ${e.message}"
            return 'unknown'
        }
    }

    /**
     * 获取构建信息
     * @return 包含分支、提交、时间等信息的 Map
     */
    static Map getBuildInfo() {
        return [
            branch: getGitBranch(),
            commit: getGitCommitHash(),
            message: getGitCommitMessage(),
            committer: getGitCommitter(),
            timestamp: getTimestamp(),
            version: getBuildVersion()
        ]
    }

    /**
     * 执行 Maven 命令
     * @param cmd Maven 命令
     */
    static void maven(String cmd) {
        try {
            echo "执行 Maven 命令: mvn ${cmd}"
            sh "mvn ${cmd}"
        } catch (Exception e) {
            echo "Maven 命令执行失败: ${e.message}"
            throw e
        }
    }

    /**
     * 获取项目版本号
     * @return 从 pom.xml 读取的版本号
     */
    static String getProjectVersion() {
        try {
            return sh(
                script: 'mvn help:evaluate -Dexpression=project.version -q -DforceStdout',
                returnStdout: true
            ).trim()
        } catch (Exception e) {
            echo "获取项目版本失败: ${e.message}"
            return 'unknown'
        }
    }

    /**
     * 获取项目 Artifact ID
     * @return 从 pom.xml 读取的 artifactId
     */
    static String getProjectArtifactId() {
        try {
            return sh(
                script: 'mvn help:evaluate -Dexpression=project.artifactId -q -DforceStdout',
                returnStdout: true
            ).trim()
        } catch (Exception e) {
            echo "获取项目 Artifact ID 失败: ${e.message}"
            return 'unknown'
        }
    }

    /**
     * 获取项目 Group ID
     * @return 从 pom.xml 读取的 groupId
     */
    static String getProjectGroupId() {
        try {
            return sh(
                script: 'mvn help:evaluate -Dexpression=project.groupId -q -DforceStdout',
                returnStdout: true
            ).trim()
        } catch (Exception e) {
            echo "获取项目 Group ID 失败: ${e.message}"
            return 'unknown'
        }
    }

    /**
     * 获取完整的项目坐标
     * @return groupId:artifactId:version
     */
    static String getProjectCoordinates() {
        return "${getProjectGroupId()}:${getProjectArtifactId()}:${getProjectVersion()}"
    }

    /**
     * 检查文件是否存在
     * @param filePath 文件路径
     * @return 文件是否存在
     */
    static boolean fileExists(String filePath) {
        try {
            return fileExists(filePath)
        } catch (Exception e) {
            echo "检查文件存在性失败: ${e.message}"
            return false
        }
    }

    /**
     * 读取文件内容
     * @param filePath 文件路径
     * @return 文件内容
     */
    static String readFile(String filePath) {
        try {
            return readFile(filePath).trim()
        } catch (Exception e) {
            echo "读取文件失败: ${e.message}"
            return ''
        }
    }

    /**
     * 写入文件内容
     * @param filePath 文件路径
     * @param content 文件内容
     */
    static void writeFile(String filePath, String content) {
        try {
            writeFile file: filePath, text: content
        } catch (Exception e) {
            echo "写入文件失败: ${e.message}"
            throw e
        }
    }
}
