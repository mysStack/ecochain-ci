# EcoChain-CI

## 介绍

EcoChain CI 是一个专为 Java/Maven 项目设计的 Jenkins 共享库，提供标准化的 CI/CD 流水线模板，支持单元测试、代码扫描和构建打包等功能。该共享库旨在简化企业级Java项目的持续集成和持续部署流程，提高开发效率和代码质量。

## 项目结构

```
ecochain-ci/
├── vars/
│   ├── javaMavenCI.groovy        # Java CI 主入口模板
│   ├── javaMavenCIBasic.groovy   # 基础配置方法
│   ├── javaMavenCIStandard.groovy # 标准配置方法
│   └── javaMavenCIFull.groovy    # 完整配置方法
├── src/
│   └── org/ecochain/ci/
│       ├── Scan.groovy           # 代码扫描逻辑
│       ├── Trivy.groovy          # 依赖扫描逻辑
│       └── Utils.groovy          # 公共工具函数
├── resources/
│   └── pod/
│       └── maven.yaml             # Kubernetes Pod 模板
├── examples/
│   ├── Jenkinsfile.basic         # 基础配置示例
│   ├── Jenkinsfile.standard      # 标准配置示例
│   └── Jenkinsfile.full          # 完整配置示例
└── README.md                      # 仓库说明
```

## 功能特性

- **标准化的 CI 流程**：提供开箱即用的 Maven 项目 CI 流水线，遵循业界最佳实践
- **灵活的参数配置**：支持通过参数控制测试、代码扫描和依赖扫描的开关，适应不同项目需求
- **Kubernetes 支持**：使用 Kubernetes Pod 作为构建环境，实现资源隔离和弹性扩展
- **代码质量检查**：集成 SonarQube 代码扫描，确保代码质量和安全性
- **依赖安全扫描**：集成 Trivy 依赖扫描，检测项目依赖中的安全漏洞
- **可扩展性**：易于扩展和定制，支持添加自定义构建步骤
- **多环境支持**：支持开发、测试、预生产等多种环境的部署流程
- **构建结果通知**：集成多种通知渠道，及时反馈构建状态
- **构建产物归档**：自动归档构建产物，便于后续部署和追踪

## 安装配置

### 1. 配置 Jenkins 共享库

在 Jenkins 的系统配置中添加共享库：

1. 进入 Jenkins → 系统管理 → 系统配置
2. 找到 "Global Pipeline Libraries" 部分
3. 添加新的共享库：
   - Name: `ecochain-ci`
   - Default Version: `main`（或你的默认分支）
   - Retrieval method: 选择 Modern SCM 并配置你的 Git 仓库
   - **最佳实践**：建议使用特定标签或提交哈希作为生产环境的版本，而不是直接使用分支名，以确保构建稳定性

### 高级配置

对于企业级部署，可以考虑以下高级配置选项：

- **凭证管理**：配置Git仓库访问凭证，确保安全性
- **缓存设置**：启用依赖缓存，加速构建过程
- **资源限制**：为Kubernetes Pod设置资源限制，防止资源耗尽
- **镜像配置**：自定义构建镜像，预装常用工具和依赖

### 2. 所需插件

在使用本共享库之前，请确保 Jenkins 已安装以下插件：

- **Kubernetes Plugin**：用于在 Kubernetes 集群中动态创建构建代理
- **Pipeline Utility Steps**：提供额外的流水线步骤工具
- **SonarQube Scanner**：用于集成 SonarQube 代码质量检查
- **Workspace Cleanup Plugin**：用于清理构建工作空间（推荐）

您可以通过 Jenkins → 系统管理 → 插件管理 来安装这些插件。

### 3. 准备 Kubernetes 环境

确保 Jenkins 已配置好 Kubernetes 插件，并且有可用的 Kubernetes 集群。

### 4. 配置 SonarQube（可选）

如需使用代码扫描功能，请先配置好 SonarQube 服务器。

## 使用说明

### 分层配置方法

我们提供了三种预配置的构建方法，以满足不同场景的需求：

#### 1. 基础配置 (Basic)

适用于简单的构建场景，只包含基本的编译和测试功能：

```groovy
@Library('ecochain-ci') _

// 只需指定项目键，其他参数使用默认值
javaMavenCIBasic(projectKey: 'com.ecochain:my-project')

// 或者覆盖部分参数
javaMavenCIBasic(
    projectKey: 'com.ecochain:my-project',
    src: 'src/main/java',
    buildCmd: 'mvn clean package -DskipTests'
)
```

#### 2. 标准配置 (Standard)

适用于大多数项目，包含代码扫描和依赖扫描：

```groovy
@Library('ecochain-ci') _

// 使用标准配置
javaMavenCIStandard(projectKey: 'com.ecochain:my-project')

// 或者覆盖部分参数
javaMavenCIStandard(
    projectKey: 'com.ecochain:my-project',
    src: 'src/main/java',
    depScanSeverity: 'CRITICAL',
    notifyOnSuccess: true
)
```

#### 3. 完整配置 (Full)

适用于需要全面配置的生产环境：

