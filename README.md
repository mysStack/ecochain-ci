# ecochain-ci

## 介绍

EcoChain CI 是一个专为 Java/Maven 项目设计的 Jenkins 共享库，提供标准化的 CI/CD 流水线模板，支持单元测试、代码扫描和构建打包等功能。

## 项目结构

```
ecochain-ci/
├── vars/
│   └── javaMavenCI.groovy        # Java CI 主入口模板
├── src/
│   └── org/ecochain/ci/
│       ├── Scan.groovy           # 代码扫描逻辑
│       └── Utils.groovy          # 公共工具函数
├── resources/
│   └── pod/
│       └── maven.yaml             # Kubernetes Pod 模板
└── README.md                      # 仓库说明
```

## 功能特性

- **标准化的 CI 流程**：提供开箱即用的 Maven 项目 CI 流水线
- **灵活的参数配置**：支持通过参数控制测试和代码扫描的开关
- **Kubernetes 支持**：使用 Kubernetes Pod 作为构建环境
- **代码质量检查**：集成 SonarQube 代码扫描
- **可扩展性**：易于扩展和定制

## 安装配置

### 1. 配置 Jenkins 共享库

在 Jenkins 的系统配置中添加共享库：

1. 进入 Jenkins → 系统管理 → 系统配置
2. 找到 "Global Pipeline Libraries" 部分
3. 添加新的共享库：
   - Name: `ecochain-ci`
   - Default Version: `main`（或你的默认分支）
   - Retrieval method: 选择 Modern SCM 并配置你的 Git 仓库

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

### 基本用法

在你的 Jenkinsfile 中使用共享库：

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
| testCmd | String | 'mvn test' | 单元测试命令 |
| buildCmd | String | 'mvn -DskipTests package' | 构建命令 |

### 完整示例

```groovy
@Library('ecochain-ci') _

javaMavenCI(
    projectKey: 'com.ecochain:my-project',
    src: 'src/main/java',
    enableTest: true,
    enableScan: true,
    testCmd: 'mvn clean test',
    buildCmd: 'mvn clean package -DskipTests'
)
```

## 流水线阶段说明

1. **Checkout**：检出代码
2. **Unit Test**：执行单元测试（可通过参数控制）
3. **Code Scan**：执行 SonarQube 代码扫描（可通过参数控制）
4. **Build**：执行 Maven 构建打包

## 参与贡献

欢迎贡献代码！

1. Fork 本仓库
2. 新建 Feat_xxx 分支
3. 提交代码
4. 新建 Pull Request

## 许可证

MIT License
