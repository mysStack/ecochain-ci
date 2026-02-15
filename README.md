# ecochain-ci

Jenkins Shared Library for Java Maven CI/CD with Kaniko.

## 快速开始

### 基本配置

```groovy
@Library('ecochain-ci') _
javaMavenCI {
  appName = 'your-service'
  enableImage = true
  imageName = 'your-service'
  registryUrl = 'your-registry.com'
}
```

### 完整配置选项

| 参数 | 必填 | 默认值 | 说明 |
|------|------|--------|------|
| `appName` | ✅ | '' | 应用名称 |
| `mavenCmd` | ❌ | 'mvn -B clean package' | Maven 构建命令 |
| `enableScan` | ❌ | false | 启用代码扫描 |
| `enableImage` | ❌ | false | 启用镜像构建 |
| `imageName` | ✅(如启用镜像) | '' | 镜像名称 |
| `imageTag` | ❌ | 'latest' | 镜像标签 |
| `registryUrl` | ✅(如启用镜像) | '' | 镜像仓库地址 |
| `kanikoArgs` | ❌ | '--cache=true --cache-ttl=24h' | Kaniko 参数 |
| `mavenImage` | ❌ | 默认镜像 | Maven 构建镜像 |
| `kanikoImage` | ❌ | 默认镜像 | Kaniko 构建镜像 |

## 项目要求

### Dockerfile 配置

项目根目录必须包含 `Dockerfile`，建议使用运行时镜像：

```dockerfile
FROM your-base-image:tag
WORKDIR /app
COPY target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

> **注意**: Maven 构建在 Pod 中执行，Dockerfile 只需包含运行阶段。

### 基础设施要求

- Kubernetes 集群
- 镜像仓库认证 Secret (`docker-registry-secret`)
- 网络访问权限（镜像仓库、Maven 私服等）

## 构建流程

1. **初始化** - 验证配置和模板
2. **Maven 构建** - 在专用容器中执行构建
3. **镜像构建** - 使用 Kaniko 构建 Docker 镜像
4. **后处理** - 成功/失败通知

## 架构特点

- ✅ **安全构建** - 使用 Kaniko，无需 Docker daemon
- ✅ **高效缓存** - Maven 依赖缓存优化
- ✅ **模板验证** - 自动验证 Pod 配置
- ✅ **资源限制** - 容器资源管理和限制

## 故障排除

### 常见问题

1. **Dockerfile 未找到**
   - 确保项目根目录包含 Dockerfile

2. **镜像推送失败**
   - 检查镜像仓库认证 Secret 配置
   - 验证网络连接和权限

3. **构建超时**
   - 调整资源限制或构建命令

## 技术支持

如有问题，请联系 DevOps 团队。