```groovy
@Library('ecochain-ci') _

// 使用完整配置
javaMavenCIFull(projectKey: 'com.ecochain:my-project')

// 或者覆盖部分参数
javaMavenCIFull(
    projectKey: 'com.ecochain:my-project',
    env: 'prod',
    notifyEmail: 'devops@ecochain.com',
    host: 'https://sonarqube.example.com',
    login: credentials('sonar-token')
)
```

### 基本用法

如果你的项目需要自定义所有参数，可以使用原始方法：

```groovy
@Library('ecochain-ci') _

javaMavenCI(
    projectKey: 'your-project-key',
    src: 'src/main/java'
)
```

### 参数说明

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| projectKey | String | - | SonarQube 项目标识（必需） |
| src | String | '.' | 代码源目录 |
| enableTest | Boolean | true | 是否启用单元测试 |
| enableScan | Boolean | true | 是否启用代码扫描 |
| enableDepScan | Boolean | true | 是否启用依赖扫描 |
| enableArchive | Boolean | true | 是否归档构建产物 |
| testCmd | String | 'mvn test' | 单元测试命令 |
| buildCmd | String | 'mvn -DskipTests package -Dbuild.version=${env.BUILD_VERSION}' | 构建命令 |
| mvnOpts | String | '' | Maven 额外选项 |
| buildVersion | String | 自动生成 | 构建版本号 |
| env | String | 'dev' | 部署环境（dev/test/prod） |
| notifyEmail | String | '' | 构建结果通知邮箱 |
| notifyOnSuccess | Boolean | false | 成功时是否通知 |
| notifyOnFailure | Boolean | false | 失败时是否通知 |
| timeout | Integer | 120 | 构建超时时间（分钟） |
| parallel | Boolean | false | 是否启用并行构建 |
| dockerImage | String | 'maven:3.9-eclipse-temurin-17' | 构建容器镜像 |
| artifacts | String | '**/target/*.jar,**/target/*.war' | 归档的文件模式 |
| depScanSeverity | String | 'HIGH,CRITICAL' | 依赖扫描的漏洞严重级别 |
| depScanSkipDirs | List | ['target', '.git', '.idea'] | 依赖扫描时跳过的目录 |
| projectName | String | 同projectKey | SonarQube 项目名称 |
| projectVersion | String | '1.0.0' | SonarQube 项目版本 |
| binaries | String | 'target/classes' | 编译后的class文件目录 |
| host | String | - | SonarQube 服务器地址 |
| login | String | - | SonarQube 认证令牌 |
| exclusions | String | - | SonarQube 扫描排除的文件模式 |
| coverage | String | - | SonarQube 覆盖率报告路径 |
| sourceEncoding | String | - | SonarQube 源代码编码 |
| javaVersion | String | - | SonarQube Java版本 |
| extraParams | List | [] | SonarQube 额外参数 |

### 基本示例

```groovy
@Library('ecochain-ci') _

javaMavenCI(
    projectKey: 'com.ecochain:my-project',
    src: 'src/main/java',
    enableTest: true,
    enableScan: true,
    enableDepScan: true,
    enableArchive: true,
    testCmd: 'mvn clean test',
    buildCmd: 'mvn clean package -DskipTests -Dbuild.version=${env.BUILD_VERSION}',
    mvnOpts: '-Xmx1024m',
    artifacts: '**/target/*.jar,**/target/*.war',
    depScanSeverity: 'HIGH,CRITICAL',
    depScanSkipDirs: ['target', '.git', '.idea'],
    notifyOnSuccess: true,
    notifyOnFailure: true
)
```

### 完整参数示例

```groovy
@Library('ecochain-ci') _

javaMavenCI(
    // 必需参数
    projectKey: 'com.ecochain:my-project',

    // 常用参数
    src: 'src/main/java',
    enableTest: true,
    enableScan: true,
    enableDepScan: true,
    enableArchive: true,
    testCmd: 'mvn clean test',
    buildCmd: 'mvn clean package -DskipTests -Dbuild.version=${env.BUILD_VERSION}',
    mvnOpts: '-Xmx1024m',
    artifacts: '**/target/*.jar,**/target/*.war',
    depScanSeverity: 'HIGH,CRITICAL',
    depScanSkipDirs: ['target', '.git', '.idea'],
    notifyOnSuccess: true,
    notifyOnFailure: true,

    // 默认参数（显式指定）
    buildVersion: 'v1.0.0',
    env: 'dev',
    notifyEmail: 'team@ecochain.com',
    timeout: 120,
    parallel: false,
    dockerImage: 'maven:3.9-eclipse-temurin-17',

    // SonarQube 参数
    projectName: 'My Project',
    projectVersion: '1.0.0',
    binaries: 'target/classes',
    host: 'https://sonarqube.example.com',
    login: 'your-sonar-token',
    exclusions: '**/test/**,**/generated/**',
    coverage: 'target/site/jacoco/jacoco.xml',
    sourceEncoding: 'UTF-8',
    javaVersion: '11',
    extraParams: ['-Dsonar.python.xunit.reportPath=target/xunit-reports/xunit-result.xml']
)
```

### 高级示例

#### 1. 多环境部署

```groovy
@Library('ecochain-ci') _

// 根据分支选择部署环境
def environment = env.BRANCH_NAME == 'main' ? 'prod' : 
                 env.BRANCH_NAME == 'develop' ? 'test' : 'dev'

javaMavenCI(
    projectKey: 'com.ecochain:my-project',
    env: environment,
    enableTest: environment != 'prod',  // 生产环境不执行测试
    enableScan: environment == 'test', // 仅测试环境执行代码扫描
    notifyEmail: 'team@ecochain.com',
    timeout: 60
)
```

#### 2. 并行构建

```groovy
@Library('ecochain-ci') _

javaMavenCI(
    projectKey: 'com.ecochain:my-project',
    parallel: true,
    dockerImage: 'maven:3.8-openjdk-11-slim',
    testCmd: 'mvn -T 4 clean test', // 使用4线程并行测试
    buildCmd: 'mvn -T 4 clean package -DskipTests'
)
```

#### 3. 自定义构建步骤

```groovy
@Library('ecochain-ci') _

// 使用共享库的基础功能，并添加自定义步骤
pipeline {
    agent any
    
    stages {
        stage('Pre-build') {
            steps {
                script {
                    // 自定义预构建步骤
                    sh 'echo "Running custom pre-build steps"'
                    sh './scripts/pre-build.sh'
                }
            }
        }
        
        stage('CI Process') {
            steps {
                script {
                    javaMavenCI(
                        projectKey: 'com.ecochain:my-project',
                        enableTest: true,
                        enableScan: true
                    )
                }
            }
        }
        
        stage('Post-build') {
            steps {
                script {
                    // 自定义后构建步骤
                    sh 'echo "Running custom post-build steps"'
                    sh './scripts/post-build.sh'
                }
            }
        }
    }
}
```

## 流水线阶段说明

1. **初始化环境**：设置构建环境变量和工具路径
2. **Checkout**：从版本控制系统检出代码
3. **Unit Test**：执行单元测试（可通过参数控制），生成测试报告
4. **Code Scan**：执行 SonarQube 代码扫描（可通过参数控制），生成质量报告
5. **Dependency Scan**：执行 Trivy 依赖扫描（可通过参数控制），检测安全漏洞
6. **构建打包**：执行 Maven 构建打包，生成可部署的制品
7. **Archive**：归档构建产物（可通过参数控制），便于后续部署和追踪
8. **结果通知**：发送构建结果通知（如果配置了通知渠道）
9. **清理工作区**：清理构建工作空间，释放资源

## 故障排除

### 常见问题

**问题1：构建失败，提示Maven依赖下载超时**
- 解决方案：检查网络连接，或配置Maven镜像源加速依赖下载

**问题2：SonarQube扫描失败**
- 解决方案：检查SonarQube服务器连接和项目配置，确保projectKey正确

**问题3：Kubernetes Pod启动失败**
- 解决方案：检查Kubernetes集群资源和权限配置，确保Jenkins有足够权限创建Pod

**问题4：Trivy依赖扫描失败**
- 解决方案：检查Trivy容器是否正常运行，确保有足够的磁盘空间用于缓存，检查网络连接

### 日志查看

- Jenkins构建日志：通过Jenkins UI查看详细构建日志
- Kubernetes Pod日志：使用`kubectl logs <pod-name>`查看容器日志
- SonarQube日志：通过SonarQube UI查看扫描任务日志

## 示例项目

我们提供了三种配置方法的示例文件，位于 `examples` 目录：

- `Jenkinsfile.basic` - 基础配置示例
- `Jenkinsfile.standard` - 标准配置示例  
- `Jenkinsfile.full` - 完整配置示例

您可以根据自己的需求选择合适的示例，并在此基础上进行修改。

## 参与贡献

我们欢迎所有形式的贡献！请遵循以下步骤参与项目开发：

1. **Fork 本仓库**：点击GitHub页面右上角的Fork按钮
2. **新建功能分支**：使用清晰的分支命名，如`feature/功能名称`或`fix/问题描述`
   ```bash
   git checkout -b feature/amazing-feature
   ```
3. **提交代码**：编写清晰的提交信息，说明所做的更改
   ```bash
   git commit -m "Add some amazing feature"
   ```
4. **推送分支**：将更改推送到您的Fork仓库
   ```bash
   git push origin feature/amazing-feature
   ```
5. **创建Pull Request**：在GitHub上创建Pull Request，详细描述您的更改

### 代码规范

- 遵循Groovy代码风格指南
- 添加适当的注释和文档
- 为新功能编写单元测试
- 确保所有测试通过后再提交PR

## 许可证

本项目采用 MIT 许可证 - 详见 [LICENSE](LICENSE) 文件。

## 联系我们

- 项目主页：https://github.com/your-username/ecochain-ci
- 问题反馈：https://github.com/your-username/ecochain-ci/issues
- 讨论区：https://github.com/your-username/ecochain-ci/discussions
